package com.aitutor.app.domain.repository

import com.aitutor.app.domain.engine.LearningEvent
import com.aitutor.app.domain.model.*
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface GamificationRepository {
    suspend fun onEvent(event: LearningEvent)
    fun observeAchievements(): Flow<List<AchievementWithStatus>>
    fun observeUserScore(): Flow<UserScore?>
    fun observeLeaderboard(): Flow<List<RankEntry>>

    // [v30] Paging 3 分页排行榜
    fun observeLeaderboardPaging(
        type: LeaderboardType = LeaderboardType.GLOBAL,
        pageSize: Int = 20
    ): Flow<PagingData<RankEntry>>

    fun observeStreak(): Flow<StreakResult>
    suspend fun refreshLeaderboard()
    fun getUnlockedAchievementFlow(): SharedFlow<Achievement>
    suspend fun initializeAchievements()
}
