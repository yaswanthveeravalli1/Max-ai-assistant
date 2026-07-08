import re

with open('app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt', 'r') as f:
    content = f.read()

# Replace the sendMessage function
new_func = """    fun sendMessage(isVoice: Boolean = false) {
        val text = inputText.trim()
        if (text.isEmpty() || isSending) return

        inputText = ""
        isSending = true

        viewModelScope.launch {
            try {
                com.example.core.JarvisCore.processCommand(getApplication(), text, if (isVoice) com.example.core.InputSource.VOICE else com.example.core.InputSource.CHAT)
            } catch (e: Exception) {
            } finally {
                isSending = false
            }
        }
    }"""

content = re.sub(r'    fun sendMessage\(.*?\).*?isSending = false\n            }\n        }\n    }', new_func, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt', 'w') as f:
    f.write(content)
