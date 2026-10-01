package com.example.data.model

data class SmartShoppingSuggestion(
    val name: String,
    val suggestedQuantity: Double = 1.0,
    val unit: String = "بسته",
    val category: String = "خواروبار",
    val reason: String,
    val consumptionCount: Int = 1,
    val currentStock: Double = 0.0,
    val isUrgentRecommendation: Boolean = false
)
