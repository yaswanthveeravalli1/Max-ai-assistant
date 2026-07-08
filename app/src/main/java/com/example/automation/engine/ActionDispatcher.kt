package com.example.automation.engine

import android.content.Context
import android.util.Log
import com.example.automation.actions.*
import org.json.JSONObject

object ActionDispatcher {
    private const val TAG = "ActionDispatcher"

    // Polymorphic action registry
    private val actionMap = mapOf(
        "OPEN_APP" to OpenAppAction(),
        "SYSTEM_ACTION" to SystemAction(),
        "PERFORM_BACK" to SystemAction(),
        "PERFORM_HOME" to SystemAction(),
        "PERFORM_RECENT_APPS" to SystemAction(),
        "FLASHLIGHT_ON" to FlashlightAction(true),
        "FLASHLIGHT_OFF" to FlashlightAction(false),
        "GET_TIME" to TimeAction(),
        "SEND_MESSAGE" to SendMessageAction(),
        "FORCE_STOP" to ShizukuAction(),
        "DISABLE_APP" to ShizukuAction(),
        "UNINSTALL_APP" to ShizukuAction(),
        "SHIZUKU_SHELL" to ShizukuAction()
    )

    fun dispatch(context: Context, json: JSONObject) {
        // 1. Check for specific complex automations first
        if (json.has("direct_automation") && !json.isNull("direct_automation")) {
            val directObj = json.optJSONObject("direct_automation")
            if (directObj != null) {
                DirectAutomationAction().execute(context, directObj)
                return
            }
        }
        
        if (json.has("schedule_automation") && !json.isNull("schedule_automation")) {
            val scheduleObj = json.optJSONObject("schedule_automation")
            if (scheduleObj != null) {
                ScheduleMessageAction().execute(context, scheduleObj)
                return
            }
        }
        
        if (json.has("shizuku_action") && !json.isNull("shizuku_action")) {
            val sysObj = json.optJSONObject("shizuku_action")
            if (sysObj != null) {
                ShizukuAction().execute(context, sysObj)
                return
            }
        }
        
        if (json.has("steps") && json.optJSONArray("steps")?.length() ?: 0 > 0) {
            val stepsArr = json.optJSONArray("steps")
            if (stepsArr != null) {
                AccessibilityStepsAction().execute(context, stepsArr)
                return
            }
        }

        // 2. Fall back to simple legacy actions
        var actionName = json.optString("action", "NONE")
        if (actionName == "NONE") return

        when (actionName) {
            "PERFORM_BACK" -> { actionName = "SYSTEM_ACTION"; json.put("system_action_str", "back") }
            "PERFORM_HOME" -> { actionName = "SYSTEM_ACTION"; json.put("system_action_str", "home") }
            "PERFORM_RECENT_APPS" -> { actionName = "SYSTEM_ACTION"; json.put("system_action_str", "recent") }
            "TAKE_SCREENSHOT", "SCREENSHOT" -> { actionName = "SYSTEM_ACTION"; json.put("system_action_str", "screenshot") }
        }

        val action = actionMap[actionName]
        if (action != null) {
            action.execute(context, json)
        } else {
            Log.w(TAG, "Unknown action: $actionName")
        }
    }
}
