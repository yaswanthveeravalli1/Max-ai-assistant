package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.preferences.SettingsManager
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

abstract class BaseAIProvider : AIProvider {
    protected val TAG = this::class.java.simpleName

    protected fun getPrompt(context: Context): String {
        val settings = SettingsManager(context)
        val userNameStr = if (settings.userName.isNotBlank()) " The user's name is ${settings.userName}." else ""
        val memoryContext = MemoryLoader.getMemory(context)
        return "You are MAX, a highly capable, intelligent personal AI assistant that can execute multi-step device automations using an Accessibility service, and run high-privilege system-level operations using Shizuku (such as disabling, uninstalling, or stopping apps, or running sandboxed shell commands).$userNameStr\n" +
                "$memoryContext\n" +
                "IMPORTANT: You MUST respond using the language: ${settings.languagePreference}.\n" +
                "IMPORTANT: Your conversational tone MUST be: ${settings.botPersona}.\n" +
                "Common package names: WhatsApp (com.whatsapp), YouTube (com.google.android.youtube), Spotify (com.spotify.music), Chrome (com.android.chrome), Maps (com.google.android.apps.maps), Camera (com.google.android.GoogleCamera or com.android.camera), Gallery/Photos (com.google.android.apps.photos), Gmail (com.google.android.gm), Instagram (com.instagram.android), Settings (com.android.settings), Play Store (com.android.vending), X (com.twitter.android), ChatGPT (com.openai.chatgpt), Claude (com.anthropic.claude).\n" +
                "You must ALWAYS respond with a JSON object. Ensure the JSON is valid. The JSON format MUST be:\n" +
                "{\n" +
                "  \"is_automation\": true or false,\n" +
                "  \"speech_reply\": \"your helpful response text or conversational speech reply to the user\",\n" +
                "  \"action\": \"NONE | OPEN_APP | SEND_MESSAGE | GET_TIME | SYSTEM_ACTION | TAKE_SCREENSHOT | FLASHLIGHT_ON | FLASHLIGHT_OFF\",\n" +
                "  \"steps_supported_actions\": \"OPEN_APP, CLICK_ID, CLICK_TEXT, TYPE_TEXT, PRESS_ENTER, WAIT\",\n" +
                "  \"app\": \"app name if OPEN_APP\",\n" +
                "  \"contact\": \"contact name if SEND_MESSAGE\",\n" +
                "  \"message\": \"message if SEND_MESSAGE\",\n" +
                "  \"system_action_str\": \"back, home, recent, screenshot for SYSTEM_ACTION\",\n" +
                "  \"steps\": [\n" +
                "    { \"action\": \"OPEN_APP\", \"pkg\": \"com.whatsapp\" },\n" +
                "    { \"action\": \"CLICK_ID\", \"id\": \"com.whatsapp:id/menuitem_search\" },\n" +
                "    { \"action\": \"TYPE_TEXT\", \"text\": \"Mom\" },\n" +
                "    { \"action\": \"CLICK_TEXT\", \"text\": \"Mom\" },\n" +
                "    { \"action\": \"TYPE_TEXT\", \"text\": \"hi\", \"id\": \"com.whatsapp:id/entry\" },\n" +
                "    { \"action\": \"CLICK_ID\", \"id\": \"com.whatsapp:id/send\" }\n" +
                "  ],\n" +
                "  \"direct_automation\": {\n" +
                "    \"type\": \"WHATSAPP\",\n" +
                "    \"contact\": \"name or number\",\n" +
                "    \"message\": \"message content\"\n" +
                "  },\n" +
                "  \"schedule_automation\": {\n" +
                "    \"type\": \"WHATSAPP | TELEGRAM | REMINDER\",\n" +
                "    \"contact\": \"name or number (leave empty if it's just a personal reminder)\",\n" +
                "    \"message\": \"message content (or reminder content)\",\n" +
                "    \"trigger_at_millis\": 1234567890\n" +
                "  },\n" +
                "  \"shizuku_action\": {\n" +
                "    \"action\": \"DISABLE_APP | UNINSTALL_APP | FORCE_STOP | SHIZUKU_SHELL\",\n" +
                "    \"config\": \"package_name_or_command_to_execute\"\n" +
                "  }\n" +
                "}\n" +
                "If the user wants to schedule a message for later, OR set a personal reminder/alarm, fill the \"schedule_automation\" field. Calculate \"trigger_at_millis\" based on the current time provided below. Otherwise, set \"schedule_automation\" to null.\n" +
                "If the user wants to send a message right now, fill the \"direct_automation\" field. Do NOT use \"steps\" for sending messages. Otherwise, set \"direct_automation\" to null.\n" +
                "If the user wants system tasks executed via Shizuku (e.g. \"Disable Instagram\", toggling Bluetooth), fill the \"shizuku_action\" field.\n" +
                "For simple legacy actions like toggling flashlight, time, going home/back, or opening apps, use the \"action\" string field.\n" +
                "If the user wants a normal conversation, set \"is_automation\" to false.\n" +
                "Current local system time is: " + DateFormat.getDateTimeInstance().format(Date()) + " (" + System.currentTimeMillis() + " ms)."
    }

    protected abstract suspend fun executePrompt(context: Context, command: String, prompt: String): String

    override suspend fun processCommand(context: Context, command: String): JSONObject = withContext(Dispatchers.IO) {
        val prompt = getPrompt(context)
        var responseText = executePrompt(context, command, prompt)
        
        if (responseText.startsWith("```json")) {
            responseText = responseText.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (responseText.startsWith("```")) {
            responseText = responseText.substringAfter("```").substringBeforeLast("```").trim()
        }

        Log.d(TAG, "AI output: $responseText")
        return@withContext JSONObject(responseText)
    }
}
