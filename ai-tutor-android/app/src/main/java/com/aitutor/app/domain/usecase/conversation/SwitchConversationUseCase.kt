package com.aitutor.app.domain.usecase.conversation

import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SwitchConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return chatRepository.getAllConversations()
    }

    suspend fun getConversation(id: Long): Conversation? {
        return chatRepository.getConversationById(id)
    }
}
