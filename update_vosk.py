import sys

with open('app/src/main/java/com/example/service/VoskManager.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if line.strip() == "private fun restartSafely() {":
        start_idx = i
        break

new_code = """    private fun restartSafely() {
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
            
            if (JarvisSpeaker.isSpeaking()) {
                Log.d(TAG, "Jarvis is speaking, deferring wake word restart to JarvisSpeaker")
            } else {
                mainHandler.postDelayed({
                    startWakeWordListening()
                }, 800)
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

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val intent = VoiceCommandParser.parseCommand(text)
                    if (intent != null) {
                        withContext(Dispatchers.Main) {
                            VoiceCommandParser.execute(ctx, intent)
                            Log.d(TAG, "App launched")
                        }
                    } else {
                        try {
                            AIBrainRouter.process(ctx, text)
                        } catch (e: Exception) {
                            Log.e(TAG, "AI Brain Router failed", e)
                            withContext(Dispatchers.Main) {
                                JarvisSpeaker.speak("Sorry boss")
                                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK)
                            }
                        }
                    }
                } finally {
                    Log.d(TAG, "Command finished")
                    returnToWakeWord()
                }
            }
        } ?: run {
            returnToWakeWord()
        }
    }

    fun startCommandListening() {
        if (model == null) return
        
        Log.d(TAG, "Command started")
        // Stop vosk wake word
        speechService?.let {
            Log.d(TAG, "Wake word stopped")
            it.stop()
        }
        
        checkToneGenerator()

        try {
            Log.d(TAG, "Started listening for full command with Android SpeechRecognizer")
            _state.value = State.LISTENING_COMMAND

            commandTimeoutHandler?.removeCallbacksAndMessages(null)
            commandTimeoutHandler = Handler(Looper.getMainLooper())
            commandTimeoutHandler?.postDelayed({
                if (_state.value == State.LISTENING_COMMAND) {
                    Log.d(TAG, "Command timeout (session end)")
                    ConversationSessionManager.endSession()
                    returnToWakeWord()
                }
            }, 15000)

            mainHandler.post {
                if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                    Log.e(TAG, "SpeechRecognizer is not available on this device.")
                    returnToWakeWord()
                    return@post
                }
                
                if (androidSpeechRecognizer == null) {
                    Log.d(TAG, "SpeechRecognizer created")
                    androidSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                androidSpeechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    
                    override fun onError(error: Int) {
                        Log.e(TAG, "SpeechRecognizer error code: $error")
                        if (!isCommandProcessing) {
                            if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_CLIENT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                ConversationSessionManager.endSession()
                                returnToWakeWord()
                            } else {
                                restartSafely()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            processCommandText(text)
                        } else {
                            if (!isCommandProcessing) {
                                returnToWakeWord()
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _recognizedText.value = text
                            Log.d(TAG, "Command partial: $text")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                androidSpeechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start command listening", e)
            _errorMessage.value = "Start command failed: ${e.message}"
            _state.value = State.ERROR
            restartSafely()
        }
    }

    fun stopListening() {
        Log.d(TAG, "Wake word stopped")
        speechService?.let {
            it.stop()
            it.shutdown()
        }
        speechService = null
        
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
"""

with open('app/src/main/java/com/example/service/VoskManager.kt', 'w') as f:
    f.writelines(lines[:start_idx])
    f.write(new_code)
