#!/bin/bash
sed -i 's/triggerAtMillis = triggerAtMillis/triggerAt = triggerAtMillis/g' app/src/main/java/com/example/data/repository/JarvisRepository.kt
sed -i 's/isCompleted = false/status = "pending"/g' app/src/main/java/com/example/data/repository/JarvisRepository.kt
sed -i 's/reminder.copy(isCompleted = true)/reminder.copy(status = "completed")/g' app/src/main/java/com/example/data/repository/JarvisRepository.kt

sed -i 's/triggerAtMillis = triggerAt/triggerAt = triggerAt/g' app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt
sed -i 's/isCompleted = false/status = "pending"/g' app/src/main/java/com/example/automation/actions/ScheduleMessageAction.kt
