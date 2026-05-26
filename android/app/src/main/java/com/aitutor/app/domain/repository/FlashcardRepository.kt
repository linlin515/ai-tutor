package com.aitutor.app.domain.repository

import com.aitutor.app.data.local.entity.FlashcardEntity

interface FlashcardRepository {
    suspend fun getTodayCards(): List<FlashcardEntity>
    suspend fun reviewCard(cardId: String, rating: Int)
    suspend fun syncUnsyncedLogs(): Boolean
    suspend fun countPendingCards(): Int
    suspend fun countReviewedToday(): Int
}
