package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE status = :status ORDER BY id ASC")
    fun getByStatus(status: String): Flow<List<AchievementEntity>>

    @Query("UPDATE achievements SET status = :status, unlockedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)
}
