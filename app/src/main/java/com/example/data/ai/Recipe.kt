package com.example.data.ai

import java.util.UUID

data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val cookingTimeMinutes: Int = 20,
    val difficulty: String = "آسان", // آسان, متوسط, پیشرفته
    val cuisine: String = "ایرانی",
    val expiringIngredientsUsed: List<String> = emptyList(),
    val otherIngredientsUsed: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val tips: String = ""
)
