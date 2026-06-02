package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.SmsTransaction
import com.example.data.model.SyncLog
import com.example.data.pref.DatastoreManager
import com.example.domain.SmsParser
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isEmpty()) return

        // Extract sender and build raw message content (combining parts if multi-part SMS)
        val senderAddress = msgs[0].originatingAddress ?: "UNKNOWN"
        val bodyBuilder = StringBuilder()
        for (msg in msgs) {
            bodyBuilder.append(msg.messageBody)
        }
        val rawBody = bodyBuilder.toString()

        Log.d(TAG, "Intercepted SMS from: $senderAddress, Body Length: ${rawBody.length}")

        // Retrieve background work async scope to perform secure database insertion
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val syncLogDao = db.syncLogDao()
            try {
                val datastoreManager = DatastoreManager(context)
                val isActive = datastoreManager.isGatewayActive()
                val isApproved = datastoreManager.isApproved()

                if (!isActive || !isApproved) {
                    Log.d(TAG, "Gateway inactive or device unauthorized. Ignoring received SMS.")
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            logType = "SYSTEM",
                            status = "INFO",
                            message = "Gateway inactive or unauthorized. Skipped message from $senderAddress."
                        )
                    )
                    return@launch
                }

                // Retrieve remote config dynamic filters
                val allowedSenders = datastoreManager.getAllowedSenders()
                val inflowKeywords = datastoreManager.getInflowKeywords()
                val marketingKeywords = datastoreManager.getMarketingKeywords()

                // Parse the SMS content
                val parsed = SmsParser.parseSms(
                    senderAddress = senderAddress,
                    body = rawBody,
                    allowedSenders = allowedSenders,
                    inflowKeywords = inflowKeywords,
                    marketingKeywords = marketingKeywords
                )
                
                if (parsed == null) {
                    Log.d(TAG, "SMS parsed as non-transaction or invalid confirmation message.")
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            logType = "PARSING",
                            status = "INFO",
                            message = "SMS from $senderAddress is non-transactional or filtered: \"${rawBody.take(45)}...\""
                        )
                    )
                    return@launch
                }

                Log.d(TAG, "Valid transaction parser output -> TxnID: ${parsed.txnId}, Amount: ${parsed.amount}, Service: ${parsed.senderService}")
                syncLogDao.insertSyncLog(
                    SyncLog(
                        txnId = parsed.txnId,
                        logType = "PARSING",
                        status = "SUCCESS",
                        message = "Valid transaction parsed -> Tk.${parsed.amount} via ${parsed.senderService} (TxID: ${parsed.txnId})"
                    )
                )

                // Access Room Database & check duplicates
                val transactionDao = db.smsTransactionDao()

                val exists = transactionDao.existsByTxnId(parsed.txnId)
                if (exists) {
                    Log.d(TAG, "Duplicate Transaction Detected: TxnID ${parsed.txnId} already processed. Skipping.")
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            txnId = parsed.txnId,
                            logType = "PARSING",
                            status = "FAILED",
                            message = "Duplicate Transaction Skipped: TxnID ${parsed.txnId}"
                        )
                    )
                    return@launch
                }

                // Identify SIM Slot index to assist account identification
                val slotIndex = intent.extras?.let { extras ->
                    var s = extras.getInt("slot", -1)
                    if (s == -1) s = extras.getInt("simSlot", -1)
                    if (s == -1) s = extras.getInt("simId", -1)
                    if (s == -1) s = extras.getInt("subscription", -1)
                    s
                } ?: -1

                // Query custom payment accounts for dynamic multi-account context Mapping
                val paymentAccountDao = db.paymentAccountDao()
                val accounts = paymentAccountDao.getAllPaymentAccounts()

                var mappedProjectId = "default_project"
                var mappedAccountId = "default_account"

                if (accounts.isNotEmpty()) {
                    val matchingProviderAccounts = accounts.filter {
                        it.provider.equals(parsed.senderService, ignoreCase = true)
                    }

                    if (matchingProviderAccounts.isNotEmpty()) {
                        // 1. Try slot index match
                        val slotMatch = if (slotIndex != -1) {
                            matchingProviderAccounts.find { it.simSlot == slotIndex }
                        } else null

                        // 2. Try body number match
                        val bodyMatch = matchingProviderAccounts.find {
                            it.walletNumber.isNotEmpty() && rawBody.contains(it.walletNumber)
                        }

                        // 3. Select match or fallback to first provider-specific account
                        val resolved = slotMatch ?: bodyMatch ?: matchingProviderAccounts.first()
                        mappedAccountId = resolved.id
                        mappedProjectId = resolved.projectId
                    } else {
                        // Choose first merchant account as a broader fallback
                        val resolved = accounts.first()
                        mappedAccountId = resolved.id
                        mappedProjectId = resolved.projectId
                    }
                }

                // If not duplicate, insert into local cache database
                val newTxn = SmsTransaction(
                    sender = parsed.senderService,
                    senderNumber = parsed.senderNumber,
                    amount = parsed.amount,
                    txnId = parsed.txnId,
                    time = parsed.time,
                    reference = parsed.reference,
                    rawSms = parsed.rawSms,
                    syncStatus = "PENDING",
                    projectId = mappedProjectId,
                    paymentAccountId = mappedAccountId,
                    simSlot = slotIndex
                )
                transactionDao.insertTransaction(newTxn)

                // Update health stats
                datastoreManager.saveLastSmsTime(System.currentTimeMillis())

                // Show visual confirmation toast or notification
                NotificationHelper.showNotification(
                    context,
                    NotificationHelper.TRANSACTION_CHANNEL_ID,
                    NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                    "Transaction Intercepted (${parsed.senderService})",
                    "Amount: Tk.${parsed.amount}. TrxID: ${parsed.txnId}. Sync queued."
                )

                // Trigger instant upload sync using background WorkManager
                SyncWorkManager.triggerOneTimeSync(context)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture SMS transaction safely: ${e.message}", e)
                try {
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            logType = "ERROR",
                            status = "FAILED",
                            message = "Failed to process SMS safely: ${e.message}"
                        )
                    )
                } catch (dbEx: Exception) {
                    Log.e(TAG, "Failed to log database exception: ${dbEx.message}")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
