package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aitutor.app.data.local.entity.CachedQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedQuestionDao {
    @Query("SELECT * FROM cached_questions ORDER BY cachedAt DESC")
    fun getAllFlow(): Flow<List<CachedQuestionEntity>>

    @Query("SELECT * FROM cached_questions ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_questions WHERE id = :id")
    suspend fun getById(id: String): CachedQuestionEntity?

    @Upsert
    suspend fun upsertAll(entities: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM cached_questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_questions")
    suspend fun count(): Int

    @Query("SELECT SUM(LENGTH(content) + LENGTH(options) + LENGTH(correctAnswer) + LENGTH(explanation)) FROM cached_questions")
    suspend fun dataSizeBytes(): Long?

    @Query("DELETE FROM cached_questions WHERE id IN (SELECT id FROM cached_questions ORDER BY cachedAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
