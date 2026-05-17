package com.aitutor.app.ui.chat

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.VoiceState

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentConversationId: Long = -1L,
    val conversations: List<Conversation> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val voiceState: VoiceState = VoiceState(),
    val showConversationSheet: Boolean = false,
    val searchKeyword: String = ""
)
