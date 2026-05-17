package com.aitutor.app.domain.model

/**
 * 成就状态枚举
 */
enum class AchievementStatus {
    LOCKED,
    UNLOCKED
}

/**
 * 成就及其解锁状态
 */
data class AchievementWithStatus(
    val achievement: Achievement,
    val status: AchievementStatus = AchievementStatus.LOCKED,
    val unlockedAt: Long? = null
)
