package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceActivationRequest(
    @Json(name = "api_key") val apiKey: String,
    @Json(name = "secret_token") val secretToken: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceActivationResponse(
    @Json(name = "status") val status: String, // "success", "error"
    @Json(name = "merchant_id") val merchantId: String?,
    @Json(name = "merchant_name") val merchantName: String?,
    @Json(name = "device_id") val deviceId: String?,
    @Json(name = "device_limit") val deviceLimit: Int?,
    @Json(name = "is_approved") val isApproved: Boolean?,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class DeviceCheckRequest(
    @Json(name = "api_key") val apiKey: String,
    @Json(name = "secret_token") val secretToken: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null,
    @Json(name = "last_sms_received") val lastSmsReceived: Long? = null,
    @Json(name = "last_sync_time") val lastSyncTime: Long? = null,
    @Json(name = "internet_connected") val internetConnected: Boolean? = null,
    @Json(name = "battery_optimized") val batteryOptimized: Boolean? = null,
    @Json(name = "battery_level") val batteryLevel: Int? = null,
    @Json(name = "internet_status") val internetStatus: String? = null,
    @Json(name = "app_version") val appVersion: String? = null,
    @Json(name = "sim_status") val simStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteAccountMap(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "wallet_number") val walletNumber: String,
    @Json(name = "sim_slot") val simSlot: Int,
    @Json(name = "project_id") val projectId: String
)

@JsonClass(generateAdapter = true)
data class DeviceCheckResponse(
    @Json(name = "status") val status: String,
    @Json(name = "is_approved") val isApproved: Boolean,
    @Json(name = "last_checked_status") val lastCheckedStatus: String, // APPROVED, PENDING, REVOKED, LIMIT_EXCEEDED
    @Json(name = "device_limit") val deviceLimit: Int?,
    @Json(name = "merchant_name") val merchantName: String?,
    @Json(name = "message") val message: String,
    @Json(name = "sync_frequency_minutes") val syncFrequencyMinutes: Int? = null,
    @Json(name = "force_logout") val forceLogout: Boolean? = null,
    
    // Remote configuration filters
    @Json(name = "allowed_senders") val allowedSenders: String? = null,
    @Json(name = "inflow_keywords") val inflowKeywords: String? = null,
    @Json(name = "marketing_keywords") val marketingKeywords: String? = null,
    @Json(name = "account_mappings") val accountMappings: List<RemoteAccountMap>? = null,
    
    // Remote update prompts
    @Json(name = "latest_app_version") val latestAppVersion: String? = null,
    @Json(name = "app_update_url") val appUpdateUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SmsSyncRequest(
    @Json(name = "merchant_id") val merchantId: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "project_id") val projectId: String,
    @Json(name = "payment_account_id") val paymentAccountId: String,
    @Json(name = "txn_id") val txnId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "sender") val sender: String, // bKash, Nagad, etc.
    @Json(name = "sender_number") val senderNumber: String,
    @Json(name = "time") val time: String,
    @Json(name = "raw_sms") val rawSms: String,
    @Json(name = "reference") val reference: String,
    @Json(name = "sim_slot") val simSlot: Int = -1
)

@JsonClass(generateAdapter = true)
data class SmsSyncResponse(
    @Json(name = "status") val status: String, // "success", "duplicate", "error"
    @Json(name = "txn_id") val txnId: String,
    @Json(name = "is_duplicate") val isDuplicate: Boolean,
    @Json(name = "message") val message: String
)
