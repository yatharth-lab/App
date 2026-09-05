package com.example.ai

import com.example.data.model.ActionCommand

data class AIResponse(
    val spokenText: String,
    val actionCommand: ActionCommand? = null,
    val isOffline: Boolean = false,
    val error: String? = null
)
