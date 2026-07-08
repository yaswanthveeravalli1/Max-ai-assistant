package com.example.automation.actions

import android.content.Context
import android.content.Intent
import org.json.JSONObject
import com.example.service.AutomationTask
import com.example.service.JarvisAccessibilityService

class SendMessageAction : BaseAction<JSONObject>() {
    override fun execute(context: Context, payload: JSONObject) {
        val contact = payload.optString("contact")
        val message = payload.optString("message")

        val controller = com.example.automation.WhatsAppController(isScheduled = false)
        controller.execute(context, contact, message)
    }
}
