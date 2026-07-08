#!/bin/bash
sed -i '/import com.example.brain.JarvisAppBrain/d' app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt
sed -i '/private val brain = JarvisAppBrain/d' app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt
sed -i '/brain = JarvisAppBrain/d' app/src/main/java/com/example/ui/viewmodel/JarvisViewModel.kt

sed -i '/import com.example.brain.JarvisAppBrain/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i '/private lateinit var backgroundBrain: JarvisAppBrain/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i '/backgroundBrain = JarvisAppBrain/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i '/backgroundBrain =/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i '/private lateinit var brain: JarvisAppBrain/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt
sed -i '/brain = JarvisAppBrain(this)/d' app/src/main/java/com/example/voice/JarvisVoiceTriggerActivity.kt

