package com.aitutor.app.domain.repository

interface SyncRepository {
    suspend fun pullIncremental(since: Long): Boolean
    suspend fun pushOfflineActions(): Boolean
    suspend fun syncAll(): Boolean
}
