package com.aitutor.app.domain.engine

/**
 * 积分计算规则
 */
object ScoreCalculator {

    /**
     * 根据事件类型计算获得的积分
     */
    fun calculateScore(event: LearningEvent): Int {
        return when (event) {
            is LearningEvent.MessageSent -> 2
            is LearningEvent.SolveCompleted -> 10
            is LearningEvent.QuizCompleted -> {
                if (event.totalCount > 0 && event.correctCount == event.totalCount) {
                    20 + 10 // 满分额外奖励
                } else {
                    20
                }
            }
            is LearningEvent.ReviewCompleted -> 15
            is LearningEvent.AppOpened -> 0
            is LearningEvent.StreakMaintained -> 0 // 连续学习奖励在GamificationEngine中计算
        }
    }

    /**
     * 计算连续学习额外积分奖励
     */
    fun calculateStreakBonus(currentStreakDays: Int): Int {
        return currentStreakDays * 5
    }
}
