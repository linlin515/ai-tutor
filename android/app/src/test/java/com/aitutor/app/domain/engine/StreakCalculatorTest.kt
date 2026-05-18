package com.aitutor.app.domain.engine

import com.aitutor.app.domain.model.StreakResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 单元测试：StreakCalculator
 *
 * 覆盖场景：
 * - 空列表
 * - 今日学习 / 昨日学习
 * - 连续天数计算（当前连续 + 最长连续）
 * - 连续中断场景
 * - haveLearnedToday 标志
 */
@DisplayName("StreakCalculator")
class StreakCalculatorTest {

    private val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val yesterday: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val dayBefore: String = LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val threeDaysAgo: String = LocalDate.now().minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val fourDaysAgo: String = LocalDate.now().minusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val fiveDaysAgo: String = LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Nested
    @DisplayName("calculateStreak(learningDates)")
    inner class CalculateStreak {

        @Test
        @DisplayName("空列表应返回默认值")
        fun `empty list returns defaults`() {
            val result = StreakCalculator.calculateStreak(emptyList())
            assertEquals(StreakResult(), result)
            assertEquals(0, result.currentStreak)
            assertEquals(0, result.longestStreak)
            assertFalse(result.hasLearnedToday)
        }

        @Test
        @DisplayName("仅今日学习：当前连续 1 天，最长 1 天")
        fun `only today learned returns current 1 longest 1`() {
            val result = StreakCalculator.calculateStreak(listOf(today))
            assertEquals(1, result.currentStreak)
            assertEquals(1, result.longestStreak)
            assertTrue(result.hasLearnedToday)
        }

        @Test
        @DisplayName("仅昨日学习（今日未学）：当前连续 1 天，最长 1 天")
        fun `only yesterday learned current 1 longest 1`() {
            val result = StreakCalculator.calculateStreak(listOf(yesterday))
            assertEquals(1, result.currentStreak)
            assertEquals(1, result.longestStreak)
            assertFalse(result.hasLearnedToday)
        }

        @Test
        @DisplayName("今天+昨天连续学习 2 天")
        fun `today and yesterday gives streak 2`() {
            val result = StreakCalculator.calculateStreak(listOf(yesterday, today))
            assertEquals(2, result.currentStreak)
            assertEquals(2, result.longestStreak)
            assertTrue(result.hasLearnedToday)
        }

        @Test
        @DisplayName("连续 5 天学习")
        fun `five consecutive days`() {
            val dates = listOf(fiveDaysAgo, fourDaysAgo, threeDaysAgo, dayBefore, yesterday)
            // Not learning today — from yesterday backwards chain is 5 consecutive
            val result = StreakCalculator.calculateStreak(dates)
            assertEquals(5, result.currentStreak) // yesterday starts a chain of 5
            assertEquals(5, result.longestStreak)
            assertFalse(result.hasLearnedToday)
        }

        @Test
        @DisplayName("连续 5 天 + 今日学习，当前连续 6 天")
        fun `five consecutive plus today gives streak 6`() {
            val dates = listOf(fiveDaysAgo, fourDaysAgo, threeDaysAgo, dayBefore, yesterday, today)
            val result = StreakCalculator.calculateStreak(dates)
            assertEquals(6, result.currentStreak)
            assertEquals(6, result.longestStreak)
            assertTrue(result.hasLearnedToday)
        }

        @Test
        @DisplayName("连续中断：最长连续应正确识别")
        fun `broken streak longest correctly identified`() {
            // 连续 3 天 → 中断 → 连续 2 天（含今天）
            val dates = listOf(
                LocalDate.now().minusDays(9).format(DateTimeFormatter.ISO_LOCAL_DATE),
                LocalDate.now().minusDays(8).format(DateTimeFormatter.ISO_LOCAL_DATE),
                LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE),
                // gap
                yesterday,
                today
            )
            val result = StreakCalculator.calculateStreak(dates)
            assertEquals(2, result.currentStreak)
            assertEquals(3, result.longestStreak)
            assertTrue(result.hasLearnedToday)
        }

        @Test
        @DisplayName("单日期（昨天之前）：当前 0，最长 1")
        fun `single date before yesterday current 0`() {
            val result = StreakCalculator.calculateStreak(listOf(threeDaysAgo))
            assertEquals(0, result.currentStreak)
            assertEquals(1, result.longestStreak)
            assertFalse(result.hasLearnedToday)
        }

        @Test
        @DisplayName("未排序日期列表也能正确计算")
        fun `unsorted dates produce correct result`() {
            val dates = listOf(today, fiveDaysAgo, yesterday, threeDaysAgo, dayBefore, fourDaysAgo)
            // All consecutive from 5 days ago to today = 6 days
            val result = StreakCalculator.calculateStreak(dates)
            assertEquals(6, result.currentStreak)
            assertEquals(6, result.longestStreak)
            assertTrue(result.hasLearnedToday)
        }
    }
}
