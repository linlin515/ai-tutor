package com.aitutor.app.ui.chat

import com.aitutor.app.domain.model.AgentState
import com.aitutor.app.domain.model.ToolCallInfo
import com.aitutor.app.domain.model.ToolResult

/**
 * UI state for the agent panel within the chat screen.
 *
 * @param agentEnabled Whether the AI agent is currently enabled by the user.
 * @param agentState The current execution state of the agent.
 * @param currentToolCall The tool call currently being executed, if any.
 * @param toolResult The result of the most recently executed tool, if any.
 * @param thoughtProcess A chronological list of thought-step strings from the agent.
 */
data class AgentUiState(
    val agentEnabled: Boolean = false,
    val agentState: AgentState = AgentState.IDLE,
    val currentToolCall: ToolCallInfo? = null,
    val toolResult: ToolResult? = null,
    val thoughtProcess: List<String> = emptyList()
)
