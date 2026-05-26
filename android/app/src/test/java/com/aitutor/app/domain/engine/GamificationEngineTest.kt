package com.aitutor.app.domain.engine

import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.ScoreLogDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.local.entity.AchievementEntity
import com.aitutor.app.data.local.entity.ScoreLogEntity
import com.aitutor.app.data.local.entity.UserScoreEntity
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 单元测试：GamificationEngine
 *
 * 覆盖场景：
 * - onEvent 各种学习事件（积分计算、连续学习更新、成就检测）
 * - AppOpened 跳过处理
 * - 首次事件初始化 UserScoreEntity
 * - 连续学习积分加成
 * - 成就解锁流程
 * - getLeaderboardPaging Pager 装配
 * - refreshLeaderboard 同步分数
 * - getLeaderboard / getAchievements / getUserScore / getStreak
 * - initializeDefaultAchievements
 */
@DisplayName("GamificationEngine")
class GamificationEngineTest {

    private val achievementDao: AchievementDao = mockk()
    private val userScoreDao: UserScoreDao = mockk()
    private val scoreLogDao: ScoreLogDao = mockk()
    private val gamificationApi: GamificationApi = mockk()
    private val authRepository: AuthRepository = mockk()
    private lateinit var engine: GamificationEngine

    private val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    @BeforeEach
    fun setUp() {
        engine = GamificationEngine(
            achievementDao = achievementDao,
            userScoreDao = userScoreDao,
            scoreLogDao = scoreLogDao,
            gamificationApi = gamificationApi,
            authRepository = authRepository
        )
    }

    @Nested
    @DisplayName("onEvent(event)")
    inner class OnEvent {

        @Test
        @DisplayName("MessageSent 应增加 2 分，无连续奖励")
        fun `messageSent adds 2 points without streak bonus`() = runTest {
            val existingScore = UserScoreEntity(
                totalScore = 100,
                currentStreak = 0,
                longestStreak = 0,
                lastLearningDate = ""
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())
            coEvery { achievementDao.updateStatus(any(), any()) } returns Unit

            engine.onEvent(LearningEvent.MessageSent)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(102, scoreSlot.captured.totalScore) // 100 + 2
            assertEquals(1, scoreSlot.captured.currentStreak) // 今天学习 → streak=1
            assertEquals(1, scoreSlot.captured.longestStreak)

            coVerify { scoreLogDao.insert(any<ScoreLogEntity>()) }
        }

        @Test
        @DisplayName("首次事件应初始化 UserScoreEntity")
        fun `first event initializes user score`() = runTest {
            coEvery { userScoreDao.getScoreOnce() } returns null
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())
            coEvery { achievementDao.updateStatus(any(), any()) } returns Unit

            engine.onEvent(LearningEvent.SolveCompleted)

            coVerify(exactly = 2) { userScoreDao.upsertScore(any()) } // 初始化 + 更新
        }

