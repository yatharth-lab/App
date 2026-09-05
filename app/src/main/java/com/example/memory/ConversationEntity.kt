package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ConversationMessage
import com.example.data.model.MessageSender

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val text: String,
    val actionType: String? = null,
    val actionDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ConversationMessage = ConversationMessage(
        id = id,
        sender = when (sender) {
            "USER" -> MessageSender.USER
            "STARK" -> MessageSender.STARK
            else -> MessageSender.SYSTEM
        },
        text = text,
        actionType = actionType,
        actionDetails = actionDetails,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(msg: ConversationMessage): ConversationEntity = ConversationEntity(
            id = msg.id,
            sender = msg.sender.name,
            text = msg.text,
            actionType = msg.actionType,
            actionDetails = msg.actionDetails,
            timestamp = msg.timestamp
        )
    }
}
