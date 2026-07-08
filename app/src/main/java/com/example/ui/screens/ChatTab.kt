package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import com.example.data.local.ChatMessage
import com.example.ui.viewmodel.JarvisViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatTab(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messagesState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            if (!viewModel.isListening) {
                viewModel.startListening { text ->
                    viewModel.inputText = text
                    viewModel.sendMessage(isVoice = true)
                }
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickSuggestions = listOf(
        "Remind me to take a water break in 10 seconds",
        "Remind me to check my code in 1 minute",
        "What are your core capabilities, MAX?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header (Sophisticated Dark Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = "MAX",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = if (viewModel.settings.isLocalAiMode) "SYSTEM ACTIVE • LOCAL" else "SYSTEM ACTIVE • REMOTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Conversation",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // Chat Bubble List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty state greeting card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Hello",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "I am MAX",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your responsive personal assistant with long-term memory. Ask me to remember details, set alarms, or schedule reminders instantly.",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "TRY THESE QUICK COMMANDS:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    quickSuggestions.forEach { suggestion ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.inputText = suggestion
                                    viewModel.sendMessage()
                                }
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.1f), Color(0xFFFFFFFF).copy(alpha = 0f))
                                    ),
                                    RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                }
            }
        }

        // Suggestions bar above input when chat is active
        if (messages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            viewModel.inputText = "Remind me to buy groceries in 10 seconds"
                        }
                        .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.1f), Color(0xFFFFFFFF).copy(alpha = 0f))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = "⏱️ Remind 10s",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            viewModel.inputText = "Forget keyword"
                        }
                        .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.1f), Color(0xFFFFFFFF).copy(alpha = 0f))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = "🧹 Clear memory",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Status indicator
        if (viewModel.isSending || viewModel.isListening) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .alpha(if (viewModel.isSending) alpha else 1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MAX is thinking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (viewModel.isListening) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "MAX is listening...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Chat Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isListening = viewModel.isListening
            val borderBrush = if (isListening) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFA855F7), Color(0xFF00D1FF))
                )
            } else {
                androidx.compose.ui.graphics.SolidColor(Color(0xFF00D1FF).copy(alpha = 0.5f))
            }
            
            TextField(
                value = viewModel.inputText,
                onValueChange = { viewModel.inputText = it },
                placeholder = { Text("Ask MAX anything...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF111827).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF111827).copy(alpha = 0.6f),
                    disabledContainerColor = Color(0xFF111827).copy(alpha = 0.6f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.sendMessage()
                        keyboardController?.hide()
                    }
                ),
                enabled = !viewModel.isSending
            )

            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = {
                    if (viewModel.inputText.isNotEmpty()) {
                        viewModel.sendMessage()
                        keyboardController?.hide()
                    } else {
                        if (viewModel.isListening) {
                            viewModel.stopListening()
                        } else {
                            val hasAudioPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasAudioPermission) {
                                if (!viewModel.isListening) {
                                    viewModel.startListening { text ->
                                        viewModel.inputText = text
                                        viewModel.sendMessage(isVoice = true)
                                    }
                                }
                                val missingPermissions = mutableListOf<String>()
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                                    missingPermissions.add(Manifest.permission.READ_CONTACTS)
                                }
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    missingPermissions.add(Manifest.permission.CALL_PHONE)
                                }
                                if (missingPermissions.isNotEmpty()) {
                                    permissionsLauncher.launch(missingPermissions.toTypedArray())
                                }
                            } else {
                                val requiredPermissions = arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.CALL_PHONE
                                )
                                permissionsLauncher.launch(requiredPermissions)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_message_button"),
                shape = RoundedCornerShape(24.dp),
                containerColor = if (viewModel.isListening) Color(0xFFA855F7) else Color(0xFF00D1FF),
                contentColor = Color(0xFF003543),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                if (viewModel.isSending) {
                    CircularProgressIndicator(
                        color = Color(0xFF003543),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (viewModel.isListening) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Stop Listening",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (viewModel.inputText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF003543),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color(0xFF003543),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp)
    }

    val bubbleBackground = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF111827).copy(alpha = 0.6f)
    }

    val borderModifier = if (isUser) {
        Modifier
    } else {
        Modifier.border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), bubbleShape)
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
    val formattedTime = remember(message.timestamp) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleBackground, bubbleShape)
                .then(borderModifier)
                .clip(bubbleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = bubbleTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
