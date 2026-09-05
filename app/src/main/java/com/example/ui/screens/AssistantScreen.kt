package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.components.MessageBubble
import com.example.ui.components.QuickActionRow
import com.example.ui.components.StatusTelemetryHeader
import com.example.ui.core.ArcCoreVisualizer
import com.example.ui.core.WaveformVisualizer
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkBackground
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkEmeraldActive
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkTextSecondary
import com.example.ui.theme.StarkWarningRed
import com.example.viewmodel.StarkViewModel

@Composable
fun AssistantScreen(
    viewModel: StarkViewModel,
    onNavigateMemory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onMicPermissionRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var textInputMode by remember { mutableStateOf(false) }
    var typedMessage by remember { mutableStateOf("") }

    // Scroll to latest message on new turn
    LaunchedEffect(uiState.conversations.size) {
        if (uiState.conversations.isNotEmpty()) {
            listState.animateScrollToItem(uiState.conversations.size - 1)
        }
    }

    val micButtonColor by animateColorAsState(
        targetValue = when (uiState.assistantState) {
            AssistantState.LISTENING -> StarkCyanPrimary
            AssistantState.THINKING -> Color(0xFFFFB703)
            AssistantState.SPEAKING -> StarkTealSecondary
            AssistantState.BACKGROUND_ACTIVE -> StarkEmeraldActive
            else -> StarkCyanPrimary
        },
        label = "MicColor"
    )

    Scaffold(
        containerColor = StarkDarkBackground,
        topBar = {
            StatusTelemetryHeader(
                assistantState = uiState.assistantState,
                isOnline = uiState.isOnline,
                providerName = uiState.activeProviderName,
                isBackgroundActive = uiState.isBackgroundActive,
                onNavigateMemory = onNavigateMemory,
                onNavigateSettings = onNavigateSettings
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StarkDarkSurface)
                    .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Waveform visualizer above mic
                WaveformVisualizer(
                    state = uiState.assistantState,
                    audioLevel = uiState.audioLevel,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Optional text input bar
                AnimatedVisibility(visible = textInputMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = typedMessage,
                            onValueChange = { typedMessage = it },
                            placeholder = { Text("Command Mr. Stark...", color = StarkTextDim) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StarkTextPrimary,
                                unfocusedTextColor = StarkTextPrimary,
                                focusedBorderColor = StarkCyanPrimary,
                                unfocusedBorderColor = StarkSurfaceBorder,
                                focusedContainerColor = StarkDarkBackground,
                                unfocusedContainerColor = StarkDarkBackground
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (typedMessage.isNotBlank()) {
                                    viewModel.submitUserMessage(typedMessage)
                                    typedMessage = ""
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_input_field")
                        )

                        IconButton(
                            onClick = {
                                if (typedMessage.isNotBlank()) {
                                    viewModel.submitUserMessage(typedMessage)
                                    typedMessage = ""
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StarkCyanPrimary)
                                .testTag("send_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF00363D)
                            )
                        }
                    }
                }

                // Main Voice Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyboard toggle button
                    IconButton(
                        onClick = { textInputMode = !textInputMode },
                        modifier = Modifier
                            .testTag("keyboard_toggle_button")
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StarkDarkBackground)
                            .border(1.dp, StarkSurfaceBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard",
                            tint = if (textInputMode) StarkCyanPrimary else StarkTextSecondary
                        )
                    }

                    // Main Central Mic Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(micButtonColor.copy(alpha = if (uiState.assistantState == AssistantState.LISTENING) 0.3f else 0.15f))
                            .border(
                                width = 2.dp,
                                color = micButtonColor,
                                shape = CircleShape
                            )
                            .clickable {
                                if (!viewModel.securityManager.hasRecordAudioPermission()) {
                                    onMicPermissionRequested()
                                } else {
                                    viewModel.toggleListening()
                                }
                            }
                            .testTag("mic_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.assistantState == AssistantState.LISTENING) {
                                Icons.Default.Mic
                            } else {
                                Icons.Default.MicOff
                            },
                            contentDescription = "Microphone",
                            tint = micButtonColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Stop speech / Background Toggle button
                    if (uiState.assistantState == AssistantState.SPEAKING) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .testTag("stop_speech_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StarkWarningRed.copy(alpha = 0.2f))
                                .border(1.dp, StarkWarningRed, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Mute Stark",
                                tint = StarkWarningRed
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.toggleBackgroundService() },
                            modifier = Modifier
                                .testTag("background_service_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StarkDarkBackground)
                                .border(
                                    1.dp,
                                    if (uiState.isBackgroundActive) StarkEmeraldActive else StarkSurfaceBorder,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (uiState.isBackgroundActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Background Surveillance",
                                tint = if (uiState.isBackgroundActive) StarkEmeraldActive else StarkTextSecondary
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Central Arc Core
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcCoreVisualizer(
                    state = uiState.assistantState,
                    audioLevel = uiState.audioLevel,
                    size = 175.dp
                )
            }

            // Live partial recognition text
            if (uiState.partialTranscript.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "› ${uiState.partialTranscript}",
                        color = StarkCyanPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Error display if any
            if (!uiState.errorMessage.isNullOrEmpty()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = StarkWarningRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Quick action chips row
            QuickActionRow(
                onActionSelected = { command ->
                    viewModel.submitUserMessage(command)
                }
            )

            // Conversation history list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.conversations, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}
