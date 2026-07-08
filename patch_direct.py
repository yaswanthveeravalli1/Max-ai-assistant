with open("app/src/main/java/com/example/automation/actions/DirectAutomationAction.kt", "r") as f:
    text = f.read()

text = text.replace("val message = payload.optString(\"message\")", "val message = payload.optString(\"message\")\n        val isScheduled = payload.optBoolean(\"is_scheduled\", false)")

text = text.replace("com.example.automation.WhatsAppController()", "com.example.automation.WhatsAppController(isScheduled = isScheduled)")
text = text.replace("com.example.automation.TelegramController()", "com.example.automation.TelegramController(isScheduled = isScheduled)")
text = text.replace("com.example.automation.InstagramController()", "com.example.automation.InstagramController(isScheduled = isScheduled)")

with open("app/src/main/java/com/example/automation/actions/DirectAutomationAction.kt", "w") as f:
    f.write(text)
