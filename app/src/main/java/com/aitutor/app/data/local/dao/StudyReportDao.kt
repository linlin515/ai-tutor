package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyReportDao {

    /**
     * 总览统计：总活跃天数、总消息数、今日消息数、首次使用日期
     */
    @Query("""
        SELECT 
            COUNT(DISTINCT date(timestamp / 1000, 'unixepoch')) as totalActiveDays,
            COUNT(*) as totalMessages,
            (SELECT COUNT(*) FROM messages WHERE date(timestamp / 1000, 'unixepoch') = date('now')) as todayMessages,
            MIN(date(timestamp / 1000, 'unixepoch')) as firstUseDate
        FROM messages
    """)
    suspend fun getOverviewStats(): OverviewStats

    /**
     * 近 7 天每日学习统计
     */
    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch') as day,
               COUNT(*) as msgCount,
               COUNT(DISTINCT conversationId) as convCount
        FROM messages
        WHERE date(timestamp / 1000, 'unixepoch') >= date('now', '-7 days')
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun getDailyStatsLast7Days(): List<DailyStats>

    /**
     * 近 30 天每日学习统计
     */
    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch') as day,
               COUNT(*) as msgCount,
               COUNT(DISTINCT conversationId) as convCount
        FROM messages
        WHERE date(timestamp / 1000, 'unixepoch') >= date('now', '-30 days')
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun getDailyStatsLast30Days(): List<DailyStats>

    /**
     * 总活跃天数（用于计算最长连胜）
     */
    @Query("""
        SELECT DISTINCT date(timestamp / 1000, 'unixepoch') as day
        FROM messages
        ORDER BY day ASC
    """)
    suspend fun getAllActiveDays(): List<String>

    /**
     * 总对话数
     */
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getTotalConversations(): Int

    /**
     * 对话主题分布
     */
    @Query("""
        SELECT title, messageCount FROM conversations
        WHERE title != '' ORDER BY updatedAt DESC LIMIT 20
    """)
    suspend fun getConversationTopics(): List<ConversationTopic>

    /**
     * 平均回复长度（AI 消息）
     */
    @Query("SELECT AVG(LENGTH(content)) FROM messages WHERE isUser = 0")
    suspend fun getAvgResponseLength(): Double?

    /**
     * 错题数据
     */
    @Query("""
        SELECT subject, COUNT(*) as wrongCount, question
        FROM wrong_answers
        GROUP BY knowledgePoint 
        ORDER BY wrongCount DESC 
        LIMIT 5
    """)
    suspend fun getTopWrongAnswers(): List<TopWrongAnswer>

    /**
     * 各学科错题数
     */
    @Query("""
        SELECT subject, COUNT(*) as count 
        FROM wrong_answers 
        GROUP BY subject 
        ORDER BY count DESC
    """)
    suspend fun getWrongAnswersBySubject(): List<SubjectCount>

    /**
     * 学习记录统计数据
     */
    @Query("""
        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,
               COALESCE(SUM(solveCount), 0) as totalSolved,
               COALESCE(SUM(correctCount), 0) as totalCorrect,
               COALESCE(SUM(wrongCount), 0) as totalWrong,
               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,
               COALESCE(SUM(masteredPoints), 0) as masteredKp
        FROM learning_records
    """)
    suspend fun getTotalLearningStats(): TotalLearningStats

    /**
     * 本周学习记录统计
     */
    @Query("""
        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,
               COALESCE(SUM(solveCount), 0) as totalSolved,
               COALESCE(SUM(correctCount), 0) as totalCorrect,
               COALESCE(SUM(wrongCount), 0) as totalWrong,
               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,
               COALESCE(SUM(masteredPoints), 0) as masteredKp
        FROM learning_records
        WHERE date >= date('now', 'weekday 1', '-7 days')
    """)
    suspend fun getWeeklyLearningStats(): TotalLearningStats

    /**
     * 本月学习记录统计
     */
    @Query("""
        SELECT COALESCE(SUM(learnDurationMin), 0) as totalDuration,
               COALESCE(SUM(solveCount), 0) as totalSolved,
               COALESCE(SUM(correctCount), 0) as totalCorrect,
               COALESCE(SUM(wrongCount), 0) as totalWrong,
               COALESCE(SUM(totalKnowledgePoints), 0) as totalKp,
               COALESCE(SUM(masteredPoints), 0) as masteredKp
        FROM learning_records
        WHERE strftime('%Y-%m', date) = strftime('%Y-%m', 'now')
    """)
    suspend fun getMonthlyLearningStats(): TotalLearningStats
}

// ===== Data classes for query results =====

data class OverviewStats(
    val totalActiveDays: Int = 0,
    val totalMessages: Int = 0,
    val todayMessages: Int = 0,
    val firstUseDate: String? = null
)

data class DailyStats(
    val day: String,
    val msgCount: Int,
    val convCount: Int
)

data class ConversationTopic(
    val title: String,
    val messageCount: Int
)

data class TopWrongAnswer(
    val subject: String,
    val wrongCount: Int,
    val question: String
)

data class SubjectCount(
    val subject: String,
    val count: Int
)

data class TotalLearningStats(
    val totalDuration: Int = 0,
    val totalSolved: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val totalKp: Int = 0,
    val masteredKp: Int = 0
)
