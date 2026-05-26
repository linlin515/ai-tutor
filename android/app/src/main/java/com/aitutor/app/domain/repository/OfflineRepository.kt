package com.aitutor.app.domain.repository

import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.data.local.entity.CachedConversationEntity
import com.aitutor.app.data.local.entity.CachedQuestionEntity
import com.aitutor.app.data.local.entity.CachedWrongAnswerEntity

interface OfflineRepository {
    suspend fun getCachedQuestions(): List<CachedQuestionEntity>
    suspend fun getCachedWrongAnswers(): List<CachedWrongAnswerEntity>
    suspend fun getCachedConversations(): List<CachedConversationEntity>
    suspend fun getCacheSizeInfo(): CacheSize
    suspend fun clearAllCache()
}
