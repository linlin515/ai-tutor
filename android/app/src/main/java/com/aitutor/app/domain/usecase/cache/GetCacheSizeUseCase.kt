package com.aitutor.app.domain.usecase.cache

import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject

class GetCacheSizeUseCase @Inject constructor(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke(): CacheSize {
        return offlineRepository.getCacheSizeInfo()
    }
}
