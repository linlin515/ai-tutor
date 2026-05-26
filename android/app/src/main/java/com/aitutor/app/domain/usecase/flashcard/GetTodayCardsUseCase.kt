package com.aitutor.app.domain.usecase.flashcard

import com.aitutor.app.domain.model.Flashcard
import com.aitutor.app.domain.repository.FlashcardRepository
import javax.inject.Inject

class GetTodayCardsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(): List<Flashcard> {
        return flashcardRepository.getTodayCards().map { entity ->
            Flashcard(
                id = entity.id,
                sourceId = entity.sourceId,
                question = entity.question,
                correctAnswer = entity.correctAnswer,
                explanation = entity.explanation,
                category = entity.category,
                difficulty = entity.difficulty,
                masteryLevel = entity.masteryLevel,
                intervalDays = entity.intervalDays,
                nextReviewAt = entity.nextReviewAt
            )
        }
    }
}
