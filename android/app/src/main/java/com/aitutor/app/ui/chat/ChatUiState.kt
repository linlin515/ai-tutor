package com.aitutor.app.ui.chat

import com.aitutor.app.domain.model.AgentState
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.ChatMode
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.TeachingState
import com.aitutor.app.domain.model.ToolCallInfo
import com.aitutor.app.domain.model.ToolResult
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
    val searchKeyword: String = "",
    // Feature 2 (F41): Adaptive teaching & grade awareness
    val tutorMode: Boolean = false,
    val userGrade: String? = null,
    // Feature 2 (F41): Self-adaptive step-by-step difficulty
    val difficultyLevel: String = "auto",  // "auto", "小学", "初中", "高中", "大学"
    // Feature 3 (F42): Socratic teaching mode
    val chatMode: ChatMode = ChatMode.ASSISTANT,
    val teachingState: TeachingState = TeachingState.Idle,
    // Timestamp for idle timeout auto-exit (F42: 30 min)
    val tutorModeLastActiveTime: Long = System.currentTimeMillis(),
    // Whether the difficulty switcher is visible
    val showDifficultySwitcher: Boolean = false,
    // v2.0 Agent: Agent 模式状态
    val agentEnabled: Boolean = false,
    val agentState: AgentState = AgentState.IDLE,
    val showAgentSwitch: Boolean = false,
    // P1-2: Network state
    val isOnline: Boolean = true,
    // v2.5 F1: Message long-press feedback target
    val feedbackTargetMessageId: Long? = null
)
