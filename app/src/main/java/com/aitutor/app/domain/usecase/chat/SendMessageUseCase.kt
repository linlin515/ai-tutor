package com.aitutor.app.domain.usecase.chat

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        conversationId: Long,
        content: String,
        isUser: Boolean,
        contentType: MessageType = MessageType.TEXT
    ): Result<Long> {
        return try {
            val message = ChatMessage(
                conversationId = conversationId,
                content = content,
                isUser = isUser,
                contentType = contentType
            )
            val id = chatRepository.insertMessage(message)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
