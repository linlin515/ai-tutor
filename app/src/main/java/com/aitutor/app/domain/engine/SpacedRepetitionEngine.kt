package com.aitutor.app.domain.engine

import javax.inject.Inject

/**
 * SM-2 variant spaced repetition algorithm (纯 Kotlin, 无 Android 依赖)
 * 
 * Intervals: first 1d → 3d → 7d → 14d → ...
 * Correct: interval × 2
 * Wrong: reset to 1d
 * 3 consecutive correct → mastered
 */
data class ReviewItem(
    val id: String,
    val intervalDays: Int = 1,
    val consecutiveCorrect: Int = 0,
    val isMastered: Boolean = false,
    val nextReviewAt: Long = System.currentTimeMillis()
)

class SpacedRepetitionEngine @Inject constructor() {

    companion object {
        private val INTERVALS = listOf(1, 3, 7, 14, 30, 60)
        private const val MAX_CONSECUTIVE_CORRECT = 3
    }

    /**
     * Calculate the next review schedule based on correctness.
     */
    fun calculateNextReview(
        item: ReviewItem,
        isCorrect: Boolean
    ): ReviewItem {
        if (isCorrect) {
            val newConsecutiveCorrect = item.consecutiveCorrect + 1
            val isMastered = newConsecutiveCorrect >= MAX_CONSECUTIVE_CORRECT
            
            val intervalIndex = INTERVALS.indexOf(item.intervalDays)
            val newInterval = if (intervalIndex >= 0 && intervalIndex < INTERVALS.size - 1) {
                INTERVALS[intervalIndex + 1]
            } else {
                // Exponential growth after default intervals
                (item.intervalDays * 2).coerceAtMost(180)
            }
            
            return ReviewItem(
                id = item.id,
                intervalDays = newInterval,
                consecutiveCorrect = newConsecutiveCorrect,
                isMastered = isMastered,
                nextReviewAt = System.currentTimeMillis() + (newInterval * 24L * 60 * 60 * 1000)
            )
        } else {
            // Wrong → reset to 1 day
            return ReviewItem(
                id = item.id,
                intervalDays = 1,
                consecutiveCorrect = 0,
                isMastered = false,
                nextReviewAt = System.currentTimeMillis() + (24L * 60 * 60 * 1000)
            )
        }
    }

    /**
     * Get items that are due for review (nextReviewAt <= now).
     */
    fun getDueItems(items: List<ReviewItem>): List<ReviewItem> {
        val now = System.currentTimeMillis()
        return items.filter { it.nextReviewAt <= now && !it.isMastered }
    }

    /**
     * Calculate the initial review schedule for a new wrong answer.
     */
    fun createInitialReview(): ReviewItem {
        return ReviewItem(
            id = "",
            intervalDays = 1,
            nextReviewAt = System.currentTimeMillis() + (24L * 60 * 60 * 1000)
        )
    }
}
