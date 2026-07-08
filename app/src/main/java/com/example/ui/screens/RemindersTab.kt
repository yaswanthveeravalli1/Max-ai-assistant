package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Reminder
import com.example.ui.viewmodel.JarvisViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext

@Composable
fun RemindersTab(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val remindersState = viewModel.remindersState.collectAsState()
    val reminders by remindersState
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingReminders by remember { 
        derivedStateOf { remindersState.value.filter { it.status == "pending" } } 
    }
    val completedReminders by remember { 
        derivedStateOf { remindersState.value.filter { it.status == "completed" } } 
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Reminders Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MAX Alarms",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Scheduled on-device alerts & background triggers",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_reminder_trigger_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Alarms Section
                if (pendingReminders.isNotEmpty()) {
                    item {
                        SectionHeader(title = "ACTIVE TASKS (${pendingReminders.size})")
                    }
                    items(pendingReminders, key = { "pending_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Completed Section
                if (completedReminders.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "COMPLETED (${completedReminders.size})")
                    }
                    items(completedReminders, key = { "completed_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Empty State
                if (reminders.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "No reminders",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Zero Active Alerts",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try typing: 'remind me to play football in 10 seconds' in the chat, or click 'New' to schedule manually.",
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Reminder Modal Dialog
        if (showAddDialog) {
            AddReminderDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { msg, triggerAt, type, target, content, repeat ->
                    viewModel.addAutomationReminder(msg, triggerAt, type ?: "WHATSAPP", target ?: "", content ?: "", repeat)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onDelete: () -> Unit
) {
    val isCompleted = reminder.status == "completed"
    val cardBackground = if (isCompleted) {
        Color(0xFF111827).copy(alpha = 0.4f)
    } else {
        Color(0xFF111827).copy(alpha = 0.6f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}")
            .background(cardBackground, RoundedCornerShape(28.dp))
            .border(
                1.dp,
                if (isCompleted) Color.Transparent else Color(0xFFFFFFFF).copy(alpha = 0.1f),
                RoundedCornerShape(28.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Blue Accent Glow for active reminders
            if (!isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFA855F7), Color(0xFF00D1FF))
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
                            contentDescription = "Status",
                            tint = if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reminder.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Trigger Time",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        
                        val formattedTime = remember(reminder.triggerAt) {
                            DateFormat.getDateTimeInstance().format(Date(reminder.triggerAt))
                        }
                        
                        Text(
                            text = if (isCompleted) "Completed" else "Fires at: $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_reminder_button_${reminder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete alarm",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (
        message: String,
        triggerAt: Long,
        type: String?,
        target: String?,
        msgContent: String?,
        repeatType: String
    ) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var automationType by remember { mutableStateOf("WHATSAPP") }
    var contact by remember { mutableStateOf("") }
    var automationMessage by remember { mutableStateOf("") }
    var repeatType by remember { mutableStateOf("ONCE") }
    
    // Date/Time
    val calendar = remember { Calendar.getInstance() }
    var triggerAt by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedDateStr by remember { mutableStateOf("Not Selected") }
    var selectedTimeStr by remember { mutableStateOf("Not Selected") }

    val context = LocalContext.current
    
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                triggerAt = calendar.timeInMillis
                
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val minStr = minute.toString().padStart(2, '0')
                selectedTimeStr = "$hour12:$minStr $amPm"
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                triggerAt = calendar.timeInMillis
                
                selectedDateStr = "$dayOfMonth/${month + 1}/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule Local Alarm",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Task/Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // App Selector
                var expandedType by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("App: $automationType")
                }
                DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                    listOf("WHATSAPP", "TELEGRAM", "INSTAGRAM").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { automationType = it; expandedType = false })
                    }
                }
                
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = automationMessage, onValueChange = { automationMessage = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                
                // Date & Time Picker Buttons
                OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: $selectedDateStr")
                }
                OutlinedButton(onClick = { timePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Time: $selectedTimeStr")
                }

                // Repeat
                var expandedRepeat by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expandedRepeat = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Repeat: $repeatType")
                }
                DropdownMenu(expanded = expandedRepeat, onDismissRequest = { expandedRepeat = false }) {
                    listOf("ONCE", "DAILY", "WEEKLY").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { repeatType = it; expandedRepeat = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (triggerAt <= System.currentTimeMillis()) {
                        android.widget.Toast.makeText(context, "Please choose a future date and time", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onAdd(message, triggerAt, automationType, contact, automationMessage, repeatType)
                },
                modifier = Modifier.testTag("reminder_dialog_confirm"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
