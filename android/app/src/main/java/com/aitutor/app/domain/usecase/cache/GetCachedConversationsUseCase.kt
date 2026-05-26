package com.aitutor.app.domain.usecase.cache

import com.aitutor.app.data.local.entity.CachedConversationEntity
import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject

class GetCachedConversationsUseCase @Inject constructor(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke(): List<CachedConversationEntity> {
        return offlineRepository.getCachedConversations()
    }
}
