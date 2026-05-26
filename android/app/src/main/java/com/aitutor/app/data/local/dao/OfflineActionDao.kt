package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.OfflineActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineActionDao {
    @Query("SELECT * FROM offline_actions ORDER BY createdAt ASC")
    fun getAll(): Flow<List<OfflineActionEntity>>

    @Query("SELECT * FROM offline_actions WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedActions(): List<OfflineActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(actions: List<OfflineActionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: OfflineActionEntity)

    @Query("UPDATE offline_actions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedLong(ids: List<Long>)

    @Query("UPDATE offline_actions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM offline_actions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM offline_actions")
    suspend fun count(): Int

    @Query("DELETE FROM offline_actions WHERE id IN (SELECT id FROM offline_actions ORDER BY createdAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
