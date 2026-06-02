package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.pref.DatastoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsGatewayBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsGatewayBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Device booted successfully. Reviewing active gateway start states...")

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val datastoreManager = DatastoreManager(context)
                val isGatewayActive = datastoreManager.isGatewayActive()
                val isApproved = datastoreManager.isApproved()

                if (isGatewayActive && isApproved) {
                    Log.d(TAG, "Gateway was previously active and approved. Launching service...")
                    val serviceIntent = Intent(context, SmsGatewayService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    Log.d(TAG, "Gateway was previously deactivated or device not approved. Skipping startup.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed during automatic gateway boot trigger initialization: ${e.message}")
            }
        }
    }
}
