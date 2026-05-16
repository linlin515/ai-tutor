package com.aitutor.app.domain.model

/**
 * 成就条件密封类
 */
sealed class AchievementCondition {
    data class SolveCount(val count: Int) : AchievementCondition()
    data class StreakDays(val days: Int) : AchievementCondition()
    data class QuizCorrectCount(val count: Int) : AchievementCondition()
    data object QuizPerfect : AchievementCondition()
    data class ReviewCount(val count: Int) : AchievementCondition()
    data class TotalScore(val score: Int) : AchievementCondition()
}

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

/**
 * 成就枚举
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val condition: AchievementCondition
) {
    FIRST_SOLVE(
        id = "first_solve",
        title = "初次解题",
        description = "完成第一道解题",
        icon = "\uD83C\uDFAF",
        condition = AchievementCondition.SolveCount(1)
    ),
    SOLVE_MASTER(
        id = "solve_master",
        title = "解题达人",
        description = "累计完成100道解题",
        icon = "\uD83E\uDDE0",
        condition = AchievementCondition.SolveCount(100)
    ),
    STREAK_7(
        id = "streak_7",
        title = "连续7天",
        description = "连续学习7天",
        icon = "\uD83D\uDD25",
        condition = AchievementCondition.StreakDays(7)
    ),
    STREAK_30(
        id = "streak_30",
        title = "连续30天",
        description = "连续学习30天",
        icon = "\uD83D\uDCAA",
        condition = AchievementCondition.StreakDays(30)
    ),
    QUIZ_100(
        id = "quiz_100",
        title = "百题斩",
        description = "累计答对100道题目",
        icon = "\uD83D\uDCDD",
        condition = AchievementCondition.QuizCorrectCount(100)
    ),
    QUIZ_PERFECT(
        id = "quiz_perfect",
        title = "完美通关",
        description = "在一次测验中全部答对",
        icon = "\u2B50",
        condition = AchievementCondition.QuizPerfect
    ),
    REVIEW_MASTER(
        id = "review_master",
        title = "复习大师",
        description = "完成50次复习",
        icon = "\uD83D\uDD04",
        condition = AchievementCondition.ReviewCount(50)
    ),
    SCHOLAR(
        id = "scholar",
        title = "学霸",
        description = "累计获得5000分",
        icon = "\uD83D\uDC51",
        condition = AchievementCondition.TotalScore(5000)
    );

    companion object {
        fun fromId(id: String): Achievement? = entries.find { it.id == id }
    }
}
