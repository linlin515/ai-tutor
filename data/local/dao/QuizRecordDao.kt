package com.aitutor.app.data.local.dao

import androidx.room.*
import com.aitutor.app.data.local.entity.PendingSubmissionEntity
import com.aitutor.app.data.local.entity.QuizRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: QuizRecordEntity)

    @Query("SELECT * FROM quiz_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<QuizRecordEntity>>

    @Query("SELECT * FROM quiz_records WHERE subject = :subject ORDER BY createdAt DESC")
    fun getRecordsBySubject(subject: String): Flow<List<QuizRecordEntity>>
}

@Dao
interface PendingSubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submission: PendingSubmissionEntity)

    @Query("SELECT * FROM pending_submissions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSubmissionEntity>

    @Delete
    suspend fun delete(submission: PendingSubmissionEntity)

    @Query("DELETE FROM pending_submissions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_submissions SET retryCount = :count WHERE id = :id")
    suspend fun updateRetryCount(id: String, count: Int)
}
