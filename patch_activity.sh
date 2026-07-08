#!/bin/bash
sed -i 's/backgroundBrain.executeCommand(cleanCommand)/com.example.core.JarvisCore.processCommand(this@JarvisVoiceTriggerActivity, cleanCommand, com.example.core.InputSource.HUD)/g' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i 's/brain.executeCommand(command)/com.example.core.JarvisCore.processCommand(this@JarvisVoiceTriggerActivity, command, com.example.core.InputSource.HUD)/g' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
