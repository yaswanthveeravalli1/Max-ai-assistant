import re
with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'r') as f:
    text = f.read()

new_func = """    private fun processCommandText(text: String) {
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
    }"""

text = re.sub(r'    private fun processCommandText\(text: String\) \{.*?\n    \}\n', new_func + '\n', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'w') as f:
    f.write(text)
