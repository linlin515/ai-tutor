package com.aitutor.app.data.local.dao

import androidx.room.*
import com.aitutor.app.data.local.entity.WrongAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongAnswerDao {
    @Query("SELECT * FROM wrong_answers ORDER BY nextReviewAt ASC")
    fun getAll(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE subject = :subject ORDER BY nextReviewAt ASC")
    fun getBySubject(subject: String): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE nextReviewAt <= :now AND isMastered = 0 ORDER BY nextReviewAt ASC")
    fun getDueReviews(now: Long = System.currentTimeMillis()): Flow<List<WrongAnswerEntity>>

    @Query("SELECT COUNT(*) FROM wrong_answers WHERE nextReviewAt <= :now AND isMastered = 0")
    fun getDueCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: WrongAnswerEntity)

    @Update
    suspend fun update(answer: WrongAnswerEntity)

    @Delete
    suspend fun delete(answer: WrongAnswerEntity)

    @Query("DELETE FROM wrong_answers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM wrong_answers WHERE id = :id")
    suspend fun getById(id: String): WrongAnswerEntity?
}
