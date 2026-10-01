package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "خواروبار",
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val location: String = "FRIDGE", // FRIDGE, FREEZER, PANTRY
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val expiryDateMillis: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val price: Long = 0L,
    val purchasedByMemberId: Long = 0L,
    val notes: String = ""
) {
    // Days remaining until expiration (negative means expired)
    fun daysUntilExpiry(nowMillis: Long = System.currentTimeMillis()): Long {
        val diff = expiryDateMillis - nowMillis
        return diff / (24L * 60 * 60 * 1000)
    }

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return expiryDateMillis < nowMillis
    }

    fun isExpiringSoon(thresholdDays: Int = 3, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val days = daysUntilExpiry(nowMillis)
        return days in 0..thresholdDays
    }
}
