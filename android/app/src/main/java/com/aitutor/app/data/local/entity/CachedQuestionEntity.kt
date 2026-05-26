package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val content: String,
    val options: String, // JSON string of options
    val correctAnswer: String,
    val explanation: String,
    val category: String,
    val difficulty: String,
    val cachedAt: Long = System.currentTimeMillis()
)
