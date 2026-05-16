package com.aitutor.app.domain.engine

import com.aitutor.app.domain.model.Achievement
import com.aitutor.app.domain.model.AchievementCondition
import com.aitutor.app.domain.model.AchievementStatus
import com.aitutor.app.domain.model.AchievementWithStatus
import com.aitutor.app.domain.model.UserScore

/**
 * 成就检测器
 */
object AchievementDetector {

    /**
     * 检查当前用户数据是否满足未解锁成就的条件
     * @param userScore 当前用户积分/连续数据
     * @param lockedAchievements 尚未解锁的成就列表
     * @return 新解锁的成就列表
     */
    fun checkAchievements(
        userScore: UserScore,
        lockedAchievements: List<AchievementWithStatus>
    ): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()

        for (aws in lockedAchievements) {
            if (aws.status == AchievementStatus.UNLOCKED) continue
            val achievement = aws.achievement
            if (isSatisfied(achievement, userScore, achievement.condition)) {
                newlyUnlocked.add(achievement)
            }
        }

        return newlyUnlocked
    }

    /**
     * 检查用户数据是否满足指定成就条件
     */
    private fun isSatisfied(
        achievement: Achievement,
        userScore: UserScore,
        condition: AchievementCondition
    ): Boolean {
        return when (condition) {
            is AchievementCondition.SolveCount -> {
                // SolveCount 需要额外的 solveTotal 数据，这里暂以总分间接推断
                // 实际应将 solveTotal 也放入 UserScore 或通过额外参数传入
                userScore.totalScore >= condition.count * 10 // 粗略估算
            }
            is AchievementCondition.StreakDays -> {
                userScore.longestStreak >= condition.days
            }
            is AchievementCondition.QuizCorrectCount -> {
                userScore.totalScore >= condition.count * 2 // 粗略估算
            }
            is AchievementCondition.QuizPerfect -> {
                // 需要额外数据，这里通过触发事件时直接检测
                false
            }
            is AchievementCondition.ReviewCount -> {
                userScore.totalScore >= condition.count * 15 // 粗略估算
            }
            is AchievementCondition.TotalScore -> {
                userScore.totalScore >= condition.score
            }
        }
    }

    /**
     * 直接检测某个成就条件是否满足
     */
    fun checkConditionDirectly(
        condition: AchievementCondition,
        currentValue: Int
    ): Boolean {
        return when (condition) {
            is AchievementCondition.SolveCount -> currentValue >= condition.count
            is AchievementCondition.StreakDays -> currentValue >= condition.days
            is AchievementCondition.QuizCorrectCount -> currentValue >= condition.count
            is AchievementCondition.QuizPerfect -> currentValue == 1 // 1表示完美完成
            is AchievementCondition.ReviewCount -> currentValue >= condition.count
            is AchievementCondition.TotalScore -> currentValue >= condition.score
        }
    }
}
