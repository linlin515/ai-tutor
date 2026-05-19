package com.aitutor.app.data.mapper

import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.local.entity.MessageEntity
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.FeedbackType
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    modelId = modelId,
    systemPrompt = systemPrompt,
    messageCount = messageCount
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    modelId = modelId,
    systemPrompt = systemPrompt,
    messageCount = messageCount
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversationId,
    content = content,
    isUser = isUser,
    contentType = try {
        MessageType.valueOf(contentType)
    } catch (e: Exception) {
        MessageType.TEXT
    },
    timestamp = timestamp,
    status = try {
        MessageStatus.valueOf(status)
    } catch (e: Exception) {
        MessageStatus.SENT
    },
    metadata = metadata,
    // v2.5 F2: parse feedback
    feedback = try {
        feedback?.let { FeedbackType.valueOf(it) }
    } catch (e: Exception) {
        null
    },
    // v2.5 F3: favorite
    isFavorite = isFavorite
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    content = content,
    isUser = isUser,
    contentType = contentType.name,
    timestamp = timestamp,
    status = status.name,
    metadata = metadata,
    // v2.5 F2: serialize feedback
    feedback = feedback?.name,
    // v2.5 F3: favorite
    isFavorite = isFavorite
)
