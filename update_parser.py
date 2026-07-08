import sys

with open('app/src/main/java/com/example/service/VoiceCommandParser.kt', 'r') as f:
    code = f.read()

code = code.replace("fun execute(context: Context, intent: VoiceIntent) {", "fun execute(context: Context, intent: VoiceIntent): Boolean {")
code = code.replace("        when (intent) {", "        return when (intent) {")
code = code.replace("context.startActivity(launchIntent)\n                }", "context.startActivity(launchIntent)\n                    true\n                } else false")
code = code.replace("JarvisSpeaker.speak(message)\n            }", "JarvisSpeaker.speak(message)\n                false\n            }")


with open('app/src/main/java/com/example/service/VoiceCommandParser.kt', 'w') as f:
    f.write(code)
