import re
with open("app/src/main/java/com/example/ai/BaseAIProvider.kt", "r") as f:
    text = f.read()

# I want to add some common package names to the system prompt so the AI can use them.
packages = """
                "Common package names: WhatsApp (com.whatsapp), YouTube (com.google.android.youtube), Spotify (com.spotify.music), Chrome (com.android.chrome), Maps (com.google.android.apps.maps).\\n" +"""

text = text.replace(
    '                "IMPORTANT: Your conversational tone MUST be: ${settings.botPersona}.\\n" +',
    '                "IMPORTANT: Your conversational tone MUST be: ${settings.botPersona}.\\n" +\n' + packages
)

# And I want to add a PRESS_ENTER action in JarvisAccessibilityService, just in case!
with open("app/src/main/java/com/example/ai/BaseAIProvider.kt", "w") as f:
    f.write(text)
