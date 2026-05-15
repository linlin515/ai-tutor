package com.aitutor.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var streamJob: Job? = null
    private var aiMessageId: Long = -1L

    init {
        loadConversations()
        loadUserGrade()
    }

    private fun loadUserGrade() {
        viewModelScope.launch {
            authRepository.getProfile().onSuccess { user ->
                uiState = uiState.copy(userGrade = user.grade)
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getAllConversations().collectLatest { conversations ->
                uiState = uiState.copy(conversations = conversations)
                // Auto-select first conversation if none selected
                if (uiState.currentConversationId == -1L && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }
            }
        }
    }

    fun selectConversation(id: Long) {
        uiState = uiState.copy(currentConversationId = id, errorMessage = null)
        viewModelScope.launch {
            chatRepository.getMessagesByConversation(id).collectLatest { messages ->
                uiState = uiState.copy(messages = messages)
            }
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val id = chatRepository.createConversation("")
            selectConversation(id)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (uiState.currentConversationId == id) {
                uiState = uiState.copy(currentConversationId = -1L, messages = emptyList())
                loadConversations()
            }
        }
    }

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun toggleTutorMode() {
        uiState = uiState.copy(tutorMode = !uiState.tutorMode)
    }

    fun sendMessage() {
        val text = uiState.inputText.trim()
        if (text.isEmpty() || uiState.isStreaming) return

        // Ensure a conversation exists
        if (uiState.currentConversationId == -1L) {
            viewModelScope.launch {
                val id = chatRepository.createConversation("")
                selectConversation(id)
                doSendMessage(text)
            }
        } else {
            doSendMessage(text)
        }
    }

    private fun doSendMessage(text: String) {
        val conversationId = uiState.currentConversationId
        uiState = uiState.copy(inputText = "", errorMessage = null)

        // Cancel previous streaming job to prevent coroutine leak
        streamJob?.cancel()

        viewModelScope.launch {
            // Save user message
            val userMessage = ChatMessage(
                conversationId = conversationId,
                content = text,
                isUser = true,
                status = MessageStatus.SENT
            )
            chatRepository.insertMessage(userMessage)

            // Auto-name conversation if first message
            val conv = chatRepository.getConversationById(conversationId)
            if (conv != null && conv.title.isEmpty()) {
                val title = text.take(20) + if (text.length > 20) "..." else ""
                chatRepository.updateConversationTitle(conversationId, title)
            }

            // Create AI message placeholder
            val aiMessage = ChatMessage(
                conversationId = conversationId,
                content = "",
                isUser = false,
                status = MessageStatus.SENDING
            )
            val msgId = chatRepository.insertMessage(aiMessage)
            aiMessageId = msgId

            val settings = settingsRepository.getSettings().first()

            uiState = uiState.copy(isStreaming = true, streamingContent = "")

            // Get all context messages synchronously (in coroutine scope)
            val contextMessages = chatRepository.getMessagesByConversation(conversationId).first()
                .filter { it.id != msgId }

            // Determine tutor mode parameters
            val role = if (uiState.tutorMode) "tutor" else "assistant"
            val systemPrompt = if (uiState.tutorMode) {
                val gradeHint = uiState.userGrade?.let { "学生当前年级：$it。请根据该年级水平调整讲解深度。" } ?: ""
                "你是一个AI学习助手，请用引导式教学方法帮助学生思考和解决问题。采用苏格拉底式提问，逐步引导学生自己找到答案，而不是直接给出答案。$gradeHint"
            } else {
                null
            }

            // Stream chat — single job, no race condition
            streamJob = viewModelScope.launch {
                val accumulatedContent = StringBuilder()
                chatRepository.streamChat(
                    conversationId = conversationId,
                    messages = contextMessages + userMessage,
                    modelId = settings.modelId,
                    temperature = settings.temperature,
                    topP = settings.topP,
                    maxTokens = settings.maxTokens,
                    grade = uiState.userGrade,
                    role = role,
                    systemPrompt = systemPrompt
                ).collect { chunk ->
                    accumulatedContent.append(chunk)
                    uiState = uiState.copy(streamingContent = accumulatedContent.toString())
                }

                // Streaming finished — persist the final message
                chatRepository.updateMessageStatus(msgId, MessageStatus.SENT)

                uiState = uiState.copy(isStreaming = false, streamingContent = "")
            }
        }
    }

    fun retrySend() {
        // Find the last failed user message and resend
        val lastFailed = uiState.messages.lastOrNull { it.status == MessageStatus.FAILED && it.isUser }
        if (lastFailed != null) {
            viewModelScope.launch {
                chatRepository.updateMessageStatus(lastFailed.id, MessageStatus.SENT)
            }
            doSendMessage(lastFailed.content)
        }
    }

    fun toggleConversationSheet() {
        uiState = uiState.copy(
            showConversationSheet = !uiState.showConversationSheet
        )
    }

    fun hideConversationSheet() {
        uiState = uiState.copy(showConversationSheet = false)
    }

    fun updateSearchKeyword(keyword: String) {
        uiState = uiState.copy(searchKeyword = keyword)
    }

    fun clearConversation() {
        val conversationId = uiState.currentConversationId
        if (conversationId > 0) {
            viewModelScope.launch {
                chatRepository.deleteAllMessages(conversationId)
            }
        }
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            voiceRepository.speak(text, settings.ttsSpeed)
        }
    }

    fun stopSpeaking() {
        voiceRepository.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
