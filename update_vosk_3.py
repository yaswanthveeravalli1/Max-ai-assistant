import sys

with open('app/src/main/java/com/example/service/VoskManager.kt', 'r') as f:
    code = f.read()

old_process = """    private fun processCommandText(text: String) {
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
                var launchedExternalApp = false
                try {
                    val intent = VoiceCommandParser.parseCommand(text)
                    if (intent != null) {
                        launchedExternalApp = withContext(Dispatchers.Main) {
                            VoiceCommandParser.execute(ctx, intent)
                        }
                    } else {
                        try {
                            launchedExternalApp = AIBrainRouter.process(ctx, text)
                        } catch (e: Exception) {
                            Log.e(TAG, "AI Brain Router failed", e)
                            withContext(Dispatchers.Main) {
                                JarvisSpeaker.speak("Sorry boss")
                                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK)
                            }
                        }
                    }
                } finally {
                    Log.d(TAG, "Command finished, launchedExternal=$launchedExternalApp")
                    if (launchedExternalApp) {
                        mainHandler.postDelayed({
                            returnToWakeWord()
                        }, 1500)
                    } else {
                        returnToWakeWord()
                    }
                }
            }
        } ?: run {
            returnToWakeWord()
        }
    }"""

new_process = """    private fun processCommandText(text: String) {
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
                    mainHandler.postDelayed({
                        if (!JarvisSpeaker.isProcessingSpeech && !JarvisSpeaker.isSpeaking()) {
                            returnToWakeWord()
                        }
                    }, 1500)
                }
            }
        } ?: run {
            returnToWakeWord()
        }
    }"""

code = code.replace(old_process, new_process)

old_return = """    fun returnToWakeWord() {
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
            
            if (JarvisSpeaker.isSpeaking() || JarvisSpeaker.isProcessingSpeech) {
                Log.d(TAG, "Jarvis is speaking, deferring wake word restart to JarvisSpeaker")
            } else {
                mainHandler.postDelayed({
                    startWakeWordListening()
                }, 800)
            }
        }
    }"""

new_return = """    fun returnToWakeWord() {
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
    }"""
    
code = code.replace(old_return, new_return)

with open('app/src/main/java/com/example/service/VoskManager.kt', 'w') as f:
    f.write(code)
