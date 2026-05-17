package com.aitutor.app.domain.model

/**
 * Represents the possible states of the AI agent.
 */
enum class AgentState {
    IDLE,
    THINKING,
    SEARCHING,
    REASONING,
    RESPONDING
}

/**
 * Sealed class representing events that trigger state transitions.
 */
sealed class AgentEvent {
    data object StartThinking : AgentEvent()
    data class ToolCall(val calls: List<ToolCallInfo>) : AgentEvent()
    data object ToolExecuted : AgentEvent()
    data object StreamStarted : AgentEvent()
    data object StreamComplete : AgentEvent()
}

/**
 * Information about a single tool call made by the agent.
 */
data class ToolCallInfo(
    val id: String,
    val type: String,
    val functionName: String,
    val arguments: String
)

/**
 * A finite state machine that manages transitions between [AgentState] values
 * based on incoming [AgentEvent]s.
 *
 * @param state The current state of the machine.
 */
data class AgentStateMachine(
    val state: AgentState = AgentState.IDLE
) {
    /**
     * Transitions to a new state based on the given [event].
     * Returns a new [AgentStateMachine] with the updated state.
     */
    fun transition(event: AgentEvent): AgentStateMachine {
        val newState = when (state) {
            AgentState.IDLE -> when (event) {
                is AgentEvent.StartThinking -> AgentState.THINKING
                else -> state
            }
            AgentState.THINKING -> when (event) {
                is AgentEvent.ToolCall -> AgentState.SEARCHING
                is AgentEvent.StreamStarted -> AgentState.RESPONDING
                else -> state
            }
            AgentState.SEARCHING -> when (event) {
                is AgentEvent.ToolExecuted -> AgentState.REASONING
                else -> state
            }
            AgentState.REASONING -> when (event) {
                is AgentEvent.ToolCall -> AgentState.SEARCHING
                is AgentEvent.StreamStarted -> AgentState.RESPONDING
                is AgentEvent.StartThinking -> AgentState.THINKING
                else -> state
            }
            AgentState.RESPONDING -> when (event) {
                is AgentEvent.StreamComplete -> AgentState.IDLE
                else -> state
            }
        }
        return copy(state = newState)
    }
}
