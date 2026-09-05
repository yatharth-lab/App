package com.example.data.model

data class ConversationMessage(
    val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val actionType: String? = null,
    val actionDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    STARK,
    SYSTEM
}
