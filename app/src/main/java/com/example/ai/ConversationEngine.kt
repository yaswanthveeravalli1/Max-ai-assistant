package com.example.ai

import android.content.Context
import com.example.core.InputSource
import com.example.core.JarvisCore
import com.example.core.JarvisResponse
import com.example.automation.engine.AutomationEngine
import com.example.ai.AIBrainRouter
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ConversationEngine {

    suspend fun process(context: Context, text: String, source: InputSource) {
        // 1. Save user message to history
        val db = AppDatabase.getDatabase(context)
        db.chatMessageDao().insertMessage(ChatMessage(sender = "user", text = text))

        // 2. Call AI Provider
        val jsonResult = AIBrainRouter.process(context, text)
        
        val isAutomation = jsonResult.optBoolean("is_automation", false)
        val reply = jsonResult.optString("speech_reply", "")
        
        // 3. Save assistant response to history
        if (reply.isNotBlank()) {
            db.chatMessageDao().insertMessage(ChatMessage(sender = "jarvis", text = reply))
            
            // 4. Emit response back to Core
            JarvisCore.emitResponse(JarvisResponse(reply, source, isAutomation))
        }

        // 5. Send to Automation Engine
        if (isAutomation || hasAutomationFields(jsonResult)) {
            withContext(Dispatchers.Main) {
                AutomationEngine.dispatch(context, jsonResult)
            }
        }
    }
    
    private fun hasAutomationFields(json: JSONObject): Boolean {
        return (json.has("direct_automation") && !json.isNull("direct_automation")) ||
               (json.has("schedule_automation") && !json.isNull("schedule_automation")) ||
               (json.has("system_action") && !json.isNull("system_action")) ||
               (json.has("steps") && (json.optJSONArray("steps")?.length() ?: 0) > 0) ||
               (json.has("action") && !json.isNull("action") && json.optString("action") != "NONE")
    }
}
