package com.example.voice

import android.service.voice.VoiceInteractionService
import android.util.Log

class JarvisVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.d("JarvisVoiceInteraction", "JarvisVoiceInteractionService is ready!")
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        Log.d("JarvisVoiceInteraction", "Launched from keyguard!")
    }
}
