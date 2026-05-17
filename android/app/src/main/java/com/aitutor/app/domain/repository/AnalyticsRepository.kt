package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getTodayStats(): Flow<TodayStats>
    fun getTrend(days: Int): Flow<List<TrendPoint>>
    fun getKnowledgeSummary(): Flow<List<KnowledgeSummary>>
    fun getKnowledgeGraph(subject: String): Flow<List<KnowledgeNode>>
    fun getDashboardStats(): Flow<DashboardStats>
    suspend fun refreshFromCloud()
}
