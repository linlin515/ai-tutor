package com.aitutor.app.domain.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 单元测试：ScoreCalculator
 *
 * 覆盖场景：
 * - 各事件类型积分计算
 * - Quiz 满分额外奖励
 * - 连续学习积分加成
 */
@DisplayName("ScoreCalculator")
class ScoreCalculatorTest {

    @Nested
    @DisplayName("calculateScore(event)")
    inner class CalculateScore {

        @Test
        @DisplayName("MessageSent 应返回 2 分")
        fun `messageSent returns 2 points`() {
            assertEquals(2, ScoreCalculator.calculateScore(LearningEvent.MessageSent))
        }

        @Test
        @DisplayName("SolveCompleted 应返回 10 分")
        fun `solveCompleted returns 10 points`() {
            assertEquals(10, ScoreCalculator.calculateScore(LearningEvent.SolveCompleted))
        }

        @Test
        @DisplayName("QuizCompleted 不全对应返回 20 分")
        fun `quizCompleted not perfect returns 20 points`() {
            val event = LearningEvent.QuizCompleted(correctCount = 3, totalCount = 5)
            assertEquals(20, ScoreCalculator.calculateScore(event))
        }

        @Test
        @DisplayName("QuizCompleted 全对应返回 30 分（含 10 分额外奖励）")
        fun `quizCompleted perfect returns 30 points`() {
            val event = LearningEvent.QuizCompleted(correctCount = 5, totalCount = 5)
            assertEquals(30, ScoreCalculator.calculateScore(event))
        }

        @Test
        @DisplayName("QuizCompleted totalCount=0 时不应触发全对奖励")
        fun `quizCompleted with zero total does not trigger perfect bonus`() {
            val event = LearningEvent.QuizCompleted(correctCount = 0, totalCount = 0)
            assertEquals(20, ScoreCalculator.calculateScore(event))
        }

        @Test
        @DisplayName("ReviewCompleted 应返回 15 分")
        fun `reviewCompleted returns 15 points`() {
            assertEquals(15, ScoreCalculator.calculateScore(LearningEvent.ReviewCompleted))
        }

        @Test
        @DisplayName("AppOpened 应返回 0 分")
        fun `appOpened returns 0 points`() {
            assertEquals(0, ScoreCalculator.calculateScore(LearningEvent.AppOpened))
        }

        @Test
        @DisplayName("StreakMaintained 应返回 0 分（在 GamificationEngine 中计算）")
        fun `streakMaintained returns 0 points`() {
            assertEquals(0, ScoreCalculator.calculateScore(LearningEvent.StreakMaintained))
        }
    }

    @Nested
    @DisplayName("calculateStreakBonus(currentStreakDays)")
    inner class CalculateStreakBonus {

        @Test
        @DisplayName("连续 0 天应返回 0")
        fun `zero days returns 0`() {
            assertEquals(0, ScoreCalculator.calculateStreakBonus(0))
        }

        @Test
        @DisplayName("连续 1 天应返回 5")
        fun `one day returns 5`() {
            assertEquals(5, ScoreCalculator.calculateStreakBonus(1))
        }

        @Test
        @DisplayName("连续 7 天应返回 35")
        fun `seven days returns 35`() {
            assertEquals(35, ScoreCalculator.calculateStreakBonus(7))
        }

        @Test
        @DisplayName("连续 100 天应返回 500")
        fun `hundred days returns 500`() {
            assertEquals(500, ScoreCalculator.calculateStreakBonus(100))
        }
    }
}
