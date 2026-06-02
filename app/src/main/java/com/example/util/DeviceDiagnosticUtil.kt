package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import android.util.Log

object DeviceDiagnosticUtil {
    private const val TAG = "DeviceDiagnosticUtil"

    fun getBatteryLevel(context: Context): Int {
        return try {
            val batteryStatus: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching battery level: ${e.message}")
            -1
        }
    }

    fun getInternetStatus(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "DISCONNECTED"
            val activeNetwork = cm.activeNetwork ?: return "DISCONNECTED"
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "DISCONNECTED"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "CONNECTED"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching internet status: ${e.message}")
            "UNKNOWN"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getSimStatus(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return "UNAVAILABLE"
            val state = tm.simState
            val simStateStr = when (state) {
                TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN_REQUIRED"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK_REQUIRED"
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
                TelephonyManager.SIM_STATE_READY -> "READY"
                TelephonyManager.SIM_STATE_NOT_READY -> "NOT_READY"
                TelephonyManager.SIM_STATE_PERM_DISABLED -> "DISABLED"
                TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "CARD_IO_ERROR"
                TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "RESTRICTED"
                else -> "UNKNOWN"
            }

            // Dual SIM or Subscription info check
            val activeCarrierNames = mutableListOf<String>()
            try {
                val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                if (sm != null) {
                    // Subscription info checking requires READ_PHONE_STATE permission, we must catch any SecurityException gracefully.
                    @Suppress("MissingPermission")
                    val activeList = sm.activeSubscriptionInfoList
                    if (activeList != null) {
                        for (info in activeList) {
                            val name = info.carrierName?.toString() ?: info.displayName?.toString() ?: "Carrier"
                            val slot = info.simSlotIndex
                            activeCarrierNames.add("SIM${slot + 1}:$name")
                        }
                    }
                }
            } catch (sec: SecurityException) {
                // If permission is blocked/not granted, fallback to generic sim state.
            }

            if (activeCarrierNames.isNotEmpty()) {
                activeCarrierNames.joinToString(", ")
            } else {
                "SIM State: $simStateStr"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SIM status: ${e.message}")
            "SIM State: UNKNOWN"
        }
    }
}
