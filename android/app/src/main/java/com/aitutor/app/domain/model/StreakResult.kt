package com.aitutor.app.domain.model

/**
 * 连续学习结果
 */
data class StreakResult(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val hasLearnedToday: Boolean = false
)
