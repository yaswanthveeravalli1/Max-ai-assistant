package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessage
import com.example.data.local.Reminder
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JarvisRepository(application)
    val settings = repository.getSettings()

    val messagesState: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val remindersState: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val rulesState: StateFlow<List<com.example.data.local.AutoReplyRule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state states
    var inputText by mutableStateOf("")
    var isSending by mutableStateOf(false)
        private set

    var isCheckingHealth by mutableStateOf(false)
        private set
    var backendHealthStatus by mutableStateOf("Not checked")
        private set

    val hasCompletedOnboarding: Boolean
        get() = settings.hasCompletedOnboarding

    fun completeOnboarding(name: String) {
        settings.userName = name
        settings.hasCompletedOnboarding = true
    }

    fun updateCloudAiModel(model: String) {
        settings.cloudAiModel = model
        val intent = android.content.Intent(getApplication(), com.example.service.JarvisForegroundService::class.java).apply {
            action = "ACTION_UPDATE_CLOUD_MODEL"
            putExtra("model", model)
        }
        getApplication<Application>().startService(intent)
    }

    var memoryForgetResult by mutableStateOf<String?>(null)
        private set

    var screenDumpJson by mutableStateOf("")
        private set

    var isListening by mutableStateOf(false)
        private set

    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    private var lastOnResult: ((String) -> Unit)? = null

    fun initSpeechRecognizer() {
        if (speechRecognizer == null) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(getApplication())
        }
    }

    fun startListening(onResult: ((String) -> Unit)? = null) {
        if (onResult != null) {
            lastOnResult = onResult
        }
        val recognizer = speechRecognizer ?: return
        isListening = true

        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    android.speech.SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                    android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                    android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
                    else -> "Speech recognizer error"
                }
                android.util.Log.e("JarvisViewModel", "STT Error: $errorMsg")
            }
            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    lastOnResult?.invoke(matches[0])
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun captureScreenDump() {
        val service = com.example.service.JarvisAccessibilityService.instance
        screenDumpJson = if (service != null) {
            service.dumpScreenToJson()
        } else {
            "Accessibility Service is Inactive. Please enable it in Settings to dump screen nodes."
        }
    }

    init {
        // Initial health check if server mode is set
        if (!settings.isLocalAiMode) {
            checkBackendHealth()
        }
        
        // Initialize SpeechRecognizer
        initSpeechRecognizer()
        
        // Re-schedule all pending reminders on startup so they survive app restarts!
        viewModelScope.launch {
            reSchedulePendingAlarms()
        }
    }


    fun speak(text: String) {
        repository.speak(text)
    }

    fun sendMessage(isVoice: Boolean = false) {
        val text = inputText.trim()
        if (text.isEmpty() || isSending) return

        inputText = ""
        isSending = true

        viewModelScope.launch {
            try {
                com.example.core.JarvisCore.processCommand(getApplication(), text, if (isVoice) com.example.core.InputSource.VOICE else com.example.core.InputSource.CHAT)
            } catch (e: Exception) {
            } finally {
                isSending = false
            }
        }
    }

    fun addReminder(message: String, triggerAtMillis: Long) {
        viewModelScope.launch {
            repository.addManualReminder(message, triggerAtMillis)
        }
    }

    fun addAutomationReminder(
        message: String,
        triggerAtMillis: Long,
        type: String,
        target: String,
        msgContent: String,
        repeatType: String,
        isEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            repository.addManualAutomationReminder(message, triggerAtMillis, type, target, msgContent, repeatType, isEnabled)
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun forgetMemory(keyword: String) {
        memoryForgetResult = "Forgetting \"$keyword\"..."
        viewModelScope.launch {
            val result = repository.forgetMemory(keyword)
            memoryForgetResult = result
        }
    }

    fun clearForgetResult() {
        memoryForgetResult = null
    }

    fun checkBackendHealth() {
        isCheckingHealth = true
        viewModelScope.launch {
            val status = repository.checkBackendHealth()
            backendHealthStatus = status
            isCheckingHealth = false
        }
    }

    fun registerToken(token: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.registerFCMToken(token)
            onComplete(success)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateIsLocalAiMode(isLocal: Boolean) {
        settings.isLocalAiMode = isLocal
        if (!isLocal) {
            checkBackendHealth()
        }
    }

    fun updateBackendUrl(url: String) {
        settings.backendUrl = url
        checkBackendHealth()
    }

    fun updateUserId(userId: String) {
        settings.userId = userId
    }

    fun updateGeminiApiKey(key: String) {
        settings.geminiApiKey = key
    }

    fun updateLanguagePreference(lang: String) {
        settings.languagePreference = lang
    }

    fun updateBotPersona(persona: String) {
        settings.botPersona = persona
    }

    fun updateTelegramBotEnabled(enabled: Boolean) {
        settings.isTelegramBotEnabled = enabled
    }

    fun updateCloudBotUrl(url: String) {
        settings.cloudBotUrl = url
    }

    fun updateCloudAppSecret(secret: String) {
        settings.cloudAppSecret = secret
    }

    fun addAutoReplyRule(trigger: String, reply: String, targetContact: String? = null) {
        viewModelScope.launch {
            repository.insertRule(com.example.data.local.AutoReplyRule(
                triggerKeyword = trigger,
                replyMessage = reply,
                targetContact = targetContact
            ))
        }
    }

    fun deleteAutoReplyRule(id: Int) {
        viewModelScope.launch {
            repository.deleteRuleById(id)
        }
    }

    fun updateAutoReplyEnabled(enabled: Boolean) {
        settings.isAutoReplyEnabled = enabled
    }

    fun updateAiFallbackEnabled(enabled: Boolean) {
        settings.isAiFallbackEnabled = enabled
    }

    fun updateAiAllowedContacts(contacts: String) {
        settings.aiAllowedContacts = contacts
    }

    fun updateQuickReplies(replies: String) {
        settings.quickReplies = replies
    }

    private suspend fun reSchedulePendingAlarms() {
        // Fetches pending reminders and schedules alarms in system
        // Ensures reboot / app restart alarm survivability!
        val db = com.example.data.local.AppDatabase.getDatabase(getApplication())
        val pending = db.reminderDao().getPendingReminders()
        val now = System.currentTimeMillis()
        for (reminder in pending) {
            if (reminder.triggerAt > now) {
                repository.scheduleSystemAlarm(reminder.id, reminder.message, reminder.triggerAt)
            } else {
                // If the app was offline and missed the exact time, complete it now!
                repository.markReminderCompleted(reminder.id)
            }
        }
    }
}
