package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "sms_gateway_service_channel"
    const val TRANSACTION_CHANNEL_ID = "sms_gateway_transaction_channel"
    
    private const val SERVICE_CHANNEL_NAME = "SMS Gateway Background Monitor"
    private const val TRANSACTION_CHANNEL_NAME = "Transaction sync alerts"
    
    const val FOREGROUND_NOTIFICATION_ID = 1001
    const val SYNC_SUCCESS_NOTIFICATION_ID = 2001
    const val SYNC_FAILED_NOTIFICATION_ID = 2002
    const val SECURITY_NOTIFICATION_ID = 2003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Foreground Service Channel (Low importance to stay silent in status bar)
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the SMS listener running active in the background"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            // 2. Transaction Alerts (High importance)
            val transactionChannel = NotificationChannel(
                TRANSACTION_CHANNEL_ID,
                TRANSACTION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when transactions are successfully synced or failed"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(transactionChannel)
        }
    }

    fun getServiceNotification(context: Context, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("SMS Gateway Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showNotification(context: Context, channelId: String, notificationId: Int, title: String, text: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            // Note: We check permission, if we don't have it, showNotification will safely be ignored
            // or the OS handles it on post-13.
            manager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission checked via standard OS triggers in view/activity
        }
    }
}
