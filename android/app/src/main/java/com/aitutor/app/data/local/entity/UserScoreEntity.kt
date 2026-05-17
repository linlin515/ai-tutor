package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_score")
data class UserScoreEntity(
    @PrimaryKey val id: String = "user_score",
    val totalScore: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLearningDate: String = "", // 逗号分隔的日期列表 yyyy-MM-dd
    val updatedAt: Long = System.currentTimeMillis()
)
