package com.aitutor.app.domain.usecase.sync

import com.aitutor.app.domain.repository.SyncRepository
import javax.inject.Inject

class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val result = syncRepository.syncAll()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
