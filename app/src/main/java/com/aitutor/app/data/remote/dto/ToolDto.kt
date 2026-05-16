package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * v2.0 Agent: OpenAI 兼容的工具定义 DTO（用于请求体中的 tools 参数）
 */
data class ToolDefinitionDto(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: ToolFunctionDto
)

data class ToolFunctionDto(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("parameters") val parameters: Map<String, Any>? = null
)

/**
 * v2.0 Agent: SSE delta 中解析到的工具调用信息
 */
data class ToolCallChunkDto(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("function") val function: ToolCallFunctionDto? = null
)

data class ToolCallFunctionDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("arguments") val arguments: String? = null
)

/**
 * v2.0 Agent: 工具结果回传消息体（role = "tool"）
 */
data class ToolMessageDto(
    @SerializedName("role") val role: String = "tool",
    @SerializedName("content") val content: String,
    @SerializedName("tool_call_id") val toolCallId: String
)

/**
 * v2.0 Agent: 扩展 ChatCompletionRequest，新增 tools 和 tool_choice 字段
 */
data class AgentCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>,
    @SerializedName("stream") val stream: Boolean,
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("top_p") val topP: Float = 1.0f,
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    @SerializedName("tools") val tools: List<ToolDefinitionDto>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null
)
