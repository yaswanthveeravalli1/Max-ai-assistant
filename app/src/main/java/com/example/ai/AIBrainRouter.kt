package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.preferences.SettingsManager
import org.json.JSONObject

object AIBrainRouter {
    private const val TAG = "AIBrainRouter"

    suspend fun process(context: Context, text: String): JSONObject {
        val settings = SettingsManager(context)
        val apiKey = settings.geminiApiKey
        
        if (apiKey.startsWith("gsk_")) {
            Log.d(TAG, "Using Groq")
            try {
                return GroqProvider().processCommand(context, text)
            } catch (e: Exception) {
                Log.e(TAG, "Groq failed, falling back to Gemini", e)
            }
        }

        Log.d(TAG, "Using Gemini")
        try {
            return GeminiProvider().processCommand(context, text)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini failed", e)
            throw e 
        }
    }
}
