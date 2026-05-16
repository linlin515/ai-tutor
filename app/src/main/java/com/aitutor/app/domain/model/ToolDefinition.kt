package com.aitutor.app.domain.model

/**
 * Describes a tool that the AI agent can invoke.
 *
 * @param name The unique name of the tool (e.g. "web_search", "calculator").
 * @param description A natural-language description of what the tool does.
 * @param parameters A JSON-schema-style map defining the expected parameters.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

/**
 * Encapsulates the result returned from executing a tool.
 *
 * @param toolName The name of the tool that was executed.
 * @param query The original query or input sent to the tool, if applicable.
 * @param result The output produced by the tool (plain text).
 * @param durationMs How long the tool took to execute, in milliseconds.
 * @param isError Whether the tool execution resulted in an error.
 */
data class ToolResult(
    val toolName: String,
    val query: String?,
    val result: String,
    val durationMs: Long,
    val isError: Boolean = false
)
