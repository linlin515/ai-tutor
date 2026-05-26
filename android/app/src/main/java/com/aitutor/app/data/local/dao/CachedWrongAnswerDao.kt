package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aitutor.app.data.local.entity.CachedWrongAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedWrongAnswerDao {
    @Query("SELECT * FROM cached_wrong_answers ORDER BY cachedAt DESC")
    fun getAllFlow(): Flow<List<CachedWrongAnswerEntity>>

    @Query("SELECT * FROM cached_wrong_answers ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<CachedWrongAnswerEntity>

    @Query("SELECT * FROM cached_wrong_answers WHERE id = :id")
    suspend fun getById(id: String): CachedWrongAnswerEntity?

    @Upsert
    suspend fun upsertAll(entities: List<CachedWrongAnswerEntity>)

    @Query("DELETE FROM cached_wrong_answers WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM cached_wrong_answers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_wrong_answers")
    suspend fun count(): Int

    @Query("SELECT SUM(LENGTH(questionContent) + LENGTH(userAnswer) + LENGTH(correctAnswer)) FROM cached_wrong_answers")
    suspend fun dataSizeBytes(): Long?

    @Query("DELETE FROM cached_wrong_answers WHERE id IN (SELECT id FROM cached_wrong_answers ORDER BY cachedAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
