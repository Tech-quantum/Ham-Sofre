package com.example.data.prediction

import com.example.data.model.ConsumptionLog
import com.example.data.model.FoodItem
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

enum class RepurchaseReason(val faTitle: String, val description: String) {
    DEPLETION("اتمام موجودی در اثر مصرف", "موجودی این قلم زودتر از موعد انقضا تمام خواهد شد."),
    EXPIRATION("انقضای تاریخ مصرف", "با نرخ فعلی، این خوراکی قبل از مصرف منقضی می‌شود و باید جایگزین شود."),
    OUT_OF_STOCK("موجودی ناموجود", "این خوراکی در انبار تمام شده و باید فوراً خریداری شود.")
}

enum class PredictionUrgency(val faTitle: String) {
    CRITICAL("بحرانی (ظرف ۴۸ ساعت)"),
    MODERATE("متوسط (۳ تا ۵ روز آینده)"),
    NORMAL("مناسب (بیش از ۵ روز)")
}

data class RepurchasePrediction(
    val foodItem: FoodItem,
    val currentStock: Double,
    val unit: String,
    val dailyConsumptionRate: Double, // واحدهای مصرف‌شده در هر ۲۴ ساعت
    val stockThreshold: Double,       // آستانه موجودی مجاز بر پایه نرخ مصرف (حداقل مصرف ۲.۵ روزه)
    val isBelowThreshold: Boolean,    // آیا موجودی فعلی کمتر از آستانه پیش‌بینی‌شده است؟
    val daysUntilDepletion: Double?,  // روزهای باقیمانده تا خالی شدن بر اثر مصرف
    val daysUntilExpiry: Long,        // روزهای باقیمانده تا تاریخ انقضا
    val predictedRepurchaseDateMillis: Long, // تاریخ دقیق پیشنهادی برای خرید بعدی
    val daysUntilRepurchase: Int,     // چند روز تا موعد خرید مجدد باقی مانده
    val reason: RepurchaseReason,
    val urgency: PredictionUrgency,
    val recommendedPurchaseQuantity: Double, // حجم پیشنهادی خرید (دوره مصرف ۷ روزه)
    val explanationNote: String
)

object PantryRepurchasePredictor {

