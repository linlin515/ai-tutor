package com.aitutor.app.domain.model

data class Flashcard(
    val id: String,
    val sourceId: String,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = "",
    val category: String = "",
    val difficulty: String = "medium",
    val masteryLevel: Float = 0f,
    val intervalDays: Int = 1,
    val nextReviewAt: Long = System.currentTimeMillis()
)
