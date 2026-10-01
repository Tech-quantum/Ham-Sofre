package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#1B6A45",
    val avatarEmoji: String = "👤",
    val phone: String = "",
    val isMe: Boolean = false
)
