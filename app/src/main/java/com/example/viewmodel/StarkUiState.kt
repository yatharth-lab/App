package com.example.viewmodel

import com.example.data.model.AssistantState
import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem

data class StarkUiState(
    val assistantState: AssistantState = AssistantState.IDLE,
    val audioLevel: Float = 0f,
    val isOnline: Boolean = true,
    val partialTranscript: String = "",
    val conversations: List<ConversationMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val isBackgroundActive: Boolean = false,
    val activeProviderName: String = "Google Gemini",
    val isApiKeyConfigured: Boolean = false,
    val errorMessage: String? = null,
    val isAppUnlocked: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val handsFreeEnabled: Boolean = true,
    val overlayHudEnabled: Boolean = true,
    // Permissions Status
    val hasAudioPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isBatteryOptimized: Boolean = true,
    val hasNotificationPermission: Boolean = false
)
