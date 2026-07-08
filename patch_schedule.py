with open("app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt", "r") as f:
    text = f.read()

replacement = """                    Reminder(
                        message = "Send $type to $contact: $message",
                        triggerAt = triggerAt,
                        status = "pending",
                        automationType = type.uppercase(),
                        automationTarget = contact,
                        automationMessage = message
                    )"""

import re
text = re.sub(r'                    Reminder\(\s*message = "Send \$type to \$contact: \$message",\s*triggerAt = triggerAt,\s*status = "pending"\s*\)', replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt", "w") as f:
    f.write(text)
