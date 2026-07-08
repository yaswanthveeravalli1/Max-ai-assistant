with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "r") as f:
    text = f.read()

text = text.replace(
    'tween(durationMillis = 100, easing = FastOutSlowInEasing)',
    'tween(durationMillis = 0)'
)

text = text.replace(
    'animationSpec = tween(100)',
    'animationSpec = tween(0)'
)

with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "w") as f:
    f.write(text)
