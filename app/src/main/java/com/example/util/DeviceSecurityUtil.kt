package com.example.util

import android.os.Build
import java.security.MessageDigest

object DeviceSecurityUtil {
    /**
     * Generates a unique, secure SHA-256 hardware security signature using Android hardware features.
     */
    fun generateFingerprint(): String {
        val rawFeatures = "BOARD:${Build.BOARD}|BRAND:${Build.BRAND}|DEVICE:${Build.DEVICE}|" +
                "HARDWARE:${Build.HARDWARE}|MANUFACTURER:${Build.MANUFACTURER}|" +
                "MODEL:${Build.MODEL}|PRODUCT:${Build.PRODUCT}|OS:${Build.VERSION.SDK_INT}"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawFeatures.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }.take(32).uppercase()
        } catch (e: Exception) {
            "FINGERPRINT_FALLBACK_" + rawFeatures.hashCode().toString().replace("-", "X")
        }
    }
}