        @Test
        @DisplayName("SolveCompleted 应增加 10 分")
        fun `solveCompleted adds 10 points`() = runTest {
            val existingScore = UserScoreEntity(totalScore = 50, lastLearningDate = "")
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.SolveCompleted)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(60, scoreSlot.captured.totalScore) // 50 + 10
        }

        @Test
        @DisplayName("QuizCompleted 非全对应增加 20 分")
        fun `quizCompleted not perfect adds 20 points`() = runTest {
            val existingScore = UserScoreEntity(totalScore = 0, lastLearningDate = "")
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.QuizCompleted(correctCount = 3, totalCount = 5))

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(20, scoreSlot.captured.totalScore)
        }

        @Test
        @DisplayName("QuizCompleted 全对应增加 30 分（含 10 分额外奖励）")
        fun `quizCompleted perfect adds 30 points`() = runTest {
            val existingScore = UserScoreEntity(totalScore = 0, lastLearningDate = "")
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.QuizCompleted(correctCount = 5, totalCount = 5))

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(30, scoreSlot.captured.totalScore)
        }

        @Test
        @DisplayName("ReviewCompleted 应增加 15 分")
        fun `reviewCompleted adds 15 points`() = runTest {
            val existingScore = UserScoreEntity(totalScore = 10, lastLearningDate = "")
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.ReviewCompleted)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(25, scoreSlot.captured.totalScore) // 10 + 15
        }

        @Test
        @DisplayName("AppOpened 不应更新积分或连续学习")
        fun `appOpened does not update anything`() = runTest {
            coEvery { userScoreDao.getScoreOnce() } returns null
            // 不应调用 upsertScore、scoreLogDao.insert、achievementDao.updateStatus
            engine.onEvent(LearningEvent.AppOpened)

            coVerify(exactly = 0) { userScoreDao.upsertScore(any()) }
            coVerify(exactly = 0) { scoreLogDao.insert(any()) }
        }

        @Test
        @DisplayName("连续学习 3 天时应加 streak bonus = 15")
        fun `streak bonus adds 15 for 3 consecutive days`() = runTest {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayBefore = LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingScore = UserScoreEntity(
                totalScore = 0,
                lastLearningDate = "$dayBefore,$yesterday"
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.SolveCompleted)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            // 10 (solve) + 15 (3 days × 5) = 25
            assertEquals(25, scoreSlot.captured.totalScore)
            assertEquals(3, scoreSlot.captured.currentStreak)
        }

        @Test
        @DisplayName("连续学习奖励中 baseScore=0 时不应加 streak bonus")
        fun `streak bonus not applied when baseScore is 0`() = runTest {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingScore = UserScoreEntity(
                totalScore = 100,
                lastLearningDate = yesterday
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            // StreakMaintained has baseScore = 0
            engine.onEvent(LearningEvent.StreakMaintained)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            assertEquals(100, scoreSlot.captured.totalScore) // no addition
        }

        @Test
        @DisplayName("今日已学习过不应重复记录日期")
        fun `repeated event today does not duplicate date`() = runTest {
            val existingScore = UserScoreEntity(
                totalScore = 50,
                lastLearningDate = today
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())

            engine.onEvent(LearningEvent.MessageSent)

            val scoreSlot = slot<UserScoreEntity>()
            coVerify { userScoreDao.upsertScore(capture(scoreSlot)) }
            // lastLearningDate should still contain only one today
            val dates = scoreSlot.captured.lastLearningDate.split(",")
            assertEquals(1, dates.count { it == today })
        }

        @Test
        @DisplayName("成就条件满足时自动解锁并通过 Flow 发射")
        fun `achievement unlocked when condition met`() = runTest {
            val existingScore = UserScoreEntity(
                totalScore = 0,
                lastLearningDate = ""
            )
            val lockedAchievement = AchievementEntity(
                id = "first_solve",
                title = "初次解题",
                description = "完成第一道解题",
                icon = "\uD83C\uDFAF",
                conditionType = "SolveCount",
                conditionValue = 1,
                status = "LOCKED"
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(listOf(lockedAchievement))
            coEvery { achievementDao.updateStatus(any(), any()) } returns Unit

            engine.onEvent(LearningEvent.SolveCompleted)

            coVerify { achievementDao.updateStatus("first_solve", "UNLOCKED") }

            val flowValue = engine.unlockAchievementFlow.replayCache
            assertTrue(flowValue.contains(Achievement.FIRST_SOLVE))
        }

        @Test
        @DisplayName("已经解锁的成就不会再次触发")
        fun `already unlocked achievement not triggered again`() = runTest {
            val existingScore = UserScoreEntity(
                totalScore = 10000,
                lastLearningDate = ""
            )
            val unlockedAchievement = AchievementEntity(
                id = "first_solve",
                title = "初次解题",
                description = "完成第一道解题",
                icon = "\uD83C\uDFAF",
                conditionType = "SolveCount",
                conditionValue = 1,
                status = "UNLOCKED",
                unlockedAt = 1000L
            )
            coEvery { userScoreDao.getScoreOnce() } returns existingScore
            coEvery { userScoreDao.upsertScore(any()) } returns Unit
            coEvery { scoreLogDao.insert(any()) } returns Unit
            coEvery { achievementDao.getAll() } returns flowOf(listOf(unlockedAchievement))

            engine.onEvent(LearningEvent.SolveCompleted)

            // should not call updateStatus for already unlocked
            coVerify(exactly = 0) { achievementDao.updateStatus(any(), any()) }
        }
    }

    @Nested
    @DisplayName("getLeaderboardPaging")
    inner class GetLeaderboardPaging {

        @Test
        @DisplayName("应返回 Flow<PagingData> 并正确装配 Pager")
        fun `returns flow and configures pager`() {
            every { authRepository.getCurrentUserId() } returns "user_123"

            val flow = engine.getLeaderboardPaging(LeaderboardType.GLOBAL, 20)

            assertNotNull(flow)
            verify { authRepository.getCurrentUserId() }
        }

        @Test
        @DisplayName("currentUserId 为空时也应正常工作")
        fun `works with empty userId`() {
            every { authRepository.getCurrentUserId() } returns null

            val flow = engine.getLeaderboardPaging(LeaderboardType.FRIENDS, 10)

            assertNotNull(flow)
            verify { authRepository.getCurrentUserId() }
        }
    }

    @Nested
    @DisplayName("refreshLeaderboard")
    inner class RefreshLeaderboard {

        @Test
        @DisplayName("应调用 api.syncScore()")
        fun `calls syncScore`() = runTest {
            coEvery { gamificationApi.syncScore() } returns mockk(relaxed = true)

            engine.refreshLeaderboard()

            coVerify { gamificationApi.syncScore() }
        }

        @Test
        @DisplayName("网络异常时不应抛出异常")
        fun `exception does not propagate`() = runTest {
            coEvery { gamificationApi.syncScore() } throws Exception("Network error")

            engine.refreshLeaderboard() // should not throw

            coVerify { gamificationApi.syncScore() }
        }
    }

    @Nested
    @DisplayName("getLeaderboard(type)")
    inner class GetLeaderboard {

        @Test
        @DisplayName("用户有积分时应返回包含当前用户的列表")
        fun `returns list with current user when score exists`() = runTest {
            val entity = UserScoreEntity(totalScore = 500)
            every { userScoreDao.getScore() } returns flowOf(entity)

            val result = engine.getLeaderboard(LeaderboardType.GLOBAL)

            result.collect { list ->
                assertEquals(1, list.size)
                assertEquals("我", list[0].nickname)
                assertEquals(500, list[0].score)
                assertEquals(1, list[0].rank)
                assertTrue(list[0].isMe)
            }
        }

        @Test
        @DisplayName("用户无积分时应返回空列表")
        fun `returns empty list when no score`() = runTest {
            every { userScoreDao.getScore() } returns flowOf(null)

            val result = engine.getLeaderboard(LeaderboardType.GLOBAL)

            result.collect { list ->
                assertTrue(list.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("getAchievements")
    inner class GetAchievements {

        @Test
        @DisplayName("应正确映射实体到 AchievementWithStatus")
        fun `maps entities to achievement with status`() = runTest {
            val entities = listOf(
                AchievementEntity(
                    id = "first_solve", title = "初次解题", description = "",
                    icon = "", conditionType = "", conditionValue = 1,
                    status = "UNLOCKED", unlockedAt = 1000L
                ),
                AchievementEntity(
                    id = "streak_7", title = "连续7天", description = "",
                    icon = "", conditionType = "", conditionValue = 7,
                    status = "LOCKED"
                )
            )
            every { achievementDao.getAll() } returns flowOf(entities)

            val result = engine.getAchievements()

            result.collect { list ->
                assertEquals(2, list.size)
                assertEquals(Achievement.FIRST_SOLVE, list[0].achievement)
                assertEquals(AchievementStatus.UNLOCKED, list[0].status)
                assertEquals(1000L, list[0].unlockedAt)
                assertEquals(Achievement.STREAK_7, list[1].achievement)
                assertEquals(AchievementStatus.LOCKED, list[1].status)
            }
        }

        @Test
        @DisplayName("fromId 找不到时应使用 FIRST_SOLVE 作为默认值")
        fun `uses FIRST_SOLVE as fallback for unknown id`() = runTest {
            val entities = listOf(
                AchievementEntity(
                    id = "unknown_id", title = "未知", description = "",
                    icon = "", conditionType = "", conditionValue = 0,
                    status = "LOCKED"
                )
            )
            every { achievementDao.getAll() } returns flowOf(entities)

            val result = engine.getAchievements()

            result.collect { list ->
                assertEquals(1, list.size)
                assertEquals(Achievement.FIRST_SOLVE, list[0].achievement)
            }
        }
    }

    @Nested
    @DisplayName("getUserScore")
    inner class GetUserScore {

        @Test
        @DisplayName("用户有数据时应正确映射")
        fun `maps entity to UserScore`() = runTest {
            val todayStr = today
            val entity = UserScoreEntity(
                totalScore = 500,
                currentStreak = 3,
                longestStreak = 10,
                lastLearningDate = todayStr
            )
            every { userScoreDao.getScore() } returns flowOf(entity)

            val result = engine.getUserScore()

            result.collect { score ->
                assertNotNull(score)
                assertEquals(500, score!!.totalScore)
                assertEquals(3, score.currentStreak)
                assertEquals(10, score.longestStreak)
                assertEquals(todayStr, score.lastLearningDate)
                assertTrue(score.hasLearnedToday)
            }
        }

        @Test
        @DisplayName("用户无数据时应返回 null")
        fun `returns null when no data`() = runTest {
            every { userScoreDao.getScore() } returns flowOf(null)

            val result = engine.getUserScore()

            result.collect { score ->
                assertEquals(null, score)
            }
        }
    }

    @Nested
    @DisplayName("getStreak")
    inner class GetStreak {

        @Test
        @DisplayName("有学习记录时应使用 StreakCalculator 计算")
        fun `calculates streak from learning dates`() = runTest {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entity = UserScoreEntity(lastLearningDate = yesterday)
            every { userScoreDao.getScore() } returns flowOf(entity)

            val result = engine.getStreak()

            result.collect { streak ->
                assertEquals(1, streak.currentStreak)
                assertEquals(1, streak.longestStreak)
                assertFalse(streak.hasLearnedToday)
            }
        }

        @Test
        @DisplayName("无学习记录时应返回默认值")
        fun `returns default when no data`() = runTest {
            every { userScoreDao.getScore() } returns flowOf(null)

            val result = engine.getStreak()

            result.collect { streak ->
                assertEquals(0, streak.currentStreak)
                assertEquals(0, streak.longestStreak)
                assertFalse(streak.hasLearnedToday)
            }
        }

        @Test
        @DisplayName("空的 lastLearningDate 也应返回默认值")
        fun `empty date string returns default`() = runTest {
            val entity = UserScoreEntity(lastLearningDate = "")
            every { userScoreDao.getScore() } returns flowOf(entity)

            val result = engine.getStreak()

            result.collect { streak ->
                assertEquals(0, streak.currentStreak)
                assertEquals(0, streak.longestStreak)
                assertFalse(streak.hasLearnedToday)
            }
        }
    }

    @Nested
    @DisplayName("initializeDefaultAchievements")
    inner class InitializeDefaultAchievements {

        @Test
        @DisplayName("首次安装时应插入所有成就")
        fun `inserts all achievements on first install`() = runTest {
            coEvery { achievementDao.getAll() } returns flowOf(emptyList())
            coEvery { achievementDao.insertAll(any()) } returns Unit

            engine.initializeDefaultAchievements()

            coVerify { achievementDao.insertAll(any<List<AchievementEntity>>()) }
        }

        @Test
        @DisplayName("已有成就时应跳过已存在的条目")
        fun `skips existing achievements`() = runTest {
            val existing = listOf(
                AchievementEntity(
                    id = "first_solve", title = "初次解题", description = "",
                    icon = "", conditionType = "", conditionValue = 1,
                    status = "UNLOCKED"
                )
            )
            coEvery { achievementDao.getAll() } returns flowOf(existing)

            engine.initializeDefaultAchievements()

            // insertAll should not be called if all achievements already exist
            coVerify(exactly = 0) { achievementDao.insertAll(any()) }
        }
    }
}
