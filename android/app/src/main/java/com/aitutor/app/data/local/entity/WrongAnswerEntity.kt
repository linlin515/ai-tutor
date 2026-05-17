package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wrong_answers")
data class WrongAnswerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val question: String,
    val correctAnswer: String,
    val userAnswer: String,
    val subject: String,
    val knowledgePoint: String = "",
    val source: String = "quiz", // chat / quiz / solve
    val intervalDays: Int = 1,
    val consecutiveCorrect: Int = 0,
    val isMastered: Boolean = false,
    val nextReviewAt: Long = System.currentTimeMillis() + (24L * 60 * 60 * 1000),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
