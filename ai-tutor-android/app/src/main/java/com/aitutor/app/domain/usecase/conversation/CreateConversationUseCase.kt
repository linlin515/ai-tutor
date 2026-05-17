package com.aitutor.app.domain.usecase.conversation

import com.aitutor.app.domain.repository.ChatRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        title: String = "",
        modelId: String = "default"
    ): Result<Long> {
        return try {
            val id = chatRepository.createConversation(title, modelId)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
