#!/bin/bash
set -e

DIR="app/src/main/java/com/example/automation/actions"

cat << 'INNER_EOF' > $DIR/JarvisAction.kt
package com.example.automation.actions

import android.content.Context

interface JarvisAction<T> {
    fun execute(context: Context, payload: T)
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/BaseAction.kt
package com.example.automation.actions

import android.util.Log

abstract class BaseAction<T> : JarvisAction<T> {
    protected val TAG = this::class.java.simpleName

    protected fun log(message: String) {
        Log.d(TAG, message)
    }
    
    protected fun logError(message: String, e: Exception? = null) {
        Log.e(TAG, message, e)
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/OpenAppAction.kt
package com.example.automation.actions

import android.content.Context
import android.content.Intent
import org.json.JSONObject

class OpenAppAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val appName = payload.optString("app")
        val pkg = payload.optString("pkg").ifEmpty {
            when (appName.lowercase()) {
                "whatsapp" -> "com.whatsapp"
                "instagram" -> "com.instagram.android"
                "telegram" -> "org.telegram.messenger"
                "youtube" -> "com.google.android.youtube"
                "chrome", "browser", "google" -> "com.android.chrome"
                "spotify", "music" -> "com.spotify.music"
                "maps", "navigation", "navigate" -> "com.google.android.apps.maps"
                "messages", "messaging" -> "com.google.android.apps.messaging"
                else -> ""
            }
        }
        
        if (pkg.isNotEmpty()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            } else {
                logError("App not installed: $pkg")
            }
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/SystemAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONObject
import com.example.service.JarvisAccessibilityService

class SystemAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val actionName = payload.optString("system_action_str")
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            when (actionName.lowercase()) {
                "back" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                "home" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                "recent" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/FlashlightAction.kt
package com.example.automation.actions

import android.content.Context
import android.hardware.camera2.CameraManager
import org.json.JSONObject

class FlashlightAction(private val state: Boolean) : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, state)
        } catch (e: Exception) {
            logError("Failed to toggle flashlight", e)
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/TimeAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONObject

class TimeAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        // Handled by speech_reply now, but kept for legacy
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/SendMessageAction.kt
package com.example.automation.actions

import android.content.Context
import android.content.Intent
import org.json.JSONObject
import com.example.service.AutomationTask
import com.example.service.JarvisAccessibilityService

class SendMessageAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val contact = payload.optString("contact")
        val message = payload.optString("message")

        JarvisAccessibilityService.pendingTask = AutomationTask(
            type = "SEND_MESSAGE_DEEP_LINK",
            appName = "WhatsApp",
            contact = contact,
            message = message,
            step = 0
        )
        
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/DirectAutomationAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONObject

class DirectAutomationAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val type = payload.optString("type")
        val contact = payload.optString("contact")
        val message = payload.optString("message")
        
        if (contact.isNotEmpty() && message.isNotEmpty()) {
            val controller = when (type.lowercase()) {
                "whatsapp" -> com.example.automation.WhatsAppController()
                "telegram" -> com.example.automation.TelegramController()
                "instagram" -> com.example.automation.InstagramController()
                else -> com.example.automation.WhatsAppController()
            }
            controller.execute(context, contact, message)
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/ScheduleMessageAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONObject
import com.example.data.local.AppDatabase
import com.example.data.local.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleMessageAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val type = payload.optString("type")
        val contact = payload.optString("contact")
        val message = payload.optString("message")
        val triggerAt = payload.optLong("trigger_at_millis", 0)
        
        if (contact.isNotEmpty() && message.isNotEmpty() && triggerAt > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                db.reminderDao().insertReminder(
                    Reminder(
                        message = "Send $type to $contact: $message",
                        triggerAtMillis = triggerAt,
                        isCompleted = false
                    )
                )
                // Need to set Android AlarmManager here
                com.example.utils.NotificationHelper.scheduleReminder(context, "Send to $contact", message, triggerAt)
            }
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/ShizukuAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONObject

class ShizukuAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val action = payload.optString("action")
        val config = payload.optString("config")
        
        when (action.uppercase()) {
            "DISABLE_APP" -> com.example.automation.ShizukuExecutor.runCommand("pm disable-user --user 0 $config")
            "UNINSTALL_APP" -> com.example.automation.ShizukuExecutor.runCommand("pm uninstall --user 0 $config")
            "FORCE_STOP" -> com.example.automation.ShizukuExecutor.runCommand("am force-stop $config")
            "SHIZUKU_SHELL" -> com.example.automation.ShizukuExecutor.runCommand(config)
        }
    }
}
INNER_EOF

cat << 'INNER_EOF' > $DIR/AccessibilityStepsAction.kt
package com.example.automation.actions

import android.content.Context
import org.json.JSONArray

class AccessibilityStepsAction : BaseAction<JSONArray>() {
    override fun execute(context: Context, payload: JSONArray) {
        // Convert JSON array to list of steps, trigger accessibility task
        // We'll set this to JarvisAccessibilityService
        com.example.service.JarvisAccessibilityService.pendingStepsArray = payload
    }
}
INNER_EOF

