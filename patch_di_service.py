with open("app/src/main/java/com/example/overlay/DynamicIslandOverlayService.kt", "r") as f:
    text = f.read()

import re

# Remove PROCESSING case from when
text = re.sub(r'VoiceSessionManager\.State\.PROCESSING -> \{\s*overlayManager\.showProcessing\(\)\s*\}', '', text)

with open("app/src/main/java/com/example/overlay/DynamicIslandOverlayService.kt", "w") as f:
    f.write(text)
