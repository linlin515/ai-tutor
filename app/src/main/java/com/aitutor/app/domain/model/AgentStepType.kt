package com.aitutor.app.domain.model

/**
 * Represents the type of a single step within the agent's execution trace.
 */
enum class AgentStepType {
    THOUGHT,
    TOOL_CALL,
    TOOL_RESULT,
    OBSERVATION
}
