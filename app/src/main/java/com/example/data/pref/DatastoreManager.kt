package com.example.data.pref

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_gateway_prefs")

class DatastoreManager(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val SECRET_TOKEN = stringPreferencesKey("secret_token")
        private val API_URL = stringPreferencesKey("api_url")
        private val MERCHANT_ID = stringPreferencesKey("merchant_id")
        private val MERCHANT_NAME = stringPreferencesKey("merchant_name")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val DEVICE_NAME = stringPreferencesKey("device_name")
        private val DEVICE_LIMIT = intPreferencesKey("device_limit")
        private val IS_APPROVED = booleanPreferencesKey("is_approved")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val IS_GATEWAY_ACTIVE = booleanPreferencesKey("is_gateway_active")
        private val LAST_STATUS_CHECK = stringPreferencesKey("last_status_check")
        private val SYNC_FREQUENCY_MINUTES = intPreferencesKey("sync_frequency_minutes")
        private val REPEATED_SYNC_FAILURE = booleanPreferencesKey("repeated_sync_failure")
        private val LAST_SMS_TIME = longPreferencesKey("last_sms_time")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val ALLOWED_SENDERS = stringPreferencesKey("allowed_senders")
        private val INFLOW_KEYWORDS = stringPreferencesKey("inflow_keywords")
        private val MARKETING_KEYWORDS = stringPreferencesKey("marketing_keywords")
        private val LATEST_APP_VERSION = stringPreferencesKey("latest_app_version")
        private val APP_UPDATE_URL = stringPreferencesKey("app_update_url")
        private val ADMIN_PIN = stringPreferencesKey("admin_pin")
        private val ACTIVE_LAUNCHER_ALIAS = stringPreferencesKey("active_launcher_alias")
        private val CUSTOM_APP_NAME = stringPreferencesKey("custom_app_name")
        private val CUSTOM_APP_ICON_PATH = stringPreferencesKey("custom_app_icon_path")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val secretTokenFlow: Flow<String> = context.dataStore.data.map { it[SECRET_TOKEN] ?: "" }
    val apiUrlFlow: Flow<String> = context.dataStore.data.map { it[API_URL] ?: "https://api.easypaycenter.com/v1" }
    val merchantIdFlow: Flow<String> = context.dataStore.data.map { it[MERCHANT_ID] ?: "" }
    val merchantNameFlow: Flow<String> = context.dataStore.data.map { it[MERCHANT_NAME] ?: "Unknown Merchant" }
    val deviceLimitFlow: Flow<Int> = context.dataStore.data.map { it[DEVICE_LIMIT] ?: 5 }
    val isApprovedFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_APPROVED] ?: false }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val isGatewayActiveFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_GATEWAY_ACTIVE] ?: false }
    val lastStatusCheckFlow: Flow<String> = context.dataStore.data.map { it[LAST_STATUS_CHECK] ?: "UNKNOWN" }
    val syncFrequencyFlow: Flow<Int> = context.dataStore.data.map { it[SYNC_FREQUENCY_MINUTES] ?: 15 }
    val repeatedSyncFailureFlow: Flow<Boolean> = context.dataStore.data.map { it[REPEATED_SYNC_FAILURE] ?: false }
    val lastSmsTimeFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SMS_TIME] ?: 0L }
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }
    val allowedSendersFlow: Flow<String> = context.dataStore.data.map { it[ALLOWED_SENDERS] ?: "" }
    val inflowKeywordsFlow: Flow<String> = context.dataStore.data.map { it[INFLOW_KEYWORDS] ?: "" }
    val marketingKeywordsFlow: Flow<String> = context.dataStore.data.map { it[MARKETING_KEYWORDS] ?: "" }
    val latestAppVersionFlow: Flow<String> = context.dataStore.data.map { it[LATEST_APP_VERSION] ?: "1.0.0" }
    val appUpdateUrlFlow: Flow<String> = context.dataStore.data.map { it[APP_UPDATE_URL] ?: "" }
    val adminPinFlow: Flow<String> = context.dataStore.data.map { it[ADMIN_PIN] ?: "2580" }
    val activeLauncherAliasFlow: Flow<String> = context.dataStore.data.map { it[ACTIVE_LAUNCHER_ALIAS] ?: "com.example.AliasDefault" }
    val customAppNameFlow: Flow<String> = context.dataStore.data.map { it[CUSTOM_APP_NAME] ?: "" }
    val customAppIconPathFlow: Flow<String> = context.dataStore.data.map { it[CUSTOM_APP_ICON_PATH] ?: "" }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val existing = prefs[DEVICE_ID]
        if (existing.isNullOrEmpty()) {
            val newId = UUID.randomUUID().toString()
            saveDeviceId(newId)
            newId
        } else {
            existing
        }
    }

    val deviceNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val existing = prefs[DEVICE_NAME]
        if (existing.isNullOrEmpty()) {
            val autoName = "${Build.MANUFACTURER} ${Build.MODEL}".uppercase()
            saveDeviceName(autoName)
            autoName
        } else {
            existing
        }
    }

    suspend fun saveMerchantSession(
        apiKey: String,
        secretToken: String,
        apiUrl: String,
        merchantId: String,
        merchantName: String,
        deviceLimit: Int,
        isApproved: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = apiKey
            prefs[SECRET_TOKEN] = secretToken
            prefs[API_URL] = apiUrl
            prefs[MERCHANT_ID] = merchantId
            prefs[MERCHANT_NAME] = merchantName
            prefs[DEVICE_LIMIT] = deviceLimit
            prefs[IS_APPROVED] = isApproved
            prefs[IS_LOGGED_IN] = true
            prefs[LAST_STATUS_CHECK] = if (isApproved) "APPROVED" else "PENDING"
        }
    }

    suspend fun saveGatewayActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_GATEWAY_ACTIVE] = active
        }
    }

    suspend fun setApproved(approved: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_APPROVED] = approved
            prefs[LAST_STATUS_CHECK] = if (approved) "APPROVED" else "PENDING"
        }
    }

    suspend fun saveLastStatusCheck(status: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_STATUS_CHECK] = status
            if (status == "APPROVED") {
                prefs[IS_APPROVED] = true
            } else if (status == "REVOKED" || status == "LIMIT_EXCEEDED" || status == "UNAUTHORIZED") {
                prefs[IS_APPROVED] = false
            }
        }
    }

    suspend fun saveCustomDeviceDetails(id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID] = id
            prefs[DEVICE_NAME] = name
        }
    }

    private suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID] = id
        }
    }

    private suspend fun saveDeviceName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[DEVICE_NAME] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = ""
            prefs[SECRET_TOKEN] = ""
            prefs[MERCHANT_ID] = ""
            prefs[MERCHANT_NAME] = "Unknown Merchant"
            prefs[IS_LOGGED_IN] = false
            prefs[IS_APPROVED] = false
            prefs[IS_GATEWAY_ACTIVE] = false
            prefs[LAST_STATUS_CHECK] = "UNKNOWN"
        }
    }

    // Helper synchronous getters
    suspend fun getDeviceId(): String = deviceIdFlow.first()
    suspend fun getDeviceName(): String = deviceNameFlow.first()
    suspend fun getApiUrl(): String = apiUrlFlow.first()
    suspend fun getApiKey(): String = apiKeyFlow.first()
    suspend fun getSecretToken(): String = secretTokenFlow.first()
    suspend fun getMerchantId(): String = merchantIdFlow.first()
    suspend fun isApproved(): Boolean = isApprovedFlow.first()
    suspend fun isGatewayActive(): Boolean = isGatewayActiveFlow.first()
    suspend fun getSyncFrequency(): Int = syncFrequencyFlow.first()
    suspend fun hasRepeatedSyncFailure(): Boolean = repeatedSyncFailureFlow.first()
    suspend fun getLastSmsTime(): Long = lastSmsTimeFlow.first()
    suspend fun getLastSyncTime(): Long = lastSyncTimeFlow.first()

    suspend fun saveSyncFrequency(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[SYNC_FREQUENCY_MINUTES] = minutes
        }
    }

    suspend fun saveRepeatedSyncFailure(failed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REPEATED_SYNC_FAILURE] = failed
        }
    }

    suspend fun saveLastSmsTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SMS_TIME] = time
        }
    }

    suspend fun saveLastSyncTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIME] = time
        }
    }

    suspend fun getAllowedSenders(): String = allowedSendersFlow.first()
    suspend fun getInflowKeywords(): String = inflowKeywordsFlow.first()
    suspend fun getMarketingKeywords(): String = marketingKeywordsFlow.first()
    suspend fun getLatestAppVersion(): String = latestAppVersionFlow.first()
    suspend fun getAppUpdateUrl(): String = appUpdateUrlFlow.first()
    suspend fun getAdminPin(): String = adminPinFlow.first()
    suspend fun getActiveLauncherAlias(): String = activeLauncherAliasFlow.first()
    suspend fun getCustomAppName(): String = customAppNameFlow.first()
    suspend fun getCustomAppIconPath(): String = customAppIconPathFlow.first()

    suspend fun saveCustomAppName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_APP_NAME] = name
        }
    }

    suspend fun saveCustomAppIconPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_APP_ICON_PATH] = path
        }
    }

    suspend fun saveActiveLauncherAlias(alias: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_LAUNCHER_ALIAS] = alias
        }
    }

    suspend fun saveAdminPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[ADMIN_PIN] = pin
        }
    }

    suspend fun saveRemoteConfig(
        senders: String,
        inflow: String,
        marketing: String,
        version: String,
        updateUrl: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[ALLOWED_SENDERS] = senders
            prefs[INFLOW_KEYWORDS] = inflow
            prefs[MARKETING_KEYWORDS] = marketing
            prefs[LATEST_APP_VERSION] = version
            prefs[APP_UPDATE_URL] = updateUrl
        }
    }
}
