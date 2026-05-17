package com.aitutor.app.domain.model

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val contentType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val metadata: String? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    AUDIO
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}
