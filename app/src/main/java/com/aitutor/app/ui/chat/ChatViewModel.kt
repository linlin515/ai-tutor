package com.aitutor.app.ui.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.local.AppLifecycleTracker
import com.aitutor.app.data.local.NotificationHelper
import com.aitutor.app.data.repository.UserProfileRepository
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.ChatMode
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.TeachingState
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.repository.VoiceRepository
import com.aitutor.app.domain.usecase.chat.ProcessTeachingResponseUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val processTeachingResponseUseCase: ProcessTeachingResponseUseCase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var streamJob: Job? = null
    private var aiMessageId: Long = -1L

    // Job for auto-exiting TUTOR mode after idle timeout (F42)
    private var idleTimeoutJob: Job? = null

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

    // ===== F41: Adaptive Step-by-Step =====

    /**
     * Set the difficulty level for adaptive teaching.
     * "auto" uses the user's profile grade; other values override it.
     */
    fun setDifficultyLevel(level: String) {
        uiState = uiState.copy(difficultyLevel = level)
    }

    /**
     * Toggle the difficulty switcher visibility.
     */
    fun toggleDifficultySwitcher() {
        uiState = uiState.copy(showDifficultySwitcher = !uiState.showDifficultySwitcher)
    }

    /**
     * Get the effective grade for API calls.
     * If difficultyLevel is "auto", use the user's profile grade.
     * Otherwise use the selected difficulty level.
     */
    private fun getEffectiveGrade(): String? {
        return when (uiState.difficultyLevel) {
            "auto" -> uiState.userGrade
            else -> uiState.difficultyLevel
        }
    }

    /**
     * Get the difficulty-based system prompt hint.
     */
    private fun getDifficultySystemPrompt(): String? {
        val effectiveGrade = getEffectiveGrade()
        return if (effectiveGrade != null) {
            userProfileRepository.getGradeSystemPrompt(effectiveGrade)
        } else {
            null
        }
    }

    // ===== F42: Socratic Teaching Mode =====

    /**
     * Toggle between ASSISTANT and TUTOR modes (legacy compatibility).
     */
    fun toggleTutorMode() {
        val newMode = if (uiState.chatMode == ChatMode.ASSISTANT) {
            ChatMode.TUTOR
        } else {
            ChatMode.ASSISTANT
        }
        setChatMode(newMode)
    }

    /**
     * Set the chat mode (ASSISTANT / TUTOR / QUIZ).
     */
    fun setChatMode(mode: ChatMode) {
        val previousMode = uiState.chatMode
        uiState = uiState.copy(
            chatMode = mode,
            tutorMode = mode != ChatMode.ASSISTANT,
            teachingState = TeachingState.Idle,
            tutorModeLastActiveTime = System.currentTimeMillis()
        )

        // Start idle timeout monitoring for TUTOR/QUIZ modes
        if (mode != ChatMode.ASSISTANT) {
            startIdleTimeoutMonitor()
        } else {
            idleTimeoutJob?.cancel()
        }

        // Reset teaching state when switching modes
        if (mode == ChatMode.ASSISTANT) {
            uiState = uiState.copy(teachingState = TeachingState.Idle)
        }
    }

    /**
     * Monitor for idle timeout — auto-exit TUTOR/QUIZ mode after 30 min of inactivity.
     */
    private fun startIdleTimeoutMonitor() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = viewModelScope.launch {
            while (true) {
                delay(60_000L) // Check every minute
                val elapsed = System.currentTimeMillis() - uiState.tutorModeLastActiveTime
                if (elapsed >= ProcessTeachingResponseUseCase.TUTOR_IDLE_TIMEOUT_MS) {
                    // Auto-exit to ASSISTANT mode
                    uiState = uiState.copy(
                        chatMode = ChatMode.ASSISTANT,
                        tutorMode = false,
                        teachingState = TeachingState.Idle
                    )
                    break
                }
            }
        }
    }

    /**
     * Update the last active timestamp (called on user interaction).
     */
    private fun updateLastActiveTime() {
        uiState = uiState.copy(tutorModeLastActiveTime = System.currentTimeMillis())
    }

    /**
     * Process teaching events from the streamed content.
     * Parses markers and updates TeachingState accordingly.
     */
    private fun processTeachingEvents(content: String) {
        val events = processTeachingResponseUseCase.parseTeachingEvents(content)

        for (event in events) {
            when (event.type) {
                ProcessTeachingResponseUseCase.EventType.SOCRATIC_QUESTION -> {
                    val steps = processTeachingResponseUseCase.countTeachingSteps(uiState.messages)
                    uiState = uiState.copy(
                        teachingState = TeachingState.Questioning(
                            question = event.content,
                            step = steps + 1,
                            totalSteps = steps + 1
                        )
                    )
                }
                ProcessTeachingResponseUseCase.EventType.CORRECTNESS -> {
                    val isCorrect = event.metadata["isCorrect"]?.toBoolean() ?: false
                    uiState = uiState.copy(
                        teachingState = TeachingState.Evaluating(
                            isCorrect = isCorrect,
                            feedback = event.content,
                            step = 1,
                            totalSteps = 1
                        )
                    )
                }
                ProcessTeachingResponseUseCase.EventType.TEACHING_COMPLETE -> {
                    uiState = uiState.copy(
                        teachingState = TeachingState.Complete
                    )
                }
                ProcessTeachingResponseUseCase.EventType.MODE_SWITCH -> {
                    // Mode switch events from server
                }
                ProcessTeachingResponseUseCase.EventType.NORMAL_CONTENT -> {
                    // No teaching event — keep current state
                }
            }
        }
    }

    // ===== Messaging =====

    fun sendMessage() {
        val text = uiState.inputText.trim()
        if (text.isEmpty() || uiState.isStreaming) return

        updateLastActiveTime()

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

            // Determine role and system prompt based on chat mode (F41 + F42)
            val role = when (uiState.chatMode) {
                ChatMode.ASSISTANT -> "assistant"
                ChatMode.TUTOR -> "tutor"
                ChatMode.QUIZ -> "quiz"
            }

            val systemPrompt = buildSystemPrompt()

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
                    grade = getEffectiveGrade(),
                    role = role,
                    systemPrompt = systemPrompt
                ).collect { chunk ->
                    accumulatedContent.append(chunk)
                    val currentContent = accumulatedContent.toString()
                    uiState = uiState.copy(streamingContent = currentContent)

                    // Process teaching events from content (F42)
                    if (uiState.chatMode != ChatMode.ASSISTANT) {
                        processTeachingEvents(currentContent)
                    }
                }

                // Streaming finished — clean up content and persist
                val finalContent = accumulatedContent.toString()
                val cleanedContent = if (uiState.chatMode != ChatMode.ASSISTANT) {
                    processTeachingResponseUseCase.stripTeachingMarkers(finalContent)
                } else {
                    finalContent
                }

                // Update the message in DB with cleaned content
                chatRepository.updateMessageStatus(msgId, MessageStatus.SENT)
                // Update the actual content through repository
                if (cleanedContent.isNotEmpty()) {
                    val updatedMessage = ChatMessage(
                        id = msgId,
                        conversationId = conversationId,
                        content = cleanedContent,
                        isUser = false,
                        status = MessageStatus.SENT,
                        timestamp = System.currentTimeMillis()
                    )
                    // Re-insert to update content (simplified approach)
                    chatRepository.insertMessage(updatedMessage)
                }

                // F32: Send notification when AI reply completes and app is not in foreground
                if (!AppLifecycleTracker.isInForeground) {
                    val conv = chatRepository.getConversationById(conversationId)
                    val title = conv?.title?.ifBlank { null } ?: "AI 助手"
                    val preview = cleanedContent.take(120).replace('\n', ' ')
                    NotificationHelper.sendMessageNotification(
                        context = appContext,
                        title = title,
                        content = preview,
                        conversationId = conversationId
                    )
                }

                uiState = uiState.copy(isStreaming = false, streamingContent = "")
            }
        }
    }

    /**
     * Build the system prompt based on current chat mode and settings.
     * Combines F41 (adaptive difficulty) and F42 (Socratic teaching) prompts.
     */
    private fun buildSystemPrompt(): String? {
        return when (uiState.chatMode) {
            ChatMode.ASSISTANT -> null
            ChatMode.TUTOR -> {
                val gradeHint = getDifficultySystemPrompt()
                val basePrompt = "你是一个AI学习导师，请采用苏格拉底式教学方法。" +
                        "通过逐步提问引导学生自己找到答案，而不是直接给出答案。" +
                        "每次只问一个问题，等待学生回答后再提出下一个问题。" +
                        "如果学生回答正确，给予肯定并进入下一个问题。" +
                        "如果学生回答错误，给予提示并引导他们重新思考。" +
                        "请在关键节点使用以下标记来标识教学状态：\n" +
                        "- 当你想问学生一个引导性问题时，在问题前加上 [socratic_question]\n" +
                        "- 当你要评价学生的回答时，在评价前加上 [correctness] 并说明是否正确\n" +
                        "- 当教学完成时，在最后加上 [teaching_complete]\n\n" +
                        "请保持耐心和鼓励的态度。"
                if (gradeHint != null) {
                    "$basePrompt\n\n$gradeHint"
                } else {
                    basePrompt
                }
            }
            ChatMode.QUIZ -> {
                val gradeHint = getDifficultySystemPrompt()
                val basePrompt = "你是一个AI测验官，请通过提问来检验学生的知识掌握程度。" +
                        "每次问一个选择题或简答题，等待学生回答。" +
                        "回答正确时给予表扬，回答错误时给出正确答案和解释。" +
                        "题目难度应循序渐进。"
                if (gradeHint != null) {
                    "$basePrompt\n\n$gradeHint"
                } else {
                    basePrompt
                }
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
        idleTimeoutJob?.cancel()
    }
}
