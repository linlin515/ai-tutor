package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.StudyReportDao
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.StudyReportRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyReportRepositoryImpl @Inject constructor(
    private val studyReportDao: StudyReportDao
) : StudyReportRepository {

    override suspend fun getReportData(
        reportType: ReportType,
        nickname: String
    ): ReportData {
        val overview = studyReportDao.getOverviewStats()
        val totalConv = studyReportDao.getTotalConversations()
        val totalStats = when (reportType) {
            ReportType.WEEKLY -> studyReportDao.getWeeklyLearningStats()
            ReportType.MONTHLY -> studyReportDao.getMonthlyLearningStats()
        }
        val dailyStatsRaw = when (reportType) {
            ReportType.WEEKLY -> studyReportDao.getDailyStatsLast7Days()
            ReportType.MONTHLY -> studyReportDao.getDailyStatsLast30Days()
        }
        val topWrong = studyReportDao.getTopWrongAnswers()
        val wrongBySubject = studyReportDao.getWrongAnswersBySubject()
        val topics = studyReportDao.getConversationTopics()
        val avgLen = studyReportDao.getAvgResponseLength()
        val activeDays = studyReportDao.getAllActiveDays()
        val streakDays = calculateStreak(activeDays)

        // Calculate accuracy rate
        val totalAttempts = totalStats.totalCorrect + totalStats.totalWrong
        val accuracyRate = if (totalAttempts > 0) {
            totalStats.totalCorrect.toFloat() / totalAttempts.toFloat() * 100f
        } else 0f

        // Calculate mastery rate
        val masteryRate = if (totalStats.totalKp > 0) {
            totalStats.masteredKp.toFloat() / totalStats.totalKp.toFloat() * 100f
        } else 0f

        // Generate period description
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val period = when (reportType) {
            ReportType.WEEKLY -> generateWeeklyPeriod()
            ReportType.MONTHLY -> generateMonthlyPeriod()
        }

        // Subject distribution
        val totalWrongCount = wrongBySubject.sumOf { it.count }
        val subjectDistribution = if (totalWrongCount > 0) {
            wrongBySubject.map {
                SubjectStat(
                    subject = it.subject,
                    percentage = it.count.toFloat() / totalWrongCount.toFloat() * 100f,
                    count = it.count
                )
            }
        } else emptyList()

        // Conversation topics
        val topicTitles = topics.map { it.title }

        return ReportData(
            reportType = reportType,
            period = period,
            nickname = nickname,
            generationDate = dateFormat.format(Date()),
            totalActiveDays = overview.totalActiveDays,
            totalConversations = totalConv,
            totalMessages = overview.totalMessages,
            todayMessages = overview.todayMessages,
            firstUseDate = overview.firstUseDate ?: "",
            totalStudyMinutes = totalStats.totalDuration,
            totalSolved = totalStats.totalSolved,
            totalCorrect = totalStats.totalCorrect,
            totalWrong = totalStats.totalWrong,
            accuracyRate = accuracyRate,
            totalKnowledgePoints = totalStats.totalKp,
            masteredKnowledgePoints = totalStats.masteredKp,
            masteryRate = masteryRate,
            dailyStats = dailyStatsRaw.map { DayStat(it.day, it.msgCount, it.convCount) },
            subjectDistribution = subjectDistribution,
            topWrongAnswers = topWrong.map {
                WrongAnswerReport(it.subject, it.wrongCount, it.question)
            },
            wrongAnswersBySubject = wrongBySubject.map {
                SubjectCount(it.subject, it.count)
            },
            conversationTopics = topicTitles,
            streakDays = streakDays,
            avgResponseLength = avgLen?.toInt() ?: 0
        )
    }

    private fun calculateStreak(activeDays: List<String>): Int {
        if (activeDays.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sortedDates = activeDays.map { dateFormat.parse(it) }
            .filterNotNull()
            .sortedDescending()

        var streak = 1
        val calendar = Calendar.getInstance()

        for (i in 1 until sortedDates.size) {
            calendar.time = sortedDates[i - 1]
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val expectedPrevious = calendar.time

            if (sortedDates[i].time == expectedPrevious.time) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    private fun generateWeeklyPeriod(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        // Go to the start of the current week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = dateFormat.format(calendar.time)
        val today = dateFormat.format(Date())
        return "$weekStart ~ $today"
    }

    private fun generateMonthlyPeriod(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendar.set(year, month, 1)
        val monthStart = dateFormat.format(calendar.time)
        val today = dateFormat.format(Date())
        return "$monthStart ~ $today"
    }
}
