package com.aitutor.app.domain.model

data class WrongAnswerItem(
    val id: String,
    val question: String,
    val correctAnswer: String,
    val userAnswer: String,
    val subject: String,
    val knowledgePoint: String = "",
    val source: String = "quiz",
    val intervalDays: Int = 1,
    val consecutiveCorrect: Int = 0,
    val isMastered: Boolean = false,
    val nextReviewAt: Long = System.currentTimeMillis() + (24L * 60 * 60 * 1000),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
