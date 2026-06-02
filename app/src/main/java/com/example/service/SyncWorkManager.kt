package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.api.RetrofitClient
import com.example.data.api.SmsSyncRequest
import com.example.data.local.AppDatabase
import com.example.data.model.SyncLog
import com.example.data.pref.DatastoreManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorkManager(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorkManager"
        
        fun schedulePeriodicSync(context: Context, intervalMinutes: Int = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // WorkManager periodic work requires at least 15 minutes.
            val finalMinutes = if (intervalMinutes < 15) 15 else intervalMinutes

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorkManager>(finalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SmsGatewayPeriodicSync",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun triggerOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorkManager>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SmsGatewayOneTimeSync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val transactionDao = database.smsTransactionDao()
        val syncLogDao = database.syncLogDao()
        val datastoreManager = DatastoreManager(applicationContext)
        val context = applicationContext

        // 1. AUTO SELF-HEALING: Restart foreground service if we expect it to be active but has stopped
        val isGatewayActive = datastoreManager.isGatewayActive()
        val isApproved = datastoreManager.isApproved()
        if (isGatewayActive && isApproved) {
            val isRunning = SmsGatewayService.isServiceRunning
            if (!isRunning) {
                Log.d(TAG, "[SELF-HEALING] SMS gateway service stopped in background. Automatically resurrecting...")
                try {
                    val serviceIntent = android.content.Intent(context, SmsGatewayService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            logType = "SYSTEM",
                            status = "SUCCESS",
                            message = "[SELF-HEALING] SMS service stops. Automatically recovered active gateway service."
                        )
                    )
                } catch (e: Exception) {
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            logType = "ERROR",
                            status = "FAILED",
                            message = "[SELF-HEALING] Failed to recover active foreground gateway service: ${e.message}"
                        )
                    )
                }
            }
        }

        // 2. AUTO SELF-HEALING: If Sync has been failing repeatedly, reset configurations
        if (datastoreManager.hasRepeatedSyncFailure()) {
            Log.d(TAG, "[SELF-HEALING] Sync fails repeatedly. Resetting WorkManager schedules and reporting diagnostics.")
            try {
                syncLogDao.insertSyncLog(
                    SyncLog(
                        logType = "SYSTEM",
                        status = "INFO",
                        message = "[SELF-HEALING] Repeated sync failures detected. Automatically enqueuing unique sync tasks."
                    )
                )
                val freq = datastoreManager.getSyncFrequency()
                schedulePeriodicSync(context, freq)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // SERVER CONTROLLED DEVICE ACCESS: Verify authorization periodically during background sync
        val apiUrl = datastoreManager.getApiUrl()
        val merchantId = datastoreManager.getMerchantId()
        val deviceId = datastoreManager.getDeviceId()
        val apiKey = datastoreManager.getApiKey()
        val secretToken = datastoreManager.getSecretToken()

        if (datastoreManager.isLoggedInFlow.first() &&
            !apiUrl.uppercase().contains("DEMO") &&
            !apiUrl.lowercase().contains("EASYPAYCENTER.COM") &&
            !apiUrl.lowercase().contains("easypaycenter.com")) {
            try {
                val lastCapturedSms = datastoreManager.getLastSmsTime()
                val lastFinishedSync = datastoreManager.getLastSyncTime()
                
                val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val batteryIgnoring = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pm.isIgnoringBatteryOptimizations(applicationContext.packageName)
                } else {
                    true
                }

                val batteryLevelValue = com.example.util.DeviceDiagnosticUtil.getBatteryLevel(context)
                val internetStatusValue = com.example.util.DeviceDiagnosticUtil.getInternetStatus(context)
                val appVersionValue = com.example.util.DeviceDiagnosticUtil.getAppVersion(context)
                val simStatusValue = com.example.util.DeviceDiagnosticUtil.getSimStatus(context)

                val checkRequest = com.example.data.api.DeviceCheckRequest(
                    apiKey = apiKey,
                    secretToken = secretToken,
                    deviceId = deviceId,
                    deviceFingerprint = com.example.util.DeviceSecurityUtil.generateFingerprint(),
                    lastSmsReceived = lastCapturedSms,
                    lastSyncTime = lastFinishedSync,
                    internetConnected = internetStatusValue != "DISCONNECTED",
                    batteryOptimized = batteryIgnoring,
                    batteryLevel = batteryLevelValue,
                    internetStatus = internetStatusValue,
                    appVersion = appVersionValue,
                    simStatus = simStatusValue
                )
                val apiService = RetrofitClient.getApiService(apiUrl)
                val response = apiService.checkDeviceStatus(checkRequest)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    datastoreManager.saveLastStatusCheck(body.lastCheckedStatus)
                    
                    // Remotely override sync frequency
                    body.syncFrequencyMinutes?.let { mins ->
                        if (mins in 1..1440 && mins != datastoreManager.getSyncFrequency()) {
                            datastoreManager.saveSyncFrequency(mins)
                        }
                    }

                    // Dynamic remote configuration filters override
                    val senders = body.allowedSenders ?: ""
                    val inflow = body.inflowKeywords ?: ""
                    val marketing = body.marketingKeywords ?: ""
                    val latestVersion = body.latestAppVersion ?: "1.0.0"
                    val updateUrl = body.appUpdateUrl ?: ""

                    datastoreManager.saveRemoteConfig(senders, inflow, marketing, latestVersion, updateUrl)

                    // Dynamic payment account mappings override
                    body.accountMappings?.let { remoteMaps ->
                        if (remoteMaps.isNotEmpty()) {
                            val paymentAccountDao = database.paymentAccountDao()
                            paymentAccountDao.clearAllPaymentAccounts()
                            remoteMaps.forEach { rm ->
                                paymentAccountDao.insertPaymentAccount(
                                    com.example.data.model.PaymentAccount(
                                        id = rm.id,
                                        name = rm.name,
                                        provider = rm.provider,
                                        walletNumber = rm.walletNumber,
                                        simSlot = rm.simSlot,
                                        projectId = rm.projectId
                                    )
                                )
                            }
                            syncLogDao.insertSyncLog(
                                SyncLog(
                                    logType = "SYSTEM",
                                    status = "SUCCESS",
                                    message = "Dynamically synchronized ${remoteMaps.size} account maps from remote SaaS server."
                                )
                            )
                        }
                    }

                    // Auto update check
                    if (latestVersion != appVersionValue && updateUrl.isNotEmpty()) {
                        NotificationHelper.showNotification(
                            applicationContext,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "App Update Available (${latestVersion})",
                            "New enterprise gateway release published. Click to view."
                        )
                    }

                    // Remotely enforce secure logout / session termination
                    if (body.forceLogout == true || body.lastCheckedStatus == "REVOKED" || body.lastCheckedStatus == "FORCE_LOGOUT") {
                        datastoreManager.clearSession()
                        NotificationHelper.showNotification(
                            applicationContext,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "Security Action",
                            "Session terminated remotely. All listening streams closed."
                        )
                        return ListenableWorker.Result.success()
                    }

                    if (body.lastCheckedStatus != "APPROVED") {
                        Log.d(TAG, "Sync cancelled: Device status has changed to ${body.lastCheckedStatus}")
                        datastoreManager.saveGatewayActive(false)
                        datastoreManager.setApproved(false)
                        NotificationHelper.showNotification(
                            applicationContext,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "Gateway Alert",
                            "Device status: ${body.lastCheckedStatus}. Background listening paused."
                        )
                        return ListenableWorker.Result.success()
                    } else {
                        datastoreManager.setApproved(true)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not check status with server: ${e.message}")
            }
        }

        if (!datastoreManager.isApproved() || !datastoreManager.isGatewayActive()) {
            Log.d(TAG, "Sync cancelled: Merchant and Gateway must be active & authorized.")
            return ListenableWorker.Result.success()
        }

        val pendingTxns = transactionDao.getTransactionsBySyncStatus("PENDING") +
                transactionDao.getTransactionsBySyncStatus("FAILED")

        if (pendingTxns.isEmpty()) {
            return ListenableWorker.Result.success()
        }

        Log.d(TAG, "Found ${pendingTxns.size} pending/failed transactions to process.")

        var hasFailure = false

        for (txn in pendingTxns) {
            try {
                // If the user uses a fake url or "DEMO", we do rapid simulated syncs to demonstrate SaaS payment verification flow 
                if (apiUrl.uppercase().contains("DEMO") || apiUrl.lowercase().contains("easypaycenter.com")) {
                    // Simulate processing delay
                    kotlinx.coroutines.delay(1000)
                    
                    // Update Transaction State in database
                    transactionDao.updateSyncStatus(txn.id, "SUCCESS")

                    // Log locally
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            txnId = txn.txnId,
                            status = "SUCCESS",
                            message = "[SIMULATED SUCCESS] [Project: ${txn.projectId} | Account: ${txn.paymentAccountId}] Txn parsed & verified on mock server."
                        )
                    )

                    // Notify
                    NotificationHelper.showNotification(
                        applicationContext,
                        NotificationHelper.TRANSACTION_CHANNEL_ID,
                        NotificationHelper.SYNC_SUCCESS_NOTIFICATION_ID,
                        "Simulated Transaction Synced",
                        "${txn.sender} TXN ${txn.txnId} of Tk.${txn.amount} verified."
                    )
                    continue
                }

                // SECURE PHYSICAL NETWORKING WITH SERVER GIVEN BY MERCHANT
                val request = SmsSyncRequest(
                    merchantId = merchantId,
                    deviceId = deviceId,
                    projectId = txn.projectId,
                    paymentAccountId = txn.paymentAccountId,
                    txnId = txn.txnId,
                    amount = txn.amount,
                    sender = txn.sender,
                    senderNumber = txn.senderNumber,
                    time = txn.time,
                    rawSms = txn.rawSms,
                    reference = txn.reference,
                    simSlot = txn.simSlot
                )

                val apiService = RetrofitClient.getApiService(apiUrl)
                val response = apiService.syncTransaction(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status.lowercase() == "success" || body.status.lowercase() == "duplicate") {
                        // Success!
                        transactionDao.updateSyncStatus(txn.id, "SUCCESS")
                        syncLogDao.insertSyncLog(
                            SyncLog(
                                txnId = txn.txnId,
                                status = "SUCCESS",
                                message = "Synced flawlessly. Server returned: ${body.message}"
                            )
                        )
                        NotificationHelper.showNotification(
                            applicationContext,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SYNC_SUCCESS_NOTIFICATION_ID,
                            "Transaction Synced Successfully",
                            "${txn.sender} TXN ${txn.txnId} of Tk.${txn.amount} recorded."
                        )
                    } else {
                        // Merchant API returned validation error (e.g. invalid merchant, limit reached, device locked)
                        transactionDao.updateSyncStatus(txn.id, "FAILED")
                        syncLogDao.insertSyncLog(
                            SyncLog(
                                txnId = txn.txnId,
                                status = "FAILED",
                                message = "Server rejected: ${body.message}"
                            )
                        )
                        NotificationHelper.showNotification(
                            applicationContext,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SYNC_FAILED_NOTIFICATION_ID,
                            "Sync Validation Error",
                            "Server rejected transaction ${txn.txnId}: ${body.message}"
                        )
                        hasFailure = true
                    }
                } else {
                    // HTTP Status failure 
                    transactionDao.updateSyncStatus(txn.id, "FAILED")
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            txnId = txn.txnId,
                            status = "FAILED",
                            message = "HTTP Server Error: Code ${response.code()}"
                        )
                    )
                    NotificationHelper.showNotification(
                        applicationContext,
                        NotificationHelper.TRANSACTION_CHANNEL_ID,
                        NotificationHelper.SYNC_FAILED_NOTIFICATION_ID,
                        "Gateway Server Unreachable",
                        "Failed syncing ${txn.txnId}: Server returned ${response.code()}"
                    )
                    hasFailure = true
                }
            } catch (e: Exception) {
                // Connection Timeout / Host not resolved - keep as failed, can retry later
                transactionDao.updateSyncStatus(txn.id, "FAILED")
                syncLogDao.insertSyncLog(
                    SyncLog(
                        txnId = txn.txnId,
                        status = "FAILED",
                        message = "Network Connection Problem: ${e.message}"
                    )
                )
                NotificationHelper.showNotification(
                    applicationContext,
                    NotificationHelper.TRANSACTION_CHANNEL_ID,
                    NotificationHelper.SYNC_FAILED_NOTIFICATION_ID,
                    "Payment Network Connection Failed",
                    "Stored locally. Transaction will auto-sync when internet is restored."
                )
                hasFailure = true
            }
        }

        // Save last background sync execution epoch timestamp
        datastoreManager.saveLastSyncTime(System.currentTimeMillis())

        if (hasFailure) {
            val attempt = runAttemptCount
            if (attempt >= 2) {
                datastoreManager.saveRepeatedSyncFailure(true)
                NotificationHelper.showNotification(
                    applicationContext,
                    NotificationHelper.TRANSACTION_CHANNEL_ID,
                    NotificationHelper.SYNC_FAILED_NOTIFICATION_ID,
                    "Gateway Sync Failing Repeatedly",
                    "Sync failed repeatedly ($attempt retries done). Stored offline in database waiting for connection."
                )
            }
            return ListenableWorker.Result.retry()
        } else {
            datastoreManager.saveRepeatedSyncFailure(false)
            return ListenableWorker.Result.success()
        }
    }
}
