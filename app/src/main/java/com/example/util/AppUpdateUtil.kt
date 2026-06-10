package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateUtil {

    /**
     * Check if install from unknown sources permission is granted.
     * If not, redirect user to the settings screen to turn it on.
     */
    fun checkInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(
                        context,
                        "Please authorize 'Allow from this source' to install the app update directly.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.e("AppUpdateUtil", "Failed to open unknown sources screen: ${e.message}")
                }
                return false
            }
        }
        return true
    }

    /**
     * Downloads APK in a background thread and triggers package installer on success.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        updateUrl: String,
        version: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure permission is checked/requested
                withContext(Dispatchers.Main) {
                    checkInstallPermission(context)
                }

                val url = URL(updateUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        onError("Download server returned Http Code ${connection.responseCode}")
                    }
                    return@withContext
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val outputFile = File(context.cacheDir, "Gateway_Update_$version.apk")
                
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                val output = FileOutputStream(outputFile)
                val data = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int

                while (input.read(data).also { bytesRead = it } != -1) {
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val progress = totalBytesRead.toFloat() / fileLength.toFloat()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                    output.write(data, 0, bytesRead)
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    onProgress(1.0f)
                    triggerApkInstall(context, outputFile)
                }
            } catch (e: Exception) {
                Log.e("AppUpdateUtil", "Direct download failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown network exception during update")
                }
            }
        }
    }

    /**
     * Launch package installer with FileProvider uri
     */
    fun triggerApkInstall(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Log.e("AppUpdateUtil", "Cannot launch installer: File is missing")
                return
            }

            val authority = "com.example.fileprovider"
            val mimeType = "application/vnd.android.package-archive"
            val apkUri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateUtil", "Failure during startActivity for package-archive: ${e.message}", e)
            Toast.makeText(context, "Cannot open package installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
