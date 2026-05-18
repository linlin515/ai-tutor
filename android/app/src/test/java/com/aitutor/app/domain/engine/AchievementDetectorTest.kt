package com.aitutor.app.domain.engine

import com.aitutor.app.domain.model.Achievement
import com.aitutor.app.domain.model.AchievementCondition
import com.aitutor.app.domain.model.AchievementStatus
import com.aitutor.app.domain.model.AchievementWithStatus
import com.aitutor.app.domain.model.UserScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 单元测试：AchievementDetector
 *
 * 覆盖场景：
 * - checkAchievements 各条件类型
 * - UNLOCKED 成就跳过
 * - checkConditionDirectly 各条件
 * - 多成就同时解锁
 */
@DisplayName("AchievementDetector")
class AchievementDetectorTest {

    @Nested
    @DisplayName("checkAchievements(userScore, lockedAchievements)")
    inner class CheckAchievements {

        @Test
        @DisplayName("SolveCount 条件满足时解锁成就")
        fun `solveCount condition satisfied`() {
            val userScore = UserScore(totalScore = 100) // >= 1 * 10
            val locked = listOf(
                AchievementWithStatus(Achievement.FIRST_SOLVE, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(listOf(Achievement.FIRST_SOLVE), unlocked)
        }

        @Test
        @DisplayName("SolveCount 条件不满足时不解锁")
        fun `solveCount condition not satisfied`() {
            val userScore = UserScore(totalScore = 0) // < 1 * 10
            val locked = listOf(
                AchievementWithStatus(Achievement.FIRST_SOLVE, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertTrue(unlocked.isEmpty())
        }

        @Test
        @DisplayName("StreakDays 条件满足时解锁成就")
        fun `streakDays condition satisfied`() {
            val userScore = UserScore(longestStreak = 10)
            val locked = listOf(
                AchievementWithStatus(Achievement.STREAK_7, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(listOf(Achievement.STREAK_7), unlocked)
        }

        @Test
        @DisplayName("StreakDays 条件不满足时不解锁")
        fun `streakDays condition not satisfied`() {
            val userScore = UserScore(longestStreak = 3)
            val locked = listOf(
                AchievementWithStatus(Achievement.STREAK_7, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertTrue(unlocked.isEmpty())
        }

        @Test
        @DisplayName("TotalScore 条件满足时解锁成就")
        fun `totalScore condition satisfied`() {
            val userScore = UserScore(totalScore = 5000)
            val locked = listOf(
                AchievementWithStatus(Achievement.SCHOLAR, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(listOf(Achievement.SCHOLAR), unlocked)
        }

        @Test
        @DisplayName("TotalScore 条件不满足时不解锁")
        fun `totalScore condition not satisfied`() {
            val userScore = UserScore(totalScore = 4999)
            val locked = listOf(
                AchievementWithStatus(Achievement.SCHOLAR, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertTrue(unlocked.isEmpty())
        }

        @Test
        @DisplayName("QuizCorrectCount 条件估算满足")
        fun `quizCorrectCount condition satisfied`() {
            val userScore = UserScore(totalScore = 200) // >= 100 * 2
            val locked = listOf(
                AchievementWithStatus(Achievement.QUIZ_100, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(listOf(Achievement.QUIZ_100), unlocked)
        }

        @Test
        @DisplayName("ReviewCount 条件估算满足")
        fun `reviewCount condition satisfied`() {
            val userScore = UserScore(totalScore = 750) // >= 50 * 15
            val locked = listOf(
                AchievementWithStatus(Achievement.REVIEW_MASTER, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(listOf(Achievement.REVIEW_MASTER), unlocked)
        }

        @Test
        @DisplayName("已经 UNLOCKED 的成就应该被跳过")
        fun `already unlocked achievements are skipped`() {
            val userScore = UserScore(totalScore = 9999, longestStreak = 100)
            val locked = listOf(
                AchievementWithStatus(Achievement.STREAK_7, AchievementStatus.UNLOCKED),
                AchievementWithStatus(Achievement.SCHOLAR, AchievementStatus.UNLOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertTrue(unlocked.isEmpty())
        }

        @Test
        @DisplayName("QuizPerfect 条件始终返回 false（需事件触发）")
        fun `quizPerfect condition always false in checkAchievements`() {
            val userScore = UserScore(totalScore = 10000)
            val locked = listOf(
                AchievementWithStatus(Achievement.QUIZ_PERFECT, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertTrue(unlocked.isEmpty())
        }

        @Test
        @DisplayName("多成就同时满足条件时全部解锁")
        fun `multiple achievements unlocked at once`() {
            val userScore = UserScore(totalScore = 5000, longestStreak = 30)
            val locked = listOf(
                AchievementWithStatus(Achievement.FIRST_SOLVE, AchievementStatus.LOCKED),
                AchievementWithStatus(Achievement.STREAK_7, AchievementStatus.LOCKED),
                AchievementWithStatus(Achievement.STREAK_30, AchievementStatus.LOCKED),
                AchievementWithStatus(Achievement.SCHOLAR, AchievementStatus.LOCKED)
            )
            val unlocked = AchievementDetector.checkAchievements(userScore, locked)
            assertEquals(4, unlocked.size)
            assertTrue(unlocked.containsAll(listOf(
                Achievement.FIRST_SOLVE,
                Achievement.STREAK_7,
                Achievement.STREAK_30,
                Achievement.SCHOLAR
            )))
        }
    }

    @Nested
    @DisplayName("checkConditionDirectly(condition, currentValue)")
    inner class CheckConditionDirectly {

        @Test
        @DisplayName("SolveCount 条件：值 >= 计数时满足")
        fun `solveCount check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.SolveCount(5), 10
            ))
        }

        @Test
        @DisplayName("StreakDays 条件：值 >= 天数时满足")
        fun `streakDays check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.StreakDays(7), 7
            ))
        }

        @Test
        @DisplayName("QuizCorrectCount 条件：值 >= 计数时满足")
        fun `quizCorrectCount check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.QuizCorrectCount(100), 200
            ))
        }

        @Test
        @DisplayName("QuizPerfect 条件：currentValue == 1 时满足")
        fun `quizPerfect check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.QuizPerfect, 1
            ))
        }

        @Test
        @DisplayName("QuizPerfect 条件：currentValue != 1 时不满足")
        fun `quizPerfect not satisfied`() {
            org.junit.jupiter.api.Assertions.assertFalse(
                AchievementDetector.checkConditionDirectly(
                    AchievementCondition.QuizPerfect, 0
                )
            )
        }

        @Test
        @DisplayName("ReviewCount 条件：值 >= 计数时满足")
        fun `reviewCount check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.ReviewCount(50), 100
            ))
        }

        @Test
        @DisplayName("TotalScore 条件：值 >= 分数时满足")
        fun `totalScore check`() {
            assertTrue(AchievementDetector.checkConditionDirectly(
                AchievementCondition.TotalScore(5000), 5000
            ))
        }
    }
}
