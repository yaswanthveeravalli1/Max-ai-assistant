with open("app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt", "r") as f:
    text = f.read()

replacement = """
                val reminderId = db.reminderDao().insertReminder(
                    Reminder(
                        message = "Send $type to $contact: $message",
                        triggerAt = triggerAt,
                        status = "pending",
                        automationType = type.uppercase(),
                        automationTarget = contact,
                        automationMessage = message
                    )
                )
                
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val intent = android.content.Intent(context, com.example.receiver.ReminderReceiver::class.java).apply {
                        putExtra("reminder_id", reminderId.toInt())
                        putExtra("message", "Send $type to $contact: $message")
                    }
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        reminderId.toInt(),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
"""

import re
# Replace the insertReminder block and comments after it
text = re.sub(r'                db\.reminderDao\(\)\.insertReminder\([\s\S]*?//com\.example\.utils\.NotificationHelper\.scheduleReminder\(context, "Send to \$contact", message, triggerAt\)', replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt", "w") as f:
    f.write(text)
