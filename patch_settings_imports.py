with open("app/src/main/java/com/example/ui/screens/SettingsTab.kt", "r") as f:
    text = f.read()

import re

# Add imports if missing
if 'import android.content.Intent' not in text:
    text = text.replace('import android.content.Context', 'import android.content.Context\nimport android.content.Intent')

if 'import androidx.compose.foundation.clickable' not in text:
    text = text.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.clickable')

with open("app/src/main/java/com/example/ui/screens/SettingsTab.kt", "w") as f:
    f.write(text)
