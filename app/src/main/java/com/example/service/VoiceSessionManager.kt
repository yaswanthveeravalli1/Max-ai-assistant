package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object VoiceSessionManager {
    private const val TAG = "VoiceSessionManager"
    
    enum class State {
        UNINITIALIZED, UNPACKING, READY, ERROR, LISTENING_WAKE_WORD, LISTENING_COMMAND
    }
    
    private val _state = MutableStateFlow(State.UNINITIALIZED)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var androidSpeechRecognizer: SpeechRecognizer? = null
    private var isCommandProcessing = false
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private var appContext: Context? = null
    private var commandTimeoutHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null

    fun initModel(context: Context) {
        if (_state.value == State.UNPACKING || _state.value == State.READY) return
        
        appContext = context.applicationContext
        
        checkToneGenerator()
        
        _state.value = State.UNPACKING
        Log.d(TAG, "Unpacking Vosk model from assets...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelDir = File(context.filesDir, "model_en_us")
                val readmeFile = File(modelDir, "README")
                
                Log.d("VOSK_PATH", modelDir.absolutePath)
                
                if (!readmeFile.exists()) {
                    Log.d(TAG, "Model not found in internal storage, copying from assets...")
                    modelDir.mkdirs()
                    copyAssetFolder(context, "model", modelDir)
                } else {
                    Log.d(TAG, "Model already exists, skipping copy.")
                }
                
                Log.d("VOSK_README", File(modelDir, "README").exists().toString())
                
                val m = Model(modelDir.absolutePath)
                
                withContext(Dispatchers.Main) {
                    model = m
                    _state.value = State.READY
                    Log.d(TAG, "Vosk model successfully unpacked and loaded.")
                    Log.d("VOSK_DEBUG", "Model loaded successfully! Auto-starting wake word listening. State = ${_state.value}")
                    startWakeWordListening()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VOSK_FATAL", "Model creation failed", e)
                    _errorMessage.value = e.stackTraceToString()
                    _state.value = State.ERROR
                }
            }
        }
    }

    private fun copyAssetFolder(context: Context, sourceAsset: String, targetFolder: File) {
        val assets = context.assets.list(sourceAsset) ?: return
        if (assets.isEmpty()) return
        
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        
        for (asset in assets) {
            val assetPath = if (sourceAsset.isEmpty()) asset else "$sourceAsset/$asset"
            val targetFile = File(targetFolder, asset)
            
            val subAssets = context.assets.list(assetPath)
            if (subAssets != null && subAssets.isNotEmpty()) {
                copyAssetFolder(context, assetPath, targetFile)
            } else {
                try {
                    context.assets.open(assetPath).use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy asset: $assetPath", e)
                }
            }
        }
    }

    private fun checkToneGenerator() {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ToneGenerator", e)
            }
        }
    }

    private fun isWakeWordDetected(text: String): Boolean {
        val t = text.lowercase()
        return (t.contains("hey") || t.contains("heyy")) && 
               (t.contains("max") || t.contains("macs") || t.contains("mex"))
    }

    private var wakeWordTriggered = false

    fun startWakeWordListening() {
        if (model == null) {
            Log.e(TAG, "Model not loaded yet")
            _errorMessage.value = "Model not loaded yet"
            _state.value = State.ERROR
            return
        }
        if (_state.value == State.LISTENING_WAKE_WORD) {
            Log.d(TAG, "Already listening for wake word, ignoring start request")
            return
        }

        wakeWordTriggered = false
        stopListening()
        checkToneGenerator()
        
        try {
            // Wake word grammar: Only listen for "hey max" variations or unknowns
            val recognizer = Recognizer(model, 16000.0f, "[\"hey\", \"heyy\", \"max\", \"macs\", \"mex\", \"[unk]\"]")
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String) {
                    val text = parseJsonString(hypothesis)
                    if (isWakeWordDetected(text)) {
                        if (wakeWordTriggered) return
                        wakeWordTriggered = true
                        Log.d(TAG, "WAKE WORD DETECTED IN PARTIAL!")
                        ConversationSessionManager.startSession()
                        VoiceOutputManager.speak("Yes boss")
                    }
                }

                override fun onResult(hypothesis: String) {
                    val text = parseJsonString(hypothesis)
                    if (isWakeWordDetected(text)) {
                        if (wakeWordTriggered) return
                        wakeWordTriggered = true
                        Log.d(TAG, "WAKE WORD DETECTED IN RESULT!")
                        ConversationSessionManager.startSession()
                        VoiceOutputManager.speak("Yes boss")
                    }
                }

                override fun onFinalResult(hypothesis: String) {}
                override fun onError(e: Exception) {
                    Log.e(TAG, "Vosk wake word error", e)
                    _errorMessage.value = "Wake word error: ${e.message}"
                    _state.value = State.ERROR
                    restartSafely()
                }
                override fun onTimeout() {
                    Log.d(TAG, "Vosk wake word timeout")
                    restartSafely()
                }
            })
            _state.value = State.LISTENING_WAKE_WORD
            Log.d(TAG, "Started listening for wake word 'hey max'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word listening", e)
            _errorMessage.value = "Start listening failed: ${e.message}"
            _state.value = State.ERROR
            restartSafely()
        }
    }

    private fun restartSafely() {
        stopListening()
        mainHandler.postDelayed({
            startWakeWordListening()
        }, 1000)
    }

    fun returnToWakeWord() {
        Log.d(TAG, "Returning to wake mode")
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer stopped")
                it.cancel()
            }
            
            isCommandProcessing = false
            commandTimeoutHandler?.removeCallbacksAndMessages(null)
            _recognizedText.value = ""
            
            _state.value = State.READY
            
            startWakeWordListening()
        }
    }
    
    fun onTTSDone() {
        Log.d(TAG, "TTS Done")
        isCommandProcessing = false
        mainHandler.post {
            if (ConversationSessionManager.isSessionActive()) {
                startCommandListening()
            } else {
                startWakeWordListening()
            }
        }
    }


    private fun startCommandListening() {
        appContext?.let { ctx ->
            mainHandler.post {
                try {
                    _state.value = State.LISTENING_COMMAND
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                    if (androidSpeechRecognizer == null) {
                        androidSpeechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(ctx)
                        androidSpeechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                            override fun onReadyForSpeech(params: android.os.Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                android.util.Log.e(TAG, "SpeechRecognizer Error: $error")
                                restartSafely()
                            }
                            override fun onResults(results: android.os.Bundle?) {
                                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    processCommandText(matches[0])
                                } else {
                                    restartSafely()
                                }
                            }
                            override fun onPartialResults(partialResults: android.os.Bundle?) {}
                            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                        })
                    }
                    androidSpeechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to start command listening", e)
                    restartSafely()
                }
            }
        }
    }

    private fun processCommandText(text: String) {
        if (isCommandProcessing) return
        if (text.isEmpty()) return
        
        isCommandProcessing = true
        _recognizedText.value = text
        Log.d(TAG, "EXECUTING COMMAND: $text")
        
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer stopped")
                it.cancel()
            }
        }
        
        appContext?.let { ctx ->
            checkToneGenerator()
            ConversationSessionManager.updateInteraction()

            val securitySettings = com.example.security.SecuritySettings(ctx)
            val codeWord = securitySettings.voiceUnlockCodeWord.lowercase()
            val km = ctx.getSystemService(android.app.KeyguardManager::class.java)
            val isUnlockCommand = text.lowercase().contains(codeWord) || 
                                  text.lowercase().contains("unlock") || 
                                  text.lowercase().contains("open my phone")
                                  
            if (km.isKeyguardLocked && isUnlockCommand) {
                if (securitySettings.autoUnlockEnabled) {
                    val pin = securitySettings.getDecryptedPin()
                    if (pin.isNotEmpty()) {
                        val service = com.example.service.JarvisAccessibilityService.instance
                        if (service != null) {
                            com.example.security.UnlockManager(ctx).wakeScreen()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                service.unlockWithPin(pin) {
                                    try {
                                        val cleanCommand = text.lowercase().replace(codeWord, "").trim()
                                        if (cleanCommand.isNotEmpty() && cleanCommand != "unlock" && cleanCommand != "open my phone") {
                                            com.example.core.JarvisCore.processCommand(ctx, cleanCommand, com.example.core.InputSource.VOICE)
                                        } else {
                                            VoiceOutputManager.speak("Phone unlocked.")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Post-unlock brain execution failed", e)
                                        VoiceOutputManager.speak("Sorry boss, I failed to unlock the device.")
                                    }
                                }
                            }, 500)
                            return@let
                        } else {
                            Log.e(TAG, "Accessibility service is null. Cannot perform automation.")
                        }
                    } else {
                        Log.e(TAG, "Decrypted PIN is empty")
                    }
                } else {
                    Log.e(TAG, "Auto Unlock is disabled in settings")
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.core.JarvisCore.processCommand(ctx, text, com.example.core.InputSource.VOICE)
                } catch (e: Exception) {
                    Log.e(TAG, "AI Brain Router failed", e)
                    withContext(Dispatchers.Main) {
                        VoiceOutputManager.speak("Sorry boss")
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK)
                    }
                }
            }
        }
    }

    fun stopListening() {
        Log.d(TAG, "Wake word stopped")
        speechService?.let {
            it.stop()
        }
        
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer stopped")
                it.cancel()
            }
        }
        
        toneGenerator?.release()
        toneGenerator = null
        
        if (_state.value == State.LISTENING_WAKE_WORD || _state.value == State.LISTENING_COMMAND) {
             _state.value = State.READY
        }
    }

    fun destroy() {
        stopListening()
        speechService?.shutdown()
        speechService = null
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer destroyed")
                it.destroy()
            }
            androidSpeechRecognizer = null
        }
    }

    private fun parseJsonString(json: String): String {
        return try {
            JSONObject(json).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}
