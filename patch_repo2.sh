#!/bin/bash
sed -i 's/val reminder = reminderDao.getReminderById(id)//g' app/src/main/java/com/example/data/repository/JarvisRepository.kt
sed -i 's/if (reminder != null) {//g' app/src/main/java/com/example/data/repository/JarvisRepository.kt
sed -i 's/reminderDao.updateReminder(reminder.copy(status = "completed"))/reminderDao.updateReminderStatus(id, "completed")/g' app/src/main/java/com/example/data/repository/JarvisRepository.kt
# need to remove a dangling brace since I removed the if. Let's just use Python for precision.
