package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val category: String = "خواروبار",
    val addedByMemberId: Long = 0L,
    val addedByMemberName: String = "",
    val assignedToMemberId: Long? = null,
    val assignedToMemberName: String? = null,
    val isPurchased: Boolean = false,
    val estimatedPrice: Long = 0L,
    val isUrgent: Boolean = false,
    val createdDateMillis: Long = System.currentTimeMillis()
)
