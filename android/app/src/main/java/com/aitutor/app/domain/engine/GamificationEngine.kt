package com.aitutor.app.domain.engine

import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.ScoreLogDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.local.entity.AchievementEntity
import com.aitutor.app.data.local.entity.ScoreLogEntity
import com.aitutor.app.data.local.entity.UserScoreEntity
import com.aitutor.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.data.repository.paging.LeaderboardPagingSource
import com.aitutor.app.domain.model.LeaderboardType
import com.aitutor.app.domain.repository.AuthRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 游戏化引擎 - 核心积分、成就、连续学习逻辑
 */
@Singleton
class GamificationEngine @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userScoreDao: UserScoreDao,
    private val scoreLogDao: ScoreLogDao,
    private val gamificationApi: GamificationApi,
    // [v30] 用于在 LeaderboardPagingSource 里定位"我"
    private val authRepository: AuthRepository
) {
    private val _unlockAchievementFlow = MutableSharedFlow<Achievement>(extraBufferCapacity = 10)
    val unlockAchievementFlow: SharedFlow<Achievement> = _unlockAchievementFlow.asSharedFlow()

    /**
     * 处理学习事件：积分计算、连续学习更新、成就检测
     */
    suspend fun onEvent(event: LearningEvent) {
        // AppOpened 是纯无操作事件，不处理任何逻辑
        if (event is LearningEvent.AppOpened) return

        // 1. 计算基础积分
        val baseScore = ScoreCalculator.calculateScore(event)

        // 2. 获取或初始化用户积分
        var userScoreEntity = userScoreDao.getScoreOnce()
        if (userScoreEntity == null) {
            userScoreEntity = UserScoreEntity()
            userScoreDao.upsertScore(userScoreEntity)
        }

        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 3. 更新学习日期记录，重新计算连续学习
            val learningDates = userScoreEntity.lastLearningDate
                .takeIf { it.isNotEmpty() }
                ?.split(",")
                ?: emptyList()

            val updatedDates = if (!learningDates.contains(todayStr)) {
                learningDates + todayStr
            } else {
                learningDates
            }

            // 4. 重新计算连续学习
            val streakResult = StreakCalculator.calculateStreak(updatedDates)

            // 5. 计算连续学习额外积分（至少连续2天才开始奖励）
            val streakBonus = if (streakResult.hasLearnedToday && baseScore > 0 && streakResult.currentStreak > 1) {
                ScoreCalculator.calculateStreakBonus(streakResult.currentStreak)
            } else {
                0
            }

            val totalScoreChange = baseScore + streakBonus
            val newTotalScore = userScoreEntity.totalScore + totalScoreChange

            // 6. 更新用户积分
            val updatedScore = userScoreEntity.copy(
                totalScore = newTotalScore,
                currentStreak = streakResult.currentStreak,
                longestStreak = maxOf(userScoreEntity.longestStreak, streakResult.longestStreak),
                lastLearningDate = updatedDates.joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
            userScoreDao.upsertScore(updatedScore)

            // 7. 记录积分日志（使用 ScoreLogDao）
            val scoreLog = ScoreLogEntity(
                eventType = event::class.simpleName ?: "Unknown",
                score = totalScoreChange,
                description = buildScoreDescription(event, streakBonus),
                date = todayStr,
                createdAt = System.currentTimeMillis()
            )
            scoreLogDao.insert(scoreLog)

            // 8. 检查成就
            val userScore = UserScore(
                totalScore = newTotalScore,
                currentStreak = streakResult.currentStreak,
                longestStreak = maxOf(userScoreEntity.longestStreak, streakResult.longestStreak),
                lastLearningDate = updatedDates.lastOrNull() ?: todayStr,
                hasLearnedToday = streakResult.hasLearnedToday
            )

            checkAndUnlockAchievements(userScore)
    }

    /**
     * [v30] 获取 Paging 3 排行榜流
     * @param type      排行榜类型 (GLOBAL / FRIENDS)
     * @param pageSize  每页条目数
     */
    fun getLeaderboardPaging(
        type: LeaderboardType = LeaderboardType.GLOBAL,
        pageSize: Int = 20
    ): Flow<PagingData<RankEntry>> {
        // [v30] 预获取当前用户 ID，确保 getCurrentUserId() 被立即调用
        val currentUserId = authRepository.getCurrentUserId().orEmpty()
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = 3,
                maxSize = 200,
                jumpThreshold = 1000
            ),
            pagingSourceFactory = {
                LeaderboardPagingSource(
                    gamificationApi = gamificationApi,
                    type = type,
                    currentUserId = currentUserId
                )
            }
        ).flow
    }

    /**
     * [v30 BugFix] 刷新排行榜（同步分数 + 通知云端刷新）
     * 数据拉取由 PagingSource.load() 完成，此处仅触发同步逻辑
     */
    suspend fun refreshLeaderboard() {
        try {
            gamificationApi.syncScore()
        } catch (_: Exception) {
            // 证书/网络失败不中断刷新流程
        }
        // 丢弃 Pager：ViewModel 层在刷新时调用 .cachedIn() 新 scope
        // 或通过 pagingItems.refresh() 由 Paging 3 framework 触发 PagingSource 新实例
    }

    /**
     * 获取排行榜（本地版）— 保留旧接口兼容
     */
    fun getLeaderboard(type: LeaderboardType): Flow<List<RankEntry>> {
        return userScoreDao.getScore().map { score ->
            if (score != null) {
                listOf(
                    RankEntry(
                        rank = 1,
                        nickname = "我",
                        score = score.totalScore,
                        isMe = true
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    /**
     * 获取成就列表
     */
    fun getAchievements(): Flow<List<AchievementWithStatus>> {
        return achievementDao.getAll().map { entities ->
            entities.map { entity ->
                val achievement = Achievement.fromId(entity.id)
                AchievementWithStatus(
                    achievement = achievement ?: Achievement.FIRST_SOLVE,
                    status = if (entity.status == "UNLOCKED") AchievementStatus.UNLOCKED else AchievementStatus.LOCKED,
                    unlockedAt = entity.unlockedAt
                )
            }
        }
    }

    /**
     * 获取用户积分
     */
    fun getUserScore(): Flow<UserScore?> {
        return userScoreDao.getScore().map { entity ->
            entity?.let {
                UserScore(
                    totalScore = it.totalScore,
                    currentStreak = it.currentStreak,
                    longestStreak = it.longestStreak,
                    lastLearningDate = it.lastLearningDate,
                    hasLearnedToday = hasLearnedToday(it.lastLearningDate)
                )
            }
        }
    }

    /**
     * 获取连续学习情况
     */
    fun getStreak(): Flow<StreakResult> {
        return userScoreDao.getScore().map { entity ->
            if (entity != null) {
                val learningDates = entity.lastLearningDate
                    .takeIf { it.isNotEmpty() }
                    ?.split(",")
                    ?: emptyList()
                StreakCalculator.calculateStreak(learningDates)
            } else {
                StreakResult()
            }
        }
    }

    /**
     * 初始化默认成就数据（首次安装时调用）
     */
    suspend fun initializeDefaultAchievements() {
        val existing = achievementDao.getAll().first().map { it.id }.toSet()
        val defaultAchievements = Achievement.entries.filter { it.id !in existing }
        if (defaultAchievements.isNotEmpty()) {
            val entities = defaultAchievements.map { achievement ->
                AchievementEntity(
                    id = achievement.id,
                    title = achievement.title,
                    description = achievement.description,
                    icon = achievement.icon,
                    conditionType = achievement.condition::class.simpleName ?: "",
                    conditionValue = extractConditionValue(achievement),
                    status = "LOCKED",
                    unlockedAt = null
                )
            }
            achievementDao.insertAll(entities)
        }
    }

    private fun hasLearnedToday(lastLearningDateStr: String): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (lastLearningDateStr.isEmpty()) return false
        val dates = lastLearningDateStr.split(",")
        return dates.contains(today)
    }

    private suspend fun checkAndUnlockAchievements(userScore: UserScore) {
        val allAchievements = achievementDao.getAll().first().map { entity ->
            val achievement = Achievement.fromId(entity.id)
            AchievementWithStatus(
                achievement = achievement ?: Achievement.FIRST_SOLVE,
                status = if (entity.status == "UNLOCKED") AchievementStatus.UNLOCKED else AchievementStatus.LOCKED,
                unlockedAt = entity.unlockedAt
            )
        }

        val lockedAchievements = allAchievements.filter { it.status == AchievementStatus.LOCKED }
        val newlyUnlocked = AchievementDetector.checkAchievements(userScore, lockedAchievements)

        for (achievement in newlyUnlocked) {
            achievementDao.updateStatus(achievement.id, "UNLOCKED")
            _unlockAchievementFlow.tryEmit(achievement)
        }
    }

    private fun buildScoreDescription(event: LearningEvent, streakBonus: Int): String {
        val baseDesc = when (event) {
            is LearningEvent.MessageSent -> "发送消息 +2分"
            is LearningEvent.SolveCompleted -> "完成解题 +10分"
            is LearningEvent.QuizCompleted -> "完成测验 +20分"
            is LearningEvent.ReviewCompleted -> "完成复习 +15分"
            is LearningEvent.AppOpened -> "打开应用"
            is LearningEvent.StreakMaintained -> "保持连续学习"
        }
        return if (streakBonus > 0) {
            "$baseDesc，连续奖励 +${streakBonus}分"
        } else {
            baseDesc
        }
    }

    private fun extractConditionValue(achievement: Achievement): Int {
        return when (val condition = achievement.condition) {
            is AchievementCondition.SolveCount -> condition.count
            is AchievementCondition.StreakDays -> condition.days
            is AchievementCondition.QuizCorrectCount -> condition.count
            is AchievementCondition.QuizPerfect -> 0
            is AchievementCondition.ReviewCount -> condition.count
            is AchievementCondition.TotalScore -> condition.score
        }
    }
}
