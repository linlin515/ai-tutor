package com.aitutor.app.domain.model

/**
 * 学习报告聚合数据模型 (F50)
 */
data class ReportData(
    val reportType: ReportType = ReportType.WEEKLY,
    val period: String = "",                    // 报告期间描述，如 "2026-05-11 ~ 05-17"
    val nickname: String = "",
    val generationDate: String = "",

    // 概览统计
    val totalActiveDays: Int = 0,
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val todayMessages: Int = 0,
    val firstUseDate: String = "",

    // 学习时长与解题统计
    val totalStudyMinutes: Int = 0,
    val totalSolved: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val accuracyRate: Float = 0f,

    // 知识点掌握度
    val totalKnowledgePoints: Int = 0,
    val masteredKnowledgePoints: Int = 0,
    val masteryRate: Float = 0f,

    // 每日趋势数据
    val dailyStats: List<DayStat> = emptyList(),

    // 学科分布
    val subjectDistribution: List<SubjectStat> = emptyList(),

    // 错题汇总
    val topWrongAnswers: List<WrongAnswerReport> = emptyList(),
    val wrongAnswersBySubject: List<SubjectCount> = emptyList(),

    // 对话主题
    val conversationTopics: List<String> = emptyList(),

    // 连胜
    val streakDays: Int = 0,

    // 平均回复长度
    val avgResponseLength: Int = 0
)

enum class ReportType {
    WEEKLY,
    MONTHLY
}

data class DayStat(
    val day: String,
    val msgCount: Int,
    val convCount: Int
)

data class SubjectStat(
    val subject: String,
    val percentage: Float,  // 0..100
    val count: Int
)

data class WrongAnswerReport(
    val subject: String,
    val wrongCount: Int,
    val question: String
)

data class SubjectCount(
    val subject: String,
    val count: Int
)
