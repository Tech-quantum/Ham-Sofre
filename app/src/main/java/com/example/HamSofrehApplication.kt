package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.notification.ExpiryNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HamSofrehApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val notificationManager by lazy { ExpiryNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
        // Database triggers initial seed on first create
    }
}
