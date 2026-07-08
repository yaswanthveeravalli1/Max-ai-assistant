import re

with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'r') as f:
    content = f.read()

# Replace the block
new_block = """            CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.core.JarvisCore.processCommand(ctx, text, com.example.core.InputSource.VOICE)
                } catch (e: Exception) {
                    Log.e(TAG, "AI Brain Router failed", e)
                    withContext(Dispatchers.Main) {
                        VoiceOutputManager.speak("Sorry boss")
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK)
                    }
                }
            }"""

content = re.sub(r'            CoroutineScope\(Dispatchers\.IO\)\.launch \{\s*try \{\s*val intent = VoiceCommandParser.*?\}\s*\} catch \(e: Exception\)', new_block + " catch (e: Exception)", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/VoiceSessionManager.kt', 'w') as f:
    f.write(content)
