package com.aitutor.app.domain.repository

import com.aitutor.app.domain.engine.LearningEvent
import com.aitutor.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface GamificationRepository {
    suspend fun onEvent(event: LearningEvent)
    fun observeAchievements(): Flow<List<AchievementWithStatus>>
    fun observeUserScore(): Flow<UserScore?>
    fun observeLeaderboard(): Flow<List<RankEntry>>
    fun observeStreak(): Flow<StreakResult>
    suspend fun refreshLeaderboard()
    fun getUnlockedAchievementFlow(): SharedFlow<Achievement>
    suspend fun initializeAchievements()
}
