package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.StarkPreferences
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.theme.StarkCyanPrimary
import com.example.ui.theme.StarkDarkBackground
import com.example.ui.theme.StarkDarkSurface
import com.example.ui.theme.StarkEmeraldActive
import com.example.ui.theme.StarkSurfaceBorder
import com.example.ui.theme.StarkSurfaceVariant
import com.example.ui.theme.StarkTealSecondary
import com.example.ui.theme.StarkTextDim
import com.example.ui.theme.StarkTextPrimary
import com.example.ui.theme.StarkTextSecondary
import com.example.ui.theme.StarkWarningRed
import com.example.viewmodel.StarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StarkViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences = viewModel.preferences
    val securityManager = viewModel.securityManager

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    var selectedProvider by remember { mutableStateOf(preferences.aiProvider) }
    var apiKeyInput by remember { mutableStateOf(preferences.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(preferences.aiModel) }

    var selectedLanguage by remember { mutableStateOf(preferences.voiceLanguage) }
    var speechSpeed by remember { mutableFloatStateOf(preferences.speechSpeed) }
    var speechPitch by remember { mutableFloatStateOf(preferences.speechPitch) }
    var speechVolume by remember { mutableFloatStateOf(preferences.speechVolume) }

    var wakeWordEnabled by remember { mutableStateOf(preferences.wakeWordEnabled) }
    var handsFreeEnabled by remember { mutableStateOf(preferences.handsFreeAutoListen) }
    var overlayHudEnabled by remember { mutableStateOf(preferences.overlayHudEnabled) }
    var foregroundServiceEnabled by remember { mutableStateOf(uiState.isBackgroundActive) }

    var pinInput by remember { mutableStateOf(preferences.appLockPin ?: "") }
    var showPinSavedToast by remember { mutableStateOf(false) }
    var showApiKeySaved by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showClearHistoryConfirm) {
        ActionConfirmationDialog(
            title = "Erase Conversation Log?",
            message = "This will permanently remove all past queries and responses from the screen.",
            confirmButtonText = "ERASE LOG",
            onConfirm = {
                viewModel.clearConversations()
                showClearHistoryConfirm = false
            },
            onDismiss = { showClearHistoryConfirm = false }
        )
    }

    Scaffold(
        containerColor = StarkDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "STARK SYSTEM PROTOCOLS",
                            color = StarkCyanPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Jarvis Autonomous Phone Automation",
                            color = StarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StarkCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StarkDarkSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ---------------------------------------------------------
            // 1. MASTER PERMISSIONS & ACCESS HUB (CRITICAL JARVIS HUB)
            // ---------------------------------------------------------
            SectionHeader(title = "SYSTEM PERMISSIONS & ACCESS PROTOCOLS", icon = Icons.Default.Security)

            // Permission Card: Accessibility Service (Screen Reading & App Action)
            PermissionCard(
                title = "Accessibility Protocol (Phone Control)",
                description = "Enables Mr. Stark to scan visible screen text, tap buttons, write inputs, and automate actions inside any application.",
                isGranted = uiState.hasAccessibilityPermission,
                grantedLabel = "ACTIVE • FULL CONTROL",
                missingLabel = "DISABLED • ACTION REQUIRED",
                actionButtonText = if (uiState.hasAccessibilityPermission) "SYSTEM CONFIG" else "AUTHORIZE ACCESSIBILITY",
                onActionClick = { securityManager.openAccessibilitySettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permission Card: Display Over Other Apps (50% Blue HUD Overlay)
            PermissionCard(
                title = "Draw Over Other Apps (50% Blue HUD)",
                description = "Enables Mr. Stark to deploy the 50% capacity blue holographic HUD screen on top of any active app when you say 'Mr. Stark'.",
                isGranted = uiState.hasOverlayPermission,
                grantedLabel = "AUTHORIZED • OVERLAY ACTIVE",
                missingLabel = "RESTRICTED • TAP TO AUTHORIZE",
                actionButtonText = if (uiState.hasOverlayPermission) "OVERLAY CONFIG" else "AUTHORIZE DRAW OVER APPS",
                onActionClick = { securityManager.openOverlaySettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permission Card: Battery Optimization Exemption
            PermissionCard(
                title = "Battery Optimization (24/7 Jarvis Hotword)",
                description = "Prevents Android from killing Mr. Stark's voice listener in the background, allowing hands-free hotword detection anywhere.",
                isGranted = !uiState.isBatteryOptimized,
                grantedLabel = "UNRESTRICTED • 24/7 LISTENING",
                missingLabel = "OPTIMIZED • MAY SLEEP IN BG",
                actionButtonText = if (!uiState.isBatteryOptimized) "CONFIGURED" else "DISABLE BATTERY RESTRICTIONS",
                onActionClick = { securityManager.requestIgnoreBatteryOptimizations() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permission Card: Microphone
            PermissionCard(
                title = "Audio Recording (Microphone)",
                description = "Required for speech-to-text recognition and waking up to 'Mr. Stark'.",
                isGranted = uiState.hasAudioPermission,
                grantedLabel = "GRANTED • HARDWARE READY",
                missingLabel = "MICROPHONE BLOCKED",
                actionButtonText = if (uiState.hasAudioPermission) "PERMITTED" else "OPEN APP PERMISSIONS",
                onActionClick = { securityManager.openAppDetailsSettings() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------------------------------------------------
            // 2. VOICE & WAKE WORD PROTOCOLS
            // ---------------------------------------------------------
            SectionHeader(title = "VOICE & WAKE WORD PROTOCOLS", icon = Icons.Default.Mic)

            SettingsCard {
                // Wake word toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hotword Detection ('Mr. Stark')", color = StarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Triggers voice recognition upon hearing 'Mr. Stark', 'Hey Stark', or 'Stark'", color = StarkTextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            viewModel.updateWakeWordSettings(it, handsFreeEnabled, overlayHudEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StarkCyanPrimary,
                            checkedTrackColor = StarkDarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 50% Blue HUD Overlay on Hotword Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("50% Blue Holographic HUD", color = StarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pop up 50% opacity blue screen over any active app when wake word is spoken", color = StarkTextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = overlayHudEnabled,
                        onCheckedChange = {
                            overlayHudEnabled = it
                            viewModel.updateWakeWordSettings(wakeWordEnabled, handsFreeEnabled, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StarkCyanPrimary,
                            checkedTrackColor = StarkDarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hands-free continuous auto-listen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hands-Free Auto Re-Listen", color = StarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Automatically resume listening after speaking without pressing the button", color = StarkTextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = handsFreeEnabled,
                        onCheckedChange = {
                            handsFreeEnabled = it
                            viewModel.updateWakeWordSettings(wakeWordEnabled, it, overlayHudEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StarkCyanPrimary,
                            checkedTrackColor = StarkDarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background Foreground Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Surveillance Service", color = StarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Keeps Stark listening even when screen is locked or app is closed", color = StarkTextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = foregroundServiceEnabled,
                        onCheckedChange = {
                            foregroundServiceEnabled = it
                            viewModel.toggleBackgroundService()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StarkEmeraldActive,
                            checkedTrackColor = StarkDarkSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------------------------------------------------
            // 3. AI COGNITIVE CORE (GEMINI / LOCAL)
            // ---------------------------------------------------------
            SectionHeader(title = "AI COGNITIVE CORE", icon = Icons.Default.Psychology)

            SettingsCard {
                Text("Select Intelligence Provider", color = StarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local Rule Engine (Offline)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedProvider == StarkPreferences.PROVIDER_OFFLINE_RULES) StarkCyanPrimary else StarkDarkBackground)
                            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedProvider = StarkPreferences.PROVIDER_OFFLINE_RULES
                                viewModel.updateAiProvider(StarkPreferences.PROVIDER_OFFLINE_RULES)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Local (Offline)",
                            color = if (selectedProvider == StarkPreferences.PROVIDER_OFFLINE_RULES) Color(0xFF00363D) else StarkTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Google Gemini
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedProvider == StarkPreferences.PROVIDER_GEMINI) StarkCyanPrimary else StarkDarkBackground)
                            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedProvider = StarkPreferences.PROVIDER_GEMINI
                                viewModel.updateAiProvider(StarkPreferences.PROVIDER_GEMINI)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Google Gemini",
                            color = if (selectedProvider == StarkPreferences.PROVIDER_GEMINI) Color(0xFF00363D) else StarkTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (selectedProvider == StarkPreferences.PROVIDER_GEMINI) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Gemini API Key", color = StarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Configured via Secrets or custom key below", color = StarkTextDim, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...", color = StarkTextDim) },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Key",
                                    tint = StarkCyanPrimary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StarkCyanPrimary,
                            unfocusedBorderColor = StarkSurfaceBorder,
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateApiKey(apiKeyInput)
                            showApiKeySaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkCyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE API KEY", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }

                    if (showApiKeySaved) {
                        Text("API Key saved securely.", color = StarkEmeraldActive, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------------------------------------------------
            // 4. SPEECH & VOCAL FREQUENCY (TTS)
            // ---------------------------------------------------------
            SectionHeader(title = "VOCAL FREQUENCY & SYNTHESIS", icon = Icons.AutoMirrored.Filled.VolumeUp)

            SettingsCard {
                Text("Voice Language Code", color = StarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("en-IN" to "English (India)", "hi-IN" to "Hindi", "en-US" to "English (US)").forEach { (code, label) ->
                        val isSel = selectedLanguage == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) StarkTealSecondary else StarkDarkBackground)
                                .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedLanguage = code
                                    viewModel.updateVoiceConfig(code, speechSpeed, speechPitch, speechVolume)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color(0xFF00363D) else StarkTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Speech Speed Slider
                Text("Speech Rate: ${"%.1f".format(speechSpeed)}x", color = StarkTextPrimary, fontSize = 12.sp)
                Slider(
                    value = speechSpeed,
                    onValueChange = {
                        speechSpeed = it
                        viewModel.updateVoiceConfig(selectedLanguage, it, speechPitch, speechVolume)
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StarkCyanPrimary,
                        activeTrackColor = StarkCyanPrimary
                    )
                )

                // Speech Pitch Slider
                Text("Vocal Pitch: ${"%.2f".format(speechPitch)}", color = StarkTextPrimary, fontSize = 12.sp)
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        viewModel.updateVoiceConfig(selectedLanguage, speechSpeed, it, speechVolume)
                    },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = StarkTealSecondary,
                        activeTrackColor = StarkTealSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------------------------------------------------
            // 5. SECURITY & DATA MAINTENANCE
            // ---------------------------------------------------------
            SectionHeader(title = "SECURITY & DATA MAINTENANCE", icon = Icons.Default.Lock)

            SettingsCard {
                Button(
                    onClick = { showClearHistoryConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkWarningRed.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StarkWarningRed, RoundedCornerShape(8.dp))
                ) {
                    Text("CLEAR CONVERSATION LOGS", color = StarkWarningRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StarkCyanPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = StarkCyanPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StarkDarkSurface)
            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    grantedLabel: String,
    missingLabel: String,
    actionButtonText: String,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StarkDarkSurface)
            .border(
                1.dp,
                if (isGranted) StarkEmeraldActive.copy(alpha = 0.5f) else Color(0xFFFFB703),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = StarkTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGranted) StarkEmeraldActive.copy(alpha = 0.2f) else Color(0xFFFFB703).copy(alpha = 0.2f))
                        .border(1.dp, if (isGranted) StarkEmeraldActive else Color(0xFFFFB703), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isGranted) grantedLabel else missingLabel,
                        color = if (isGranted) StarkEmeraldActive else Color(0xFFFFB703),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = StarkTextDim,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) StarkDarkBackground else Color(0xFFFFB703)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isGranted) StarkCyanPrimary else Color(0xFFFFB703),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = actionButtonText,
                    color = if (isGranted) StarkCyanPrimary else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
