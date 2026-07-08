package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JarvisViewModel
import rikka.shizuku.Shizuku

@Composable
fun SettingsTab(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Temporary input buffers
    var backendUrlInput by remember { mutableStateOf(viewModel.settings.backendUrl) }
    var userIdInput by remember { mutableStateOf(viewModel.settings.userId) }
    var geminiKeyInput by remember { mutableStateOf(viewModel.settings.geminiApiKey) }
    var forgetKeyword by remember { mutableStateOf("") }
    var tokenRegisterSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isTelegramBotEnabled by remember { mutableStateOf(viewModel.settings.isTelegramBotEnabled) }
    var cloudBotUrlInput by remember { mutableStateOf(viewModel.settings.cloudBotUrl) }
    var cloudAppSecretInput by remember { mutableStateOf(viewModel.settings.cloudAppSecret) }

    val isServiceRunningState = com.example.service.JarvisServiceStateManager.isServiceRunning.collectAsState()
    val isListeningState = com.example.service.JarvisServiceStateManager.isListening.collectAsState()
    val voskState = com.example.service.VoiceSessionManager.state.collectAsState()
    val voskErrorMessage = com.example.service.VoiceSessionManager.errorMessage.collectAsState()
    val recognizedCommand = com.example.service.VoiceSessionManager.recognizedText.collectAsState()

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            com.example.CustomAppIconManager.createShortcut(context, it, "My Jarvis")
            android.widget.Toast.makeText(context, "Shortcut requested! Check your home screen to place it.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java).apply {
                    action = com.example.service.JarvisForegroundService.ACTION_START_LISTENING
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsTab", "Error starting JarvisForegroundService", e)
            }
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required for background listening.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = "MAX Engine Settings",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configure AI routing, servers, and diagnostic tools",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // Routing Toggle Option Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (viewModel.settings.isLocalAiMode) Icons.Default.OfflineBolt else Icons.Default.Cloud,
                            contentDescription = "Routing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Local AI Mode (Gemini)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (viewModel.settings.isLocalAiMode) "Bypasses servers, runs direct from Google AI Studio" else "Talks to Python FastAPI Remote backend",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Switch(
                        checked = viewModel.settings.isLocalAiMode,
                        onCheckedChange = { viewModel.updateIsLocalAiMode(it) },
                        modifier = Modifier.testTag("local_ai_mode_switch")
                    )
                }
            }
        }

        // Cloud AI Model Selection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Model Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cloud Brain Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                var currentModel by remember { mutableStateOf(viewModel.settings.cloudAiModel) }
                // Update local state if it changes in the backend (from telegram sync)
                LaunchedEffect(viewModel.settings.cloudAiModel) {
                    currentModel = viewModel.settings.cloudAiModel
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val models = listOf("groq", "openrouter", "gemini", "cohere")
                    models.forEach { model ->
                        FilterChip(
                            selected = currentModel == model,
                            onClick = { 
                                currentModel = model
                                viewModel.updateCloudAiModel(model)
                            },
                            label = { Text(model.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Assistant Persona Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Persona Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Assistant Persona",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Language Selection
                var currentLanguage by remember { mutableStateOf(viewModel.settings.languagePreference) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Language Preference", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf("English", "Telugu", "Tanglish")
                        languages.forEach { lang ->
                            FilterChip(
                                selected = currentLanguage == lang,
                                onClick = { 
                                    viewModel.updateLanguagePreference(lang)
                                    currentLanguage = lang
                                },
                                label = { Text(lang) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tone Selection
                var currentPersona by remember { mutableStateOf(viewModel.settings.botPersona) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Response Tone", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val personas = listOf("Human Type Talking", "Formal/Robotic")
                        personas.forEach { persona ->
                            FilterChip(
                                selected = currentPersona == persona,
                                onClick = { 
                                    viewModel.updateBotPersona(persona)
                                    currentPersona = persona
                                },
                                label = { Text(persona, fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Custom App Icon Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Custom App Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Custom App Icon / Shortcut",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Pick an image from your gallery (like the portrait you just uploaded) to create a custom shortcut to this app on your home screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Pick Image")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Custom Image from Gallery")
                }
            }
        }

        // Telegram Bot Integration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Telegram Bot Integration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = isTelegramBotEnabled,
                        onCheckedChange = { 
                            isTelegramBotEnabled = it
                            viewModel.updateTelegramBotEnabled(it)
                            if (it) {
                                // Start the service when enabled
                                val serviceIntent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java).apply {
                                    action = com.example.service.JarvisForegroundService.ACTION_START_LISTENING
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            }
                        }
                    )
                }
                
                Text(
                    text = "Control your phone remotely and chat with the AI using Telegram. The App acts as the server directly, ensuring your data stays completely private and runs instantly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                if (isTelegramBotEnabled) {
                    var telegramSavedSuccess by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = cloudBotUrlInput,
                        onValueChange = { 
                            cloudBotUrlInput = it
                            telegramSavedSuccess = false
                        },
                        label = { Text("Cloud Server WebSocket URL (wss://...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = cloudAppSecretInput,
                        onValueChange = { 
                            cloudAppSecretInput = it
                            telegramSavedSuccess = false
                        },
                        label = { Text("Secret Connection Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.updateCloudBotUrl(cloudBotUrlInput.trim())
                            viewModel.updateCloudAppSecret(cloudAppSecretInput.trim())
                            telegramSavedSuccess = true
                            
                            // Restart cloud socket with new credentials
                            try {
                                val serviceIntent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java).apply {
                                    action = com.example.service.JarvisForegroundService.ACTION_RESTART_TELEGRAM
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            } catch (e: Exception) {
                                // Service may not be running yet, that's OK
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (telegramSavedSuccess) "Saved!" else "Save Credentials")
                    }
                }
            }
        }

        // Contextual Config Card
        if (viewModel.settings.isLocalAiMode) {
            // Local AI Configuration (Gemini API Settings)
            var apiKeySavedSuccess by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Google AI Studio / Groq API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Defaults to the secure key configured in Google AI Studio secrets. To use Groq's high-speed inference, paste a Groq API Key starting with 'gsk_'.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = {
                            geminiKeyInput = it
                            apiKeySavedSuccess = false
                        },
                        label = { Text("API Key Override (gsk_... or AIza...)") },
                        placeholder = { Text("AIzaSy...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("gemini_key_input_field"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateGeminiApiKey(geminiKeyInput)
                                apiKeySavedSuccess = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Icon(
                                imageVector = if (apiKeySavedSuccess) Icons.Default.Check else Icons.Default.Save,
                                contentDescription = "Save Key"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (apiKeySavedSuccess) "Key Saved ✅" else "Save API Key")
                        }

                        if (apiKeySavedSuccess) {
                            Text(
                                text = "Saved successfully! ✅",
                                color = Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Remote Python FastAPI Server Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Python Brain (FastAPI) Backend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        label = { Text("Backend URL") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("backend_url_input_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        label = { Text("App User Token (UUID)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("user_id_input_field"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateBackendUrl(backendUrlInput)
                                viewModel.updateUserId(userIdInput)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Configurations")
                        }

                        Button(
                            onClick = { viewModel.checkBackendHealth() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (viewModel.isCheckingHealth) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ping Server")
                            }
                        }
                    }

                    // Backend Ping result status
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.backendHealthStatus.contains("Online"))
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.backendHealthStatus.contains("Online")) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Status icon",
                                tint = if (viewModel.backendHealthStatus.contains("Online")) Color.Green else Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.backendHealthStatus,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Memory Forget Utility Card (FastAPI memory cleanup)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Memory Cleaning Command",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Clean memory items matching a keyword from the central brain databases (triggers remote /forget API):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = forgetKeyword,
                        onValueChange = { forgetKeyword = it },
                        label = { Text("Memory keyword") },
                        placeholder = { Text("e.g. coffee, chess") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("forget_keyword_input_field"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (forgetKeyword.isNotBlank()) {
                                viewModel.forgetMemory(forgetKeyword.trim())
                                forgetKeyword = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp).testTag("forget_memory_submit_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "forget")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Erase")
                    }
                }

                viewModel.memoryForgetResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Result: $result", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { viewModel.clearForgetResult() }, modifier = Modifier.size(16.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "close", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // Notification and FCM simulator card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Notification / Push Diagnostics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Simulated FCM Token:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                SelectionContainerText(text = viewModel.settings.fcmToken)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.registerToken(viewModel.settings.fcmToken) { success ->
                                tokenRegisterSuccess = success
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("register_token_button")
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = "reg", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Register Device", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            // Instant simulation trigger: broadcast directly to ReminderReceiver
                            val triggerIntent = android.content.Intent(
                                context,
                                com.example.receiver.ReminderReceiver::class.java
                            ).apply {
                                putExtra("reminder_id", 9999)
                                putExtra("message", "MAX Simulator: System alert channel online! ✅")
                            }
                            context.sendBroadcast(triggerIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("trigger_test_notification_button")
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "notif", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trigger Alert", fontSize = 11.sp)
                    }
                }

                tokenRegisterSuccess?.let { success ->
                    Text(
                        text = if (success) "Device FCM registration succeeded on server." else "Register failed (Offline or server unreachable).",
                        fontSize = 11.sp,
                        color = if (success) Color.Green else Color.Red
                    )
                }
            }
        }

        // Accessibility Service Card (Feature 9)
        val isAccessibilityActive = remember { mutableStateOf(false) }

        // Shizuku States
        val isShizukuAvailable = remember { mutableStateOf(com.example.automation.ShizukuManager.isShizukuAvailable()) }
        val isShizukuPermissionGranted = remember { mutableStateOf(com.example.automation.ShizukuManager.isPermissionGranted()) }
        var testCommandText by remember { mutableStateOf("") }
        var testCommandOutput by remember { mutableStateOf("") }

        val checkShizuku = {
            isShizukuAvailable.value = com.example.automation.ShizukuManager.isShizukuAvailable()
            isShizukuPermissionGranted.value = com.example.automation.ShizukuManager.isPermissionGranted()
        }

        // Security Settings (Feature 21)
        val securitySettings = remember { com.example.security.SecuritySettings(context) }
        var autoUnlock by remember { mutableStateOf(securitySettings.autoUnlockEnabled) }
        var pin by remember { mutableStateOf(securitySettings.getDecryptedPin()) }
        var codeWord by remember { mutableStateOf(securitySettings.voiceUnlockCodeWord) }
        var autoRelock by remember { mutableStateOf(securitySettings.autoRelockEnabled) }
        var lockDelay by remember { mutableStateOf(securitySettings.lockDelayMs / 1000) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("security_settings_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Scheduled Automation Security",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Allow MAX to wake and unlock your phone to execute scheduled messages.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Unlock for Scheduled Tasks")
                    Switch(
                        checked = autoUnlock,
                        onCheckedChange = { 
                            autoUnlock = it
                            securitySettings.autoUnlockEnabled = it
                        }
                    )
                }

                if (autoUnlock) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { 
                            pin = it
                            securitySettings.encryptedPin = it
                        },
                        label = { Text("Device PIN") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = codeWord,
                        onValueChange = { 
                            codeWord = it
                            securitySettings.voiceUnlockCodeWord = it
                        },
                        label = { Text("Voice Unlock Code Word") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Lock After Task")
                    Switch(
                        checked = autoRelock,
                        onCheckedChange = { 
                            autoRelock = it
                            securitySettings.autoRelockEnabled = it
                            if (it) {
                                val intent = android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, android.content.ComponentName(context, com.example.receiver.MyDeviceAdminReceiver::class.java))
                                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to auto-lock the device after scheduled automations.")
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                if (autoRelock) {
                    OutlinedTextField(
                        value = lockDelay.toString(),
                        onValueChange = { 
                            val v = it.toLongOrNull() ?: 20L
                            lockDelay = v
                            securitySettings.lockDelayMs = v * 1000
                        },
                        label = { Text("Lock Delay (seconds)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Helper function to update state
        val checkAccessibility = {
            val service = "${context.packageName}/com.example.service.JarvisAccessibilityService"
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            isAccessibilityActive.value = enabled?.contains(service) == true
        }

        val isNotificationAccessActive = remember { mutableStateOf(false) }
        val checkNotificationAccess = {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            isNotificationAccessActive.value = enabledListeners?.contains(context.packageName) == true
        }

        val isDefaultAssistant = remember { mutableStateOf(false) }
        val checkDefaultAssistant = {
            isDefaultAssistant.value = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as? android.app.role.RoleManager
                roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT) == true
            } else {
                val setting = android.provider.Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
                setting != null && setting.contains(context.packageName)
            }
        }

        // Check on initial composition
        LaunchedEffect(Unit) {
            checkAccessibility()
            checkShizuku()
            checkNotificationAccess()
            checkDefaultAssistant()
        }

        // Automatic re-checking of all system permissions and roles on app resume
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    checkAccessibility()
                    checkShizuku()
                    checkNotificationAccess()
                    checkDefaultAssistant()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        DisposableEffect(Unit) {
            val binderReceivedListener = Shizuku.OnBinderReceivedListener {
                checkShizuku()
            }
            val binderDeadListener = Shizuku.OnBinderDeadListener {
                checkShizuku()
            }
            val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == com.example.automation.ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE) {
                    isShizukuPermissionGranted.value = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }

            try {
                Shizuku.addBinderReceivedListener(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionListener)
            } catch (e: Exception) {
                android.util.Log.e("SettingsTab", "Failed to add Shizuku listeners", e)
            }

            onDispose {
                try {
                    Shizuku.removeBinderReceivedListener(binderReceivedListener)
                    Shizuku.removeBinderDeadListener(binderDeadListener)
                    Shizuku.removeRequestPermissionResultListener(permissionListener)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsTab", "Failed to remove Shizuku listeners", e)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("accessibility_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Accessibility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Accessibility Automation Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Required to execute WhatsApp, Telegram, and Instagram message automations.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAccessibilityActive.value) "Active (Enabled) ✅" else "Inactive (Disabled) ⚠️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isAccessibilityActive.value) Color.Green else Color.Red
                    )

                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsTab", "Error opening accessibility settings", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibilityActive.value) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("enable_accessibility_button")
                    ) {
                        Text(if (isAccessibilityActive.value) "Configure Service" else "Enable in Settings")
                    }
                }
            }
        }

        // Notification Access Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("notification_access_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Access",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Notification Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Required to read incoming WhatsApp messages for auto-reply.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNotificationAccessActive.value) "Active (Enabled) ✅" else "Inactive (Disabled) ⚠️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isNotificationAccessActive.value) Color.Green else Color.Red
                    )

                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsTab", "Error opening notification settings", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNotificationAccessActive.value) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("enable_notification_access_button")
                    ) {
                        Text(if (isNotificationAccessActive.value) "Configure Service" else "Enable in Settings")
                    }
                }
            }
        }

        // Default Assistant App Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                .testTag("default_assistant_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Default Assistant",
                        tint = Color(0xFF00D1FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Default Digital Assistant App",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure MAX as your system-wide assistant to handle the long-press Home gesture and swipe gestures.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDefaultAssistant.value) "Active (Default Assistant) ✅" else "Inactive (Not Set) ⚠️",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDefaultAssistant.value) Color(0xFF00FF7F) else Color(0xFFFF003C)
                    )

                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as? android.app.role.RoleManager
                                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT)
                                    var currentContext = context
                                    while (currentContext is android.content.ContextWrapper) {
                                        if (currentContext is android.app.Activity) break
                                        currentContext = currentContext.baseContext
                                    }
                                    val activity = currentContext as? android.app.Activity
                                    if (activity != null) {
                                        activity.startActivityForResult(intent, 2002)
                                    } else {
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                } else {
                                    // Fallback to legacy assistant settings
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            android.widget.Toast.makeText(context, "Could not open Assistant settings", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                // Legacy Android version fallback
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Could not open Assistant settings", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDefaultAssistant.value) Color(0xFF111827).copy(alpha = 0.6f) else Color(0xFF00D1FF),
                            contentColor = if (isDefaultAssistant.value) MaterialTheme.colorScheme.secondary else Color(0xFF003543)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isDefaultAssistant.value) "Configure" else "Set Default Assistant")
                    }
                }
            }
        }

        // Battery Optimization
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Battery Optimization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (isIgnoringBatteryOptimizations) "Unrestricted 🔋✅\nYour app can run in the background freely." else "Optimized 🔋⚠️\nAndroid might kill the foreground service. Allow unrestricted usage for continuous listening.",
                    fontSize = 13.sp,
                    color = if (isIgnoringBatteryOptimizations) Color(0xFF00FF7F) else Color(0xFFFF003C)
                )
                if (!isIgnoringBatteryOptimizations) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable Battery Optimization")
                    }
                }
            }
        }

        // Continuous Background Voice Assistant Service Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                .testTag("continuous_voice_service_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Background Listening",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Voice Assistant",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Runs a persistent low-latency foreground service that keeps MAX awake for hotword triggers.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)

                // Dynamic Island / Draw Overlays Permission
                val canDrawOverlays = android.provider.Settings.canDrawOverlays(context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!canDrawOverlays) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Island Overlay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (canDrawOverlays) "Granted ✅" else "Tap to grant permission ⚠️",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canDrawOverlays) Color(0xFF00FF7F) else Color(0xFFFF003C)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)

                // Service & Mic Capture Status Indicator Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Service state:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isServiceRunningState.value) "Active (Vosk Engine) ✅" else "Inactive (Stopped) ⚠️",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunningState.value) Color(0xFF00FF7F) else Color(0xFFFF003C)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vosk Engine Status:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = voskState.value.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (voskState.value == com.example.service.VoiceSessionManager.State.LISTENING_WAKE_WORD) Color(0xFFA855F7) else if (voskState.value == com.example.service.VoiceSessionManager.State.LISTENING_COMMAND) Color(0xFF00FF7F) else if (voskState.value == com.example.service.VoiceSessionManager.State.ERROR) Color.Red else MaterialTheme.colorScheme.secondary
                    )
                }

                if (voskState.value == com.example.service.VoiceSessionManager.State.ERROR && voskErrorMessage.value.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = voskErrorMessage.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }

                if (recognizedCommand.value.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last Detected Command: \"${recognizedCommand.value}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00D1FF)
                    )
                }

                // Removed sound visualizer since Vosk directly uses the microphone.

                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)

                // Interactive control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start / Stop Service Button
                    Button(
                        onClick = {
                            if (isServiceRunningState.value) {
                                // Stop Service
                                val intent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java).apply {
                                    action = com.example.service.JarvisForegroundService.ACTION_STOP_SERVICE
                                }
                                context.startService(intent)
                            } else {
                                // Start Service
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    try {
                                        val intent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsTab", "Error starting service", e)
                                    }
                                } else {
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunningState.value) Color(0xFF111827).copy(alpha = 0.6f) else Color(0xFFA855F7),
                            contentColor = if (isServiceRunningState.value) MaterialTheme.colorScheme.secondary else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isServiceRunningState.value) "Stop Service" else "Start Service")
                    }

                    // Toggle Mic Capture Button
                    if (isServiceRunningState.value) {
                        Button(
                            onClick = {
                                val action = if (isListeningState.value) {
                                    com.example.service.JarvisForegroundService.ACTION_STOP_LISTENING
                                } else {
                                    com.example.service.JarvisForegroundService.ACTION_START_LISTENING
                                }
                                val intent = android.content.Intent(context, com.example.service.JarvisForegroundService::class.java).apply {
                                    this.action = action
                                }
                                context.startService(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListeningState.value) Color(0xFF111827).copy(alpha = 0.6f) else Color(0xFF00D1FF),
                                contentColor = if (isListeningState.value) MaterialTheme.colorScheme.secondary else Color(0xFF003543)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isListeningState.value) "Mute mic" else "Listen mic")
                        }
                    }
                }
            }
        }

        // Shizuku System Power Layer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("shizuku_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Shizuku",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Shizuku System Power Layer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Enables system-level actions like silent app disable, uninstall, force stop, and shell command sandbox.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isShizukuAvailable.value) "Service: Available ✅" else "Service: Not Running ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isShizukuAvailable.value) Color.Green else Color.Red
                        )
                        Text(
                            text = if (isShizukuPermissionGranted.value) "Permission: Granted ✅" else "Permission: Not Granted ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isShizukuPermissionGranted.value) Color.Green else Color.Red
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                checkShizuku()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isShizukuAvailable.value && !isShizukuPermissionGranted.value) {
                            Button(
                                onClick = {
                                    var currentContext = context
                                    while (currentContext is android.content.ContextWrapper) {
                                        if (currentContext is android.app.Activity) break
                                        currentContext = currentContext.baseContext
                                    }
                                    val activity = currentContext as? android.app.Activity
                                    if (activity != null) {
                                        com.example.automation.ShizukuManager.requestPermission(activity)
                                    } else {
                                        android.widget.Toast.makeText(context, "Could not find Activity context", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("request_shizuku_permission_button")
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                if (isShizukuAvailable.value && isShizukuPermissionGranted.value) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                    Text(
                        text = "Interactive Shizuku Sandbox",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = testCommandText,
                        onValueChange = { testCommandText = it },
                        label = { Text("Shell Command") },
                        placeholder = { Text("e.g. pm list packages -d") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            testCommandOutput = "Running..."
                            val result = com.example.automation.ShizukuExecutor.runCommand(testCommandText)
                            testCommandOutput = result
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End).testTag("run_shizuku_cmd_button")
                    ) {
                        Text("Run via Shizuku")
                    }

                    if (testCommandOutput.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testCommandOutput,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Automation Debug Screen and "Dump Screen" Tool
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("automation_debug_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Automation Debug Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Analyze live screen hierarchy nodes in JSON format to locate element IDs, class names, or text labels for precise AI targeting.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Button(
                    onClick = { viewModel.captureScreenDump() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dump_screen_button")
                ) {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = "Dump Screen")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dump Screen (JSON)")
                }

                if (viewModel.screenDumpJson.isNotEmpty()) {
                    Text(
                        text = "Active Screen Nodes (JSON):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    SelectionContainerText(text = viewModel.screenDumpJson)
                }
            }
        }

        // WhatsApp Auto-Reply Engine Card
        var autoReplyEnabled by remember { mutableStateOf(viewModel.settings.isAutoReplyEnabled) }
        var aiFallbackEnabled by remember { mutableStateOf(viewModel.settings.isAiFallbackEnabled) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("whatsapp_auto_reply_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WhatsApp Auto-Reply Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Automatically replies to incoming messages in WhatsApp & WhatsApp Business via native Notification Quick-Replies (fast path) with Accessibility UI automation fallback.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Auto-Reply Service", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Intercepts notifications and sends quick-replies", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = autoReplyEnabled,
                        onCheckedChange = {
                            autoReplyEnabled = it
                            viewModel.updateAutoReplyEnabled(it)
                        },
                        modifier = Modifier.testTag("auto_reply_active_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable AI Fallback", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Reply using Groq/Gemini when no trigger rules match", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = aiFallbackEnabled,
                        onCheckedChange = {
                            aiFallbackEnabled = it
                            viewModel.updateAiFallbackEnabled(it)
                        },
                        modifier = Modifier.testTag("ai_fallback_switch"),
                        enabled = autoReplyEnabled
                    )
                }

                if (aiFallbackEnabled && autoReplyEnabled) {
                    var allowedContacts by remember { mutableStateOf(viewModel.settings.aiAllowedContacts) }
                    OutlinedTextField(
                        value = allowedContacts,
                        onValueChange = { 
                            allowedContacts = it
                            viewModel.updateAiAllowedContacts(it)
                        },
                        label = { Text("AI Allowed Contacts (Optional)") },
                        placeholder = { Text("e.g. John Doe, Mom (Comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        text = "If empty, AI replies to EVERYONE. Specify exact names to restrict AI replies.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                if (autoReplyEnabled) {
                    var quickReplies by remember { mutableStateOf(viewModel.settings.quickReplies) }
                    OutlinedTextField(
                        value = quickReplies,
                        onValueChange = { 
                            quickReplies = it
                            viewModel.updateQuickReplies(it)
                        },
                        label = { Text("Quick Replies (Comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Trigger Keywords & Custom Replies",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                val rules by viewModel.rulesState.collectAsState()

                if (rules.isEmpty()) {
                    Text(
                        text = "No custom rules configured. Incoming messages will trigger AI Fallback if enabled.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    rules.forEach { rule ->
                        key(rule.id) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "If message contains: \"${rule.triggerKeyword}\"",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (!rule.targetContact.isNullOrBlank()) {
                                        Text(
                                            text = "For contact: ${rule.targetContact}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reply: \"${rule.replyMessage}\"",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteAutoReplyRule(rule.id) },
                                    modifier = Modifier.testTag("delete_rule_btn_${rule.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Rule",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                // Add rule controls
                var triggerInput by remember { mutableStateOf("") }
                var replyInput by remember { mutableStateOf("") }
                var contactInput by remember { mutableStateOf("") }
                var showAddForm by remember { mutableStateOf(false) }

                if (showAddForm) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = triggerInput,
                            onValueChange = { triggerInput = it },
                            label = { Text("Trigger Keyword") },
                            placeholder = { Text("e.g. status, pricing, hello") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("rule_trigger_input")
                        )

                        OutlinedTextField(
                            value = replyInput,
                            onValueChange = { replyInput = it },
                            label = { Text("Reply Message") },
                            placeholder = { Text("e.g. I am away, please email me.") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("rule_reply_input")
                        )

                        OutlinedTextField(
                            value = contactInput,
                            onValueChange = { contactInput = it },
                            label = { Text("Target Contact (Optional)") },
                            placeholder = { Text("e.g. John Doe") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("rule_contact_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddForm = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (triggerInput.isNotBlank() && replyInput.isNotBlank()) {
                                        viewModel.addAutoReplyRule(
                                            trigger = triggerInput,
                                            reply = replyInput,
                                            targetContact = contactInput.takeIf { it.isNotBlank() }
                                        )
                                        triggerInput = ""
                                        replyInput = ""
                                        contactInput = ""
                                        showAddForm = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                enabled = triggerInput.isNotBlank() && replyInput.isNotBlank(),
                                modifier = Modifier.testTag("submit_add_rule_button")
                            ) {
                                Text("Save Rule")
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("show_add_rule_form_button"),
                        enabled = autoReplyEnabled
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "add_rule")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Trigger Rule")
                    }
                }
            }
        }

        // Cache Management Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Database Management",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_database_history_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "delete")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Messages from SQLite Database")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Developer Credit
        Text(
            text = "Created by Yaswanth",
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SelectionContainerText(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = text,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
