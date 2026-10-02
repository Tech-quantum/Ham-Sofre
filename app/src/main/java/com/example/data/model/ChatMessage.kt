package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderMemberId: Long,
    val senderMemberName: String,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // "TEXT", "SHOPPING_REQ", "EXPENSE_ALERT", "FOOD_ALERT"
    val isFromMe: Boolean = false
)
