package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.ScoreLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ScoreLogEntity)

    @Query("SELECT * FROM score_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<ScoreLogEntity>>

    @Query("SELECT * FROM score_logs WHERE date = :date ORDER BY createdAt DESC")
    fun getLogsByDate(date: String): Flow<List<ScoreLogEntity>>

    @Query("SELECT * FROM score_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getLogsByRange(startDate: String, endDate: String): Flow<List<ScoreLogEntity>>

    @Query("SELECT * FROM score_logs WHERE date >= :startDate ORDER BY createdAt DESC")
    fun getLogsSince(startDate: String): Flow<List<ScoreLogEntity>>
}
