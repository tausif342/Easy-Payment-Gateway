package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_accounts")
data class PaymentAccount(
    @PrimaryKey val id: String, // e.g. UUID or custom string "acc_bkash_a"
    val name: String,           // e.g. "bKash Account A"
    val provider: String,       // "bKash", "Nagad", "Rocket", "Upay"
    val walletNumber: String,   // Phone number (e.g. "+8801700000001")
    val simSlot: Int = -1,      // -1: Any, 0: SIM 1, 1: SIM 2
    val projectId: String,      // Associated project ID
    val timestamp: Long = System.currentTimeMillis()
)
