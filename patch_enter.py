with open("app/src/main/java/com/example/service/JarvisAccessibilityService.kt", "r") as f:
    text = f.read()

import re

replacement = """            "TYPE_TEXT" -> {
                val textToType = step.text ?: return
                val idToType = step.id
                val nodes = if (idToType != null) {
                    findNodesWithFallback(root, idToType)
                } else {
                    findFocusedEditText(root)
                }
                if (nodes.isNotEmpty()) {
                    val args = android.os.Bundle().apply {
                        putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                    }
                    nodes[0].performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    android.util.Log.d("JarvisAccessibility", "Typed text '$textToType' successfully")
                    task.currentStepIndex++
                    triggerPostStepDelay(task, 800L) // Wait 800ms for text field to update
                }
            }
            "PRESS_ENTER" -> {
                android.util.Log.d("JarvisAccessibility", "Pressing ENTER via Shizuku")
                try {
                    com.example.automation.ShizukuExecutor.runCommand("input keyevent 66")
                } catch (e: Exception) {
                    android.util.Log.e("JarvisAccessibility", "Failed to press enter", e)
                }
                task.currentStepIndex++
                triggerPostStepDelay(task, 800L)
            }"""

text = re.sub(r'"TYPE_TEXT" -> \{[\s\S]*?task\.currentStepIndex\+\+\s*triggerPostStepDelay\(task, 800L\).*?\}\s*\}', replacement, text)

with open("app/src/main/java/com/example/service/JarvisAccessibilityService.kt", "w") as f:
    f.write(text)
