package com.aitutor.app.domain.usecase.cache

import com.aitutor.app.data.local.entity.CachedWrongAnswerEntity
import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject

class GetCachedWrongAnswersUseCase @Inject constructor(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke(): List<CachedWrongAnswerEntity> {
        return offlineRepository.getCachedWrongAnswers()
    }
}