    /**
     * پیش‌بینی زمان دقیق نیاز به خرید مجدد بر اساس داده‌های موجود در Room:
     * - اقلام انبار (FoodItem) شامل موجودی فعلی و تاریخ انقضا
     * - تاریخچه مصرف (ConsumptionLog) برای استخراج نرخ مصرف واقعی در روز
     */
    fun predictRepurchaseDates(
        foodItems: List<FoodItem>,
        consumptionLogs: List<ConsumptionLog>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RepurchasePrediction> {
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        return foodItems.map { item ->
            // 1. استخراج سوابق مصرف مربوط به این قلم (بر اساس id یا نام خوراکی)
            val itemLogs = consumptionLogs.filter { log ->
                log.foodItemId == item.id || log.foodItemName.equals(item.name, ignoreCase = true)
            }

            // 2. محاسبه نرخ مصرف روزانه (Daily Consumption Rate)
            val dailyRate: Double = if (itemLogs.isNotEmpty()) {
                val totalConsumed = itemLogs.sumOf { it.quantity }
                val earliestLogTime = itemLogs.minOf { it.timestampMillis }
                val elapsedDays = max(1.0, (nowMillis - earliestLogTime).toDouble() / oneDayMillis)
                // نرخ مصرف روزانه محاسبه شده
                (totalConsumed / elapsedDays).coerceAtLeast(0.05)
            } else {
                // اگر هنوز لاگ مصرف ثبت نشده، تخمین بر اساس مدت زمان نگهداری تا انقضا
                val daysSincePurchase = max(1.0, (nowMillis - item.purchaseDateMillis).toDouble() / oneDayMillis)
                val totalExpectedDays = max(3.0, (item.expiryDateMillis - item.purchaseDateMillis).toDouble() / oneDayMillis)
                (item.quantity / totalExpectedDays).coerceIn(0.1, 2.0)
            }

            val daysUntilExpiry = item.daysUntilExpiry(nowMillis)
            val currentStock = item.quantity
            // حد آستانه ایمن موجودی: معادل ۲.۵ روز مصرف روزانه
            val stockThreshold = (dailyRate * 2.5).coerceAtLeast(0.5)

            // 3. بررسی شرایط اتمام یا انقضا
            if (currentStock <= 0.0) {
                // موجودی کاملاً تمام شده
                RepurchasePrediction(
                    foodItem = item,
                    currentStock = 0.0,
                    unit = item.unit,
                    dailyConsumptionRate = dailyRate,
                    stockThreshold = stockThreshold,
                    isBelowThreshold = true,
                    daysUntilDepletion = 0.0,
                    daysUntilExpiry = daysUntilExpiry,
                    predictedRepurchaseDateMillis = nowMillis,
                    daysUntilRepurchase = 0,
                    reason = RepurchaseReason.OUT_OF_STOCK,
                    urgency = PredictionUrgency.CRITICAL,
                    recommendedPurchaseQuantity = recommendQuantity(dailyRate, item.unit),
                    explanationNote = "موجودی این قلم در انبار صفر است و باید فوراً خریداری شود."
                )
            } else {
                // روزهای باقیمانده تا اتمام فیزیکی موجودی
                val daysToDepletion = currentStock / dailyRate

                // مقایسه: آیا زودتر تمام می‌شود یا زودتر منقضی می‌شود؟
                val willExpireBeforeDepletion = daysUntilExpiry >= 0 && daysUntilExpiry.toDouble() < daysToDepletion

                val (daysToRepurchaseDouble, reason) = if (daysUntilExpiry < 0) {
                    // قبلاً منقضی شده
                    Pair(0.0, RepurchaseReason.EXPIRATION)
                } else if (willExpireBeforeDepletion) {
                    // قبل از مصرف کامل تاریخش تمام می‌شود
                    Pair(daysUntilExpiry.toDouble(), RepurchaseReason.EXPIRATION)
                } else {
                    // موجودی به اتمام می‌رسد
                    Pair(daysToDepletion, RepurchaseReason.DEPLETION)
                }

                val daysToRepurchaseInt = ceil(daysToRepurchaseDouble).toInt().coerceAtLeast(0)
                val targetRepurchaseTimestamp = nowMillis + (daysToRepurchaseInt.toLong() * oneDayMillis)

                val isBelowThreshold = currentStock <= stockThreshold || daysToRepurchaseInt <= 2 || daysUntilExpiry <= 2

                val urgency = when {
                    daysToRepurchaseInt <= 2 -> PredictionUrgency.CRITICAL
                    daysToRepurchaseInt in 3..5 -> PredictionUrgency.MODERATE
                    else -> PredictionUrgency.NORMAL
                }

                val roundedRate = String.format(java.util.Locale.US, "%.2f", dailyRate)
                val note = when (reason) {
                    RepurchaseReason.DEPLETION ->
                        "با نرخ مصرف $roundedRate ${item.unit} در روز، ظرف $daysToRepurchaseInt روز آینده تمام می‌شود."
                    RepurchaseReason.EXPIRATION ->
                        if (daysUntilExpiry < 0) "این قلم منقضی شده و باید جایگزین شود."
                        else "قبل از اتمام کامل، ظرف $daysToRepurchaseInt روز دیگر منقضی می‌شود."
                    RepurchaseReason.OUT_OF_STOCK ->
                        "موجودی تمام شده است."
                }

                RepurchasePrediction(
                    foodItem = item,
                    currentStock = currentStock,
                    unit = item.unit,
                    dailyConsumptionRate = dailyRate,
                    stockThreshold = stockThreshold,
                    isBelowThreshold = isBelowThreshold,
                    daysUntilDepletion = daysToDepletion,
                    daysUntilExpiry = daysUntilExpiry,
                    predictedRepurchaseDateMillis = targetRepurchaseTimestamp,
                    daysUntilRepurchase = daysToRepurchaseInt,
                    reason = reason,
                    urgency = urgency,
                    recommendedPurchaseQuantity = recommendQuantity(dailyRate, item.unit),
                    explanationNote = note
                )
            }
        }.sortedWith(compareBy({ it.daysUntilRepurchase }, { it.urgency.ordinal }))
    }

    private fun recommendQuantity(dailyRate: Double, unit: String): Double {
        // پیشنهاد حجم خرید متناسب با چرخه تأمین ۷ روزه
        val rawQty = dailyRate * 7.0
        return when (unit) {
            "عدد", "بسته", "قوطی", "شانه" -> ceil(rawQty).coerceAtLeast(1.0)
            "کیلوگرم", "لیتر" -> (ceil(rawQty * 2.0) / 2.0).coerceAtLeast(0.5)
            "گرم" -> (ceil(rawQty / 50.0) * 50.0).coerceAtLeast(100.0)
            else -> ceil(rawQty).coerceAtLeast(1.0)
        }
    }
}
