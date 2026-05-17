package com.aitutor.app.domain.model

/**
 * Sealed class representing events emitted during a streaming response
 * from the AI agent.
 */
sealed class StreamEvent {

    /**
     * A chunk of text content streamed from the model.
     */
    data class TextChunk(val content: String) : StreamEvent()

    /**
     * One or more tool calls received as part of the stream.
     */
    data class ToolCallChunk(val toolCalls: List<ToolCallInfo>) : StreamEvent()

    /**
     * Indicates the role of the message author (e.g. "assistant", "user").
     */
    data class RoleChunk(val role: String) : StreamEvent()

    /**
     * Signals that the stream has completed successfully.
     */
    data object Done : StreamEvent()

    /**
     * Indicates that an error occurred during streaming.
     */
    data class ErrorEvent(val message: String) : StreamEvent()
}
