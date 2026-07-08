import sys

with open('app/src/main/java/com/example/service/JarvisSpeaker.kt', 'r') as f:
    code = f.read()

old_done = """                    override fun onDone(id: String?) {
                        isProcessingSpeech = false
                        Handler(Looper.getMainLooper()).post {
                            if (ConversationSessionManager.isSessionActive()) {
                                VoskManager.startCommandListening()
                            } else {
                                VoskManager.startWakeWordListening()
                            }
                        }
                    }"""

new_done = """                    override fun onDone(id: String?) {
                        isProcessingSpeech = false
                        VoskManager.onTTSDone()
                    }"""

code = code.replace(old_done, new_done)

with open('app/src/main/java/com/example/service/JarvisSpeaker.kt', 'w') as f:
    f.write(code)
