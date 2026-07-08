with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "r") as f:
    text = f.read()

import re

# Add animateColor import
if 'import androidx.compose.animation.animateColor' not in text:
    text = text.replace('import androidx.compose.animation.core.*', 'import androidx.compose.animation.core.*\nimport androidx.compose.animation.animateColor')

with open("app/src/main/java/com/example/overlay/DynamicIslandManager.kt", "w") as f:
    f.write(text)
