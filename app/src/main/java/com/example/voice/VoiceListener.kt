package com.example.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.*

class VoiceListener(private val context: Context) {

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        // In Activity use startActivityForResult or launcher
    }
}
