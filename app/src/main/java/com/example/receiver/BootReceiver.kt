package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Restoring alarms and starting background service...")
            val pendingResult = goAsync()
            
            // Auto-start Jarvis persistent listening service
            try {
                val serviceIntent = Intent(context, com.example.service.JarvisForegroundService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("BootReceiver", "JarvisForegroundService started successfully on boot.")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to auto-start JarvisForegroundService on boot", e)
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val reminderDao = db.reminderDao()
                    val repository = JarvisRepository(context)
                    
                    val pendingReminders = reminderDao.getPendingReminders()
                    val currentTime = System.currentTimeMillis()
                    
                    for (reminder in pendingReminders) {
                        if (reminder.triggerAt > currentTime) {
                            repository.scheduleSystemAlarm(reminder.id, reminder.message, reminder.triggerAt)
                            Log.d("BootReceiver", "Rescheduled reminder ${reminder.id} for ${reminder.triggerAt}")
                        } else {
                            // Optionally mark as completed if missed while powered off
                            reminderDao.updateReminderStatus(reminder.id, "completed")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
