import sys

with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'r') as f:
    code = f.read()

code = code.replace("fun execute(context: Context, json: JSONObject) {", "fun execute(context: Context, json: JSONObject): Boolean {")
code = code.replace("openApp(context, app)\n            }", "return openApp(context, app)\n            }")
code = code.replace("sendWhatsApp(context, contact, message)\n            }", "return sendWhatsApp(context, contact, message)\n            }")

code = code.replace("private fun performSystemAction(actionName: String) {", "private fun performSystemAction(actionName: String): Boolean {")
code = code.replace("private fun openApp(context: Context, appName: String) {", "private fun openApp(context: Context, appName: String): Boolean {")
code = code.replace("private fun toggleFlashlight(context: Context, state: Boolean) {", "private fun toggleFlashlight(context: Context, state: Boolean): Boolean {")
code = code.replace("private fun sendWhatsApp(context: Context, contact: String, msg: String) {", "private fun sendWhatsApp(context: Context, contact: String, msg: String): Boolean {")

code = code.replace("                performSystemAction(sysAction)\n            }", "                return performSystemAction(sysAction)\n            }")
code = code.replace("                toggleFlashlight(context, true)\n            }", "                return toggleFlashlight(context, true)\n            }")
code = code.replace("                toggleFlashlight(context, false)\n            }", "                return toggleFlashlight(context, false)\n            }")
code = code.replace("\"PERFORM_BACK\" -> performSystemAction(\"back\")", "\"PERFORM_BACK\" -> return performSystemAction(\"back\")")
code = code.replace("\"PERFORM_HOME\" -> performSystemAction(\"home\")", "\"PERFORM_HOME\" -> return performSystemAction(\"home\")")
code = code.replace("\"PERFORM_RECENT_APPS\" -> performSystemAction(\"recent\")", "\"PERFORM_RECENT_APPS\" -> return performSystemAction(\"recent\")")

code = code.replace("""            "GET_TIME" -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())
                JarvisSpeaker.speak("Time is $time")
            }""", """            "GET_TIME" -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())
                JarvisSpeaker.speak("Time is $time")
                return false
            }""")
code = code.replace("""            "NONE" -> {
                // Conversational only, do nothing
            }""", """            "NONE" -> {
                // Conversational only, do nothing
                return false
            }""")
code = code.replace("""            else -> {
                Log.w(TAG, "Unknown action: $action")
            }""", """            else -> {
                Log.w(TAG, "Unknown action: $action")
                return false
            }""")

# For performSystemAction
code = code.replace("Log.w(TAG, \"Unknown system action: $actionName\")\n            }", "Log.w(TAG, \"Unknown system action: $actionName\")\n            }\n            return true")
code = code.replace("Log.w(TAG, \"JarvisAccessibilityService not available to perform $actionName\")\n        }", "Log.w(TAG, \"JarvisAccessibilityService not available to perform $actionName\")\n        }\n        return false")

# For openApp
code = code.replace("context.startActivity(dialIntent)\n                return", "context.startActivity(dialIntent)\n                return true")
code = code.replace("context.startActivity(smsIntent)\n                    return", "context.startActivity(smsIntent)\n                    return true")
code = code.replace("context.startActivity(launchIntent)\n            } else {", "context.startActivity(launchIntent)\n                return true\n            } else {")
code = code.replace("Log.w(TAG, \"App not installed: $appName\")\n            }", "Log.w(TAG, \"App not installed: $appName\")\n                return false\n            }")
code = code.replace("Log.w(TAG, \"Unsupported app for open: $appName\")\n        }", "Log.w(TAG, \"Unsupported app for open: $appName\")\n            return false\n        }")

# For toggleFlashlight
code = code.replace("cameraManager.setTorchMode(cameraId, state)\n        }", "cameraManager.setTorchMode(cameraId, state)\n            return false\n        }")
code = code.replace("Log.e(TAG, \"Failed to toggle flashlight\", e)\n        }", "Log.e(TAG, \"Failed to toggle flashlight\", e)\n            return false\n        }")

# For sendWhatsApp
code = code.replace("context.startActivity(launchIntent)\n        } else {", "context.startActivity(launchIntent)\n            return true\n        } else {")
code = code.replace("Log.w(TAG, \"WhatsApp not installed\")\n        }", "Log.w(TAG, \"WhatsApp not installed\")\n            return false\n        }")


with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'w') as f:
    f.write(code)
