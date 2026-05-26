package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.CachedConversationDao
import com.aitutor.app.data.local.dao.CachedQuestionDao
import com.aitutor.app.data.local.dao.CachedWrongAnswerDao
import com.aitutor.app.data.local.dao.OfflineActionDao
import com.aitutor.app.data.local.dao.SyncMetadataDao
import com.aitutor.app.data.remote.api.SyncApi
import com.aitutor.app.data.remote.dto.SyncActionDto
import com.aitutor.app.data.remote.dto.SyncPushRequest
import com.aitutor.app.data.remote.dto.toEntity
import com.aitutor.app.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncApi: SyncApi,
    private val cachedQuestionDao: CachedQuestionDao,
    private val cachedWrongAnswerDao: CachedWrongAnswerDao,
    private val cachedConversationDao: CachedConversationDao,
    private val offlineActionDao: OfflineActionDao,
    private val syncMetadataDao: SyncMetadataDao
) : SyncRepository {

    override suspend fun pullIncremental(since: Long): Boolean {
        return try {
            val response = syncApi.pullIncremental(since)
            if (response.isSuccessful) {
                val body = response.body() ?: return false

                // Upsert pulled data into local cache
                if (body.questions.isNotEmpty()) {
                    cachedQuestionDao.upsertAll(body.questions.map { it.toEntity() })
                }
                if (body.wrongAnswers.isNotEmpty()) {
                    cachedWrongAnswerDao.upsertAll(body.wrongAnswers.map { it.toEntity() })
                }
                if (body.conversations.isNotEmpty()) {
                    cachedConversationDao.upsertAll(body.conversations.map { it.toEntity() })
                }

                // Update last sync timestamp
                syncMetadataDao.upsert(
                    com.aitutor.app.data.local.entity.SyncMetadataEntity(
                        key = "last_sync_timestamp",
                        value = body.lastSyncTimestamp.toString()
                    )
                )

                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun pushOfflineActions(): Boolean {
        return try {
            val unsyncedActions = offlineActionDao.getUnsyncedActions()
            if (unsyncedActions.isEmpty()) return true

            val actionDtos = unsyncedActions.map {
                SyncActionDto(
                    id = it.id,
                    type = it.type,
                    targetId = it.targetId,
                    payload = it.payload,
                    createdAt = it.createdAt
                )
            }

            val response = syncApi.pushOfflineActions(SyncPushRequest(actionDtos))
            if (response.isSuccessful) {
                offlineActionDao.markSynced(unsyncedActions.map { it.id })
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncAll(): Boolean {
        val pushOk = pushOfflineActions()
        val lastSync = try {
            syncMetadataDao.get("last_sync_timestamp")?.value?.toLongOrNull() ?: 0L
        } catch (e: Exception) { 0L }
        val pullOk = pullIncremental(lastSync)
        return pushOk && pullOk
    }
}
