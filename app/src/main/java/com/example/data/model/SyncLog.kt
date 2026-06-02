package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val txnId: String = "",
    val status: String,             // SUCCESS, FAILED, INFO, ERROR
    val message: String,            // Description of log
    val logType: String = "SYNC",   // PARSING, SYNC, ERROR, SYSTEM
    val timestamp: Long = System.currentTimeMillis()
)
