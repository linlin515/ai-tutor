package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.WrongAnswerDao
import com.aitutor.app.data.local.entity.WrongAnswerEntity
import com.aitutor.app.domain.engine.ReviewItem
import com.aitutor.app.domain.engine.SpacedRepetitionEngine
import com.aitutor.app.domain.model.WrongAnswerItem
import com.aitutor.app.domain.repository.WrongAnswerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WrongAnswerRepositoryImpl @Inject constructor(
    private val wrongAnswerDao: WrongAnswerDao,
    private val spacedRepetitionEngine: SpacedRepetitionEngine
) : WrongAnswerRepository {

    override fun getAllWrongAnswers(): Flow<List<WrongAnswerItem>> =
        wrongAnswerDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getWrongAnswersBySubject(subject: String): Flow<List<WrongAnswerItem>> =
        wrongAnswerDao.getBySubject(subject).map { entities -> entities.map { it.toDomain() } }

    override fun getDueReviews(): Flow<List<WrongAnswerItem>> =
        wrongAnswerDao.getDueReviews().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addWrongAnswer(
        question: String,
        correctAnswer: String,
        userAnswer: String,
        subject: String,
        knowledgePoint: String,
        source: String
    ) {
        val review = spacedRepetitionEngine.createInitialReview()
        wrongAnswerDao.insert(
            WrongAnswerEntity(
                question = question,
                correctAnswer = correctAnswer,
                userAnswer = userAnswer,
                subject = subject,
                knowledgePoint = knowledgePoint,
                source = source,
                nextReviewAt = review.nextReviewAt
            )
        )
    }

    override suspend fun updateReview(id: String, isCorrect: Boolean) {
        val entity = wrongAnswerDao.getById(id) ?: return
        val currentReview = ReviewItem(
            id = entity.id,
            intervalDays = entity.intervalDays,
            consecutiveCorrect = entity.consecutiveCorrect,
            isMastered = entity.isMastered,
            nextReviewAt = entity.nextReviewAt
        )
        val nextReview = spacedRepetitionEngine.calculateNextReview(currentReview, isCorrect)
        wrongAnswerDao.update(
            entity.copy(
                intervalDays = nextReview.intervalDays,
                consecutiveCorrect = nextReview.consecutiveCorrect,
                isMastered = nextReview.isMastered,
                nextReviewAt = nextReview.nextReviewAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteWrongAnswer(id: String) = wrongAnswerDao.deleteById(id)

    override suspend fun getWrongAnswerCount(): Flow<Int> = wrongAnswerDao.getDueCount()

    private fun WrongAnswerEntity.toDomain() = WrongAnswerItem(
        id = id,
        question = question,
        correctAnswer = correctAnswer,
        userAnswer = userAnswer,
        subject = subject,
        knowledgePoint = knowledgePoint,
        source = source,
        intervalDays = intervalDays,
        consecutiveCorrect = consecutiveCorrect,
        isMastered = isMastered,
        nextReviewAt = nextReviewAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
