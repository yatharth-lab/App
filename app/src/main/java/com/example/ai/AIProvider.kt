package com.example.ai

import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem

interface AIProvider {
    val providerName: String
    suspend fun processQuery(
        userMessage: String,
        history: List<ConversationMessage>,
        memories: List<MemoryItem>
    ): AIResponse
}
