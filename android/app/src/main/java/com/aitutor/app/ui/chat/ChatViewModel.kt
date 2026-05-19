package com.aitutor.app.ui.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.local.AppLifecycleTracker
import com.aitutor.app.data.local.NotificationHelper
import com.aitutor.app.data.remote.datastore.LanguagePreferences
import com.aitutor.app.data.repository.UserProfileRepository
import com.aitutor.app.data.tool.impl.CalculatorTool
import com.aitutor.app.data.tool.impl.DateTimeTool
import com.aitutor.app.data.tool.registry.ToolRegistry
import com.aitutor.app.domain.model.AgentState
import com.aitutor.app.domain.model.AgentStepType
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.ChatMode
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.domain.model.StreamEvent
import com.aitutor.app.domain.model.TeachingState
import com.aitutor.app.domain.model.ToolCallInfo
import com.aitutor.app.domain.model.ToolResult
import com.aitutor.app.domain.repository.AgentRepository
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.repository.SolveRepository
import com.aitutor.app.domain.repository.VoiceRepository
import com.aitutor.app.domain.usecase.chat.ProcessTeachingResponseUseCase
import com.aitutor.app.util.NetworkMonitor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private val solveRepository: SolveRepository,
    private val agentRepository: AgentRepository,
    private val languagePreferences: LanguagePreferences,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var streamJob: Job? = null
    private var aiMessageId: Long = -1L
    private var idleTimeoutJob: Job? = null
    private val gson = Gson()

    init {
        loadConversations()
        loadUserGrade()
        observeAgentEnabled()
        observeNetworkState()
        initTools()
    }

    /**
     * 初始化 ToolRegistry，注册内置工具
     */
    private fun initTools() {
        // Register tools once
        if (ToolRegistry.getToolDefinitions().isEmpty()) {
            ToolRegistry.register(CalculatorTool())
            ToolRegistry.register(DateTimeTool())
            // WebSearchTool requires OkHttpClient, registered in NetworkModule or lazily
        }
    }

    /**
     * 观察 Agent 模式开关状态
     */
    private fun observeAgentEnabled() {
        viewModelScope.launch {
            agentRepository.getAgentEnabled().collectLatest { enabled ->
                uiState = uiState.copy(agentEnabled = enabled)
            }
        }
    }

    /**
     * P1-2: 观察网络连接状态
     */
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                uiState = uiState.copy(isOnline = online)
            }
        }
    }

    /**
     * 切换 Agent 模式
     */
    fun toggleAgentMode() {
        viewModelScope.launch {
            val newValue = !uiState.agentEnabled
            agentRepository.setAgentEnabled(newValue)
        }
    }

    /**
     * 显示/隐藏 Agent Switch
     */
    fun toggleAgentSwitch() {
        uiState = uiState.copy(showAgentSwitch = !uiState.showAgentSwitch)
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

    fun setDifficultyLevel(level: String) {
        uiState = uiState.copy(difficultyLevel = level)
    }

    fun toggleDifficultySwitcher() {
        uiState = uiState.copy(showDifficultySwitcher = !uiState.showDifficultySwitcher)
    }

    private fun getEffectiveGrade(): String? {
        return when (uiState.difficultyLevel) {
            "auto" -> uiState.userGrade
            else -> uiState.difficultyLevel
        }
    }

    private fun getDifficultySystemPrompt(): String? {
        val effectiveGrade = getEffectiveGrade()
        return if (effectiveGrade != null) {
            userProfileRepository.getGradeSystemPrompt(effectiveGrade)
        } else {
            null
        }
    }

    // ===== F42: Socratic Teaching Mode =====

    fun toggleTutorMode() {
        val newMode = if (uiState.chatMode == ChatMode.ASSISTANT) {
            ChatMode.TUTOR
        } else {
            ChatMode.ASSISTANT
        }
        setChatMode(newMode)
    }

    fun setChatMode(mode: ChatMode) {
        uiState = uiState.copy(
            chatMode = mode,
            tutorMode = mode != ChatMode.ASSISTANT,
            teachingState = TeachingState.Idle,
            tutorModeLastActiveTime = System.currentTimeMillis()
        )
        if (mode != ChatMode.ASSISTANT) {
            startIdleTimeoutMonitor()
        } else {
            idleTimeoutJob?.cancel()
        }
        if (mode == ChatMode.ASSISTANT) {
            uiState = uiState.copy(teachingState = TeachingState.Idle)
        }
    }

    private fun startIdleTimeoutMonitor() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val elapsed = System.currentTimeMillis() - uiState.tutorModeLastActiveTime
                if (elapsed >= ProcessTeachingResponseUseCase.TUTOR_IDLE_TIMEOUT_MS) {
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

    private fun updateLastActiveTime() {
        uiState = uiState.copy(tutorModeLastActiveTime = System.currentTimeMillis())
    }

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
                    uiState = uiState.copy(teachingState = TeachingState.Complete)
                }
                ProcessTeachingResponseUseCase.EventType.MODE_SWITCH -> {}
                ProcessTeachingResponseUseCase.EventType.NORMAL_CONTENT -> {}
            }
        }
    }

    // ===== Messaging =====

    fun sendMessage() {
        val text = uiState.inputText.trim()
        if (text.isEmpty() || uiState.isStreaming) return

        // P1-2: Check network before sending
        if (!networkMonitor.isCurrentlyOnline()) {
            uiState = uiState.copy(errorMessage = "当前无网络连接，无法发送消息")
            return
        }

        updateLastActiveTime()

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

            // Auto-name conversation
            val conv = chatRepository.getConversationById(conversationId)
            if (conv != null && conv.title.isEmpty()) {
                val title = text.take(20) + if (text.length > 20) "..." else ""
                chatRepository.updateConversationTitle(conversationId, title)
            }

            val settings = settingsRepository.getSettings().first()

            // F51: 读取用户语言偏好
            val currentLocale = languagePreferences.currentLocale
            val langCode = try {
                // Read current value - use first() to get one-shot
                currentLocale.first().language
            } catch (e: Exception) {
                "zh"
            }

            // Decide streaming mode based on agent enabled
            val isAgentMode = uiState.agentEnabled
            val agents = if (isAgentMode) agentRepository.getToolsDefinitions() else null

            // Create AI message placeholder
            val aiMessage = ChatMessage(
                conversationId = conversationId,
                content = "",
                isUser = false,
                status = MessageStatus.SENDING
            )
            val msgId = chatRepository.insertMessage(aiMessage)
            aiMessageId = msgId

            uiState = uiState.copy(
                isStreaming = true,
                streamingContent = "",
                agentState = if (isAgentMode) AgentState.THINKING else AgentState.IDLE
            )

            val contextMessages = chatRepository.getMessagesByConversation(conversationId).first()
                .filter { it.id != msgId }

            val role = when (uiState.chatMode) {
                ChatMode.ASSISTANT -> "assistant"
                ChatMode.TUTOR -> "tutor"
                ChatMode.QUIZ -> "quiz"
            }
            val systemPrompt = buildSystemPrompt()

            if (isAgentMode && agents != null && agents.isNotEmpty()) {
                // Agent mode: stream with events and tool calling support
                streamJob = viewModelScope.launch {
                    agentStreamChat(
                        conversationId, contextMessages + userMessage, msgId,
                        settings.modelId, settings.temperature, settings.topP, settings.maxTokens,
                        agents, role, systemPrompt, langCode
                    )
                }
            } else {
                // Normal mode: stream text
                streamJob = viewModelScope.launch {
                    normalStreamChat(
                        conversationId, contextMessages + userMessage, msgId,
                        settings.modelId, settings.temperature, settings.topP, settings.maxTokens,
                        role, systemPrompt, langCode
                    )
                }
            }
        }
    }

    /**
     * Agent 模式流式对话 - 支持 tool calling
     */
    private suspend fun agentStreamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        msgId: Long,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        tools: List<Map<String, Any>>,
        role: String?,
        systemPrompt: String?,
        language: String?
    ) {
        val accumulatedContent = StringBuilder()
        var pendingToolCalls = mutableListOf<ToolCallInfo>()
        uiState = uiState.copy(agentState = AgentState.THINKING)

        // First pass: stream with tools to detect tool_calls
        chatRepository.streamChatWithEvents(
            conversationId, messages, modelId, temperature, topP, maxTokens,
            tools = tools, grade = getEffectiveGrade(), role = role,
            systemPrompt = systemPrompt, language = language
        ).collect { event ->
            when (event) {
                is StreamEvent.TextChunk -> {
                    accumulatedContent.append(event.content)
                    uiState = uiState.copy(
                        streamingContent = accumulatedContent.toString(),
                        agentState = AgentState.RESPONDING
                    )
                }
                is StreamEvent.ToolCallChunk -> {
                    pendingToolCalls.addAll(event.toolCalls)
                    uiState = uiState.copy(agentState = AgentState.SEARCHING)
                }
                is StreamEvent.RoleChunk -> { /* ignore role */ }
                is StreamEvent.Done -> {
                    // Stream complete - check if we need to execute tools
                    if (pendingToolCalls.isNotEmpty()) {
                        executeToolsAndRespond(
                            conversationId, messages, msgId, modelId, temperature, topP,
                            maxTokens, pendingToolCalls, accumulatedContent.toString(),
                            role, systemPrompt, language
                        )
                        pendingToolCalls = mutableListOf()
                    } else {
                        finalizeStream(msgId, accumulatedContent.toString(), conversationId)
                    }
                }
                is StreamEvent.ErrorEvent -> {
                    uiState = uiState.copy(
                        errorMessage = event.message,
                        agentState = AgentState.IDLE
                    )
                }
            }
        }
    }

    /**
     * 执行工具并将结果回传给 LLM
     */
    private suspend fun executeToolsAndRespond(
        conversationId: Long,
        messages: List<ChatMessage>,
        msgId: Long,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        toolCalls: List<ToolCallInfo>,
        currentContent: String,
        role: String?,
        systemPrompt: String?,
        language: String?
    ) {
        // Insert AgentStep message for each tool call
        val toolResults = mutableListOf<Map<String, Any>>()

        for (tc in toolCalls) {
            uiState = uiState.copy(agentState = AgentState.SEARCHING)

            // Save tool call as AgentStep message
            val toolCallMsg = ChatMessage(
                conversationId = conversationId,
                content = currentContent,
                isUser = false,
                contentType = MessageType.AGENT_STEP,
                status = MessageStatus.SENT,
                agentStepType = AgentStepType.TOOL_CALL,
                toolName = tc.functionName,
                toolQuery = tc.arguments
            )
            chatRepository.insertMessage(toolCallMsg)

            // Parse arguments
            val args = try {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(tc.arguments, mapType) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            // Execute tool
            val result = ToolRegistry.execute(tc.functionName, args)

            // Save tool result as AgentStep message
            val resultMsg = ChatMessage(
                conversationId = conversationId,
                content = result.result,
                isUser = false,
                contentType = MessageType.AGENT_STEP,
                status = MessageStatus.SENT,
                agentStepType = AgentStepType.TOOL_RESULT,
                toolName = tc.functionName,
                toolQuery = tc.arguments,
                toolResult = result.result
            )
            chatRepository.insertMessage(resultMsg)

            toolResults.add(mapOf(
                "tool_call_id" to tc.id,
                "result" to result.result
            ))
        }

        // Second pass: send tool results back to LLM for reasoning
        uiState = uiState.copy(agentState = AgentState.REASONING)
        val accumulatedContent2 = StringBuilder()

        // Build messages with tool results
        val toolCallMaps = toolCalls.map { tc ->
            mapOf(
                "id" to tc.id,
                "type" to tc.type,
                "function" to mapOf(
                    "name" to tc.functionName,
                    "arguments" to tc.arguments
                )
            )
        }

        chatRepository.streamChatWithToolResult(
            conversationId, messages, toolCallMaps, toolResults,
            modelId, temperature, topP, maxTokens,
            grade = getEffectiveGrade(), role = role,
            systemPrompt = systemPrompt, language = language
        ).collect { event ->
            when (event) {
                is StreamEvent.TextChunk -> {
                    accumulatedContent2.append(event.content)
                    uiState = uiState.copy(
                        streamingContent = accumulatedContent2.toString(),
                        agentState = AgentState.RESPONDING
                    )
                }
                is StreamEvent.ToolCallChunk -> {
                    // Handle multi-turn tool calling (max 3 rounds)
                    uiState = uiState.copy(agentState = AgentState.SEARCHING)
                }
                is StreamEvent.Done -> {
                    finalizeStream(msgId, accumulatedContent2.toString(), conversationId)
                }
                is StreamEvent.ErrorEvent -> {
                    uiState = uiState.copy(errorMessage = event.message, agentState = AgentState.IDLE)
                }
                else -> {}
            }
        }
    }

    /**
     * 普通模式（非 Agent）流式对话
     */
    private suspend fun normalStreamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        msgId: Long,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        role: String?,
        systemPrompt: String?,
        language: String?
    ) {
        val accumulatedContent = StringBuilder()
        chatRepository.streamChat(
            conversationId = conversationId,
            messages = messages,
            modelId = modelId,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            grade = getEffectiveGrade(),
            role = role,
            systemPrompt = systemPrompt,
            language = language
        ).collect { chunk ->
            accumulatedContent.append(chunk)
            val currentContent = accumulatedContent.toString()
            uiState = uiState.copy(streamingContent = currentContent)

            if (uiState.chatMode != ChatMode.ASSISTANT) {
                processTeachingEvents(currentContent)
            }
        }

        val finalContent = accumulatedContent.toString()
        val cleanedContent = if (uiState.chatMode != ChatMode.ASSISTANT) {
            processTeachingResponseUseCase.stripTeachingMarkers(finalContent)
        } else {
            finalContent
        }

        finalizeStream(msgId, cleanedContent, conversationId)
    }

    /**
     * 流结束处理：持久化消息 + 更新状态
     */
    private suspend fun finalizeStream(msgId: Long, content: String, conversationId: Long) {
        chatRepository.updateMessageStatus(msgId, MessageStatus.SENT)

        if (content.isNotEmpty()) {
            val updatedMessage = ChatMessage(
                id = msgId,
                conversationId = conversationId,
                content = content,
                isUser = false,
                status = MessageStatus.SENT,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(updatedMessage)
        }

        // F32: Send notification if app is not in foreground
        if (!AppLifecycleTracker.isInForeground) {
            val conv = chatRepository.getConversationById(conversationId)
            val title = conv?.title?.ifBlank { null } ?: "AI 助手"
            val preview = content.take(120).replace('\n', ' ')
            NotificationHelper.sendMessageNotification(
                context = appContext,
                title = title,
                content = preview,
                conversationId = conversationId
            )
        }

        uiState = uiState.copy(
            isStreaming = false,
            streamingContent = "",
            agentState = AgentState.IDLE
        )
    }

    // ===== System Prompt =====

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
                if (gradeHint != null) "$basePrompt\n\n$gradeHint" else basePrompt
            }
            ChatMode.QUIZ -> {
                val gradeHint = getDifficultySystemPrompt()
                val basePrompt = "你是一个AI测验官，请通过提问来检验学生的知识掌握程度。" +
                        "每次问一个选择题或简答题，等待学生回答。" +
                        "回答正确时给予表扬，回答错误时给出正确答案和解释。" +
                        "题目难度应循序渐进。"
                if (gradeHint != null) "$basePrompt\n\n$gradeHint" else basePrompt
            }
        }
    }

    // ===== Retry / Misc =====

    fun retrySend() {
        val lastFailed = uiState.messages.lastOrNull { it.status == MessageStatus.FAILED && it.isUser }
        if (lastFailed != null) {
            viewModelScope.launch {
                chatRepository.updateMessageStatus(lastFailed.id, MessageStatus.SENT)
            }
            doSendMessage(lastFailed.content)
        }
    }

    fun toggleConversationSheet() {
        uiState = uiState.copy(showConversationSheet = !uiState.showConversationSheet)
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

    // ===== v2.5 F1: Long-press operations =====

    fun copyMessage(content: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", content)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(appContext, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun setFeedbackTarget(messageId: Long) {
        uiState = uiState.copy(feedbackTargetMessageId = messageId)
    }

    fun clearFeedbackTarget() {
        uiState = uiState.copy(feedbackTargetMessageId = null)
    }

    // ===== v2.5 F2: Feedback =====

    fun submitFeedback(messageId: Long, positive: Boolean) {
        viewModelScope.launch {
            val feedback = if (positive) "POSITIVE" else "NEGATIVE"
            chatRepository.updateMessageFeedback(messageId, feedback)
            // Show toast
            val label = if (positive) "👍 感谢反馈" else "👎 感谢反馈"
            android.widget.Toast.makeText(appContext, label, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ===== v2.5 F3: Favorite =====

    fun toggleFavorite(messageId: Long) {
        viewModelScope.launch {
            // Read current message to get its favorite status
            val messages = uiState.messages
            val msg = messages.find { it.id == messageId } ?: return@launch
            val newFavorite = !msg.isFavorite
            chatRepository.toggleFavorite(messageId, newFavorite)
            val label = if (newFavorite) "⭐ 已收藏" else "已取消收藏"
            android.widget.Toast.makeText(appContext, label, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchSolveSteps(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val result = solveRepository.getSolveSteps(
                question = question,
                grade = uiState.userGrade.orEmpty(),
                subject = uiState.difficultyLevel
            )
            result.onSuccess { response ->
                val stepsText = response.steps.mapIndexed { i, step ->
                    "**步骤 ${i + 1}: ${step.title}**\n\n${step.content}" +
                        (step.formula?.let { "\n\n$${it}$" } ?: "")
                }.joinToString("\n\n---\n\n")
                val stepMessage = ChatMessage(
                    conversationId = uiState.currentConversationId,
                    content = "### 📖 分步讲解\n\n$stepsText",
                    isUser = false,
                    contentType = MessageType.TEXT,
                    status = MessageStatus.SENT
                )
            }.onFailure {
                uiState = uiState.copy(errorMessage = "获取讲解失败: ${it.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        idleTimeoutJob?.cancel()
    }
}
