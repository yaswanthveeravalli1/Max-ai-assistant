package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val user_id: String,
    val message: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val reply: String? = null,
    val response: String? = null, // Support potential fallback naming
    val memory_entries: List<String>? = null
) {
    val finalReply: String
        get() = reply ?: response ?: "No response received"
}

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(
    val user_id: String,
    val fcm_token: String
)

@JsonClass(generateAdapter = true)
data class ForgetRequest(
    val user_id: String,
    val keyword: String
)

@JsonClass(generateAdapter = true)
data class CommonResponse(
    val status: String? = null,
    val message: String? = null
)
