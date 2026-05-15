package com.aitutor.app.domain.repository

import com.aitutor.app.domain.engine.ReviewItem
import com.aitutor.app.domain.model.WrongAnswerItem
import kotlinx.coroutines.flow.Flow

interface WrongAnswerRepository {
    fun getAllWrongAnswers(): Flow<List<WrongAnswerItem>>
    fun getWrongAnswersBySubject(subject: String): Flow<List<WrongAnswerItem>>
    fun getDueReviews(): Flow<List<WrongAnswerItem>>
    suspend fun addWrongAnswer(
        question: String,
        correctAnswer: String,
        userAnswer: String,
        subject: String,
        knowledgePoint: String,
        source: String
    )
    suspend fun updateReview(
        id: String,
        isCorrect: Boolean
    )
    suspend fun deleteWrongAnswer(id: String)
    suspend fun getWrongAnswerCount(): Flow<Int>
}
