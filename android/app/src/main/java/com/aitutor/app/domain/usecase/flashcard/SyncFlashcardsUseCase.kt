package com.aitutor.app.domain.usecase.flashcard

import com.aitutor.app.domain.repository.FlashcardRepository
import javax.inject.Inject

class SyncFlashcardsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(): Boolean {
        return flashcardRepository.syncUnsyncedLogs()
    }
}
