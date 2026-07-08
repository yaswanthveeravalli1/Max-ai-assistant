import sys

with open('app/src/main/java/com/example/service/VoskManager.kt', 'r') as f:
    code = f.read()

old_block = """            CoroutineScope(Dispatchers.IO).launch {
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
            }"""

new_block = """            CoroutineScope(Dispatchers.IO).launch {
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
            }"""

code = code.replace(old_block, new_block)

with open('app/src/main/java/com/example/service/VoskManager.kt', 'w') as f:
    f.write(code)
