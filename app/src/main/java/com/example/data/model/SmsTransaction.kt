package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_transactions")
data class SmsTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,            // bKash, Nagad, Rocket, Upay, Bank
    val senderNumber: String,      // Phone number that sent original SMS or customer source
    val amount: Double,
    val txnId: String,             // Transaction ID
    val time: String,              // Extracted time/date
    val reference: String,         // Extracted reference
    val rawSms: String,            // Original raw SMS text
    val syncStatus: String,        // PENDING, SUCCESS, FAILED
    val projectId: String = "default_project",
    val paymentAccountId: String = "default_account",
    val simSlot: Int = -1,
    val timestamp: Long = System.currentTimeMillis()
)
