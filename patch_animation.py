with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "r") as f:
    text = f.read()

text = text.replace(
    '.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))',
    '.animateContentSize(animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing))'
)

text = text.replace(
    '        AnimatedContent(targetState = state) { targetState ->',
    '        Crossfade(targetState = state, animationSpec = tween(100)) { targetState ->'
)

with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "w") as f:
    f.write(text)
