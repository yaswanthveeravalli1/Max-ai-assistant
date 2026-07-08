with open("app/src/main/java/com/example/ai/BaseAIProvider.kt", "r") as f:
    text = f.read()

text = text.replace(
    '"  \\"action\\": \\"NONE | OPEN_APP | SEND_MESSAGE | GET_TIME | SYSTEM_ACTION | FLASHLIGHT_ON | FLASHLIGHT_OFF\\",\\n" +',
    '"  \\"action\\": \\"NONE | OPEN_APP | SEND_MESSAGE | GET_TIME | SYSTEM_ACTION | FLASHLIGHT_ON | FLASHLIGHT_OFF\\",\\n" +\n                "  \\"steps_supported_actions\\": \\"OPEN_APP, CLICK_ID, CLICK_TEXT, TYPE_TEXT, PRESS_ENTER, WAIT\\",\\n" +'
)

with open("app/src/main/java/com/example/ai/BaseAIProvider.kt", "w") as f:
    f.write(text)
