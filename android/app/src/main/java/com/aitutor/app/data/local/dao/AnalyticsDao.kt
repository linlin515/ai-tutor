package com.aitutor.app.data.local.dao

import androidx.room.*
import com.aitutor.app.data.local.entity.LearningRecordEntity
import com.aitutor.app.data.local.entity.KnowledgePointEntity
import com.aitutor.app.domain.model.KnowledgeSummary
import com.aitutor.app.domain.model.TodayStats
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Query("""
        SELECT COALESCE(SUM(solveCount), 0) as solveCount,
               COALESCE(SUM(correctCount), 0) as correctCount,
               COALESCE(SUM(learnDurationMin), 0) as duration
        FROM learning_records WHERE date = :today
    """)
    fun getTodayStats(today: String): Flow<TodayStats>

    @Query("SELECT * FROM learning_records WHERE date >= :since ORDER BY date ASC")
    fun getTrend(since: String): Flow<List<LearningRecordEntity>>

    @Query("SELECT status, COUNT(*) as count FROM knowledge_points GROUP BY status")
    fun getKnowledgeSummary(): Flow<List<KnowledgeSummary>>

    @Query("SELECT * FROM knowledge_points WHERE subject = :subject ORDER BY name")
    fun getKnowledgeGraph(subject: String): Flow<List<KnowledgePointEntity>>

    @Query("SELECT COUNT(DISTINCT date) FROM learning_records WHERE date >= :since AND solveCount > 0")
    fun getActiveDays(since: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningRecord(record: LearningRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(point: KnowledgePointEntity)

    @Query("SELECT * FROM knowledge_points ORDER BY name")
    fun getAllKnowledgePoints(): Flow<List<KnowledgePointEntity>>

    @Query("SELECT * FROM learning_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecord(): LearningRecordEntity?
}
