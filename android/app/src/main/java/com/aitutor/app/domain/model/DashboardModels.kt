package com.aitutor.app.domain.model

data class DashboardStats(
    val todayDuration: Int = 0,
    val todaySolveCount: Int = 0,
    val todayCorrectCount: Int = 0,
    val todayWrongCount: Int = 0,
    val streakDays: Int = 0,
    val totalKnowledgePoints: Int = 0,
    val masteredKnowledgePoints: Int = 0,
    val learningKnowledgePoints: Int = 0,
    val weakKnowledgePoints: Int = 0
)

data class TrendPoint(
    val date: String,
    val duration: Int = 0,
    val solveCount: Int = 0,
    val correctRate: Float = 0f
)

data class KnowledgeNode(
    val id: String,
    val name: String,
    val subject: String,
    val parentId: String?,
    val status: String, // mastered/learning/weak
    val confidence: Float,
    val children: List<KnowledgeNode> = emptyList()
)

data class KnowledgeSummary(
    val status: String,
    val count: Int
)

data class TodayStats(
    val solveCount: Int,
    val correctCount: Int,
    val duration: Int
)
