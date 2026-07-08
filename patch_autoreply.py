with open("app/src/main/java/com/example/automation/AutoReplyController.kt", "r") as f:
    text = f.read()

text = text.replace('repository.generateShortReply(message)', 'com.example.ai.AIBrainRouter.process(context, "Respond briefly to this message: $message").optString("speech_reply", "Got it!")')

with open("app/src/main/java/com/example/automation/AutoReplyController.kt", "w") as f:
    f.write(text)
