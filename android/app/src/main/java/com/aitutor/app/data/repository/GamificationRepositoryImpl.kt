package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.data.repository.paging.LeaderboardPagingSource
import com.aitutor.app.domain.engine.GamificationEngine
import com.aitutor.app.domain.engine.LearningEvent
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import androidx.paging.PagingData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val gamificationEngine: GamificationEngine,
    private val gamificationApi: GamificationApi,
    // [v30] 用于透传当前用户 ID 到分页源（"我"的高亮）
    private val authRepository: AuthRepository
) : GamificationRepository {

    override suspend fun onEvent(event: LearningEvent) {
        gamificationEngine.onEvent(event)
    }

    override fun observeAchievements(): Flow<List<AchievementWithStatus>> {
        return gamificationEngine.getAchievements()
    }

    override fun observeUserScore(): Flow<UserScore?> {
        return gamificationEngine.getUserScore()
    }

    override fun observeLeaderboard(): Flow<List<RankEntry>> =
        gamificationEngine.getLeaderboard(LeaderboardType.GLOBAL)

    // [v30] Paging 3 分页排行榜（替代旧 List<RankEntry> 接口）
    override fun observeLeaderboardPaging(
        type: LeaderboardType,
        pageSize: Int
    ): Flow<PagingData<RankEntry>> {
        return gamificationEngine.getLeaderboardPaging(type, pageSize)
    }

    override fun observeStreak(): Flow<StreakResult> {
        return gamificationEngine.getStreak()
    }

    // [v30 BugFix] 旧 refreshLeaderboard 逻辑迁移至 Engine
    // 仅触发分数同步和分页源失效，数据通过 Paging 3 从云端拉取
    override suspend fun refreshLeaderboard() = gamificationEngine.refreshLeaderboard()

    override fun getUnlockedAchievementFlow(): SharedFlow<Achievement> {
        return gamificationEngine.unlockAchievementFlow
    }

    override suspend fun initializeAchievements() {
        gamificationEngine.initializeDefaultAchievements()
    }
}
