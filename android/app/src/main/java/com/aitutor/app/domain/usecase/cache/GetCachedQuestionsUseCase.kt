package com.aitutor.app.domain.usecase.cache

import com.aitutor.app.data.local.entity.CachedQuestionEntity
import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject

class GetCachedQuestionsUseCase @Inject constructor(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke(): List<CachedQuestionEntity> {
        return offlineRepository.getCachedQuestions()
    }
}
