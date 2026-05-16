package com.aitutor.app.domain.engine

/**
 * 学习事件密封类
 */
sealed class LearningEvent {
    data object MessageSent : LearningEvent()
    data object SolveCompleted : LearningEvent()
    data class QuizCompleted(
        val correctCount: Int,
        val totalCount: Int
    ) : LearningEvent()
    data object ReviewCompleted : LearningEvent()
    data object AppOpened : LearningEvent()
    data object StreakMaintained : LearningEvent()
}
