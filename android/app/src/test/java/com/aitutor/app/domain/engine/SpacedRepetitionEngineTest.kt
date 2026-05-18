package com.aitutor.app.domain.engine

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 单元测试：SpacedRepetitionEngine
 *
 * 覆盖场景：
 * - 正确/错误回答的间隔变化
 * - 连续正确导致掌握
 * - 间隔上限
 * - 到期筛选
 * - 初始排期
 */
@DisplayName("SpacedRepetitionEngine")
class SpacedRepetitionEngineTest {

    private val engine = SpacedRepetitionEngine()
    private val fixedNow = 1_000_000_000_000L // fixed timestamp for testing

    @BeforeEach
    fun setUp() {
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns fixedNow
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(System::class)
    }

    @Nested
    @DisplayName("calculateNextReview")
    inner class CalculateNextReview {

        @Test
        @DisplayName("正确回答后间隔从 1 天推进到 3 天")
        fun `correct answer advances interval 1 to 3`() {
            val item = ReviewItem(id = "q1", intervalDays = 1)
            val result = engine.calculateNextReview(item, isCorrect = true)

            assertEquals(3, result.intervalDays)
            assertEquals(1, result.consecutiveCorrect)
            assertFalse(result.isMastered)
            assertEquals("q1", result.id)
        }

        @Test
        @DisplayName("正确回答后间隔从 3 天推进到 7 天")
        fun `correct answer advances interval 3 to 7`() {
            val item = ReviewItem(id = "q1", intervalDays = 3, consecutiveCorrect = 1)
            val result = engine.calculateNextReview(item, isCorrect = true)

            assertEquals(7, result.intervalDays)
            assertEquals(2, result.consecutiveCorrect)
            assertFalse(result.isMastered)
        }

        @Test
        @DisplayName("3 次连续正确后标记为已掌握")
        fun `three consecutive correct marks as mastered`() {
            val item = ReviewItem(id = "q1", intervalDays = 7, consecutiveCorrect = 2)
            val result = engine.calculateNextReview(item, isCorrect = true)

            assertEquals(14, result.intervalDays)
            assertEquals(3, result.consecutiveCorrect)
            assertTrue(result.isMastered)
        }

        @Test
        @DisplayName("错误回答重置间隔为 1 天，连续正确计数清零")
        fun `wrong answer resets interval to 1`() {
            val item = ReviewItem(id = "q1", intervalDays = 14, consecutiveCorrect = 2)
            val result = engine.calculateNextReview(item, isCorrect = false)

            assertEquals(1, result.intervalDays)
            assertEquals(0, result.consecutiveCorrect)
            assertFalse(result.isMastered)
        }

        @Test
        @DisplayName("间隔达到 180 天上限后不再增长")
        fun `interval is capped at 180 days`() {
            // Simulate item at 100 days (beyond built-in INTERVALS list)
            val item = ReviewItem(id = "q1", intervalDays = 100, consecutiveCorrect = 1)
            val result = engine.calculateNextReview(item, isCorrect = true)

            // 100 * 2 = 200, but capped at 180
            assertEquals(180, result.intervalDays)
        }

        @Test
        @DisplayName("间隔在 INTERVALS 列表末尾之后采用指数增长")
        fun `interval grows exponentially after built-in intervals`() {
            // Last built-in interval is 60, next should be 60*2=120
            val item = ReviewItem(id = "q1", intervalDays = 60, consecutiveCorrect = 1)
            val result = engine.calculateNextReview(item, isCorrect = true)

            assertEquals(120, result.intervalDays)
        }

        @Test
        @DisplayName("nextReviewAt 设置为当前时间 + 间隔天数")
        fun `nextReviewAt is set to now plus interval`() {
            val item = ReviewItem(id = "q1", intervalDays = 1)
            val result = engine.calculateNextReview(item, isCorrect = true)

            val expectedNextReview = fixedNow + (3L * 24 * 60 * 60 * 1000)
            assertEquals(expectedNextReview, result.nextReviewAt)
        }

        @Test
        @DisplayName("错误回答后 nextReviewAt 设为 1 天后")
        fun `wrong answer sets nextReviewAt to 1 day later`() {
            val item = ReviewItem(id = "q1", intervalDays = 14, consecutiveCorrect = 2)
            val result = engine.calculateNextReview(item, isCorrect = false)

            val expectedNextReview = fixedNow + (24L * 60 * 60 * 1000)
            assertEquals(expectedNextReview, result.nextReviewAt)
        }
    }

    @Nested
    @DisplayName("getDueItems")
    inner class GetDueItems {

        @Test
        @DisplayName("应返回未掌握且到期时间 <= 当前时间的项目")
        fun `returns unmastered due items`() {
            val dueItem = ReviewItem(id = "due", nextReviewAt = fixedNow - 1000)
            val notDueItem = ReviewItem(id = "not_due", nextReviewAt = fixedNow + 1000)
            val masteredItem = ReviewItem(id = "mastered", nextReviewAt = fixedNow - 1000, isMastered = true)

            val due = engine.getDueItems(listOf(dueItem, notDueItem, masteredItem))
            assertEquals(listOf(dueItem), due)
        }

        @Test
        @DisplayName("没有到期项目时返回空列表")
        fun `returns empty list when no items due`() {
            val notDueItem = ReviewItem(id = "future", nextReviewAt = fixedNow + 10000)
            val result = engine.getDueItems(listOf(notDueItem))
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("空输入返回空列表")
        fun `empty input returns empty list`() {
            val result = engine.getDueItems(emptyList())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("createInitialReview")
    inner class CreateInitialReview {

        @Test
        @DisplayName("初始间隔为 1 天，nextReviewAt 为当前时间 + 24h")
        fun `initial review has interval 1 and correct nextReviewAt`() {
            val result = engine.createInitialReview()
            assertEquals(1, result.intervalDays)
            assertEquals(0, result.consecutiveCorrect)
            assertFalse(result.isMastered)
            assertEquals("", result.id)
            val expectedNextReview = fixedNow + (24L * 60 * 60 * 1000)
            assertEquals(expectedNextReview, result.nextReviewAt)
        }
    }
}
