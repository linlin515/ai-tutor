package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_review_logs")
data class FlashcardReviewLogEntity(
    @PrimaryKey val id: String,
    val flashcardId: String,
    val userId: String = "",
    val rating: Int, // 0=again, 1=hard, 2=good, 3=easy
    val reviewedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
