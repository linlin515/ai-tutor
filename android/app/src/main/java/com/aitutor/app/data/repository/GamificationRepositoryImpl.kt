package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.domain.engine.GamificationEngine
import com.aitutor.app.domain.engine.LearningEvent
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val gamificationEngine: GamificationEngine,
    private val gamificationApi: GamificationApi
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

    override fun observeLeaderboard(): Flow<List<RankEntry>> {
        return gamificationEngine.getLeaderboard(LeaderboardType.GLOBAL)
    }

    override fun observeStreak(): Flow<StreakResult> {
        return gamificationEngine.getStreak()
    }

    override suspend fun refreshLeaderboard() {
        try {
            val response = gamificationApi.getLeaderboard()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    body.data?.let { data ->
                        // 排行榜数据从云端获取，本地引擎负责展示
                        // 目前本地仅存储当前用户数据
                    }
                }
            }
        } catch (_: Exception) {
            // 静默失败，使用本地数据
        }
    }

    override fun getUnlockedAchievementFlow(): SharedFlow<Achievement> {
        return gamificationEngine.unlockAchievementFlow
    }

    override suspend fun initializeAchievements() {
        gamificationEngine.initializeDefaultAchievements()
    }
}
