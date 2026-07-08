with open("app/src/main/java/com/example/service/JarvisForegroundService.kt", "r") as f:
    text = f.read()

import re

# We will start the DynamicIslandOverlayService in onCreate of JarvisForegroundService
replacement_oncreate = """        // Initialize Vosk Model
        VoiceSessionManager.initModel(this)
        
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, com.example.overlay.DynamicIslandOverlayService::class.java))
        }
    }"""

text = re.sub(r'        // Initialize Vosk Model\s*VoiceSessionManager\.initModel\(this\)\s*\}', replacement_oncreate, text)

# And stop it in onDestroy
replacement_ondestroy = """    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
        stopService(Intent(this, com.example.overlay.DynamicIslandOverlayService::class.java))"""

text = re.sub(r'    override fun onDestroy\(\) \{\s*super\.onDestroy\(\)\s*Log\.d\(TAG, "onDestroy\(\) called"\)', replacement_ondestroy, text)

with open("app/src/main/java/com/example/service/JarvisForegroundService.kt", "w") as f:
    f.write(text)
