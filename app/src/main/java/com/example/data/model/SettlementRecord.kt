package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlement_records")
data class SettlementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amount: Long,
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)
