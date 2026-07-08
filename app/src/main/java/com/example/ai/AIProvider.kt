package com.example.ai

import android.content.Context
import org.json.JSONObject

interface AIProvider {
    suspend fun processCommand(context: Context, command: String): JSONObject
}
