package com.example.ai

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryLoader {
    private const val TAG = "MemoryLoader"
    private var cachedMemory: String? = null

    fun getMemory(context: Context): String {
        if (cachedMemory != null) return cachedMemory!!

        val memoryFiles = listOf(
            "memory/profile/user_profile.md",
            "memory/profile/current_context.md",
            "memory/profile/birthdays.md",
            "memory/profile/academics.md",
            "memory/relationships/jayasri_relationship.md",
            "memory/relationships/sarika_relationship.md"
        )

        val memoryBuilder = java.lang.StringBuilder()
        memoryBuilder.append("--- USER KNOWLEDGE BASE (EXTERNAL MEMORY) ---\n")

        for (path in memoryFiles) {
            try {
                val inputStream = context.assets.open(path)
                val reader = BufferedReader(InputStreamReader(inputStream))
                memoryBuilder.append("### FILE: $path ###\n")
                memoryBuilder.append(reader.readText())
                memoryBuilder.append("\n\n")
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load memory file: $path", e)
            }
        }

        memoryBuilder.append("--- END OF USER KNOWLEDGE BASE ---\n")
        cachedMemory = memoryBuilder.toString()
        return cachedMemory!!
    }
}
