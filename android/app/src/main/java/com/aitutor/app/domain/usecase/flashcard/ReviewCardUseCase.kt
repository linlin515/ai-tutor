package com.aitutor.app.domain.usecase.flashcard

import com.aitutor.app.domain.repository.FlashcardRepository
import javax.inject.Inject

class ReviewCardUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(cardId: String, judgment: String) {
        val rating = when (judgment) {
            "mastered" -> 5
            "familiar" -> 3
            "unfamiliar" -> 1
            else -> 1
        }
        flashcardRepository.reviewCard(cardId, rating)
    }
}
