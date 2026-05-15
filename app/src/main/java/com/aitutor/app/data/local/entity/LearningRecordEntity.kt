package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val learnDurationMin: Int = 0,
    val solveCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val streakDays: Int = 0,
    val totalKnowledgePoints: Int = 0,
    val masteredPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
