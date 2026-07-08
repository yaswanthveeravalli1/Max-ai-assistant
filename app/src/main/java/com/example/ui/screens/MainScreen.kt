package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun MainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("chat") } // "chat" | "reminders" | "settings"
    var showOnboarding by remember { mutableStateOf(!viewModel.hasCompletedOnboarding) }

    if (showOnboarding) {
        OnboardingScreen(viewModel = viewModel, onComplete = { showOnboarding = false })
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == "chat",
                    onClick = { activeTab = "chat" },
                    label = { Text("MAX") },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "chat") Icons.Default.ChatBubble else Icons.Outlined.ChatBubble,
                            contentDescription = "Chat Conversation"
                        )
                    },
                    modifier = Modifier.testTag("nav_item_chat")
                )

                NavigationBarItem(
                    selected = activeTab == "reminders",
                    onClick = { activeTab = "reminders" },
                    label = { Text("Alarms") },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "reminders") Icons.Default.Alarm else Icons.Outlined.Alarm,
                            contentDescription = "Active Reminders"
                        )
                    },
                    modifier = Modifier.testTag("nav_item_reminders")
                )

                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { activeTab = "settings" },
                    label = { Text("Settings") },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "settings") Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Configurations"
                        )
                    },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "chat" -> ChatTab(viewModel = viewModel)
                "reminders" -> RemindersTab(viewModel = viewModel)
                "settings" -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}
