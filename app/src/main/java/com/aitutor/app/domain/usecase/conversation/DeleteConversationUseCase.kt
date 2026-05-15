package com.aitutor.app.domain.usecase.conversation

import com.aitutor.app.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: Long): Result<Unit> {
        return try {
            chatRepository.deleteConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
