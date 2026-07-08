package com.example.security

import android.content.Context
import android.os.PowerManager
import android.util.Log

class UnlockManager(private val context: Context) {
    @Suppress("DEPRECATION")
    fun wakeScreen() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Jarvis:WakeLock"
            )
            wakeLock.acquire(10000)
        } catch (e: Exception) {
            Log.e("UnlockManager", "Failed to wake screen", e)
        }
    }
}
