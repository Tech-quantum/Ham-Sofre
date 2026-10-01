package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumption_logs")
data class ConsumptionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodItemId: Long,
    val foodItemName: String,
    val consumedByMemberId: Long,
    val consumedByMemberName: String,
    val quantity: Double,
    val unit: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)
