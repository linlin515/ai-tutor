package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P1-2: Pending message entity for offline message queue.
 * Stores messages that were sent while offline, to be retried when network is restored.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
