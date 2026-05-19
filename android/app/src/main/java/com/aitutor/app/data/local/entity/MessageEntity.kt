package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val contentType: String = "TEXT",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT",
    val metadata: String? = null,
    // v2.5 F2: AI 回复反馈 (null / "POSITIVE" / "NEGATIVE")
    val feedback: String? = null,
    // v2.5 F3: 收藏
    val isFavorite: Boolean = false
)
