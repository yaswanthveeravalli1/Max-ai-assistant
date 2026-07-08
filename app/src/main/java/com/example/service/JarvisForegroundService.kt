package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

class JarvisForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var telegramManager: com.example.telegram.CloudSocketManager? = null

    companion object {
        private const val TAG = "JarvisForegroundService"
        private const val NOTIFICATION_ID = 4004
        private const val CHANNEL_ID = "jarvis_foreground_service_channel"
        private const val CHANNEL_NAME = "MAX Active Service"

        const val ACTION_START_LISTENING = "com.example.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.ACTION_STOP_LISTENING"
        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
        const val ACTION_RESTART_TELEGRAM = "com.example.ACTION_RESTART_TELEGRAM"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
        JarvisServiceStateManager.setServiceRunning(true)
        createNotificationChannel()
        VoiceOutputManager.init(applicationContext)

        // Acquire partial WakeLock safely
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis::ForegroundWakeLock")
            try {
                wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes safe limit
                Log.d(TAG, "WakeLock acquired successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to acquire WakeLock", e)
            }
        }
        
        // Initialize Vosk Model
        VoiceSessionManager.initModel(this)
        
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, com.example.overlay.DynamicIslandOverlayService::class.java))
        }

        telegramManager = com.example.telegram.CloudSocketManager(this)
        telegramManager?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() called with action: ${intent?.action}")

        val notification = createNotification("MAX Active Service", "Continuous background engine is running.")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service in foreground", e)
        }

        when (intent?.action) {
            ACTION_START_LISTENING -> {
                updateNotification("MAX Listening...", "MAX is listening for hotwords locally.")
                JarvisServiceStateManager.setListening(true)
                if (VoiceSessionManager.state.value == VoiceSessionManager.State.READY || VoiceSessionManager.state.value == VoiceSessionManager.State.LISTENING_COMMAND) {
                    VoiceSessionManager.startWakeWordListening()
                }
            }
            ACTION_STOP_LISTENING -> {
                updateNotification("MAX Idle", "MAX is running in background.")
                JarvisServiceStateManager.setListening(false)
                VoiceSessionManager.stopListening()
            }
            ACTION_STOP_SERVICE -> {
                VoiceSessionManager.stopListening()
                stopSelf()
            }
            ACTION_RESTART_TELEGRAM -> {
                telegramManager?.stop()
                telegramManager = com.example.telegram.CloudSocketManager(this)
                telegramManager?.start()
            }
            "ACTION_UPDATE_CLOUD_MODEL" -> {
                val model = intent?.getStringExtra("model") ?: "groq"
                telegramManager?.updateSettings(model)
            }
            else -> {
                JarvisServiceStateManager.setListening(true)
                if (VoiceSessionManager.state.value == VoiceSessionManager.State.READY) {
                    VoiceSessionManager.startWakeWordListening()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors environment for MAX vocal assistant triggers."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, createNotification(title, text))
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        telegramManager?.stop()
        VoiceSessionManager.destroy()
        VoiceOutputManager.shutdown()

        // Safely release the WakeLock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock safely", e)
        } finally {
            wakeLock = null
        }

        JarvisServiceStateManager.setServiceRunning(false)
        super.onDestroy()
    }
}
