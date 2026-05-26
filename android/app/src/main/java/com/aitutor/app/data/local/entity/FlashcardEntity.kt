package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["sourceId"], unique = true)]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val question: String,
    val options: String, // JSON string
    val correctAnswer: String,
    val explanation: String,
    val category: String = "",
    val difficulty: String = "medium",
    val masteryLevel: Float = 0f,
    val intervalDays: Int = 1,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
