package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val id: String,
    val sessionTitle: String,
    val messages: String, // JSON string of messages array
    val cachedAt: Long = System.currentTimeMillis()
)
