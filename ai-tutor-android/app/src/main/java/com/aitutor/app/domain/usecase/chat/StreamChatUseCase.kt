package com.aitutor.app.domain.usecase.chat

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreamChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float = 0.7f,
        topP: Float = 1.0f,
        maxTokens: Int = 2048
    ): Flow<String> {
        return chatRepository.streamChat(
            conversationId = conversationId,
            messages = messages,
            modelId = modelId,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens
        )
    }
}
