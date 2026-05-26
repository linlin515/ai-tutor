package com.aitutor.app.data.remote.dto

data class FlashcardPullResponse(
    val cards: List<FlashcardDto> = emptyList()
)

data class FlashcardDto(
    val id: String,
    val sourceId: String,
    val question: String,
    val options: String = "[]",
    val correctAnswer: String,
    val explanation: String = "",
    val category: String = "",
    val difficulty: String = "medium",
    val masteryLevel: Float = 0f,
    val intervalDays: Int = 1,
    val nextReviewAt: Long = 0L
)

data class FlashcardReviewRequest(
    val flashcardId: String,
    val rating: Int
)

data class FlashcardReviewLogDto(
    val id: String,
    val flashcardId: String,
    val rating: Int,
    val reviewedAt: Long
)

data class FlashcardSyncRequest(
    val logs: List<FlashcardReviewLogDto>
)
