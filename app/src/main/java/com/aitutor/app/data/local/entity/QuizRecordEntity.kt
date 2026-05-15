package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_records")
data class QuizRecordEntity(
    @PrimaryKey val quizId: String,
    val subject: String,
    val knowledgePoints: String = "[]",
    val difficulty: String = "medium",
    val questionCount: Int = 0,
    val score: Float? = null,
    val durationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
)
