package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.FoodItem

class ExpiryNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "channel_expiry_alerts"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_expiry_name)
            val descriptionText = context.getString(R.string.channel_expiry_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyExpiringItems(items: List<FoodItem>): Int {
        val now = System.currentTimeMillis()
        val expiringOrExpired = items.filter { item ->
            item.daysUntilExpiry(now) <= 3
        }

        if (expiringOrExpired.isEmpty()) {
            return 0
        }

        val expiredCount = expiringOrExpired.count { it.isExpired(now) }
        val expiringCount = expiringOrExpired.size - expiredCount

        val title = if (expiredCount > 0) {
            "هشدار: $expiredCount قلم منقضی شده و $expiringCount قلم در آستانه انقضا!"
        } else {
            "توجه: $expiringCount قلم خوراکی در آستانه انقضا در یخچال/انبار!"
        }

        val itemNames = expiringOrExpired.take(4).joinToString("، ") { item ->
            val days = item.daysUntilExpiry(now)
            if (days < 0) "${item.name} (منقضی)"
            else if (days == 0L) "${item.name} (امروز)"
            else "${item.name} ($days روز)"
        }

        val content = if (expiringOrExpired.size > 4) {
            "$itemNames و ${expiringOrExpired.size - 4} قلم دیگر را پیش از خراب شدن مصرف کنید."
        } else {
            "برای جلوگیری از هدر رفت، $itemNames را زودتر مصرف کنید."
        }

        sendNotification(title, content)
        return expiringOrExpired.size
    }

    private fun sendNotification(title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (_: SecurityException) {
            // Handled when permission is not granted
        }
    }
}
