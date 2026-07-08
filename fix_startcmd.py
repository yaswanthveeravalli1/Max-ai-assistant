import re
with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'r') as f:
    text = f.read()

func = """
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

"""

# Insert it before processCommandText
text = text.replace("    private fun processCommandText(text: String) {", func + "    private fun processCommandText(text: String) {")

with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'w') as f:
    f.write(text)
