import sys

with open('app/src/main/java/com/example/service/JarvisSpeaker.kt', 'r') as f:
    code = f.read()

code = code.replace("""                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        Handler(Looper.getMainLooper()).post {
                            if (ConversationSessionManager.isSessionActive()) {
                                VoskManager.startCommandListening()
                            } else {
                                VoskManager.startWakeWordListening()
                            }
                        }
                    }
                    override fun onError(id: String?) {}
                })""", """                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        isProcessingSpeech = false
                        Handler(Looper.getMainLooper()).post {
                            if (ConversationSessionManager.isSessionActive()) {
                                VoskManager.startCommandListening()
                            } else {
                                VoskManager.startWakeWordListening()
                            }
                        }
                    }
                    override fun onError(id: String?) {
                        isProcessingSpeech = false
                    }
                })""")

code = code.replace("""    fun speak(text: String) {
        if (isInitialized) {
            VoskManager.stopListening()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        } else {
            Log.w("JarvisSpeaker", "TTS not initialized yet")
        }
    }""", """    fun speak(text: String) {
        if (isInitialized) {
            isProcessingSpeech = true
            VoskManager.stopListening()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        } else {
            Log.w("JarvisSpeaker", "TTS not initialized yet")
        }
    }""")

with open('app/src/main/java/com/example/service/JarvisSpeaker.kt', 'w') as f:
    f.write(code)
