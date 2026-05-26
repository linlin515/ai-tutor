package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aitutor.app.data.local.entity.FlashcardEntity
import com.aitutor.app.data.local.entity.FlashcardReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun getTodayCardsFlow(now: Long = System.currentTimeMillis()): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    suspend fun getCardsForReview(now: Long = System.currentTimeMillis()): List<FlashcardEntity>

    @Query("UPDATE flashcards SET masteryLevel = :mastery, intervalDays = :interval, nextReviewAt = :nextReview, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMastery(id: String, mastery: Float, interval: Int, nextReview: Long, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: FlashcardReviewLogEntity)

    @Upsert
    suspend fun upsertAll(cards: List<FlashcardEntity>)

    @Query("SELECT * FROM flashcard_review_logs WHERE synced = 0")
    suspend fun getUnsyncedReviewLogs(): List<FlashcardReviewLogEntity>

    @Query("UPDATE flashcard_review_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM flashcards WHERE nextReviewAt <= :now")
    suspend fun countPendingCards(now: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM flashcard_review_logs WHERE reviewedAt >= :startOfDay")
    suspend fun countReviewedToday(startOfDay: Long): Int
}
