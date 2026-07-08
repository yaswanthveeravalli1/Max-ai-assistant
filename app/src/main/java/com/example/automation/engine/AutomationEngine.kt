package com.example.automation.engine

import android.content.Context
import android.util.Log
import com.example.automation.actions.*
import org.json.JSONObject

object AutomationEngine {
    private const val TAG = "AutomationEngine"

    fun dispatch(context: Context, json: JSONObject) {
        Log.d(TAG, "Dispatching automation: $json")
        ActionDispatcher.dispatch(context, json)
    }
}
