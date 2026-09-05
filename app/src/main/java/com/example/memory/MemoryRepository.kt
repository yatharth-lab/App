package com.example.memory

import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepository(private val database: StarkDatabase) {
    private val memoryDao = database.memoryDao()
    private val conversationDao = database.conversationDao()

    val allMemories: Flow<List<MemoryItem>> = memoryDao.getAllMemories().map { entities ->
        entities.map { it.toDomain() }
    }

    val allConversations: Flow<List<ConversationMessage>> = conversationDao.getAllConversations().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAllMemoriesList(): List<MemoryItem> {
        return memoryDao.getAllMemoriesList().map { it.toDomain() }
    }

    suspend fun searchMemories(query: String): List<MemoryItem> {
        return memoryDao.searchMemories(query).map { it.toDomain() }
    }

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Long {
        return memoryDao.insertMemory(
            MemoryEntity(key = key, value = value, category = category)
        )
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteMemoryByKey(key: String) {
        memoryDao.deleteMemoryByKey(key)
    }

    suspend fun clearMemories() {
        memoryDao.clearAllMemories()
    }

    suspend fun saveConversationMessage(msg: ConversationMessage): Long {
        return conversationDao.insertMessage(ConversationEntity.fromDomain(msg))
    }

    suspend fun getRecentConversations(limit: Int = 10): List<ConversationMessage> {
        return conversationDao.getRecentConversations(limit).map { it.toDomain() }.reversed()
    }

    suspend fun clearConversations() {
        conversationDao.clearAllConversations()
    }
}
