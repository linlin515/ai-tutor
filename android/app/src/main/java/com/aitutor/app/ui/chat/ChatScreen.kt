package com.aitutor.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.AgentState
import com.aitutor.app.domain.model.AgentStepType
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.ChatMode
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.domain.model.TeachingState
import com.aitutor.app.ui.chat.components.AgentStatusIndicator
import com.aitutor.app.ui.chat.components.AgentSwitch
import com.aitutor.app.ui.chat.components.AgentThoughtBubble
import com.aitutor.app.ui.chat.components.ChatInputBar
import com.aitutor.app.ui.chat.components.CollapsibleStepCard
import com.aitutor.app.ui.chat.components.DifficultySwitcher
import com.aitutor.app.ui.chat.components.MessageBubble
import com.aitutor.app.ui.chat.components.SocraticBanner
import com.aitutor.app.ui.chat.components.SocraticQuestionBubble
import com.aitutor.app.ui.chat.components.StepProgressIndicator
import com.aitutor.app.ui.chat.components.StreamingText
import com.aitutor.app.ui.chat.components.TeachingModeToggle
import com.aitutor.app.ui.chat.components.UnderstandingBadge
import com.aitutor.app.ui.chat.components.VoiceInputBar
import com.aitutor.app.ui.common.EmptyStateView
import com.aitutor.app.ui.components.NetworkBanner
import com.aitutor.app.ui.conversation.ConversationListSheet
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long? = null,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val listState = rememberLazyListState()
    var showVoicePanel by remember { mutableStateOf(false) }
    var showModeSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // RECORD_AUDIO runtime permission
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) showVoicePanel = true
    }

    fun handleVoiceClick() {
        if (hasAudioPermission) {
            showVoicePanel = !showVoicePanel
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Select conversation if provided
    LaunchedEffect(conversationId) {
        if (conversationId != null && conversationId > 0) {
            viewModel.selectConversation(conversationId)
        }
    }

    // Auto scroll to bottom on new messages
    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = state.conversations
                        .find { it.id == state.currentConversationId }?.title
                        ?: "AI 对话"
                    Text(
                        text = if (title.isEmpty()) "AI 对话" else title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleConversationSheet() }) {
                        Icon(Icons.Default.Menu, contentDescription = "会话列表")
                    }
                },
                actions = {
                    // Teaching mode toggle (F42)
                    IconButton(onClick = { showModeSelector = !showModeSelector }) {
                        Icon(
                            imageVector = when (state.chatMode) {
                                ChatMode.ASSISTANT -> Icons.Default.AutoAwesome
                                ChatMode.TUTOR -> Icons.Default.Psychology
                                ChatMode.QUIZ -> Icons.Default.Quiz
                            },
                            contentDescription = "教学模式",
                            tint = if (state.chatMode != ChatMode.ASSISTANT)
                                   MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Difficulty level button (F41)
                    IconButton(onClick = { viewModel.toggleDifficultySwitcher() }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "讲解难度",
                            tint = if (state.showDifficultySwitcher)
                                   MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // v2.0 Agent: Agent 模式切换按钮
                    IconButton(onClick = { viewModel.toggleAgentSwitch() }) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Agent 模式",
                            tint = if (state.agentEnabled)
                                   MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建对话")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "个人中心")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Mode selector dropdown (F42)
                if (showModeSelector) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "教学模式",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            TeachingModeToggle(
                                currentMode = state.chatMode,
                                onModeChange = { mode ->
                                    viewModel.setChatMode(mode)
                                    showModeSelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Difficulty switcher (F41)
                if (state.showDifficultySwitcher) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DifficultySwitcher(
                            currentLevel = state.difficultyLevel,
                            onLevelChange = { level ->
                                viewModel.setDifficultyLevel(level)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // v2.0 Agent: Agent 模式开关
                if (state.showAgentSwitch) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AgentSwitch(
                            enabled = state.agentEnabled,
                            onToggle = { viewModel.toggleAgentMode() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ChatInputBar(
                    inputText = state.inputText,
                    onTextChange = { viewModel.updateInputText(it) },
                    onSend = { viewModel.sendMessage() },
                    onVoiceClick = { handleVoiceClick() },
                    onAddAttachment = onNavigateToCamera,
                    enabled = !state.isStreaming,
                    isOnline = state.isOnline
                )
            }
        }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (state.messages.isEmpty() && !state.isStreaming) {
                // Empty state with mode-specific hints
                EmptyStateView(
                    title = "开始一段新的对话吧",
                    subtitle = when (state.chatMode) {
                        ChatMode.ASSISTANT -> "输入问题或点击麦克风语音输入"
                        ChatMode.TUTOR -> "当前为苏格拉底式教学模式，AI 会通过提问引导你思考"
                        ChatMode.QUIZ -> "当前为测验模式，AI 会出题检验你的知识掌握程度"
                    }
                )
            } else {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Socratic teaching banner (F42) — shown at the top when in teaching mode
                    if (state.chatMode != ChatMode.ASSISTANT) {
                        item(key = "teaching_banner") {
                            SocraticBanner(
                                chatMode = state.chatMode,
                                onDismiss = { viewModel.setChatMode(ChatMode.ASSISTANT) }
                            )
                        }
                    }

                    // Difficulty banner — shown briefly when a difficulty is selected (F41)
                    if (state.difficultyLevel != "auto" && state.chatMode == ChatMode.TUTOR) {
                        item(key = "difficulty_indicator") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "当前讲解难度：${state.difficultyLevel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Teaching state indicators (F42)
                    if (state.teachingState is TeachingState.Questioning) {
                        item(key = "step_progress") {
                            StepProgressIndicator(
                                currentStep = state.teachingState.step,
                                totalSteps = state.teachingState.totalSteps,
                                stepTitle = "引导提问"
                            )
                        }
                    }

                    if (state.teachingState is TeachingState.Evaluating) {
                        item(key = "understanding_badge") {
                            UnderstandingBadge(
                                level = if (state.teachingState.isCorrect) 0.85f else 0.35f,
                                label = if (state.teachingState.isCorrect) "回答正确！" else "再想想...",
                                feedback = state.teachingState.feedback
                            )
                        }
                    }

                    if (state.teachingState is TeachingState.Complete) {
                        item(key = "teaching_complete") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "教学完成！你已经掌握了这个知识点。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Messages
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        if (message.contentType == MessageType.AGENT_STEP && message.agentStepType != null) {
                            // v2.0 Agent: Agent 步骤可视化
                            AgentThoughtBubble(
                                agentStepType = message.agentStepType,
                                toolName = message.toolName,
                                toolQuery = message.toolQuery,
                                toolResult = message.toolResult
                            )
                        } else if (!message.isUser && isStepContent(message.content)) {
                            // Render step-based messages as CollapsibleStepCards (F41)
                            val steps = parseSteps(message.content)
                            steps.forEach { (stepNum, totalSteps, title, content) ->
                                CollapsibleStepCard(
                                    stepNumber = stepNum,
                                    totalSteps = totalSteps,
                                    title = title,
                                    content = content,
                                    isCompleted = stepNum < totalSteps,
                                    isLocked = false,
                                    isExpanded = stepNum == 1
                                )
                            }
                        } else if (!message.isUser && isSocraticQuestion(message.content)) {
                            // Render Socratic questions with special bubble (F42)
                            val cleanContent = stripMarkers(message.content)
                            val socraticQuestion = extractQuestion(cleanContent)
                            SocraticQuestionBubble(
                                question = socraticQuestion,
                                stepNumber = calculateStep(state.messages, message),
                                totalSteps = estimateTotalSteps(state.messages, message),
                                hint = extractHint(cleanContent)
                            )
                        } else {
                            MessageBubble(
                                message = message,
                                onRetry = {
                                    if (message.status == MessageStatus.FAILED) {
                                        viewModel.retrySend()
                                    }
                                },
                                onSpeak = {
                                    if (!message.isUser && message.content.isNotEmpty()) {
                                        viewModel.speakText(message.content)
                                    }
                                },
                                // v2.5 F1: Long-press menu callbacks
                                onCopy = { content -> viewModel.copyMessage(content) },
                                onFavorite = { id -> viewModel.toggleFavorite(id) },
                                onFeedback = { id -> viewModel.setFeedbackTarget(id) },
                                // v2.5 F2: Thumbs up/down
                                onThumbsUp = { id -> viewModel.submitFeedback(id, true) },
                                onThumbsDown = { id -> viewModel.submitFeedback(id, false) }
                            )
                        }
                    }

                    // Socratic question for streaming content (F42)
                    if (state.isStreaming && state.teachingState is TeachingState.Questioning) {
                        item(key = "streaming_question") {
                            SocraticQuestionBubble(
                                question = state.teachingState.question,
                                stepNumber = state.teachingState.step,
                                totalSteps = state.teachingState.totalSteps
                            )
                        }
                    }

                    // v2.0 Agent: Agent 状态指示器
                    if (state.agentEnabled && state.agentState != AgentState.IDLE) {
                        item(key = "agent_status") {
                            AgentStatusIndicator(agentState = state.agentState)
                        }
                    }

                    // Streaming AI message
                    if (state.isStreaming) {
                        item(key = "streaming") {
                            StreamingText(
                                text = state.streamingContent,
                                isStreaming = true
                            )
                        }
                    }
                }
            }

            // P1-2: Network disconnected banner at top
            NetworkBanner(
                isOnline = state.isOnline,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Error
            if (state.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(state.errorMessage)
                }
            }

            // Voice panel overlay
            if (showVoicePanel) {
                VoiceInputBar(
                    voiceState = state.voiceState,
                    onDismiss = { showVoicePanel = false }
                )
            }

            // Conversation list bottom sheet
            if (state.showConversationSheet) {
                ConversationListSheet(
                    conversations = state.conversations,
                    currentConversationId = state.currentConversationId,
                    searchKeyword = state.searchKeyword,
                    onSelectConversation = { id ->
                        viewModel.selectConversation(id)
                        viewModel.hideConversationSheet()
                    },
                    onDeleteConversation = { id ->
                        viewModel.deleteConversation(id)
                    },
                    onNewConversation = {
                        viewModel.createNewConversation()
                        viewModel.hideConversationSheet()
                    },
                    onSearchKeywordChange = { viewModel.updateSearchKeyword(it) },
                    onDismiss = { viewModel.hideConversationSheet() }
                )
            }
        }
    }
}

// ===== Helper functions for step/Socratic content detection =====

/**
 * Check if message content contains structured step information (F41).
 * Steps are delimited by "## Step N" or "第N步" or numbered lists.
 */
private fun isStepContent(content: String): Boolean {
    if (content.isBlank()) return false
    // Check for step markers
    val stepPatterns = listOf(
        Regex("##\\s*步骤\\s*\\d+", RegexOption.IGNORE_CASE),
        Regex("##\\s*第\\s*\\d+\\s*步", RegexOption.IGNORE_CASE),
        Regex("步骤\\s*\\d+[:：]", RegexOption.IGNORE_CASE),
        Regex("第\\d+步[:：]", RegexOption.IGNORE_CASE),
        Regex("Step\\s*\\d+", RegexOption.IGNORE_CASE)
    )
    return stepPatterns.any { it.containsMatchIn(content) }
}

/**
 * Parse message content into structured steps (F41).
 * Returns list of (stepNumber, totalSteps, title, content).
 */
private fun parseSteps(content: String): List<StepInfo> {
    val steps = mutableListOf<StepInfo>()
    val lines = content.split("\n")

    var currentStep = 0
    var currentTitle = ""
    val currentLines = mutableListOf<String>()
    var totalSteps = 0

    // First pass: count total steps
    val stepHeaderRegex = Regex(
        "(?:##\\s*)?(?:步骤|第\\s*(\\d+)\\s*步|Step\\s*(\\d+))\\s*[:：]?\\s*(.*)",
        RegexOption.IGNORE_CASE
    )
    for (line in lines) {
        val match = stepHeaderRegex.find(line)
        if (match != null) {
            totalSteps++
        }
    }
    if (totalSteps == 0) totalSteps = 1

    // Second pass: extract steps
    for (line in lines) {
        val match = stepHeaderRegex.find(line)
        if (match != null) {
            // Save previous step
            if (currentStep > 0) {
                steps.add(
                    StepInfo(
                        stepNumber = currentStep,
                        totalSteps = totalSteps,
                        title = currentTitle,
                        content = currentLines.joinToString("\n").trim()
                    )
                )
            }
            currentLines.clear()

            // Parse new step header
            val numFromCapture = match.groupValues[1].toIntOrNull()
            val numFromCapture2 = match.groupValues[2].toIntOrNull()
            currentStep = numFromCapture ?: numFromCapture2 ?: (currentStep + 1)
            currentTitle = match.groupValues[3].trim()
        } else {
            currentLines.add(line)
        }
    }

    // Add last step
    if (currentStep > 0) {
        steps.add(
            StepInfo(
                stepNumber = currentStep,
                totalSteps = totalSteps,
                title = currentTitle,
                content = currentLines.joinToString("\n").trim()
            )
        )
    }

    // If no steps were parsed but content is present, create a single step
    if (steps.isEmpty() && content.isNotBlank()) {
        steps.add(
            StepInfo(
                stepNumber = 1,
                totalSteps = 1,
                title = "讲解",
                content = content
            )
        )
    }

    return steps
}

/**
 * Check if message content contains a Socratic question (F42).
 */
private fun isSocraticQuestion(content: String): Boolean {
    return content.contains("[socratic_question]") ||
            content.contains("[correctness]") ||
            content.contains("[teaching_complete]")
}

/**
 * Strip markers from content for display.
 */
private fun stripMarkers(content: String): String {
    return content
        .replace("[socratic_question]", "")
        .replace("[correctness]", "")
        .replace("[teaching_complete]", "")
        .replace("[mode_switch]", "")
        .trim()
}

/**
 * Extract the actual question text from content with markers.
 */
private fun extractQuestion(content: String): String {
    // If there's a [socratic_question] marker, extract the text after it
    val cleaned = stripMarkers(content)
    // Take first paragraph or first 200 chars
    val firstParagraph = cleaned.split("\n\n").firstOrNull()?.trim() ?: cleaned
    return firstParagraph.take(500)
}

/**
 * Extract hint text if present.
 */
private fun extractHint(content: String): String? {
    return content.split("\n\n").drop(1).firstOrNull()?.trim()?.take(200)
}

/**
 * Calculate the current step number for a message in the conversation.
 */
private fun calculateStep(allMessages: List<ChatMessage>, currentMessage: ChatMessage): Int {
    val index = allMessages.indexOf(currentMessage)
    if (index < 0) return 1
    // Count AI messages up to this point
    return allMessages.take(index + 1).count { !it.isUser }
}

/**
 * Estimate total steps for a teaching session.
 */
private fun estimateTotalSteps(allMessages: List<ChatMessage>, currentMessage: ChatMessage): Int {
    val currentIndex = allMessages.indexOf(currentMessage)
    if (currentIndex < 0) return 1
    val remainingMessages = allMessages.size - currentIndex
    // Rough estimate: assume at least as many more as have passed
    return maxOf(currentIndex + 1, remainingMessages) + 2
}

/**
 * Data class representing a parsed step.
 */
private data class StepInfo(
    val stepNumber: Int,
    val totalSteps: Int,
    val title: String,
    val content: String
)

// ===== Preview =====
@Preview(name = "AI 对话 预览", showBackground = true, backgroundColor = 0xFF1C1B1F, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "AI 对话 预览 (深色)", showBackground = true, backgroundColor = 0xFFFEFBFF, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChatScreen() { AiTutorTheme { ChatScreen() } }
