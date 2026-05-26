package com.aitutor.app.data.repository

import com.aitutor.app.data.local.CacheManager
import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.data.local.dao.CachedConversationDao
import com.aitutor.app.data.local.dao.CachedQuestionDao
import com.aitutor.app.data.local.dao.CachedWrongAnswerDao
import com.aitutor.app.data.local.db.AiTutorDatabase
import com.aitutor.app.data.local.entity.CachedConversationEntity
import com.aitutor.app.data.local.entity.CachedQuestionEntity
import com.aitutor.app.data.local.entity.CachedWrongAnswerEntity
import com.aitutor.app.domain.repository.OfflineRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineRepositoryImpl @Inject constructor(
    private val cachedQuestionDao: CachedQuestionDao,
    private val cachedWrongAnswerDao: CachedWrongAnswerDao,
    private val cachedConversationDao: CachedConversationDao,
    private val cacheManager: CacheManager,
    private val database: AiTutorDatabase
) : OfflineRepository {

    override suspend fun getCachedQuestions(): List<CachedQuestionEntity> {
        return cachedQuestionDao.getAll()
    }

    override suspend fun getCachedWrongAnswers(): List<CachedWrongAnswerEntity> {
        return cachedWrongAnswerDao.getAll()
    }

    override suspend fun getCachedConversations(): List<CachedConversationEntity> {
        return cachedConversationDao.getAll()
    }

    override suspend fun getCacheSizeInfo(): CacheSize {
        return cacheManager.calculateSize()
    }

    override suspend fun clearAllCache() {
        cachedQuestionDao.deleteAll()
        cachedWrongAnswerDao.deleteAll()
        cachedConversationDao.deleteAll()
        cacheManager.clearAll()
    }
}
