package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Conversation operations
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: Long): Conversation?
    suspend fun createConversation(title: String, modelId: String = "default"): Long
    suspend fun updateConversationTitle(id: Long, title: String)
    suspend fun deleteConversation(id: Long)

    // Message operations
    fun getMessagesByConversation(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage): Long
    suspend fun updateMessageStatus(id: Long, status: MessageStatus)
    suspend fun deleteAllMessages(conversationId: Long)

    // Streaming
    fun streamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        grade: String? = null,
        role: String? = null,  // "tutor" or "assistant"
        systemPrompt: String? = null
    ): Flow<String>

    // Image solving
    suspend fun solvePhoto(
        imageUri: String,
        conversationId: Long,
        grade: String? = null
    ): Result<String>

    // Search
    fun searchMessagesByKeyword(keyword: String): Flow<List<Conversation>>

    // Clear all
    suspend fun clearAll()
}
