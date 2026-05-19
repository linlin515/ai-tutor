package com.aitutor.app.domain.model

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val contentType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val metadata: String? = null,
    // v2.0 Agent: 新增字段
    val agentStepType: AgentStepType? = null,  // Agent 步骤类型
    val toolName: String? = null,              // 工具名称（如 web_search）
    val toolQuery: String? = null,             // 工具的输入参数
    val toolResult: String? = null,            // 工具的返回结果
    // v2.5 F2: AI 回复反馈
    val feedback: FeedbackType? = null,
    // v2.5 F3: 收藏
    val isFavorite: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE,
    AUDIO,
    AGENT_STEP  // v2.0 Agent: Agent 步骤消息类型
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}

enum class FeedbackType {
    POSITIVE,
    NEGATIVE
}
