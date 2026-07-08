with open("app/src/main/java/com/example/data/repository/JarvisRepository.kt", "r") as f:
    text = f.read()

text = text.replace('''    suspend fun markReminderCompleted(id: Int) = withContext(Dispatchers.IO) {
        val reminder = reminderDao.getReminderById(id)
        if (reminder != null) {
            reminderDao.updateReminder(reminder.copy(status = "completed"))
        }
    }''', '''    suspend fun markReminderCompleted(id: Int) = withContext(Dispatchers.IO) {
        reminderDao.updateReminderStatus(id, "completed")
    }''')

with open("app/src/main/java/com/example/data/repository/JarvisRepository.kt", "w") as f:
    f.write(text)
