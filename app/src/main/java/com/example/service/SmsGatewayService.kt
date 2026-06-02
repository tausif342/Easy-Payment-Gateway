package com.example.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SmsGatewayService : Service() {

    companion object {
        private const val TAG = "SmsGatewayService"
        @Volatile var isServiceRunning = false
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isReceiverRegistered = false
    private val smsReceiver = SmsBroadcastReceiver()

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d(TAG, "SmsGatewayService onCreate()")
        
        // Ensure notification channel exists
        NotificationHelper.createNotificationChannels(applicationContext)

        // Show permanent notification for the foreground service compliance
        val notification = NotificationHelper.getServiceNotification(
            this,
            "Gateway active. Listening for mobile banking transaction signals..."
        )
        
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)

        // On newer Android versions, programmatically register receiver as well to guarantee delivery
        registerSmsReceiverProgrammatically()

        // Kick off periodic synchronization jobs (WorkManager)
        SyncWorkManager.schedulePeriodicSync(applicationContext)
    }

    private fun registerSmsReceiverProgrammatically() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                    priority = 999 // High priority to process payment notifications promptly
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(smsReceiver, filter, RECEIVER_EXPORTED)
                } else {
                    registerReceiver(smsReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Programmatic SmsBroadcastReceiver registered successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error programmatically registering SmsBroadcastReceiver: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SmsGatewayService onStartCommand()")
        // Keep service alive sticky
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        Log.d(TAG, "SmsGatewayService onDestroy()")
        
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(smsReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering SmsBroadcastReceiver: ${e.message}")
            }
        }
        
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
