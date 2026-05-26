package com.aitutor.app.domain.usecase.cache

import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject

class ClearCacheUseCase @Inject constructor(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke() {
        offlineRepository.clearAllCache()
    }
}
