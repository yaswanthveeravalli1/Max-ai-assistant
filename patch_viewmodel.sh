#!/bin/bash
cat << 'INNER_EOF' > /tmp/viewmodel.patch
--- app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt
+++ app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt
@@ -162,19 +162,12 @@
     fun sendMessage(isVoice: Boolean = false) {
         val text = inputText.trim()
         if (text.isEmpty() || isSending) return
 
         inputText = ""
         isSending = true
 
         viewModelScope.launch {
             try {
-                com.example.core.JarvisCore.processCommand(repository.getContext(), text, com.example.core.InputSource.CHAT)
-                    }
-                }
+                com.example.core.JarvisCore.processCommand(getApplication(), text, if (isVoice) com.example.core.InputSource.VOICE else com.example.core.InputSource.CHAT)
             } catch (e: Exception) {
                 // Handled gracefully inside the repository, but caught here for safety
             } finally {
                 isSending = false
             }
         }
     }
INNER_EOF
patch app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt /tmp/viewmodel.patch
