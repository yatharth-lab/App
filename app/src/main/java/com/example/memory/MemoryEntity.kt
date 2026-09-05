package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MemoryItem

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): MemoryItem = MemoryItem(
        id = id,
        key = key,
        value = value,
        category = category,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(item: MemoryItem): MemoryEntity = MemoryEntity(
            id = item.id,
            key = item.key,
            value = item.value,
            category = item.category,
            timestamp = item.timestamp
        )
    }
}
