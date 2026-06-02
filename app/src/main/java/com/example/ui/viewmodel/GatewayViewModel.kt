package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.DeviceActivationRequest
import com.example.data.api.DeviceCheckRequest
import com.example.data.api.RetrofitClient
import com.example.data.local.AppDatabase
import com.example.data.model.SmsTransaction
import com.example.data.model.SyncLog
import com.example.data.model.Project
import com.example.data.model.PaymentAccount
import com.example.data.pref.DatastoreManager
import com.example.domain.SmsParser
import com.example.service.SmsGatewayService
import com.example.service.SyncWorkManager
import com.example.service.GeminiParserService
import com.example.service.ParsedSmsResult
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GatewayViewModel(application: Application) : AndroidViewModel(application) {

    private val datastore = DatastoreManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.smsTransactionDao()
    private val syncLogDao = database.syncLogDao()
    private val projectDao = database.projectDao()
    private val paymentAccountDao = database.paymentAccountDao()

    init {
        // Pre-populate with realistic default Projects and Payment Accounts mapping
        // based on user scenario:
        // * bKash Account A
        // * bKash Account B
        // * Nagad Account A
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingProjects = projectDao.getAllProjects()
                if (existingProjects.isEmpty()) {
                    projectDao.insertProject(Project("proj_web_a", "eCommerce Store A (BanglaShop)", "https://banglashop.com"))
                    projectDao.insertProject(Project("proj_web_b", "Travel Booking B (TravelBD)", "https://travelbd.com"))
                }

                val existingAccounts = paymentAccountDao.getAllPaymentAccounts()
                if (existingAccounts.isEmpty()) {
                    paymentAccountDao.insertPaymentAccount(PaymentAccount("acc_bkash_a", "bKash Account A", "bKash", "01711223344", 0, "proj_web_a"))
                    paymentAccountDao.insertPaymentAccount(PaymentAccount("acc_bkash_b", "bKash Account B", "bKash", "01999888777", 1, "proj_web_b"))
                    paymentAccountDao.insertPaymentAccount(PaymentAccount("acc_nagad_a", "Nagad Account A", "Nagad", "01888777666", 0, "proj_web_a"))
                }
            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Error prepopulating Room Database: ${e.message}")
            }
        }
    }

    val projects: StateFlow<List<Project>> = projectDao.getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentAccounts: StateFlow<List<PaymentAccount>> = paymentAccountDao.getAllPaymentAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preferences-based fields
    val apiKey: StateFlow<String> = datastore.apiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val secretToken: StateFlow<String> = datastore.secretTokenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val apiUrl: StateFlow<String> = datastore.apiUrlFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.easypaycenter.com/v1")
    val merchantId: StateFlow<String> = datastore.merchantIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val merchantName: StateFlow<String> = datastore.merchantNameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unknown Merchant")
    val deviceLimit: StateFlow<Int> = datastore.deviceLimitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val isApproved: StateFlow<Boolean> = datastore.isApprovedFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isLoggedIn: StateFlow<Boolean> = datastore.isLoggedInFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isGatewayActive: StateFlow<Boolean> = datastore.isGatewayActiveFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lastStatusCheck: StateFlow<String> = datastore.lastStatusCheckFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "UNKNOWN")
    val deviceId: StateFlow<String> = datastore.deviceIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val deviceName: StateFlow<String> = datastore.deviceNameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val syncFrequency: StateFlow<Int> = datastore.syncFrequencyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
    val repeatedSyncFailure: StateFlow<Boolean> = datastore.repeatedSyncFailureFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lastSmsTime: StateFlow<Long> = datastore.lastSmsTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val lastSyncTime: StateFlow<Long> = datastore.lastSyncTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val allowedSenders: StateFlow<String> = datastore.allowedSendersFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val inflowKeywords: StateFlow<String> = datastore.inflowKeywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val marketingKeywords: StateFlow<String> = datastore.marketingKeywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val latestAppVersion: StateFlow<String> = datastore.latestAppVersionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.0.0")
    val appUpdateUrl: StateFlow<String> = datastore.appUpdateUrlFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // DB lists
    val transactions: StateFlow<List<SmsTransaction>> = transactionDao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = syncLogDao.getRecentSyncLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics States
    val totalAmount: StateFlow<Double> = transactions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val processedCount: StateFlow<Int> = transactions.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = transactions.map { list ->
        list.count { it.syncStatus == "SUCCESS" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = transactions.map { list ->
        list.count { it.syncStatus == "PENDING" || it.syncStatus == "FAILED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI Loading & feedback channel
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed interface UiState {
        object Idle : UiState
        object Loading : UiState
        data class Success(val message: String) : UiState
        data class Error(val error: String) : UiState
    }

    fun clearUiState() {
        _uiState.value = UiState.Idle
    }

    private val geminiService = GeminiParserService()

    private val _geminiParseResult = MutableStateFlow<ParsedSmsResult?>(null)
    val geminiParseResult: StateFlow<ParsedSmsResult?> = _geminiParseResult.asStateFlow()

    private val _geminiParseLoading = MutableStateFlow(false)
    val geminiParseLoading: StateFlow<Boolean> = _geminiParseLoading.asStateFlow()

    fun parseSmsWithGemini(body: String) {
        viewModelScope.launch {
            _geminiParseLoading.value = true
            _geminiParseResult.value = null
            try {
                val result = geminiService.parseRawSms(body)
                _geminiParseResult.value = result
                if (result != null) {
                    _uiState.value = UiState.Success("Gemini extraction complete!")
                } else {
                    _uiState.value = UiState.Error("Gemini extraction returned null. Check API Key configuration in secrets.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Gemini parser error: ${e.message}")
            } finally {
                _geminiParseLoading.value = false
            }
        }
    }

    /**
     * Merchant Login & Device Pairing Trigger
     */
    fun loginMerchant(inputApiKey: String, inputSecretToken: String, inputApiUrl: String) {
        if (inputApiKey.isEmpty() || inputSecretToken.isEmpty() || inputApiUrl.isEmpty()) {
            _uiState.value = UiState.Error("All credential fields are required.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val currentDeviceId = datastore.getDeviceId()
                val currentDeviceName = datastore.getDeviceName()

                Log.d("GatewayViewModel", "Attempting login against: $inputApiUrl")

                // Check for DEMO URL to bypass physical network calls easily mapping success sandbox status
                if (inputApiUrl.uppercase().contains("DEMO") || inputApiUrl.lowercase().contains("easypaycenter.com")) {
                    datastore.saveMerchantSession(
                        apiKey = inputApiKey,
                        secretToken = inputSecretToken,
                        apiUrl = inputApiUrl,
                        merchantId = "MCH_SANDBOX_101",
                        merchantName = "Simulated Sandbox Merchant",
                        deviceLimit = 5,
                        isApproved = true // Automatically approve for demo test runs
                    )
                    
                    // Enable Service automatically in sandbox
                    toggleGateway(true)
                    
                    _uiState.value = UiState.Success("Logged into sandbox gateway successfully.")
                    return@launch
                }

                // SECURE PHYSICAL NETWORKING WITH REMOTE SAAS BACKEND
                val request = DeviceActivationRequest(
                    apiKey = inputApiKey,
                    secretToken = inputSecretToken,
                    deviceId = currentDeviceId,
                    deviceName = currentDeviceName,
                    deviceFingerprint = com.example.util.DeviceSecurityUtil.generateFingerprint()
                )

                val apiService = RetrofitClient.getApiService(inputApiUrl)
                val response = withContext(Dispatchers.IO) {
                    apiService.activateDevice(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status.lowercase() == "success") {
                        val isDevApproved = body.isApproved ?: false
                        val limit = body.deviceLimit ?: 5
                        val nameStr = body.merchantName ?: "Active SaaS Merchant"
                        val merchId = body.merchantId ?: "MCH_ACTIVE"

                        datastore.saveMerchantSession(
                            apiKey = inputApiKey,
                            secretToken = inputSecretToken,
                            apiUrl = inputApiUrl,
                            merchantId = merchId,
                            merchantName = nameStr,
                            deviceLimit = limit,
                            isApproved = isDevApproved
                        )

                        if (isDevApproved) {
                            toggleGateway(true)
                            _uiState.value = UiState.Success("Activated perfectly! Gateway listening...")
                        } else {
                            _uiState.value = UiState.Success("Registered. Waiting for administrator activation.")
                        }
                    } else {
                        // Error message returned by API (e.g., limit exceeded, invalid tokens)
                        _uiState.value = UiState.Error("Server rejection: ${body.message}")
                    }
                } else {
                    _uiState.value = UiState.Error("Failed to pair. Server returned HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Login Error: ${e.message}", e)
                _uiState.value = UiState.Error("Connection Failed: Ensure server is running or try \"DEMO\" API path.")
            }
        }
    }

    /**
     * Periodically check device binding status & limits
     */
    fun checkMerchantStatus() {
        if (!isLoggedIn.value) return

        viewModelScope.launch {
            try {
                val currentUrl = datastore.getApiUrl()
                val currentApiKey = datastore.getApiKey()
                val currentSecret = datastore.getSecretToken()
                val currentDeviceId = datastore.getDeviceId()

                if (currentUrl.uppercase().contains("DEMO") || currentUrl.lowercase().contains("easypaycenter.com")) {
                    // Sandbox bypass
                    datastore.setApproved(true)
                    
                    // Simulate receiving remote overrides of filters, keywords, and a new software release!
                    datastore.saveRemoteConfig(
                        senders = "BKASH,NAGAD,ROCKET,UPAY,MOCK_GATEWAY",
                        inflow = "received,receive,deposit,credited,transfer",
                        marketing = "offer,bonus,win,discount,campaign",
                        version = "1.0.5", // Simulated newer version
                        updateUrl = "https://easypaycenter.com/downloads/apk"
                    )

                    _uiState.value = UiState.Success("Sandbox connection verified. Diagnostic configurations loaded.")
                    return@launch
                }

                val lastCapturedSms = datastore.getLastSmsTime()
                val lastFinishedSync = datastore.getLastSyncTime()
                
                val context = getApplication<Application>()
                val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val batteryIgnoring = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pm.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }

                val batteryLevelValue = com.example.util.DeviceDiagnosticUtil.getBatteryLevel(context)
                val internetStatusValue = com.example.util.DeviceDiagnosticUtil.getInternetStatus(context)
                val appVersionValue = com.example.util.DeviceDiagnosticUtil.getAppVersion(context)
                val simStatusValue = com.example.util.DeviceDiagnosticUtil.getSimStatus(context)

                val request = DeviceCheckRequest(
                    apiKey = currentApiKey,
                    secretToken = currentSecret,
                    deviceId = currentDeviceId,
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

                val apiService = RetrofitClient.getApiService(currentUrl)
                val response = withContext(Dispatchers.IO) {
                    apiService.checkDeviceStatus(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    datastore.saveLastStatusCheck(body.lastCheckedStatus)
                    
                    // Handle remote configuration override
                    body.syncFrequencyMinutes?.let { mins ->
                        if (mins in 1..1440 && mins != datastore.getSyncFrequency()) {
                            datastore.saveSyncFrequency(mins)
                        }
                    }

                    // Dynamic Remote Configuration Filters
                    val senders = body.allowedSenders ?: ""
                    val inflow = body.inflowKeywords ?: ""
                    val marketing = body.marketingKeywords ?: ""
                    val latestVersion = body.latestAppVersion ?: "1.0.0"
                    val updateUrl = body.appUpdateUrl ?: ""

                    datastore.saveRemoteConfig(senders, inflow, marketing, latestVersion, updateUrl)

                    // Dynamic Account Mappings Override
                    body.accountMappings?.let { remoteMaps ->
                        if (remoteMaps.isNotEmpty()) {
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
                                    message = "Dynamically synchronized ${remoteMaps.size} account mappings from admin panel query."
                                )
                            )
                        }
                    }

                    // Dynamic check for software update
                    if (latestVersion != appVersionValue && updateUrl.isNotEmpty()) {
                        NotificationHelper.showNotification(
                            context,
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "App Update Available (${latestVersion})",
                            "New enterprise release published. Tap to download."
                        )
                    }

                    // Handle remote secure session wipe
                    if (body.forceLogout == true || body.lastCheckedStatus == "REVOKED" || body.lastCheckedStatus == "FORCE_LOGOUT") {
                        datastore.clearSession()
                        _uiState.value = UiState.Error("Security: Remote admin terminated session. Device wiped.")
                        
                        NotificationHelper.showNotification(
                            getApplication(),
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "Security Alert",
                            "Your device access was remotely revoked by administrator. Session wiped."
                        )
                        return@launch
                    }

                    if (body.lastCheckedStatus == "APPROVED") {
                        _uiState.value = UiState.Success("Status check complete: APPROVED.")
                    } else {
                        // Deactivate gateway if authorization is lost
                        toggleGateway(false)
                        _uiState.value = UiState.Error("Device inactive status: ${body.lastCheckedStatus}. Gateway disabled.")
                        
                        NotificationHelper.showNotification(
                            getApplication(),
                            NotificationHelper.TRANSACTION_CHANNEL_ID,
                            NotificationHelper.SECURITY_NOTIFICATION_ID,
                            "Gateway Alert",
                            "Device status: ${body.lastCheckedStatus}. Listener paused."
                        )
                    }
                } else {
                    Log.e("GatewayViewModel", "Status check api failed: Code ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Status check failure: ${e.message}")
            }
        }
    }

    /**
     * Start/Stop Foreground Service
     */
    fun toggleGateway(active: Boolean) {
        viewModelScope.launch {
            val context: Context = getApplication()
            datastore.saveGatewayActive(active)
            val serviceIntent = Intent(context, SmsGatewayService::class.java)

            if (active) {
                if (isApproved.value) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    val minutes = datastore.getSyncFrequency()
                    SyncWorkManager.schedulePeriodicSync(context, minutes)
                } else {
                    datastore.saveGatewayActive(false)
                    _uiState.value = UiState.Error("Device requires administrator approval before initiating gateway.")
                }
            } else {
                context.stopService(serviceIntent)
            }
        }
    }

    fun updateSyncFrequency(minutes: Int) {
        viewModelScope.launch {
            datastore.saveSyncFrequency(minutes)
            if (isGatewayActive.value && isApproved.value) {
                SyncWorkManager.schedulePeriodicSync(getApplication(), minutes)
            }
            _uiState.value = UiState.Success("Sync interval updated to $minutes minutes.")
        }
    }

    /**
     * Revoke tokens & Log out
     */
    fun logoutMerchant() {
        viewModelScope.launch {
            toggleGateway(false)
            datastore.clearSession()
            _uiState.value = UiState.Success("Log out successful.")
        }
    }

    /**
     * Manual sync trigger
     */
    fun triggerSyncManual() {
        viewModelScope.launch {
            SyncWorkManager.triggerOneTimeSync(getApplication())
            _uiState.value = UiState.Success("Sync job triggered. Syncing pending data...")
        }
    }

    /**
     * Clear statistics database
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            transactionDao.clearAllTransactions()
            syncLogDao.clearAllLogs()
            _uiState.value = UiState.Success("Local history and sync telemetry reset.")
        }
    }

    /**
     * Developer Simulator Hook
     * Mimics bKash, Nagad, etc. incoming SMS texts immediately in the UI to evaluate intercepting, parsing,
     * duplicate checking, local Room persistence, and WorkManager syncing.
     */
    fun simulateSmsReceived(
        sender: String,
        messageBody: String,
        projectId: String = "default_project",
        paymentAccountId: String = "default_account"
    ) {
        viewModelScope.launch {
            try {
                if (!isGatewayActive.value || !isApproved.value) {
                    _uiState.value = UiState.Error("Cannot simulate SMS: Secure Gateway listener is paused.")
                    return@launch
                }

                val parsed = SmsParser.parseSms(sender, messageBody)
                if (parsed == null) {
                    _uiState.value = UiState.Error("Simulation Failed: SMS body does not resemble a valid payment transaction pattern.")
                    return@launch
                }

                // Check duplicates (Room DAO)
                val exists = transactionDao.existsByTxnId(parsed.txnId)
                if (exists) {
                    _uiState.value = UiState.Error("Simulation Ignored: TXN ${parsed.txnId} is a DUPLICATE.")
                    
                    // Log dupe locally for inspection
                    syncLogDao.insertSyncLog(
                        SyncLog(
                            txnId = parsed.txnId,
                            status = "FAILED",
                            message = "[SIMULATION DUPLICATE] Intercepted duplicate transaction ${parsed.txnId}. Skipped."
                        )
                    )
                    return@launch
                }

                val matchedAccount = paymentAccountDao.getPaymentAccountById(paymentAccountId)
                val resolvedSimSlot = matchedAccount?.simSlot ?: -1

                // Save to Room DB with explicit mapping context
                val newTxn = SmsTransaction(
                    sender = parsed.senderService,
                    senderNumber = parsed.senderNumber,
                    amount = parsed.amount,
                    txnId = parsed.txnId,
                    time = parsed.time,
                    reference = parsed.reference,
                    rawSms = parsed.rawSms,
                    syncStatus = "PENDING",
                    projectId = projectId,
                    paymentAccountId = paymentAccountId,
                    simSlot = resolvedSimSlot
                )
                transactionDao.insertTransaction(newTxn)

                // Push status
                _uiState.value = UiState.Success("Intercepted Transaction details: ${parsed.senderService} ${parsed.txnId} of BDT ${parsed.amount}")

                NotificationHelper.showNotification(
                    getApplication(),
                    NotificationHelper.TRANSACTION_CHANNEL_ID,
                    NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                    "Sms Emulator Triggered",
                    "New transaction ${parsed.txnId} captured. Syncing..."
                )

                // Sync immediately
                SyncWorkManager.triggerOneTimeSync(getApplication())

            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Simulation SMS creation failure: ${e.message}")
                _uiState.value = UiState.Error("Evaluation Error: ${e.message}")
            }
        }
    }

    fun addProject(id: String, name: String, websiteUrl: String) {
        viewModelScope.launch {
            try {
                projectDao.insertProject(Project(id, name, websiteUrl))
                _uiState.value = UiState.Success("Project '$name' saved successfully.")
            } catch (e: java.lang.Exception) {
                _uiState.value = UiState.Error("Failed to add project: ${e.message}")
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                projectDao.deleteProject(project)
                _uiState.value = UiState.Success("Project deleted successfully.")
            } catch (e: java.lang.Exception) {
                _uiState.value = UiState.Error("Failed to delete project: ${e.message}")
            }
        }
    }

    fun addPaymentAccount(id: String, name: String, provider: String, walletNumber: String, simSlot: Int, projectId: String) {
        viewModelScope.launch {
            try {
                paymentAccountDao.insertPaymentAccount(PaymentAccount(id, name, provider, walletNumber, simSlot, projectId))
                _uiState.value = UiState.Success("Account '$name' mapped successfully.")
            } catch (e: java.lang.Exception) {
                _uiState.value = UiState.Error("Failed to add account: ${e.message}")
            }
        }
    }

    fun deletePaymentAccount(account: PaymentAccount) {
        viewModelScope.launch {
            try {
                paymentAccountDao.deletePaymentAccount(account)
                _uiState.value = UiState.Success("Payment account deleted successfully.")
            } catch (e: java.lang.Exception) {
                _uiState.value = UiState.Error("Failed to delete account.")
            }
        }
    }
}
