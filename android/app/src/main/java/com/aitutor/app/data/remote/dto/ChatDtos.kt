package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>,
    @SerializedName("stream") val stream: Boolean,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("top_p") val topP: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("role") val role: String? = null,  // "tutor" or "assistant"
    @SerializedName("system_prompt") val systemPrompt: String? = null,
    // v2.0 Agent: 工具调用
    @SerializedName("tools") val tools: List<ToolDefinitionDto>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null,
    // v2.0 F51: 多语言支持 - AI 回复语言
    @SerializedName("language") val language: String? = null
)

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionChunk(
    @SerializedName("id") val id: String?,
    @SerializedName("object") val obj: String?,
    @SerializedName("choices") val choices: List<ChunkChoice>?
)

data class ChunkChoice(
    @SerializedName("index") val index: Int?,
    @SerializedName("delta") val delta: Delta?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Delta(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?,
    // v2.0 Agent: 工具调用增量
    @SerializedName("tool_calls") val toolCalls: List<ToolCallChunkDto>? = null
)

data class ChatAskRequest(
    @SerializedName("content") val content: String,
    @SerializedName("subject") val subject: String?,
    @SerializedName("conversation_id") val conversationId: Long?
)

data class ChatAskResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("tokens_used") val tokensUsed: Int?,
    @SerializedName("conversation_id") val conversationId: Long?
)

data class ChatHistoryItem(
    @SerializedName("id") val id: Long,
    @SerializedName("content") val content: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("subject") val subject: String?,
    @SerializedName("created_at") val createdAt: String
)

data class ModelListResponse(
    @SerializedName("models") val models: List<ModelDto>
)

data class ModelDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("provider") val provider: String
)

data class SolvePhotoResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("subject") val subject: String?,
    @SerializedName("tokens_used") val tokensUsed: Int?
)
