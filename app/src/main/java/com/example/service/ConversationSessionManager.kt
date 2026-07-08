package com.example.service

object ConversationSessionManager {
    var sessionActive = false
    var lastInteraction = 0L

    fun startSession() {
        sessionActive = true
        lastInteraction = System.currentTimeMillis()
    }

    fun updateInteraction() {
        lastInteraction = System.currentTimeMillis()
    }

    fun isSessionActive(): Boolean {
        return sessionActive && (System.currentTimeMillis() - lastInteraction < 15000)
    }
    
    fun endSession() {
        sessionActive = false
    }
}
