with open("app/src/main/java/com/example/service/VoiceSessionManager.kt", "r") as f:
    text = f.read()

replacement = """        appContext?.let { ctx ->
            checkToneGenerator()
            ConversationSessionManager.updateInteraction()

            val securitySettings = com.example.security.SecuritySettings(ctx)
            val codeWord = securitySettings.voiceUnlockCodeWord.lowercase()
            val km = ctx.getSystemService(android.app.KeyguardManager::class.java)
            val isUnlockCommand = text.lowercase().contains(codeWord) || 
                                  text.lowercase().contains("unlock") || 
                                  text.lowercase().contains("open my phone")
                                  
            if (km.isKeyguardLocked && isUnlockCommand) {
                if (securitySettings.autoUnlockEnabled) {
                    val pin = securitySettings.getDecryptedPin()
                    if (pin.isNotEmpty()) {
                        val service = com.example.service.JarvisAccessibilityService.instance
                        if (service != null) {
                            com.example.security.UnlockManager(ctx).wakeScreen()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                service.unlockWithPin(pin) {
                                    try {
                                        val cleanCommand = text.lowercase().replace(codeWord, "").trim()
                                        if (cleanCommand.isNotEmpty() && cleanCommand != "unlock" && cleanCommand != "open my phone") {
                                            com.example.core.JarvisCore.processCommand(ctx, cleanCommand, com.example.core.InputSource.VOICE)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Post-unlock brain execution failed", e)
                                    }
                                }
                            }, 500)
                            return@let
                        } else {
                            Log.e(TAG, "Accessibility service is null. Cannot perform automation.")
                        }
                    } else {
                        Log.e(TAG, "Decrypted PIN is empty")
                    }
                } else {
                    Log.e(TAG, "Auto Unlock is disabled in settings")
                }
            }

            CoroutineScope(Dispatchers.IO).launch {"""

import re
text = re.sub(r'        appContext\?\.let \{ ctx ->\s*checkToneGenerator\(\)\s*ConversationSessionManager\.updateInteraction\(\)\s*CoroutineScope\(Dispatchers\.IO\)\.launch \{', replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/VoiceSessionManager.kt", "w") as f:
    f.write(text)
