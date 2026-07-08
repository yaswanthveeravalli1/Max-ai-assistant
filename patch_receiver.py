with open("app/src/main/java/com/example/receiver/ReminderReceiver.kt", "r") as f:
    text = f.read()

replacement = """
                        if (!reminder.automationTarget.isNullOrEmpty() && !reminder.automationMessage.isNullOrEmpty()) {
                            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                            val securitySettings = com.example.security.SecuritySettings(context)
                            
                            val executeAutomation = {
                                val json = org.json.JSONObject()
                                val directJson = org.json.JSONObject()
                                directJson.put("type", reminder.automationType)
                                directJson.put("contact", reminder.automationTarget)
                                directJson.put("message", reminder.automationMessage)
                                directJson.put("is_scheduled", true)
                                json.put("direct_automation", directJson)
                                com.example.automation.engine.AutomationEngine.dispatch(context, json)
                            }
"""

import re
# Find the block inside the `if` from line 36 to line 51
text = re.sub(r'                        if \(!reminder\.automationTarget\.isNullOrEmpty\(\) && !reminder\.automationMessage\.isNullOrEmpty\(\)\) \{.*?val executeAutomation = \{.*?\}\n', replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/receiver/ReminderReceiver.kt", "w") as f:
    f.write(text)
