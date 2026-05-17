package com.aitutor.app.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "default",
    val systemPrompt: String? = null,
    val messageCount: Int = 0
)
