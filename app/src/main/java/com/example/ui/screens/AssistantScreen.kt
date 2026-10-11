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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.MessageSender
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
    var showFullHistory by remember { mutableStateOf(false) }

    val latestStarkMessage = uiState.conversations.lastOrNull { it.sender == MessageSender.STARK }

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
                    .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Waveform visualizer
                WaveformVisualizer(
                    state = uiState.assistantState,
                    audioLevel = uiState.audioLevel,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Voice Controls Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Overlay HUD Trigger Button
                    IconButton(
                        onClick = { viewModel.openOverlayHud() },
                        modifier = Modifier
                            .testTag("overlay_hud_button")
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(StarkDarkBackground)
                            .border(1.dp, StarkCyanPrimary.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Open 50% Blue HUD Overlay",
                            tint = StarkCyanPrimary
                        )
                    }

                    // Main Central Voice Command Orb
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(micButtonColor.copy(alpha = if (uiState.assistantState == AssistantState.LISTENING) 0.35f else 0.15f))
                            .border(
                                width = 2.5.dp,
                                color = micButtonColor,
                                shape = CircleShape
                            )
                            .clickable {
                                if (!uiState.hasAudioPermission) {
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
                            contentDescription = "Voice Activation",
                            tint = micButtonColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Mute / Background Surveillance Toggle
                    if (uiState.assistantState == AssistantState.SPEAKING) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .testTag("stop_speech_button")
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(StarkWarningRed.copy(alpha = 0.2f))
                                .border(1.dp, StarkWarningRed, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Mute",
                                tint = StarkWarningRed
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.toggleBackgroundService() },
                            modifier = Modifier
                                .testTag("background_service_button")
                                .size(46.dp)
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
                                contentDescription = "Background Listening",
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
            // Permission Alert Banner if Accessibility or Overlay is missing
            if (!uiState.hasAccessibilityPermission || !uiState.hasOverlayPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A1502))
                        .border(1.dp, Color(0xFFFFB703), RoundedCornerShape(10.dp))
                        .clickable { onNavigateSettings() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permissions Required",
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PHONE CONTROL & HUD INACTIVE",
                                    color = Color(0xFFFFB703),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Grant Accessibility & Draw Over Apps to control phone & read screen",
                                    color = StarkTextPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Settings",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central Glowing Arc Core Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcCoreVisualizer(
                    state = uiState.assistantState,
                    audioLevel = uiState.audioLevel,
                    size = 170.dp
                )
            }

            // Real-time Spoken Transcript or Wake Detection Banner
            if (uiState.partialTranscript.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkCyanPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "› HEARING: ${uiState.partialTranscript}",
                        color = StarkCyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Voice Quick Action HUD Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip 1: Read Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.readActiveScreen() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👁️ READ SCREEN",
                        color = StarkCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Chip 2: Open 50% HUD
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkTealSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.openOverlayHud() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌌 50% BLUE HUD",
                        color = StarkTealSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Chip 3: Torch
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkEmeraldActive.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.submitUserMessage("torch on") }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔦 TORCH",
                        color = StarkEmeraldActive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Latest Stark Voice Telemetry Box
            latestStarkMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StarkDarkSurface)
                        .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Spoken Output",
                                    tint = StarkCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MR. STARK VOCAL READOUT",
                                    color = StarkCyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (msg.actionDetails != null) {
                                Text(
                                    text = msg.actionDetails,
                                    color = StarkEmeraldActive,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg.text,
                            color = StarkTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Quick command prompts
            QuickActionRow(
                onActionSelected = { command ->
                    viewModel.submitUserMessage(command)
                }
            )

            // Conversation history list (compact & scrollable)
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
