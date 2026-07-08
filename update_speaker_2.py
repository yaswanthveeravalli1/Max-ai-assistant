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

with open('app/src/main/java/com/example/service/JarvisSpeaker.kt', 'w') as f:
    f.write(code)
