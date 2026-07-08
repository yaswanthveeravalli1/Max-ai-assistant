package com.example.ai

import android.content.Context
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiRetrofitBuilder
import com.example.data.preferences.SettingsManager
import com.example.BuildConfig

class GeminiProvider : BaseAIProvider() {
    override suspend fun executePrompt(context: Context, command: String, prompt: String): String {
        val settings = SettingsManager(context)
        var apiKey = settings.geminiApiKey

        if (apiKey.startsWith("gsk_") || apiKey.isEmpty()) {
            apiKey = BuildConfig.GEMINI_API_KEY
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = command)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = prompt)))
        )

        val response = GeminiRetrofitBuilder.service.generateContent("gemini-2.5-flash", apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }
}
