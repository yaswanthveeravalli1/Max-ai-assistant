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
                "whatsapp business" -> "com.whatsapp.w4b"
                "instagram" -> "com.instagram.android"
                "threads" -> "com.instagram.barcelona"
                "telegram" -> "org.telegram.messenger"
                "youtube" -> "com.google.android.youtube"
                "chrome", "browser", "google" -> "com.android.chrome"
                "brave" -> "com.brave.browser"
                "firefox" -> "org.mozilla.firefox"
                "chatgpt" -> "com.openai.chatgpt"
                "claude" -> "com.anthropic.claude"
                "grok" -> "ai.x.grok"
                "gemini" -> "com.google.android.apps.bard"
                "deepseek", "deepc" -> "com.deepseek.chat"
                "spotify", "music" -> "com.spotify.music"
                "maps", "navigation", "navigate" -> "com.google.android.apps.maps"
                "gmail", "mail" -> "com.google.android.gm"
                "photos", "gallery", "google photos" -> "com.google.android.apps.photos"
                "camera" -> "com.android.camera"
                "dialer", "phone call", "phone", "telephone" -> "com.android.dialer"
                "contacts" -> "com.google.android.contacts"
                "settings" -> "com.android.settings"
                "termux", "termax" -> "com.termux"
                "snapseed", "snap seed" -> "com.niksoftware.snapseed"
                "linkedin" -> "com.linkedin.android"
                "discord" -> "com.discord"
                "reddit" -> "com.reddit.frontpage"
                "drive", "google drive" -> "com.google.android.apps.docs"
                "google files", "files" -> "com.google.android.apps.nbu.files"
                "calculator" -> "com.google.android.calculator"
                "calendar" -> "com.google.android.calendar"
                "keep", "notes", "google keep" -> "com.google.android.keep"
                "messages", "messaging" -> "com.google.android.apps.messaging"
                "my jio", "jio" -> "com.jio.myjio"
                "play store", "store" -> "com.android.vending"
                "signal" -> "org.thoughtcrime.securesms"
                "snapchat" -> "com.snapchat.android"
                "tasks", "google tasks" -> "com.google.android.apps.tasks"
                "x", "twitter" -> "com.twitter.android"
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
