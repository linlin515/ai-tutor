package com.aitutor.app.data.remote.dto

import com.aitutor.app.data.local.entity.CachedConversationEntity
import com.aitutor.app.data.local.entity.CachedQuestionEntity
import com.aitutor.app.data.local.entity.CachedWrongAnswerEntity

/**
 * Request DTO for pushing offline actions to the server.
 */
data class SyncPushRequest(
    val actions: List<SyncActionDto>
)

data class SyncActionDto(
    val id: String,
    val type: String,
    val targetId: String,
    val payload: String,
    val createdAt: Long
)

/**
 * Response DTO for pulling incremental data from the server.
 */
data class SyncPullResponse(
    val questions: List<SyncQuestionDto> = emptyList(),
    val wrongAnswers: List<SyncWrongAnswerDto> = emptyList(),
    val conversations: List<SyncConversationDto> = emptyList(),
    val lastSyncTimestamp: Long = 0L
)

data class SyncQuestionDto(
    val id: String,
    val content: String,
    val options: String,
    val correctAnswer: String,
    val explanation: String,
    val category: String,
    val difficulty: String
)

data class SyncWrongAnswerDto(
    val id: String,
    val questionId: String,
    val questionContent: String,
    val userAnswer: String,
    val correctAnswer: String,
    val masteryScore: Float
)

data class SyncConversationDto(
    val id: String,
    val sessionTitle: String,
    val messages: String
)

/**
 * Converts SyncQuestionDto to CachedQuestionEntity.
 */
fun SyncQuestionDto.toEntity(): CachedQuestionEntity = CachedQuestionEntity(
    id = id,
    content = content,
    options = options,
    correctAnswer = correctAnswer,
    explanation = explanation,
    category = category,
    difficulty = difficulty,
    cachedAt = System.currentTimeMillis()
)

/**
 * Converts SyncWrongAnswerDto to CachedWrongAnswerEntity.
 */
fun SyncWrongAnswerDto.toEntity(): CachedWrongAnswerEntity = CachedWrongAnswerEntity(
    id = id,
    questionId = questionId,
    questionContent = questionContent,
    userAnswer = userAnswer,
    correctAnswer = correctAnswer,
    masteryScore = masteryScore,
    cachedAt = System.currentTimeMillis()
)

/**
 * Converts SyncConversationDto to CachedConversationEntity.
 */
fun SyncConversationDto.toEntity(): CachedConversationEntity = CachedConversationEntity(
    id = id,
    sessionTitle = sessionTitle,
    messages = messages,
    cachedAt = System.currentTimeMillis()
)
