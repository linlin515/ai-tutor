package com.aitutor.app.domain.usecase.quiz

import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.QuizRepository
import javax.inject.Inject

class GenerateQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(config: QuizConfig): Result<List<Question>> {
        return quizRepository.generateQuiz(config)
    }
}
