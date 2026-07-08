package com.example.ai

import android.content.Context
import com.example.BuildConfig
import com.example.data.api.GroqMessage
import com.example.data.api.GroqRequest
import com.example.data.api.GroqRetrofitBuilder
import com.example.data.preferences.SettingsManager

class GroqProvider : BaseAIProvider() {
    override suspend fun executePrompt(context: Context, command: String, prompt: String): String {
        val settings = SettingsManager(context)
        val apiKey = settings.geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
        
        if (!apiKey.startsWith("gsk_")) {
            throw IllegalArgumentException("Not a Groq API key")
        }

        val request = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage(role = "system", content = prompt),
                GroqMessage(role = "user", content = command)
            ),
            response_format = mapOf("type" to "json_object")
        )

        val response = GroqRetrofitBuilder.service.generateContent("Bearer $apiKey", request)
        return response.choices?.firstOrNull()?.message?.content ?: ""
    }
}
