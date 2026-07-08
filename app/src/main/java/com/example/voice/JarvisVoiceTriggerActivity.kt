package com.example.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

class JarvisVoiceTriggerActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }


        setContent {
            var statusText by remember { mutableStateOf("Listening to your command...") }
            var isListeningState by remember { mutableStateOf(true) }
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            // Custom translucent neon voice trigger overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF111827).copy(alpha = 0.95f), Color(0xFF070B19))
                            ),
                            RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF00D1FF).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title HUD Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00D1FF), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AETHER HUD · VOICE ASSISTANT",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00D1FF),
                                letterSpacing = 2.sp
                            )
                        }

                        // Pulse Wave voice indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            if (isListeningState) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(pulseScale)
                                        .background(Color(0xFF00D1FF).copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                                        .border(1.dp, Color(0xFF00D1FF).copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .scale(pulseScale * 0.9f)
                                        .background(Color(0xFFA855F7).copy(alpha = 0.1f), RoundedCornerShape(38.dp))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFA855F7), Color(0xFF00D1FF))
                                        ),
                                        RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Status subtitle text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speak clearly to execute immediate device actions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Start SpeechRecognizer directly from the UI context
            LaunchedEffect(Unit) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@JarvisVoiceTriggerActivity).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                statusText = "I'm listening..."
                            }
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {
                                statusText = "Processing..."
                                isListeningState = false
                            }
                            override fun onError(error: Int) {
                                Log.e("VoiceActivity", "Recognizer error: $error")
                                com.example.voice.VoiceDiagnosticLogger.logAudioError(this@JarvisVoiceTriggerActivity, error)
                                statusText = "Didn't catch that. Closing..."
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 1500)
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    val command = matches[0]
                                    statusText = "\"$command\""
                                    Log.d("VoiceActivity", "Recognized speech: $command")
                                    com.example.voice.VoiceDiagnosticLogger.logRawAudioInput(this@JarvisVoiceTriggerActivity, command)
                                    
                                    val securitySettings = com.example.security.SecuritySettings(this@JarvisVoiceTriggerActivity)
                                    val codeWord = securitySettings.voiceUnlockCodeWord.lowercase()
                                    val km = getSystemService(android.app.KeyguardManager::class.java)
                                    val isUnlockCommand = command.lowercase().contains(codeWord) || 
                                                          command.lowercase().contains("unlock") || 
                                                          command.lowercase().contains("open my phone")
                                                          
                                    if (km.isKeyguardLocked && isUnlockCommand) {
                                        if (securitySettings.autoUnlockEnabled) {
                                            val pin = securitySettings.getDecryptedPin()
                                            if (pin.isNotEmpty()) {
                                                val service = com.example.service.JarvisAccessibilityService.instance
                                                if (service != null) {
                                                    com.example.voice.VoiceDiagnosticLogger.logTriggerSuccess(this@JarvisVoiceTriggerActivity, command)
                                                    
                                                    // CRITICAL: We MUST finish the activity first so it is not visible and does not cover the lock screen.
                                                    // This allows the accessibility service to interact with the real lock screen PIN pad.
                                                    finish()
                                                    
                                                    val appContext = applicationContext
                                                    // Post a delay to allow the activity to completely close and vanish from the screen,
                                                    // then run the unlock sequence.
                                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                        com.example.security.UnlockManager(appContext).wakeScreen()
                                                        service.unlockWithPin(pin) {
                                                            try {
                                                                val cleanCommand = command.lowercase().replace(codeWord, "").trim()
                                                                if (cleanCommand.isNotEmpty() && cleanCommand != "unlock" && cleanCommand != "open my phone") {
                                                                    com.example.core.JarvisCore.processCommand(this@JarvisVoiceTriggerActivity, cleanCommand, com.example.core.InputSource.HUD)
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.e("VoiceActivity", "Post-unlock brain execution failed", e)
                                                            }
                                                        }
                                                    }, 500)
                                                } else {
                                                    com.example.voice.VoiceDiagnosticLogger.logTriggerFailure(this@JarvisVoiceTriggerActivity, command, "Accessibility service is null. Cannot perform automation.")
                                                    finish()
                                                }
                                                return
                                            } else {
                                                com.example.voice.VoiceDiagnosticLogger.logTriggerFailure(this@JarvisVoiceTriggerActivity, command, "Decrypted PIN is empty")
                                            }
                                        } else {
                                            com.example.voice.VoiceDiagnosticLogger.logTriggerFailure(this@JarvisVoiceTriggerActivity, command, "Auto Unlock is disabled in settings")
                                        }
                                    }
                                    
                                    // Route command to brain!
                                    try {
                                        com.example.core.JarvisCore.processCommand(this@JarvisVoiceTriggerActivity, command, com.example.core.InputSource.HUD)
                                    } catch (e: Exception) {
                                        Log.e("VoiceActivity", "Brain execution failed", e)
                                    }
                                    
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        finish()
                                    }, 1000)
                                } else {
                                    finish()
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {}
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                        
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        }
                        startListening(intent)
                    }
                } catch (e: Exception) {
                    Log.e("VoiceActivity", "Failed to init SpeechRecognizer", e)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
