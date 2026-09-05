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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

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

    // Local edit state
    var selectedProvider by remember { mutableStateOf(preferences.aiProvider) }
    var apiKeyInput by remember { mutableStateOf(preferences.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(preferences.aiModel) }
    var temperature by remember { mutableFloatStateOf(preferences.aiTemperature) }

    var selectedLanguage by remember { mutableStateOf(preferences.voiceLanguage) }
    var speechSpeed by remember { mutableFloatStateOf(preferences.speechSpeed) }
    var speechPitch by remember { mutableFloatStateOf(preferences.speechPitch) }
    var speechVolume by remember { mutableFloatStateOf(preferences.speechVolume) }

    var wakeWordEnabled by remember { mutableStateOf(preferences.wakeWordEnabled) }
    var handsFreeEnabled by remember { mutableStateOf(preferences.handsFreeAutoListen) }
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
                    Text(
                        text = "SYSTEM CONFIGURATION",
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkCyanPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StarkTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarkDarkSurface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: AI BRAIN & NEURAL ENGINE
            SettingsSectionHeader(title = "AI NEURAL CORE", icon = Icons.Default.Psychology)

            SettingsCard {
                Text(
                    text = "AI PROVIDER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkCyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChoiceChip(
                        title = "Google Gemini",
                        subtitle = "Online LLM",
                        isSelected = selectedProvider == StarkPreferences.PROVIDER_GEMINI,
                        onClick = {
                            selectedProvider = StarkPreferences.PROVIDER_GEMINI
                            viewModel.updateAiProvider(StarkPreferences.PROVIDER_GEMINI)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChoiceChip(
                        title = "Local Engine",
                        subtitle = "Offline Rules",
                        isSelected = selectedProvider == StarkPreferences.PROVIDER_OFFLINE_RULES,
                        onClick = {
                            selectedProvider = StarkPreferences.PROVIDER_OFFLINE_RULES
                            viewModel.updateAiProvider(StarkPreferences.PROVIDER_OFFLINE_RULES)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedProvider == StarkPreferences.PROVIDER_GEMINI) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "GEMINI API KEY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkTextSecondary
                    )
                    Text(
                        text = "Active Status: ${if (securityManager.isApiKeyConfigured()) "Configured (${securityManager.getMaskedApiKey()})" else "Not Set (Fallback to Local)"}",
                        fontSize = 11.sp,
                        color = if (securityManager.isApiKeyConfigured()) StarkEmeraldActive else StarkWarningRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            showApiKeySaved = false
                        },
                        placeholder = { Text("Paste custom Gemini API Key", color = StarkTextDim) },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Key Visibility",
                                    tint = StarkTextDim
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary,
                            focusedBorderColor = StarkCyanPrimary,
                            unfocusedBorderColor = StarkSurfaceBorder,
                            focusedContainerColor = StarkDarkBackground,
                            unfocusedContainerColor = StarkDarkBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showApiKeySaved) {
                            Text("Key Saved Successfully", color = StarkEmeraldActive, fontSize = 12.sp)
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                viewModel.updateApiKey(apiKeyInput.trim())
                                showApiKeySaved = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StarkCyanPrimary),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text("SAVE KEY", color = StarkDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "GEMINI MODEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-pro").forEach { model ->
                            ModelPill(
                                modelName = model,
                                isSelected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    viewModel.updateAiModel(model)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TEMPERATURE: ${String.format(Locale.getDefault(), "%.2f", temperature)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarkTextSecondary
                    )
                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = it
                            preferences.aiTemperature = it
                        },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = StarkCyanPrimary,
                            activeTrackColor = StarkCyanPrimary,
                            inactiveTrackColor = StarkSurfaceBorder
                        )
                    )
                }
            }

            // SECTION 2: VOICE & SPEECH PIPELINE
            SettingsSectionHeader(title = "VOICE & SPEECH SYNTHESIS", icon = Icons.Default.RecordVoiceOver)

            SettingsCard {
                Text(
                    text = "RECOGNITION & SYNTHESIS LANGUAGE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkCyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "en-IN" to "English (IN)",
                        "hi-IN" to "Hindi (IN)",
                        "en-US" to "English (US)"
                    ).forEach { (code, label) ->
                        LanguagePill(
                            label = label,
                            isSelected = selectedLanguage == code,
                            onClick = {
                                selectedLanguage = code
                                viewModel.updateVoiceConfig(code, speechSpeed, speechPitch, speechVolume)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "VOICE SPEED / TEMPO: ${String.format(Locale.getDefault(), "%.2fx", speechSpeed)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkTextSecondary
                )
                Slider(
                    value = speechSpeed,
                    onValueChange = {
                        speechSpeed = it
                        viewModel.updateVoiceConfig(selectedLanguage, it, speechPitch, speechVolume)
                    },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = StarkCyanPrimary,
                        activeTrackColor = StarkCyanPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VOICE PITCH (CINEMATIC CALM): ${String.format(Locale.getDefault(), "%.2fx", speechPitch)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkTextSecondary
                )
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        viewModel.updateVoiceConfig(selectedLanguage, speechSpeed, it, speechVolume)
                    },
                    valueRange = 0.7f..1.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = StarkTealSecondary,
                        activeTrackColor = StarkTealSecondary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.submitUserMessage("Voice test")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = StarkCyanPrimary)
                    Text(
                        text = "TEST VOICE RESPONSE",
                        color = StarkCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // SECTION 3: ACTIVATION & HOTWORD
            SettingsSectionHeader(title = "ACTIVATION & SURVEILLANCE", icon = Icons.Default.Mic)

            SettingsCard {
                SettingsSwitchRow(
                    title = "Wake-Word Detection",
                    subtitle = "Filter for \"Mr. Stark\" hotword activation",
                    checked = wakeWordEnabled,
                    onCheckedChange = {
                        wakeWordEnabled = it
                        viewModel.updateWakeWordSettings(it, handsFreeEnabled)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Hands-Free Auto Listen",
                    subtitle = "Reopen mic automatically after Mr. Stark finishes speaking",
                    checked = handsFreeEnabled,
                    onCheckedChange = {
                        handsFreeEnabled = it
                        viewModel.updateWakeWordSettings(wakeWordEnabled, it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Background Foreground Service",
                    subtitle = "Persistent notification & continuous assistant standby",
                    checked = uiState.isBackgroundActive,
                    onCheckedChange = {
                        viewModel.toggleBackgroundService()
                    }
                )
            }

            // SECTION 4: SECURITY & ACCESS CONTROL
            SettingsSectionHeader(title = "SECURITY & PRIVACY", icon = Icons.Default.Security)

            SettingsCard {
                Text(
                    text = "APP ACCESS LOCK (PIN)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkCyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (preferences.isAppLocked) "App lock currently active" else "No lock configured (direct access)",
                    fontSize = 11.sp,
                    color = StarkTextDim
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                pinInput = it
                                showPinSavedToast = false
                            }
                        },
                        placeholder = { Text("4-6 digit PIN", color = StarkTextDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary,
                            focusedBorderColor = StarkCyanPrimary,
                            unfocusedBorderColor = StarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            viewModel.updatePin(pinInput.ifBlank { null })
                            showPinSavedToast = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkCyanPrimary)
                    ) {
                        Text(if (pinInput.isBlank()) "DISABLE" else "SET PIN", color = StarkDarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                if (showPinSavedToast) {
                    Text(
                        text = if (pinInput.isBlank()) "App lock disabled" else "PIN lock configured",
                        color = StarkEmeraldActive,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showClearHistoryConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkWarningRed.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StarkWarningRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text("CLEAR CONVERSATION HISTORY", color = StarkWarningRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // SECTION 5: ABOUT MR. STARK
            SettingsSectionHeader(title = "ABOUT SYSTEM", icon = Icons.Default.Info)

            SettingsCard {
                Text(
                    text = "Mr. Stark Personal AI Assistant",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarkTextPrimary
                )
                Text(
                    text = "Version 1.0.0 • Native Android • Kotlin & Compose",
                    fontSize = 12.sp,
                    color = StarkTextDim
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All memory data is strictly stored locally on this device via Room SQLite. Designed as an original cinematic AI voice assistant with full Hindi/English/Hinglish understanding and native Android action orchestration.",
                    fontSize = 11.sp,
                    color = StarkTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StarkCyanPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = StarkCyanPrimary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StarkDarkSurface)
            .border(1.dp, StarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun ProviderChoiceChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) StarkCyanPrimary.copy(alpha = 0.15f) else StarkSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) StarkCyanPrimary else StarkSurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) StarkCyanPrimary else StarkTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = StarkTextDim
            )
        }
    }
}

@Composable
private fun ModelPill(
    modelName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) StarkCyanPrimary else StarkSurfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = modelName.replace("gemini-", ""),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) StarkDarkBackground else StarkTextSecondary
        )
    }
}

@Composable
private fun LanguagePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) StarkTealSecondary else StarkSurfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) StarkDarkBackground else StarkTextSecondary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = StarkTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = StarkTextDim
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StarkCyanPrimary,
                checkedTrackColor = StarkCyanPrimary.copy(alpha = 0.3f),
                uncheckedThumbColor = StarkTextDim,
                uncheckedTrackColor = StarkSurfaceVariant
            )
        )
    }
}
