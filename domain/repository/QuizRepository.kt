package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    suspend fun generateQuiz(config: QuizConfig): Result<List<Question>>
    fun submitAnswers(quizId: String, answers: Map<String, String>, duration: Int): Flow<QuizSubmitResult>
    suspend fun syncPendingSubmissions()
}
