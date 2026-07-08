package com.example.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object VoiceOutputManager : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isProcessingSpeech = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceOutputManager", "The Language not supported!")
            } else {
                isInitialized = true
                                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        isProcessingSpeech = false
                        VoiceSessionManager.onTTSDone()
                    }
                    override fun onError(id: String?) {
                        isProcessingSpeech = false
                    }
                })
            }
        } else {
            Log.e("VoiceOutputManager", "Initialization Failed!")
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun speak(text: String) {
        if (isInitialized) {
            isProcessingSpeech = true
            VoiceSessionManager.stopListening()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        } else {
            Log.w("VoiceOutputManager", "TTS not initialized yet")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
