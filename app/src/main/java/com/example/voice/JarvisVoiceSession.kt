package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log

class JarvisVoiceSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("JarvisVoiceSession", "onShow() called, launching custom transparent Voice Activity...")
        
        try {
            val intent = Intent(context, JarvisVoiceTriggerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startAssistantActivity(intent)
        } catch (e: Exception) {
            Log.e("JarvisVoiceSession", "Failed to start custom voice trigger activity", e)
        }
        
        // Finish the session background wrapper so the custom HUD activity has proper focus
        finish()
    }
}
