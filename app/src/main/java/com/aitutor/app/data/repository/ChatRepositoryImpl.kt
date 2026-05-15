package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.mapper.toDomain
import com.aitutor.app.data.mapper.toEntity
import com.aitutor.app.data.remote.api.ChatStreamApi
import com.aitutor.app.data.remote.dto.ChatCompletionRequest
import com.aitutor.app.data.remote.dto.ChatMessageDto
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val chatStreamApi: ChatStreamApi,
    private val tokenManager: TokenManager
) : ChatRepository {

    // ===== Conversation Operations =====

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(id: Long): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    override suspend fun createConversation(title: String, modelId: String): Long {
        val entity = ConversationEntity(
            title = title,
            modelId = modelId
        )
        return conversationDao.insert(entity)
    }

    override suspend fun updateConversationTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
        conversationDao.updateTimestamp(id, System.currentTimeMillis())
    }

    override suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    // ===== Message Operations =====

    override fun getMessagesByConversation(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.getByConversationFlow(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertMessage(message: ChatMessage): Long {
        val entity = message.toEntity()
        val id = messageDao.insert(entity)

        // Update conversation timestamp and message count
        conversationDao.updateTimestamp(message.conversationId, System.currentTimeMillis())

        return id
    }

    override suspend fun updateMessageStatus(id: Long, status: MessageStatus) {
        messageDao.updateStatus(id, status.name)
    }

    override suspend fun deleteAllMessages(conversationId: Long) {
        messageDao.deleteByConversation(conversationId)
    }

    // ===== Streaming =====

    override fun streamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int
    ): Flow<String> {
        val chatMessages = messages.map { msg ->
            ChatMessageDto(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.content
            )
        }

        val request = ChatCompletionRequest(
            model = modelId,
            messages = chatMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens
        )

        return chatStreamApi.streamChat(request, tokenManager.getToken())
    }

    // ===== Search =====

    override fun searchMessagesByKeyword(keyword: String): Flow<List<Conversation>> {
        return conversationDao.searchByTitle(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ===== Clear All =====

    override suspend fun clearAll() {
        conversationDao.deleteAll()
    }
}
