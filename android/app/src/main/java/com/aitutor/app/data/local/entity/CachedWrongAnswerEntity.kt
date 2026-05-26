package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_wrong_answers")
data class CachedWrongAnswerEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val questionContent: String,
    val userAnswer: String,
    val correctAnswer: String,
    val masteryScore: Float = 0f,
    val cachedAt: Long = System.currentTimeMillis()
)
