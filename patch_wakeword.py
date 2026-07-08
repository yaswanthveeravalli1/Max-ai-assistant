with open("app/src/main/java/com/example/service/VoiceSessionManager.kt", "r") as f:
    text = f.read()

import re

# Patch isWakeWordDetected
new_is_wake_word = """    private fun isWakeWordDetected(text: String): Boolean {
        val t = text.lowercase()
        return (t.contains("hey") || t.contains("heyy")) && 
               (t.contains("max") || t.contains("macs") || t.contains("mex"))
    }"""

text = re.sub(
    r'    private fun isWakeWordDetected\(text: String\): Boolean \{\s*val t = text\.lowercase\(\)\s*return \(t\.contains\("hey"\) \|\| t\.contains\("heyy"\)\) && \s*\(t\.contains\("bittu"\) \|\| t\.contains\("bitu"\) \|\| t\.contains\("beta"\) \|\| t\.contains\("bit too"\) \|\| t\.contains\("bit"\)\)\s*\}',
    new_is_wake_word,
    text
)

# Patch recognizer setup
text = text.replace(
    '// Wake word grammar: Only listen for "hey bittu" variations or unknowns',
    '// Wake word grammar: Only listen for "hey max" variations or unknowns'
)

text = text.replace(
    'val recognizer = Recognizer(model, 16000.0f, "[\\"hey\\", \\"heyy\\", \\"bittu\\", \\"bitu\\", \\"beta\\", \\"bit\\", \\"too\\", \\"[unk]\\"]")',
    'val recognizer = Recognizer(model, 16000.0f, "[\\"hey\\", \\"heyy\\", \\"max\\", \\"macs\\", \\"mex\\", \\"[unk]\\"]")'
)

text = text.replace(
    'Log.d(TAG, "Started listening for wake word \'hey bittu\'")',
    'Log.d(TAG, "Started listening for wake word \'hey max\'")'
)

with open("app/src/main/java/com/example/service/VoiceSessionManager.kt", "w") as f:
    f.write(text)
