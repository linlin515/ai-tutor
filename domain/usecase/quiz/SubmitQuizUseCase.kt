package com.aitutor.app.domain.usecase.quiz

import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SubmitQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    operator fun invoke(quizId: String, answers: Map<String, String>, duration: Int): Flow<QuizSubmitResult> {
        return quizRepository.submitAnswers(quizId, answers, duration)
    }
}
