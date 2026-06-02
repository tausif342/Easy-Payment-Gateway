package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String, // e.g. UUID or custom string "proj_1"
    val name: String,           // e.g. "eCommerce Web App"
    val websiteUrl: String,     // e.g. "https://example.com"
    val timestamp: Long = System.currentTimeMillis()
)
