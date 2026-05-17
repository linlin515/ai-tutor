package com.aitutor.app.domain.model

/**
 * 用户积分与连续学习信息
 */
data class UserScore(
    val totalScore: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLearningDate: String = "", // yyyy-MM-dd
    val hasLearnedToday: Boolean = false
)
