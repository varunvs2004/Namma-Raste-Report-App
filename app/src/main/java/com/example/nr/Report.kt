package com.example.nr

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(

    @PrimaryKey
    val ticketId: String,
    val userId: String = "",
    val type: String = "Report",
    val severity: String = "Medium",
    val description: String = "",
    val timestamp: Long = 0L,
    val imagePath: String = "",
    val imageUrl: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val status: String = "Pending",
    val statusUpdatedAt: Long = 0L,
    val adminUpdate: String = "",
    val statusHistory: String = ""
)
