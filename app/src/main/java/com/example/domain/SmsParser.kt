package com.example.domain

import android.util.Log

data class ParsedSms(
    val senderService: String, // bKash, Nagad, Rocket, Upay, Bank, Unknown
    val senderNumber: String,
    val amount: Double,
    val txnId: String,
    val time: String,
    val reference: String,
    val rawSms: String
)

object SmsParser {
    private const val TAG = "SmsParser"

    // Transaction ID patterns
    private val txnPatterns = listOf(
        Regex("TrxID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("TxnID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("TxId\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("Trans\\s*ID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("Trx\\s*ID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("Transaction\\s*ID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("Txn\\s*ID\\s*[:\\s]\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE),
        Regex("ID\\s*[:\\s]\\s*([A-Z0-9]{8,16})", RegexOption.IGNORE_CASE)
    )

    // Amount patterns
    private val amountPatterns = listOf(
        Regex("(?:received|payment|of|Tk|Tk\\.|BDT|amount)\\s*[:\\s]*([0-9,]+\\.[0-9]{2})", RegexOption.IGNORE_CASE),
        Regex("(?:received|payment|of|Tk|Tk\\.|BDT|amount)\\s*[:\\s]*([0-9,]+)", RegexOption.IGNORE_CASE),
        Regex("([0-9,]+\\.[0-9]{2})\\s*(?:Tk|BDT)", RegexOption.IGNORE_CASE),
        Regex("([0-9,]+)\\s*(?:Tk|BDT)", RegexOption.IGNORE_CASE)
    )

    // Source phone number patterns (extract user/sender number from body text)
    private val phonePatterns = listOf(
        Regex("(?:from|sender)\\s*[:\\s]*(\\+?8801[3-9]\\d{8})", RegexOption.IGNORE_CASE),
        Regex("(?:from|sender)\\s*[:\\s]*(01[3-9]\\d{8})", RegexOption.IGNORE_CASE),
        Regex("(\\+?8801[3-9]\\d{8})"),
        Regex("(01[3-9]\\d{8})")
    )

    // Reference patterns
    private val refPatterns = listOf(
        Regex("Ref\\s*[:\\s]\\s*([^\\.,\\s]+)", RegexOption.IGNORE_CASE),
        Regex("Reference\\s*[:\\s]\\s*([^\\.,\\s]+)", RegexOption.IGNORE_CASE),
        Regex("Ref\\s*[:\\s]\\s*([^\\.]+)", RegexOption.IGNORE_CASE),
        Regex("Reference\\s*[:\\s]\\s*([^\\.]+)", RegexOption.IGNORE_CASE)
    )

    // Time/Date extractors
    private val timePatterns = listOf(
        Regex("at\\s+([0-9:\\/\\s\\-A-Za-z]{8,22})", RegexOption.IGNORE_CASE),
        Regex("Date\\s*:\\s*([0-9:\\/\\s\\-A-Za-z]{8,22})", RegexOption.IGNORE_CASE),
        Regex("([0-9]{2}\\/[0-9]{2}\\/[0-9]{4}\\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?\\s*(?:AM|PM)?)", RegexOption.IGNORE_CASE),
        Regex("([0-9]{2}-[0-9]{2}-[0-9]{4}\\s+[0-9]{2}:[0-9]{2}(?::[0-9]{2})?\\s*(?:AM|PM)?)", RegexOption.IGNORE_CASE)
    )

    fun parseSms(
        senderAddress: String,
        body: String,
        allowedSenders: String = "",
        inflowKeywords: String = "",
        marketingKeywords: String = ""
    ): ParsedSms? {
        val uppercaseAddress = senderAddress.uppercase()
        val lowercaseBody = body.lowercase()

        // 1. SERVICE PROVIDER RECOGNITION (FALLBACK OR EXPLICIT ALLOWED LIST)
        val cleanSenders = allowedSenders.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val senderMatched = if (cleanSenders.isNotEmpty()) {
            cleanSenders.any { uppercaseAddress.contains(it) || it.contains(uppercaseAddress) || lowercaseBody.contains(it.lowercase()) }
        } else {
            val isBkashRange = uppercaseAddress.contains("BKASH") || uppercaseAddress == "16247" || lowercaseBody.contains("bkash")
            val isNagadRange = uppercaseAddress.contains("NAGAD") || uppercaseAddress == "16167" || lowercaseBody.contains("nagad")
            val isRocketRange = uppercaseAddress.contains("ROCKET") || uppercaseAddress == "16216" || lowercaseBody.contains("rocket") || lowercaseBody.contains("dutch-bangla")
            val isUpayRange = uppercaseAddress.contains("UPAY") || uppercaseAddress == "16268" || lowercaseBody.contains("upay")
            isBkashRange || isNagadRange || isRocketRange || isUpayRange
        }

        if (!senderMatched) {
            // Check if it's an generic financial SMS containing money strings and TxID
            val containsFinancial = lowercaseBody.contains("credited") || lowercaseBody.contains("received") || lowercaseBody.contains("payment")
            val hasTxPattern = txnPatterns.any { it.containsMatchIn(body) }
            if (!(containsFinancial && hasTxPattern)) {
                Log.d(TAG, "SMS ignored: Not from a recognized payment gateway or financial source.")
                return null
            }
        }

        // 2. MARKETING FILTER: Guard against promo noise
        // Active transactions represent dynamic payment acquisitions containing receiving keywords.
        // If it does not contain a positive balance inflow word but has offer, dial, reward, purchase, pack, discount, win, etc., block it.
        val cleanInflow = inflowKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val hasInflowKeywords = if (cleanInflow.isNotEmpty()) {
            cleanInflow.any { lowercaseBody.contains(it) }
        } else {
            lowercaseBody.contains("received") || 
            lowercaseBody.contains("receive") || 
            lowercaseBody.contains("cash in") || 
            lowercaseBody.contains("deposit") || 
            lowercaseBody.contains("credited") || 
            lowercaseBody.contains("payment of") || 
            lowercaseBody.contains("payment received") || 
            lowercaseBody.contains("incoming") ||
            lowercaseBody.contains("cash-in")
        }

        val cleanMarketing = marketingKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val hasMarketingNoise = if (cleanMarketing.isNotEmpty()) {
            cleanMarketing.any { lowercaseBody.contains(it) }
        } else {
            lowercaseBody.contains("offer") || 
            lowercaseBody.contains("bonus") || 
            lowercaseBody.contains("discount") || 
            lowercaseBody.contains("win") || 
            lowercaseBody.contains("dial") || 
            lowercaseBody.contains("pack") || 
            lowercaseBody.contains("campaign") || 
            lowercaseBody.contains("bundle") ||
            lowercaseBody.contains("recharge")
        }

        if (hasMarketingNoise && !hasInflowKeywords) {
            Log.d(TAG, "SMS ignored: Identified as promotional/marketing content.")
            return null
        }

        // 3. TRANSACTION ID EXTRACTION
        var txnId: String? = null
        for (pattern in txnPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                txnId = match.groupValues[1].trim().uppercase().replace(":", "").replace(".", "")
                break
            }
        }

        if (txnId.isNullOrEmpty() || txnId.length < 5) {
            Log.d(TAG, "SMS ignored: Could not identify a valid transaction ID.")
            return null
        }

        // 4. AMOUNT EXTRACTION
        var amountValue = 0.0
        // Clean up common Bangladesh money contexts like "Tk. ", "Tk ", "BDT "
        for (pattern in amountPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                try {
                    val rawNum = match.groupValues[1].replace(",", "").trim()
                    val parsedVal = rawNum.toDoubleOrNull()
                    if (parsedVal != null && parsedVal > 0.0) {
                        amountValue = parsedVal
                        break
                    }
                } catch (e: Exception) {
                    // fallthrough to next pattern
                }
            }
        }

        if (amountValue <= 0.0) {
            // General double fallback extractor: match any floating point number or decimal near keywords
            val generalAmountRegex = Regex("([0-9,]+\\.[0-9]{2}|[0-9,]+)")
            val matches = generalAmountRegex.findAll(body)
            for (match in matches) {
                val valStr = match.value.replace(",", "")
                val dVal = valStr.toDoubleOrNull()
                if (dVal != null && dVal > 5.0 && dVal < 500000.0) { // realistic transaction range
                    amountValue = dVal
                    break
                }
            }
        }

        if (amountValue <= 0.0) {
            Log.d(TAG, "SMS ignored: Amount parsed is 0.0 or could not be found.")
            return null
        }

        // 5. SERVICE PROVIDER NORMALIZATION
        val isBkashRange = uppercaseAddress.contains("BKASH") || uppercaseAddress == "16247" || lowercaseBody.contains("bkash")
        val isNagadRange = uppercaseAddress.contains("NAGAD") || uppercaseAddress == "16167" || lowercaseBody.contains("nagad")
        val isRocketRange = uppercaseAddress.contains("ROCKET") || uppercaseAddress == "16216" || lowercaseBody.contains("rocket") || lowercaseBody.contains("dutch-bangla")
        val isUpayRange = uppercaseAddress.contains("UPAY") || uppercaseAddress == "16268" || lowercaseBody.contains("upay")

        val senderService = when {
            isBkashRange -> "bKash"
            isNagadRange -> "Nagad"
            isRocketRange -> "Rocket"
            isUpayRange -> "Upay"
            else -> "Bank SMS"
        }

        // 6. SENDER PHONE EXTRACTION
        var senderNumber = senderAddress
        for (pattern in phonePatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length >= 10) {
                    senderNumber = candidate
                    break
                }
            }
        }

        // 7. REFERENCE EXTRACTION (Pruning special characters and dot terminators)
        var reference = "N/A"
        for (pattern in refPatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && !candidate.equals("none", ignoreCase = true) && !candidate.equals("null", ignoreCase = true)) {
                    reference = candidate
                    break
                }
            }
        }
        if (reference.length > 50) {
            reference = reference.take(47) + "..."
        }

        // 8. TIMESTAMP EXTRACTION
        var timeStr: String? = null
        for (pattern in timePatterns) {
            val match = pattern.find(body)
            if (match != null) {
                timeStr = match.groupValues[0].trim()
                break
            }
        }
        if (timeStr.isNullOrEmpty()) {
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            timeStr = formatter.format(java.util.Date())
        }

        return ParsedSms(
            senderService = senderService,
            senderNumber = senderNumber,
            amount = amountValue,
            txnId = txnId,
            time = timeStr ?: "N/A",
            reference = reference,
            rawSms = body
        )
    }
}
