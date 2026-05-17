package com.aitutor.app.domain.engine

import com.aitutor.app.domain.model.StreakResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 连续学习计算器 - 纯函数，根据学习日期列表计算连续天数
 */
object StreakCalculator {

    /**
     * 根据学习日期列表计算当前连续天数
     * @param learningDates 已排序的学习日期列表 (yyyy-MM-dd)
     */
    fun calculateStreak(learningDates: List<String>): StreakResult {
        if (learningDates.isEmpty()) {
            return StreakResult()
        }

        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 计算最长连续天数
        val longestStreak = calculateLongestStreak(learningDates)

        // 计算当前连续天数
        val currentStreak = calculateCurrentStreak(learningDates, todayStr, yesterdayStr)
        val hasLearnedToday = learningDates.contains(todayStr)

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            hasLearnedToday = hasLearnedToday
        )
    }

    private fun calculateCurrentStreak(
        learningDates: List<String>,
        todayStr: String,
        yesterdayStr: String
    ): Int {
        // 如果今天没有学习，且昨天也没有学习，连续天数为0
        if (!learningDates.contains(todayStr) && !learningDates.contains(yesterdayStr)) {
            return 0
        }

        // 从今天或昨天开始往前推算
        val startDate = if (learningDates.contains(todayStr)) todayStr else yesterdayStr
        var streak = 1
        var currentDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)

        while (true) {
            val prevDate = currentDate.minusDays(1)
            val prevDateStr = prevDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (learningDates.contains(prevDateStr)) {
                streak++
                currentDate = prevDate
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateLongestStreak(learningDates: List<String>): Int {
        if (learningDates.size <= 1) return learningDates.size

        val sortedDates = learningDates
            .map { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
            .sorted()

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            if (daysBetween == 1L) {
                currentStreak++
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                }
            } else if (daysBetween > 1) {
                currentStreak = 1
            }
        }

        return maxStreak
    }
}
