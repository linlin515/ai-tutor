package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.AnalyticsDao
import com.aitutor.app.data.local.entity.LearningRecordEntity
import com.aitutor.app.data.remote.api.AnalyticsApi
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val analyticsApi: AnalyticsApi
) : AnalyticsRepository {

    override fun getTodayStats(): Flow<TodayStats> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return analyticsDao.getTodayStats(today)
    }

    override fun getTrend(days: Int): Flow<List<TrendPoint>> {
        val since = LocalDate.now().minusDays(days.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        return analyticsDao.getTrend(since).map { records ->
            records.map { r ->
                TrendPoint(
                    date = r.date,
                    duration = r.learnDurationMin,
                    solveCount = r.solveCount,
                    correctRate = if (r.solveCount > 0) r.correctCount.toFloat() / r.solveCount else 0f
                )
            }
        }
    }

    override fun getKnowledgeSummary(): Flow<List<KnowledgeSummary>> = analyticsDao.getKnowledgeSummary()

    override fun getKnowledgeGraph(subject: String): Flow<List<KnowledgeNode>> {
        return analyticsDao.getKnowledgeGraph(subject).map { entities ->
            entities.map { e ->
                KnowledgeNode(
                    id = e.id,
                    name = e.name,
                    subject = e.subject,
                    parentId = e.parentId,
                    status = e.status,
                    confidence = e.confidence
                )
            }
        }
    }

    override fun getDashboardStats(): Flow<DashboardStats> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return analyticsDao.getTodayStats(today).map { stats ->
            DashboardStats(
                todayDuration = stats.duration,
                todaySolveCount = stats.solveCount,
                todayCorrectCount = stats.correctCount
            )
        }
    }

    override suspend fun refreshFromCloud() {
        try {
            val response = analyticsApi.getAnalyticsStats()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    body.data?.let { data ->
                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        analyticsDao.insertLearningRecord(
                            LearningRecordEntity(
                                date = today,
                                learnDurationMin = data.today?.duration ?: 0,
                                solveCount = data.today?.solveCount ?: 0,
                                correctCount = ((data.today?.correctRate ?: 0f) * (data.today?.solveCount ?: 0).toFloat()).toInt()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fail — local data is the source of truth
        }
    }
}
