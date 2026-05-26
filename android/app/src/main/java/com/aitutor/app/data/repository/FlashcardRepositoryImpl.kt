package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.FlashcardDao
import com.aitutor.app.data.local.entity.FlashcardEntity
import com.aitutor.app.data.local.entity.FlashcardReviewLogEntity
import com.aitutor.app.data.remote.api.FlashcardApi
import com.aitutor.app.data.remote.dto.FlashcardReviewLogDto
import com.aitutor.app.data.remote.dto.FlashcardSyncRequest
import com.aitutor.app.domain.repository.FlashcardRepository
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val flashcardApi: FlashcardApi
) : FlashcardRepository {

    override suspend fun getTodayCards(): List<FlashcardEntity> {
        // Try to get from remote first
        return try {
            val response = flashcardApi.getTodayCards()
            if (response.isSuccessful) {
                val cards = response.body()?.cards ?: emptyList()
                val entities = cards.map { dto ->
                    FlashcardEntity(
                        id = dto.id,
                        sourceId = dto.sourceId,
                        question = dto.question,
                        options = dto.options,
                        correctAnswer = dto.correctAnswer,
                        explanation = dto.explanation,
                        category = dto.category,
                        difficulty = dto.difficulty,
                        masteryLevel = dto.masteryLevel,
                        intervalDays = dto.intervalDays,
                        nextReviewAt = if (dto.nextReviewAt > 0) dto.nextReviewAt else System.currentTimeMillis()
                    )
                }
                flashcardDao.upsertAll(entities)
                entities
            } else {
                // Fallback to local
                flashcardDao.getCardsForReview()
            }
        } catch (e: Exception) {
            // Fallback to local on network error
            flashcardDao.getCardsForReview()
        }
    }

    override suspend fun reviewCard(cardId: String, rating: Int) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Calculate next review based on rating (spaced repetition)
        // 0=again (1d), 1=hard (2d), 2=good (4d), 3=easy (8d)
        val intervalDays = when (rating) {
            0 -> 1
            1 -> 2
            2 -> 4
            3 -> 8
            else -> 1
        }

        calendar.add(Calendar.DAY_OF_YEAR, intervalDays)
        val nextReview = calendar.timeInMillis

        // Update mastery based on rating
        val masteryIncrement = when (rating) {
            0 -> -0.2f
            1 -> 0.1f
            2 -> 0.3f
            3 -> 0.5f
            else -> 0f
        }

        // Get current card to adjust mastery
        val existingCards = flashcardDao.getCardsForReview()
        val currentMastery = existingCards.find { it.id == cardId }?.masteryLevel ?: 0f
        val newMastery = (currentMastery + masteryIncrement).coerceIn(0f, 1f)

        flashcardDao.updateMastery(
            id = cardId,
            mastery = newMastery,
            interval = intervalDays,
            nextReview = nextReview
        )

        // Record review log
        val log = FlashcardReviewLogEntity(
            id = UUID.randomUUID().toString(),
            flashcardId = cardId,
            rating = rating,
            reviewedAt = now
        )
        flashcardDao.insertReviewLog(log)

        // Also submit to remote
        try {
            flashcardApi.submitReview(
                com.aitutor.app.data.remote.dto.FlashcardReviewRequest(
                    flashcardId = cardId,
                    rating = rating
                )
            )
        } catch (_: Exception) {
            // Will sync later
        }
    }

    override suspend fun syncUnsyncedLogs(): Boolean {
        return try {
            val unsyncedLogs = flashcardDao.getUnsyncedReviewLogs()
            if (unsyncedLogs.isEmpty()) return true

            val logDtos = unsyncedLogs.map {
                FlashcardReviewLogDto(
                    id = it.id,
                    flashcardId = it.flashcardId,
                    rating = it.rating,
                    reviewedAt = it.reviewedAt
                )
            }

            val response = flashcardApi.syncReviewLogs(FlashcardSyncRequest(logDtos))
            if (response.isSuccessful) {
                flashcardDao.markLogsSynced(unsyncedLogs.map { it.id })
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun countPendingCards(): Int {
        return flashcardDao.countPendingCards()
    }

    override suspend fun countReviewedToday(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return flashcardDao.countReviewedToday(calendar.timeInMillis)
    }
}
