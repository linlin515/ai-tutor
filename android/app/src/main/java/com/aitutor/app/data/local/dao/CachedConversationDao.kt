package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aitutor.app.data.local.entity.CachedConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedConversationDao {
    @Query("SELECT * FROM cached_conversations ORDER BY cachedAt DESC")
    fun getAllFlow(): Flow<List<CachedConversationEntity>>

    @Query("SELECT * FROM cached_conversations ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<CachedConversationEntity>

    @Query("SELECT * FROM cached_conversations WHERE id = :id")
    suspend fun getById(id: String): CachedConversationEntity?

    @Upsert
    suspend fun upsertAll(entities: List<CachedConversationEntity>)

    @Query("DELETE FROM cached_conversations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM cached_conversations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_conversations")
    suspend fun count(): Int

    @Query("SELECT SUM(LENGTH(sessionTitle) + LENGTH(messages)) FROM cached_conversations")
    suspend fun dataSizeBytes(): Long?

    @Query("DELETE FROM cached_conversations WHERE id IN (SELECT id FROM cached_conversations ORDER BY cachedAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
