package com.aitutor.app.domain.usecase.chat

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: Long): Flow<List<ChatMessage>> {
        return chatRepository.getMessagesByConversation(conversationId)
    }
}
