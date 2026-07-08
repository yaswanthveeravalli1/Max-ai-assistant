with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "r") as f:
    text = f.read()

import re

new_color = """    val glowColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (state) {
            OverlayState.LISTENING -> Color(0xFF00FF7F).copy(alpha = 0.5f)
            OverlayState.PROCESSING -> Color(0xFFA855F7).copy(alpha = 0.5f)
            OverlayState.SPEAKING -> Color(0xFFFF003C).copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(500, easing = LinearEasing),
        label = "glowColor"
    )"""

text = re.sub(r'    val glowColor by infiniteTransition\.animateColor\([\s\S]*?repeatMode = RepeatMode\.Reverse\s*\)\s*\)', new_color, text)

with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "w") as f:
    f.write(text)
