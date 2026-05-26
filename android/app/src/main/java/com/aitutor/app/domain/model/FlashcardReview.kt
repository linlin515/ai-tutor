package com.aitutor.app.domain.model

data class FlashcardReview(
    val id: String,
    val flashcardId: String,
    val rating: Int, // 0=again, 1=hard, 2=good, 3=easy
    val reviewedAt: Long = System.currentTimeMillis()
)
