package com.aitutor.app.domain.usecase

import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLearningStatsUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    fun getDashboardStats(): Flow<DashboardStats> = analyticsRepository.getDashboardStats()
    fun getTrend(days: Int): Flow<List<TrendPoint>> = analyticsRepository.getTrend(days)
    fun getKnowledgeGraph(subject: String): Flow<List<KnowledgeNode>> = analyticsRepository.getKnowledgeGraph(subject)
}
