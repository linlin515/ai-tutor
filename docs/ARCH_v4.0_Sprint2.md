# AI 学伴 v4.0 Sprint 2 — 架构设计文档

> **文档版本**: v1.0  
> **日期**: 2026-05-26  
> **负责人**: CFO（首席技术架构师）  
> **状态**: 待评审  
> **基准文档**: 
>   - PRD_v4.0_Sprint2.md (PM 产品需求文档)
>   - ARCH_v4.0_Sprint1.md (Sprint 1 架构设计)
>   - ARCH.md (现有架构文档)
> **Sprint 目标**: 补齐 P1/P2 核心体验断裂点，提升离线场景可用性和学习闭环完整性

---

## 目录

1. [Sprint 2 架构总览](#1-sprint-2-架构总览)
2. [P1-1: 离线模式架构设计](#2-p1-1-离线模式架构设计)
3. [P1-2: 学习报告导出架构设计](#3-p1-2-学习报告导出架构设计)
4. [P2-1: 后端 TTS 替换架构设计](#4-p2-1-后端-tts-替换架构设计)
5. [P2-2: Flashcard 抽认卡架构设计](#5-p2-2-flashcard-抽认卡架构设计)
6. [安全设计](#6-安全设计)
7. [兼容性矩阵](#7-兼容性矩阵)

---

## 1. Sprint 2 架构总览

### 1.1 核心架构原则

1. **增量演进，不动存量**: 基于 Sprint 1 架构扩展，不重构已有代码。Room DB 使用 Migration 而非 destructive 重建
2. **离线优先**: 关键数据走 Cache-first → Network-fallback 模式，离线可读、在线同步
3. **后端权威**: 所有业务状态（mastery_score、积分、报告）以服务端为准，本地仅做缓存和离线操作暂存
4. **降级兜底**: TTS 三级降级、Flashcard 部分离线可用、同步失败不阻塞 UI
5. **可观测性**: 新增功能均需日志记录和异常上报

### 1.2 Sprint 2 模块依赖关系

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                Android App (Sprint 2 新增/改造)                      │
│                                                                                     │
│  ┌──────────────────┐  ┌────────────────────┐  ┌────────────────┐  ┌──────────────┐ │
│  │ OfflineBanner    │  │ ExportReportScreen │  │ FlashcardScreen│  │ SettingsCache │ │
│  │ (离线指示器)      │  │ (报告导出)          │  │ (左右划动)      │  │ (缓存管理)    │ │
│  └────────┬─────────┘  └────────┬───────────┘  └───────┬────────┘  └──────┬───────┘ │
│           │                     │                       │                  │         │
│  ┌────────▼─────────┐  ┌────────▼───────────┐  ┌────────▼────────┐  ┌──────▼───────┐ │
│  │ OfflineViewModel │  │ ExportReportVM     │  │ FlashcardVM     │  │ SettingsVM   │ │
│  └────────┬─────────┘  └────────┬───────────┘  └────────┬────────┘  └──────┬───────┘ │
│           │                     │                       │                  │         │
│  ┌────────▼─────────────────────▼───────────────────────▼──────────────────▼───────┐ │
│  │                          Repository Layer                                       │ │
│  │  ┌──────────────┐ ┌────────────┐ ┌───────────────┐ ┌──────────┐ ┌────────────┐ │ │
│  │  │OfflineRepo   │ │SyncRepo    │ │ReportRepo     │ │Flashcard │ │NetworkMon  │ │ │
│  │  │ Cache-first  │ │增量同步     │ │下载+ShareSheet│ │Repo(缓存) │ │(已有扩展)   │ │ │
│  │  └──────┬───────┘ └─────┬──────┘ └──────┬────────┘ └─────┬────┘ └──────┬─────┘ │ │
│  │         │               │               │               │             │       │ │
│  │  ┌──────▼───────────────▼───────────────▼───────────────▼─────────────▼───────┐ │
│  │  │                       Room DB (v1→v2) + Remote Api                         │ │
│  │  │  ┌────────────────┐  ┌───────────────┐  ┌────────────┐  ┌──────────────┐ │ │
│  │  │  │CachedQuestion  │  │CachedWrongAns │  │CachedConv  │  │Flashcard     │ │ │
│  │  │  │Entity/DAO      │  │Entity/DAO     │  │Entity/DAO  │  │Entity/DAO    │ │ │
│  │  │  └────────────────┘  └───────────────┘  └────────────┘  └──────────────┘ │ │
│  │  └──────────────────────────────────────────────────────────────────────────┘ │
│  └───────────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                                 │
│          ┌─────────────────────────┼───────────────────────────┐                    │
│          ▼                         ▼                           ▼                    │
│  ┌───────────────┐     ┌──────────────────────┐      ┌──────────────────┐          │
│  │ Backend sync  │     │ Backend report       │      │ Backend flashcard│          │
│  │ GET/POST /sync│     │ POST /report/export  │      │ GET /flashcard/* │          │
│  │ 增量同步       │     │ (PDF/CSV)             │      │ POST /review     │          │
│  └───────┬───────┘     └──────────┬───────────┘      └────────┬─────────┘          │
│          │                        │                          │                    │
│          ▼                        ▼                          ▼                    │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │                        Backend Services                                       │  │
│  │  ┌─────────────┐  ┌──────────────────┐  ┌──────────────┐  ┌──────────────┐  │  │
│  │  │ SyncService │  │ ReportService    │  │ TtsProvider  │  │ FlashcardSvc │  │  │
│  │  │ 增量查询     │  │ ReportLab/CSV    │  │ Edge/OpenAI  │  │ 抽取/评分    │  │  │
│  │  │ 冲突检测     │  │ Matplotlib图表   │  │ 三级降级     │  │ 归档逻辑     │  │  │
│  │  └──────┬──────┘  └────────┬─────────┘  └──────┬───────┘  └──────┬───────┘  │  │
│  │         │                  │                    │                │           │  │
│  │  ┌──────▼──────────────────▼────────────────────▼────────────────▼──────────┐ │  │
│  │  │                           PostgreSQL + Redis                              │ │  │
│  │  │  quiz_records, quiz_questions, wrong_answers, conversations, user_scores  │ │  │
│  │  └───────────────────────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. P1-1: 离线模式架构设计

### 2.1 职责

提供「先缓存后浏览」的离线体验：用户在无网络环境下可浏览已缓存的题目、AI Tutor 对话历史和错题本；网络恢复后自动增量同步。

### 2.2 架构图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         Offline 模式组件架构                                      │
│                                                                                 │
│  UI Layer (Compose)                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐     │
│  │ ┌─────────────────┐  ┌────────────────┐  ┌─────────────────────────┐  │     │
│  │ │ OfflineBanner   │  │ QuestionList   │  │ CacheManagementSection  │  │     │
│  │ │ (顶部Banner)     │  │ (题目列表页)    │  │ (设置-缓存管理)          │  │     │
│  │ └────────┬────────┘  └───────┬────────┘  └───────────┬─────────────┘  │     │
│  └──────────┼───────────────────┼──────────────────────┼──────────────────┘     │
│             │                   │                      │                         │
│  VM Layer   ▼                   ▼                      ▼                         │
│  ┌────────────────────────────────────────────────────────────────────────┐     │
│  │ OfflineViewModel: isOnline, cacheStats, syncState                     │     │
│  │ SyncViewModel: syncProgress, lastSyncTimestamp, syncConflictCount     │     │
│  └──────────────────┬───────────────────────────────────┬────────────────┘     │
│                     │                                   │                       │
│  Domain Layer       ▼                                   ▼                       │
│  ┌────────────────────────────────────────────────────────────────────────┐     │
│  │ UseCases:                                                              │     │
│  │  - SyncDataUseCase (网络恢复后自动同步)                                 │     │
│  │  - GetCachedQuestionsUseCase (从Room获取缓存题目)                       │     │
│  │  - GetCachedWrongAnswersUseCase (从Room获取缓存错题)                    │     │
│  │  - GetCachedConversationsUseCase (从Room获取缓存对话)                   │     │
│  │  - GetCacheSizeUseCase (统计缓存大小)                                   │     │
│  │  - ClearCacheUseCase (清除全部缓存)                                     │     │
│  │  - SubmitOfflineActionsUseCase (提交离线期间操作日志)                    │     │
│  └──────────────────┬───────────────────────────────────┬────────────────┘     │
│                     │                                   │                       │
│  Data Layer         ▼                                   ▼                       │
│  ┌────────────────────────────────────────────────────────────────────────┐     │
│  │ OfflineRepositoryImpl (Cache-first → Network-fallback → UI state)      │     │
│  │  ├── getQuestions():                                                    │     │
│  │  │   1. 从 Room 读取 CachedQuestionEntity                               │     │
│  │  │   2. 如果联网: 后台拉取最新数据更新 Room + 返回新数据                │     │
│  │  │   3. 如果离线: 直接返回 Room 数据 + isStale=true                    │     │
│  │  │   4. 如果 Room 无数据: 返回空列表 + EmptyState                       │     │
│  │  ├── getWrongAnswers(): 同上                                            │     │
│  │  └── getConversations(): 同上                                           │     │
│  │                                                                         │     │
│  │ SyncRepositoryImpl:                                                     │     │
│  │  ├── pullIncremental(since: Timestamp)                                  │     │
│  │  │   → GET /api/v1/sync?since=timestamp                                │     │
│  │  │   → 解析增量数据 → 写入 Room (事务)                                  │     │
│  │  ├── pushOfflineActions(actions: List<OfflineAction>)                   │     │
│  │  │   → POST /api/v1/sync body: { actions: [...] }                      │     │
│  │  └── syncAll(): pullIncremental + pushOfflineActions + 清除已同步动作   │     │
│  │                                                                         │     │
│  │ NetworkMonitor (已有扩展):                                               │     │
│  │  ├── isOnline: StateFlow<Boolean>                                       │     │
│  │  ├── connectivityState: StateFlow<NetworkState>                         │     │
│  │  │   └── NetworkState { ONLINE, OFFLINE, WIFI, CELLULAR, VPN }        │     │
│  │  └── observe(): Flow<ConnectivityState> (基于 ConnectivityManager)      │     │
│  │                                                                         │     │
│  │ CacheManager:                                                           │     │
│  │  ├── getCacheSize(): Flow<List<CacheCategoryInfo>>                      │     │
│  │  ├── clearCache(): Completable                                          │     │
│  │  └── evictIfNeeded(): LRU 淘汰最旧 N 条                                  │     │
│  └────────────────────────────────────────────────────────────────────────┘     │
│                                                                                 │
│  Room DB (v2)                                                                   │
│  ┌────────────────────────────────────────────────────────────────────────┐     │
│  │ ┌──────────────────────┐  ┌──────────────────────┐  ┌────────────────┐ │     │
│  │ │ CachedQuestionEntity │  │ CachedWrongAnsEntity │  │CachedConvEntity│ │     │
│  │ │ - id (PK)            │  │ - id (PK)            │  │ - id (PK)      │ │     │
│  │ │ - subject             │  │ - subject             │  │ - title         │ │     │
│  │ │ - content             │  │ - questionContent    │  │ - messages(JSON)│ │     │
│  │ │ - ...                  │  │ - ...                 │  │ - ...           │ │     │
│  │ │ - cachedAt (排序/淘汰)│  │ - cachedAt            │  │ - cachedAt      │ │     │
│  │ └──────────────────────┘  └──────────────────────┘  └────────────────┘ │     │
│  │ ┌──────────────────────┐  ┌──────────────────────────────────────────┐ │     │
│  │ │ OfflineActionEntity  │  │ SyncMetadata (单行表)                    │ │     │
│  │ │ - id (PK)            │  │ - key="last_sync_timestamp"             │ │     │
│  │ │ - type               │  │ - value="2026-05-26T10:00:00Z"          │ │     │
│  │ │ - payload (JSON)     │  └──────────────────────────────────────────┘ │     │
│  │ │ - createdAt          │                                                │     │
│  │ └──────────────────────┘                                                │     │
│  └────────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 数据流

#### 2.3.1 离线读取数据流

```
用户打开题目列表
    │
    ▼
OfflineRepositoryImpl.getQuestions()
    │
    ├── 1. 读取 Room cached_questions 表
    │       SELECT * FROM cached_questions ORDER BY cachedAt DESC LIMIT 200
    │       └── 返回: List<CachedQuestionEntity>
    │
    ├── 2. 检查 NetworkMonitor.isOnline
    │       ├── ONLINE → 后台异步拉取最新数据（不阻塞 UI）
    │       │   └── SyncRepository.pullIncremental()
    │       │       └── On success → 更新 Room + 发送 UI 刷新事件
    │       └── OFFLINE → 标记 isStale=true（显示"离线数据"提示）
    │
    ├── 3. 如果 Room 有数据 → 返回数据 + isStale 状态
    │
    └── 4. 如果 Room 无数据 → 返回 EmptyState + 文案"暂无缓存内容"
```

#### 2.3.2 网络恢复自动同步流

```
ConnectivityManager 广播网络恢复
    │
    ▼
NetworkMonitor.observe() 发出 ONLINE 事件
    │
    ▼
SyncDataUseCase 自动触发
    │
    ├── Step 1: 读取本地 SyncMetadata.lastSyncTimestamp
    │
    ├── Step 2: GET /api/v1/sync?since=<lastSyncTimestamp>
    │   ├── 返回: updated_questions, updated_conversations,
    │   │         updated_wrong_answers, deleted_ids, sync_timestamp
    │   │
    │   └── 写入 Room (使用事务):
    │       ├── INSERT/UPDATE cached_questions
    │       ├── INSERT/UPDATE cached_conversations
    │       ├── INSERT/UPDATE cached_wrong_answers
    │       └── DELETE FROM cached_* WHERE id IN deleted_ids[...]
    │
    ├── Step 3: 读取 OfflineActionEntity 表（离线期间积压的操作）
    │   ├── 如果存在未同步操作:
    │   │   └── POST /api/v1/sync body: { actions: [...] }
    │   └── 如果成功 → 删除已同步的 OfflineActionEntity
    │
    └── Step 4: 更新 SyncMetadata.lastSyncTimestamp = response.sync_timestamp
    │
    ▼
发送 UI 刷新事件 → ViewModel 重组 → UI 更新
```

#### 2.3.3 缓存淘汰策略 (LRU)

```
触发条件:
  1. 写入新缓存时检查各表记录数是否超上限
  2. 用户手动点击"清除缓存"

CacheManager.evictIfNeeded():
  针对每张缓存表:
    检查表记录数 > maxRecords
    │
    ├── 是 → 删除最旧的 N 条记录:
    │       DELETE FROM ${tableName}
    │       WHERE cachedAt IN (
    │         SELECT cachedAt FROM ${tableName}
    │         ORDER BY cachedAt ASC
    │         LIMIT ${overLimit}
    │       )
    └── 否 → 不做操作

上限配置:
  cached_questions: 200 行
  cached_conversations: 50 行
  cached_wrong_answers: 200 行
  offline_actions: 500 行（超出时丢弃最旧 action）
```

### 2.4 类/接口设计

#### 2.4.1 Android 端关键接口

```kotlin
// === Domain Layer ===

// OfflineRepository.kt (新建)
interface OfflineRepository {
    // 读取缓存题目 (Cache-first → Network-fallback)
    fun getCachedQuestions(): Flow<Resource<List<Question>>>

    // 读取缓存对话
    fun getCachedConversations(): Flow<Resource<List<Conversation>>>

    // 读取缓存错题
    fun getCachedWrongAnswers(): Flow<Resource<List<WrongAnswer>>>

    // 获取各分类缓存大小
    fun getCacheSizeInfo(): Flow<List<CacheCategoryInfo>>

    // 清除所有缓存
    suspend fun clearAllCache(): Result<Unit>

    // 判断是否有缓存数据
    suspend fun hasCachedData(): Boolean
}

// SyncRepository.kt (新建)
interface SyncRepository {
    // 增量拉取
    suspend fun pullIncremental(since: String?): Result<SyncPullResult>

    // 推送离线操作
    suspend fun pushOfflineActions(actions: List<OfflineAction>): Result<SyncPushResult>

    // 全量同步 (pull + push)
    suspend fun syncAll(): Result<SyncResult>

    // 获取上次同步时间戳
    fun getLastSyncTimestamp(): Flow<String?>

    // 记录离线操作（待网络恢复后推送）
    suspend fun recordOfflineAction(action: OfflineAction)
}

// === Data Layer ===

// CachedQuestionDao.kt (新建)
@Dao
interface CachedQuestionDao {
    @Query("SELECT * FROM cached_questions ORDER BY cachedAt DESC LIMIT :limit")
    fun getAll(limit: Int = 200): Flow<List<CachedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(questions: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM cached_questions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_questions")
    fun count(): Flow<Int>

    @Query("SELECT SUM(LENGTH(content) + LENGTH(COALESCE(options, '')) + " +
           "LENGTH(COALESCE(explanation, ''))) FROM cached_questions")
    fun dataSizeBytes(): Flow<Long?>
}

// 类似的: CachedWrongAnswerDao, CachedConversationDao
// 新增: OfflineActionDao (离线操作日志持久化)

// CacheManager.kt (新建)
@Singleton
class CacheManager @Inject constructor(
    private val cachedQuestionDao: CachedQuestionDao,
    private val cachedWrongAnswerDao: CachedWrongAnswerDao,
    private val cachedConversationDao: CachedConversationDao,
    private val appContext: Context
) {
    suspend fun getCacheSizeInfo(): List<CacheCategoryInfo>
    suspend fun clearAllCache()
    suspend fun evictIfNeeded()
    private suspend fun sizeOfBytes(dao: Any): Long  // 用 SQLite LENGTH 函数计算
}

// NetworkMonitor.kt (已有，扩展)
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    val connectivityState: StateFlow<NetworkState>  // ★ 新增细化状态

    // ★ 新增:
    fun getOnlineSnapshot(): Boolean  // 快照，非 Flow，供 Repository 同步判断
    fun observe(): Flow<ConnectivityState>
}

enum class NetworkState { ONLINE, OFFLINE, WIFI, CELLULAR, VPN }
```

#### 2.4.2 后端关键类型

```python
# backend/app/schemas/sync.py (新建)
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class SyncAction(BaseModel):
    type: str  # wrong_answer_review / viewed_question / flashcard_review
    target_id: str
    data: dict = Field(default_factory=dict)
    timestamp: str

class SyncRequest(BaseModel):
    actions: list[SyncAction] = Field(default_factory=list)

class SyncResponse(BaseModel):
    updated_questions: list[dict] = Field(default_factory=list)
    updated_conversations: list[dict] = Field(default_factory=list)
    updated_wrong_answers: list[dict] = Field(default_factory=list)
    deleted_ids: dict[str, list[str]] = Field(default_factory=dict)
    synced_actions: int = 0
    conflicts: list[dict] = Field(default_factory=list)
    sync_timestamp: str  # ISO 8601
```

### 2.5 DB Schema 变更

#### 2.5.1 Room v1 → v2 Migration

```kotlin
// AiTutorDatabase.kt — 版本从 1 升至 2
@Database(
    entities = [
        ConversationEntity::class,       // v1 已有
        MessageEntity::class,            // v1 已有
        CachedQuestionEntity::class,     // ★ 新增 v2
        CachedWrongAnswerEntity::class,  // ★ 新增 v2
        CachedConversationEntity::class, // ★ 新增 v2 (或复用 MessageEntity)
        OfflineActionEntity::class,      // ★ 新增 v2
        FlashcardEntity::class,          // ★ 新增 v2 (见 5.4.1)
        FlashcardReviewLogEntity::class  // ★ 新增 v2 (见 5.4.1)
    ],
    version = 2
)
abstract class AiTutorDatabase : RoomDatabase() {
    // ... DAO 声明 ...
}

// ★ Migration v1 → v2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. CachedQuestionEntity
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_questions` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `quizId` TEXT,
                `subject` TEXT NOT NULL,
                `topic` TEXT,
                `questionType` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `options` TEXT,
                `correctAnswer` TEXT NOT NULL,
                `userAnswer` TEXT,
                `explanation` TEXT,
                `isCorrect` INTEGER,
                `masteryScore` REAL NOT NULL DEFAULT 0.0,
                `cachedAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)

        // 2. CachedWrongAnswerEntity
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_wrong_answers` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `subject` TEXT NOT NULL,
                `topic` TEXT,
                `questionContent` TEXT NOT NULL,
                `correctAnswer` TEXT NOT NULL,
                `userAnswer` TEXT NOT NULL,
                `explanation` TEXT,
                `masteryScore` REAL NOT NULL DEFAULT 0.0,
                `weakness` TEXT,
                `reviewCount` INTEGER NOT NULL DEFAULT 0,
                `lastReviewedAt` INTEGER,
                `cachedAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)

        // 3. CachedConversationEntity
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_conversations` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `title` TEXT NOT NULL,
                `messages` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `messageCount` INTEGER NOT NULL DEFAULT 0,
                `cachedAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)

        // 4. OfflineActionEntity (离线操作日志暂存)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `offline_actions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `type` TEXT NOT NULL,
                `targetId` TEXT NOT NULL,
                `payload` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)

        // 5. FlashcardEntity (见 5.4.1)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `flashcards` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `frontContent` TEXT NOT NULL,
                `frontType` TEXT NOT NULL,
                `frontOptions` TEXT,
                `backCorrectAnswer` TEXT NOT NULL,
                `backUserAnswer` TEXT NOT NULL,
                `backExplanation` TEXT,
                `backMasteryScore` REAL NOT NULL DEFAULT 0.0,
                `backSubject` TEXT NOT NULL,
                `backTopic` TEXT,
                `masteryScore` REAL NOT NULL DEFAULT 0.0,
                `reviewCount` INTEGER NOT NULL DEFAULT 0,
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `lastReviewedAt` INTEGER,
                `createdAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                `updatedAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_flashcards_source` ON `flashcards`(`sourceId`)")

        // 6. FlashcardReviewLogEntity (见 5.4.1)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `flashcard_review_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `cardId` TEXT NOT NULL,
                `judgment` TEXT NOT NULL,
                `masteryBefore` REAL NOT NULL,
                `masteryAfter` REAL NOT NULL,
                `timeSpentSeconds` INTEGER,
                `createdAt` INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                `synced` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (`cardId`) REFERENCES `flashcards`(`id`) ON DELETE CASCADE
            )
        """)

        // 7. SyncMetadata (单行键值表)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sync_metadata` (
                `key` TEXT PRIMARY KEY NOT NULL,
                `value` TEXT NOT NULL
            )
        """)
    }
}

// 在 buildDatabase() 中注册:
// .addMigrations(MIGRATION_1_2)
// 不要使用 .fallbackToDestructiveMigration()
```

#### 2.5.2 实体定义

```kotlin
// CachedQuestionEntity.kt
@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String? = null,
    val subject: String,
    val topic: String? = null,
    val questionType: String,
    val content: String,
    val options: String? = null,       // JSON 字符串
    val correctAnswer: String,
    val userAnswer: String? = null,
    val explanation: String? = null,
    val isCorrect: Boolean? = null,
    val masteryScore: Float = 0.0f,
    val cachedAt: Long = System.currentTimeMillis()
)

// CachedWrongAnswerEntity.kt
@Entity(tableName = "cached_wrong_answers")
data class CachedWrongAnswerEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val topic: String? = null,
    val questionContent: String,
    val correctAnswer: String,
    val userAnswer: String,
    val explanation: String? = null,
    val masteryScore: Float = 0.0f,
    val weakness: String? = null,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

// CachedConversationEntity.kt
@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val messages: String,             // 完整对话 JSON 数组
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)

// OfflineActionEntity.kt
@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,                 // wrong_answer_review / flashcard_review
    val targetId: String,
    val payload: String,              // JSON
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.6 API 设计

| 方法 | 路径 | 说明 | 工作人日 |
|:----:|:-----|:-----|:--------:|
| GET | `/api/v1/sync?since={timestamp}` | 增量拉取 | 0.5 |
| POST | `/api/v1/sync` | 推送离线操作 | 0.5 |

**GET /api/v1/sync?since=2026-05-25T10:00:00Z**

- 功能: 返回指定时间戳之后变更的数据
- 后端逻辑: 查询 quiz_questions, conversations, wrong_answers 表中 updated_at > since 的记录
- 安全: JWT 认证，仅返回当前用户的数据
- 响应: SyncResponse (含 updated_* 列表和 deleted_ids 映射)

**POST /api/v1/sync**

- 功能: 提交用户离线期间产生的操作日志
- 后端逻辑: 逐条处理 actions，更新对应表的 mastery_score / review_count 等字段
- 幂等: (type, target_id, timestamp) 组合去重，一分钟内相同操作忽略
- 冲突: 若服务端数据更新较晚，以服务端为准，冲突详情记录在 conflicts 列表

### 2.7 关键决策及理由

| 决策 | 选择 | 备选方案 | 理由 |
|:-----|:----|:---------|:-----|
| 缓存策略 | Cache-first → Network-fallback | Network-first | 离线场景优先保证可用性，联网时后台静默刷新 |
| DB 迁移 | `addMigrations(v1, v2)` | `fallbackToDestructiveMigration()` | 保护用户已有数据不丢失，生产环境必须 |
| 缓存淘汰 | LRU (时间戳排序) | 固定大小窗口 | 实现简单，Room 原生支持，无额外依赖 |
| 同步协议 | 带 `since` 时间戳的增量拉取 | 全量拉取 | 减少带宽消耗，适合移动网络场景 |
| 离线操作 | 存本地 Room，同步后删除 | 直接丢弃 | 不漏记录，用户离线评价不丢失 |

### 2.8 技术风险与备选方案

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| Migration 执行失败导致 App crash | 低 | 高 | 在 `prepMigration` 中 try-catch，失败时发送日志并提示用户重新安装 |
| 大量离线操作积压导致同步耗时过长 | 中 | 中 | 分批同步（每次最多 50 条），显示同步进度条 |
| 同步冲突导致数据不一致 | 中 | 中 | 服务端始终为权威源，冲突记录日志供审计 |

### 2.9 文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/local/entity/CachedQuestionEntity.kt` | 新建 | 缓存题目实体 |
| `data/local/entity/CachedWrongAnswerEntity.kt` | 新建 | 缓存错题实体 |
| `data/local/entity/CachedConversationEntity.kt` | 新建 | 缓存对话实体 |
| `data/local/entity/OfflineActionEntity.kt` | 新建 | 离线操作日志实体 |
| `data/local/dao/CachedQuestionDao.kt` | 新建 | 题目缓存 DAO |
| `data/local/dao/CachedWrongAnswerDao.kt` | 新建 | 错题缓存 DAO |
| `data/local/dao/CachedConversationDao.kt` | 新建 | 对话缓存 DAO |
| `data/local/dao/OfflineActionDao.kt` | 新建 | 离线操作 DAO |
| `data/local/db/AiTutorDatabase.kt` | 修改 | v1→v2 Migration, 添加实体/DAO |
| `data/local/db/Migration1To2.kt` | 新建 | Migration 1→2 实现 |
| `data/local/CacheManager.kt` | 新建 | 缓存大小统计 + LRU 淘汰 |
| `di/DatabaseModule.kt` | 修改 | 提供新 DAO 实例 |
| `data/repository/OfflineRepositoryImpl.kt` | 新建 | Cache-first 仓库实现 |
| `domain/repository/OfflineRepository.kt` | 新建 | 离线仓库接口 |
| `data/repository/SyncRepositoryImpl.kt` | 新建 | 同步仓库实现 |
| `domain/repository/SyncRepository.kt` | 新建 | 同步仓库接口 |
| `data/remote/api/SyncApi.kt` | 新建 | 同步 API 接口 (Retrofit) |
| `data/remote/dto/SyncDtos.kt` | 新建 | 同步请求/响应 DTO |
| `util/NetworkMonitor.kt` | 修改 | 扩展 isOnline 快照方法 |
| `ui/offline/OfflineBanner.kt` | 新建 | 离线状态指示器 Composable |
| `ui/offline/OfflineViewModel.kt` | 新建 | 离线状态 VM |
| `ui/settings/CacheManagementSection.kt` | 新建 | 缓存管理设置界面 |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加缓存管理区域 |
| `ui/settings/SettingsViewModel.kt` | 修改 | 缓存状态管理 |
| `domain/usecase/sync/SyncDataUseCase.kt` | 新建 | 同步数据用例 |
| `domain/usecase/cache/GetCachedQuestionsUseCase.kt` | 新建 | 获取缓存题目用例 |
| `domain/usecase/cache/GetCachedWrongAnswersUseCase.kt` | 新建 | 获取缓存错题用例 |
| `domain/usecase/cache/GetCachedConversationsUseCase.kt` | 新建 | 获取缓存对话用例 |
| `domain/usecase/cache/ClearCacheUseCase.kt` | 新建 | 清除缓存用例 |
| `domain/usecase/cache/GetCacheSizeUseCase.kt` | 新建 | 获取缓存大小用例 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/sync.py` | 新建 | `GET/POST /api/v1/sync` 路由 |
| `app/schemas/sync.py` | 新建 | Sync 请求/响应 Schema |
| `app/services/sync_service.py` | 新建 | 增量查询 + 冲突检测 + 操作回放 |
| `app/main.py` | 修改 | 注册 sync router |

---

## 3. P1-2: 学习报告导出架构设计

### 3.1 职责

提供学习报告导出功能：后端使用 ReportLab/Matplotlib 生成 PDF（含图表）和 CSV，Android 端触发下载并通过 ShareSheet 分享。

### 3.2 架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        报告导出系统架构                                        │
│                                                                             │
│  Android App                                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ ExportReportScreen (Compose)                                        │    │
│  │  ├── 范围选择: 今日 / 本周 / 本月 / 全部 / 自定义                    │    │
│  │  ├── 格式选择: PDF / CSV                                            │    │
│  │  ├── 导出按钮 → 触发下载                                            │    │
│  │  └── 进度显示: 生成中 → 下载中 → 分享 → 完成/失败                   │    │
│  └──────────────────────┬──────────────────────────────────────────────┘    │
│                         │                                                   │
│  ExportReportViewModel  │                                                   │
│  ├── exportState: StateFlow<ExportState>                                   │
│  │     IDLE → GENERATING → DOWNLOADING → SHARING → DONE / ERROR           │
│  └──────────────────────┬──────────────────────────────────────────────┘    │
│                         │                                                   │
│  ReportRepositoryImpl   │                                                   │
│  ├── exportReport(request)                                                 │
│  │   └── ReportApi.exportPdf(request) (POST /api/v1/report/export)         │
│  │       └── 如果 format=pdf:                                               │
│  │           ├── Retrofit @Streaming                                         │
│  │           ├── 写入 Downloads/StudyReports/report_xxx.pdf                │
│  │           └── 如果 format=csv: 同上, 后缀 .csv                          │
│  ├── shareFile(file)                                                       │
│  │   └── FileProvider.getUriForFile()                                      │
│  │       └── Intent.createChooser(ShareSheet)                              │
│  └── 缓存检查: 同参数不重复生成, 返回已存在的文件 URI                       │
│                         │                                                   │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                    │                                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
                                     ▼
POST /api/v1/report/export
Body: { format, period, subject, include_charts, start_date, end_date }
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Backend                                                                    │
│                                                                             │
│  routers/report.py                                                          │
│  ├── 1. 校验参数 + 配额检查 (免费3次/日, premium20次/日)                    │
│  ├── 2. 调用 report_service.generate_report(...)                           │
│  └── 3. 返回文件流 (application/pdf 或 text/csv)                           │
│                                                                             │
│  services/report_service.py                                                 │
│  ├── generate_report(request, user_id):                                    │
│  │   ├── 1. 查询 analytics stats 聚合数据 (复用路由 GET /stats 逻辑)       │
│  │   │      ├── daily_stats: 每日学习时长, 正确率                           │
│  │   │      ├── subject_breakdown: 各学科统计                               │
│  │   │      ├── weak_areas: 薄弱知识点列表                                 │
│  │   │      └── raw_records: 原始做题记录 (CSV 用)                          │
│  │   │                                                                      │
│  │   ├── 2. 如果 format=pdf:                                                │
│  │   │   ├── ReportLab 构建 PDF 文档                                        │
│  │   │   ├── Matplotlib 生成图表 (每日时长折线图, 正确率曲线, 雷达图)       │
│  │   │   │   ├── 每日学习时长折线图: x=日期, y=分钟                        │
│  │   │   │   ├── 正确率曲线: x=日期, y=正确率百分比                        │
│  │   │   │   └── 知识点雷达图: 按学科分类掌握度 (matplotlib雷达图)          │
│  │   │   ├── 中文字体: 在 ReportLab 中注册 Noto Sans CJK 或思源字体        │
│  │   │   ├── 7 章节: 概要→时长→正确率→雷达图→错题分布→薄弱点→建议        │
│  │   │   ├── 压缩: 如果 >10MB, 图表降至 72dpi                              │
│  │   │   └── 超时: 30 秒, 超时返回 504                                      │
│  │   │                                                                      │
│  │   └── 3. 如果 format=csv:                                                │
│  │       ├── CSV 以 UTF-8 BOM 编码 (兼容 Excel)                             │
│  │       ├── 列: date, subject, topic, question_content,                    │
│  │       │     correct_answer, user_answer, is_correct,                     │
│  │       │     time_spent_seconds, mastery_score                            │
│  │       └── 最大 365 天数据                                                │
│  │                                                                          │
│  └── 4. 配额扣减 + 返回文件流                                               │
│                                                                             │
│  ？？？: 文件缓存                                                            │
│  后端可选方案: 对相同参数组合(用户+时间+格式)在 5 分钟内返回同一文件          │
│  实现: 用 Redis 缓存文件路径 (file_hash → path), 5分钟TTL                   │
│  极端: 大文件不缓存, 每次重新生成以确保数据最新                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 数据流

```
用户选择范围=本周, 格式=PDF → 点击"导出"
    │
    ▼
ExportReportViewModel → ReportRepositoryImpl.exportReport(request)
    │
    ├── Step 1: 缓存检查
    │   └── 检查本地 Downloads/StudyReports/ 目录下是否有
    │       同参数文件 (user_id + period + format)
    │       有 → 直接返回文件 URI → 跳到 Step 5
    │       无 → 继续
    │
    ├── Step 2: POST /api/v1/report/export
    │   ├── 后端: 配额检查 → 查询 stats → ReportLab生成PDF
    │   └── 响应: 二进制流 (Retrofit @Streaming)
    │
    ├── Step 3: 写入 Downloads 目录
    │   ├── 目录: Environment.getExternalStoragePublicDirectory(
    │   │         Environment.DIRECTORY_DOWNLOADS) + "/StudyReports/"
    │   ├── 文件名: study_report_{yyyyMMdd}_{yyyyMMdd}.{pdf|csv}
    │   └── 使用 MediaStore API (Android 10+) 或直接文件写入
    │
    ├── Step 4: 更新 ExportState → DONE
    │
    └── Step 5: 打开 ShareSheet
        ├── FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)
        ├── Intent.ACTION_SEND
        │   └── .setType(getMimeType(format))
        │   └── .putExtra(Intent.EXTRA_STREAM, uri)
        └── Intent.createChooser(intent, "分享学习报告")
```

### 3.4 类/接口设计

#### 3.4.1 Android 端关键接口

```kotlin
// ReportRepository.kt (新建)
interface ReportRepository {
    // 导出报告：下载文件并返回本地 File URI
    suspend fun exportReport(request: ExportRequest): Result<ExportResult>

    // 分享文件：通过 ShareSheet 分享
    fun shareFile(context: Context, file: File)

    // 检查上次导出文件是否存在
    fun getCachedReportFile(request: ExportRequest): File?
}

// ExportState (VM 状态)
sealed class ExportState {
    object Idle : ExportState()
    data class Generating(val progress: Float) : ExportState()  // 0.0 ~ 1.0
    data class Done(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

// ExportRequest.kt
data class ExportRequest(
    val format: ExportFormat,      // PDF / CSV
    val period: ExportPeriod,      // TODAY / WEEKLY / MONTHLY / ALL
    val subject: String = "all",
    val includeCharts: Boolean = true,
    val startDate: String? = null, // ISO 8601
    val endDate: String? = null    // ISO 8601
)
enum class ExportFormat { PDF, CSV }
enum class ExportPeriod { TODAY, WEEKLY, MONTHLY, ALL, CUSTOM }
```

#### 3.4.2 后端关键类型

```python
# backend/app/schemas/report.py (新建)
from pydantic import BaseModel, Field
from typing import Optional

class ExportRequest(BaseModel):
    format: str = Field(..., pattern="^(pdf|csv)$")
    period: str = Field(..., pattern="^(today|weekly|monthly|all|custom)$")
    subject: str = "all"
    include_charts: bool = True
    start_date: Optional[str] = None  # ISO 8601, custom period 必填
    end_date: Optional[str] = None    # ISO 8601, custom period 必填

# 响应是文件流 (application/pdf / text/csv)，不走标准 JSON ApiResponse
```

### 3.5 API 设计

| 方法 | 路径 | 说明 | 工作人日 |
|:----:|:-----|:-----|:--------:|
| POST | `/api/v1/report/export` | 生成学习报告 | 1.5 |

**POST /api/v1/report/export**

- 功能: 接收导出参数，生成并返回文件
- 配额: 免费用户每日 3 次，premium 用户 20 次；存储在 Redis 或 DB 中计数器
- 性能: 耗时操作（PDF 生成含图表），设置 30s 超时
- 响应: 直接返回二进制流（不包装 ApiResponse），指定 Content-Type 和 Content-Disposition
- 错误码: 429 配额不足, 504 生成超时, 413 数据量过大

### 3.6 关键决策及理由

| 决策 | 选择 | 备选方案 | 理由 |
|:-----|:----|:---------|:-----|
| PDF 生成位置 | 后端 (ReportLab) | Android 端 (iText/Android Canvas) | 后端库成熟稳定，Android 端 PDF 库兼容性差 |
| 图表生成 | Matplotlib → 图片 → 嵌入 PDF | 纯 ReportLab 绘图 | ReportLab 的绘图能力有限，复杂图表用 Matplotlib |
| 文件传输 | 同步请求直接返回流 | 异步 (生成任务 → 轮询) | 报告生成通常 < 10s，同步简单可靠 |
| 缓存策略 | Android 本地缓存 + 后端可选缓存 | 每次都重新生成 | 避免同一报告反复下载消耗流量 |
| 中文字体 | 后端预装 Noto Sans CJK | 系统字体 | 确保 PDF 中文显示正确，依赖可控 |

### 3.7 技术风险与备选方案

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| ReportLab + Matplotlib 中文字体缺失 | 中 | 高 | Docker 镜像预装 Noto Sans CJK，配置字体路径 |
| PDF 生成高并发下内存占用 | 中 | 中 | 限制并发数 (最大 4 个生成任务)，使用 asyncio 协程 |
| 图表生成耗时过长 | 低 | 中 | 图表缓存（相同数据不重复渲染），降低 dpi |
| Android 10+ 文件存储权限 | 低 | 高 | 使用 MediaStore API + FileProvider 统一管理 |

### 3.8 文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/remote/api/ReportApi.kt` | 新建 | 导出 API 定义 (Retrofit @Streaming) |
| `data/remote/dto/ReportDtos.kt` | 新建 | 导出请求 DTO |
| `data/repository/ReportRepositoryImpl.kt` | 新建 | 下载 + 文件保存 + ShareSheet |
| `domain/repository/ReportRepository.kt` | 新建 | 报告仓库接口 |
| `ui/report/ExportReportScreen.kt` | 新建 | 导出设置页面 |
| `ui/report/ExportReportViewModel.kt` | 新建 | 导出状态管理 |
| `di/ReportModule.kt` | 新建 | 报告相关 Hilt 依赖注入 |
| `ui/navigation/Routes.kt` | 修改 | 添加报告导出路由 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/report.py` | 新建 | POST /api/v1/report/export 路由 |
| `app/schemas/report.py` | 新建 | 导出请求 Schema |
| `app/services/report_service.py` | 新建 | PDF 生成 (ReportLab) + CSV 生成 |
| `app/services/report_charts.py` | 新建 | Matplotlib 图表生成 (折线图/曲线图/雷达图) |
| `app/main.py` | 修改 | 注册 report router |
| `requirements.txt` | 修改 | 添加 `reportlab`, `matplotlib` |

---

## 4. P2-1: 后端 TTS 替换架构设计

### 4.1 职责

将 TTS 引擎从纯 OpenAI 替换为支持多 Provider 的架构，默认使用 Edge TTS（免费、中文音质优），提供三级降级兜底。

### 4.2 架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TTS Provider 架构                                    │
│                                                                             │
│  Android App                                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ CloudTtsEngine (新建)                                                 │    │
│  │  ├── synthesize(text, provider): Flow<ByteArray>                    │    │
│  │  └── 降级链:                                                         │    │
│  │       ├── 1. Cloud: GET /api/v1/tts?provider=edge                   │    │
│  │       │   ├── 成功 → 返回音频, 播放                                  │    │
│  │       │   ├── 503 → 降级到 OpenAI                                    │    │
│  │       │   │   └── POST /v1/audio/speech (原有)                      │    │
│  │       │   │       ├── 成功 → 返回音频                                │    │
│  │       │   │       └── 503 → 降级到本地 TTS                          │    │
│  │       │   └── 其余错误 → 直接降级到本地                              │    │
│  │       │                                                              │    │
│  │       └── 2. Local: LocalTtsEngine (新建)                            │    │
│  │           └── android.speech.tts.TextToSpeech.speak()               │    │
│  │                                                                      │    │
│  │ VoiceRepositoryImpl (改造)                                            │    │
│  │  ├── speak(text, speed)                                              │    │
│  │  │   └── 根据 TtsMode (LOCAL/CLOUD) + provider 设置调用引擎          │    │
│  │  ├── setTtsProvider(provider: String)                                │    │
│  │  └── getTtsProvider(): String                                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                        │
│                    ┌───────────────┴──────────────┐                        │
│                    ▼                               ▼                        │
│            GET /api/v1/tts              POST /v1/audio/speech              │
│            ?provider=edge              (原有, 兼容)                        │
│            &voice=zh-CN-Xiaoxiao                                          │
│                                                                             │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Backend                                                                    │
│                                                                             │
│  routers/audio.py (改造)                                                    │
│  ├── GET /api/v1/tts (★★★ 新增)                                            │
│  │   ├── 参数: text, provider, voice, speed                                │
│  │   ├── 验证: text 长度 ≤1024, speed 0.5-2.0                             │
│  │   └── 调用 TTSProviderFactory.get_provider(provider).synthesize(...)    │
│  │                                                                          │
│  └── POST /v1/audio/speech (原有保留)                                      │
│                                                                             │
│  services/tts_base.py (新建) — Abstract Provider 层                         │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │ abstract class TTSProvider:                                  │           │
│  │   ├── async def synthesize(text, voice, speed) → bytes      │           │
│  │   ├── def get_supported_voices() → list[str]                │           │
│  │   └── def name() → str                                      │           │
│  ├─────────────────────────────────────────────────────────────┤           │
│  │ class EdgeTTSProvider(TTSProvider):                          │           │
│  │   ├── synthesize: 调用微软 Edge TTS 非官方 API               │           │
│  │   ├── voices: zh-CN-XiaoxiaoNeural, en-US-JennyNeural ...   │           │
│  │   └── fallback: 出错时抛出 TTSException                      │           │
│  │                                                              │           │
│  │ class OpenAITTSProvider(TTSProvider):                        │           │
│  │   ├── synthesize: 调用 OpenAI /v1/audio/speech API           │           │
│  │   └── (已有逻辑，适配 TTSProvider 接口)                      │           │
│  │                                                              │           │
│  │ class TTSProviderFactory:                                    │           │
│  │   ├── get_provider(name: str) → TTSProvider                 │           │
│  │   └── get_default_provider() → TTSProvider                  │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                                                                             │
│  降级策略 (Android 端实现):                                                  │
│  ┌────────────────────────────────────────────────────────────────┐        │
│  │ CloudTtsEngine.synthesize(text, provider)                       │        │
│  │   ├── Try Edge → 成功 → 返回                                     │        │
│  │   ├── Try Edge → Fail → Try OpenAI                              │        │
│  │   │   ├── 成功 → 返回 + 日志 "TTS降级: Edge→OpenAI"             │        │
│  │   │   └── Fail → speakLocal() + Toast "语音服务降级"            │        │
│  └────────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 数据流

```
用户听到 AI 回答 → VoiceRepository.speak(text)
    │
    ├── getTtsProvider() → "edge" (或用户设置的 provider)
    │
    ├── getTtsMode() → CLOUD
    │
    ├── CloudTtsEngine.synthesize(text, "edge")
    │   │
    │   ├── GET /api/v1/tts?text=xxx&provider=edge&voice=zh-CN-Xiaoxiao
    │   │   │
    │   │   ├── Backend: TTSProviderFactory.get_provider("edge")
    │   │   │   └── EdgeTTSProvider.synthesize(text, voice, speed)
    │   │   │       ├── 1. 获取 Edge TTS 授权 Token (HTTP POST)
    │   │   │       ├── 2. 构造 SSML 请求
    │   │   │       ├── 3. 解析 audio/mpeg 流
    │   │   │       └── 4. 返回 bytes
    │   │   │
    │   │   └── 响应: audio/mpeg + header X-TTS-Provider: edge
    │   │
    │   ├── 成功 → TtsAudioPlayer 播放
    │   │
    │   └── 失败 (HTTP 503) →
    │       ├── 降级 1: GET /api/v1/tts?text=xxx&provider=openai
    │       │   ├── 成功 → 播放 + 日志
    │       │   └── 失败 → 降级 2
    │       └── 降级 2: LocalTtsEngine.speak(text)
    │           └── android.speech.tts.TextToSpeech.speak()
    │
    └── UI: Toast "语音服务降级" (仅当实际触发降级时)
```

### 4.4 类/接口设计

#### 4.4.1 Android 端关键接口

```kotlin
// CloudTtsEngine.kt (新建)
@Singleton
class CloudTtsEngine @Inject constructor(
    private val aiTutorApi: AiTutorApi,
    private val localTtsEngine: LocalTtsEngine
) {
    // 核心方法: 三级降级合成
    suspend fun synthesize(
        text: String,
        preferredProvider: String = "edge",
        voice: String = "zh-CN-XiaoxiaoNeural",
        speed: Float = 1.0f
    ): Result<ByteArray> {
        // 1. 尝试 preferredProvider
        val firstResult = tryProvider(preferredProvider, text, voice, speed)
        if (firstResult.isSuccess) return firstResult

        // 2. 降级到其他云端 provider
        val fallbackProvider = if (preferredProvider == "edge") "openai" else "edge"
        val secondResult = tryProvider(fallbackProvider, text, voice, speed)
        if (secondResult.isSuccess) {
            Log.w("TTS", "降级: $preferredProvider → $fallbackProvider")
            return secondResult
        }

        // 3. 降级到本地 TTS
        Log.w("TTS", "降级到本地 TTS")
        return Result.failure(TtsException("所有云端 TTS 均不可用"))
    }

    private suspend fun tryProvider(
        provider: String, text: String, voice: String, speed: Float
    ): Result<ByteArray> {
        return try {
            val response = aiTutorApi.tts(text, provider, voice, speed)
            if (response.isSuccessful) {
                Result.success(response.body()?.bytes() ?: ByteArray(0))
            } else {
                Result.failure(TtsException("TTS $provider 返回 ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// LocalTtsEngine.kt (新建)
@Singleton
class LocalTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    fun speak(text: String, speed: Float = 1.0f) {
        if (tts == null) {
            tts = TextToSpeech(context) { /* init callback */ }
        }
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

// VoiceRepository.kt — 扩展接口
interface VoiceRepository {
    // ... 现有方法 ...

    // ★ Sprint 2 新增:
    fun setTtsProvider(provider: String)
    fun getTtsProvider(): Flow<String>  // "edge" / "openai" / "local"
}
```

#### 4.4.2 后端关键类型

```python
# backend/app/services/tts_base.py (新建)
from abc import ABC, abstractmethod

class TTSProvider(ABC):
    @abstractmethod
    async def synthesize(self, text: str, voice: str, speed: float) -> bytes:
        """合成语音, 返回 MP3 字节数据"""
        pass

    @abstractmethod
    def get_supported_voices(self) -> list[dict]:
        """返回支持的发音人列表"""
        pass

    @abstractmethod
    def name(self) -> str:
        """Provider 标识"""
        pass


# backend/app/services/tts_edge.py (新建)
class EdgeTTSProvider(TTSProvider):
    """微软 Edge TTS 适配器"""

    EDGE_TTS_TOKEN_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

    async def synthesize(self, text: str, voice: str, speed: float) -> bytes:
        # 1. 建立 WebSocket 连接
        # 2. 发送 SSML 合成请求
        # 3. 接收音频流并拼接
        # 4. 返回完整 MP3 字节
        ...

    def get_supported_voices(self) -> list[dict]:
        return [
            {"id": "zh-CN-XiaoxiaoNeural", "locale": "zh-CN", "gender": "Female"},
            {"id": "zh-CN-YunxiNeural", "locale": "zh-CN", "gender": "Male"},
            {"id": "en-US-JennyNeural", "locale": "en-US", "gender": "Female"},
        ]


# backend/app/services/tts_factory.py (新建)
class TTSProviderFactory:
    _providers: dict[str, TTSProvider] = {}

    @classmethod
    def register(cls, name: str, provider: TTSProvider):
        cls._providers[name] = provider

    @classmethod
    def get_provider(cls, name: str) -> TTSProvider:
        provider = cls._providers.get(name)
        if not provider:
            raise ValueError(f"Unknown TTS provider: {name}")
        return provider

    @classmethod
    def get_default(cls) -> TTSProvider:
        default_name = settings.tts_default_provider  # from config
        return cls.get_provider(default_name)
```

### 4.5 API 设计

| 方法 | 路径 | 说明 | 工作人日 |
|:----:|:-----|:-----|:--------:|
| GET | `/api/v1/tts` | 文本转语音（新增） | 1 |

**GET /api/v1/tts**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| text | string | 是 | — | 要合成的文本，URL 编码，最大 1024 字符 |
| provider | string | 否 | 环境变量 `tts_default_provider` | `edge` / `openai` |
| voice | string | 否 | Provider 默认值 | 发音人标识 |
| speed | float | 否 | 1.0 | 语速 (0.5-2.0) |

**Response**: 二进制音频流
```
Content-Type: audio/mpeg
X-TTS-Provider: edge
```

**SSE 流式兼容**: 保持与现有 POST /v1/audio/speech 相同的音频二进制返回格式，Android 端 CloudTtsEngine 可同时支持两种端点。

### 4.6 关键决策及理由

| 决策 | 选择 | 备选方案 | 理由 |
|:-----|:----|:---------|:-----|
| Provider 架构 | 抽象基类 + 工厂 | if-else 硬编码 | 便于未来添加 Fish Speech / Azure TTS |
| 降级位置 | Android 端实现三级降级 | 后端降级 | Android 端可降级到本地 TTS，后端做不到；用户无感切换 |
| Edge TTS 实现 | 通过 WebSocket 连接微软服务 | HTTPS API 调用 | Edge TTS 使用 WebSocket 协议，需用 `websockets` 库 |
| 默认 provider | `edge` | `openai` | Edge TTS 免费且中文音质更优，降低运营成本 |
| 接口方法 | GET (查询式) | POST | TTS 是幂等操作，GET 符合 REST 语义，方便浏览器测试 |

### 4.7 技术风险与备选方案

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| Edge TTS 非官方 API 被封禁或变更 | 中 | 高 | 抽象 Provider 层，快速切换到 OpenAI / Fish Speech |
| WebSocket 连接不稳定 | 中 | 中 | 添加重试机制（3 次指数退避），超时 10s |
| 微软 API 调用频率限制 | 中 | 中 | 后端增加本地 TTS 缓存（相同 text 缓存 1h） |
| 长文本分段合成音质下降 | 低 | 中 | 按句号/问号分割，每段保持语义完整性 |

### 4.8 文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/media/CloudTtsEngine.kt` | 新建 | 云端 TTS 引擎（支持 provider 切换 + 三级降级） |
| `data/media/LocalTtsEngine.kt` | 新建 | 本地 TTS 封装（TextToSpeech） |
| `data/remote/api/AiTutorApi.kt` | 修改 | 添加 `GET /api/v1/tts` 接口 |
| `domain/repository/VoiceRepository.kt` | 修改 | 添加 `setTtsProvider()` / `getTtsProvider()` |
| `data/repository/VoiceRepositoryImpl.kt` | 修改 | 实现 TTS provider 切换 + 降级链路 |
| `di/SpeechModule.kt` | 修改 | 提供 CloudTtsEngine / LocalTtsEngine |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加 TTS Provider 选择器 |
| `ui/settings/SettingsViewModel.kt` | 修改 | TTS provider 状态管理 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/audio.py` | 修改 | 新增 `GET /api/v1/tts` 端点 |
| `app/services/tts_base.py` | 新建 | TTSProvider 抽象基类 |
| `app/services/tts_edge.py` | 新建 | Edge TTS 适配器 (WebSocket) |
| `app/services/tts_factory.py` | 新建 | TTSProvider 工厂 + 注册 |
| `app/services/tts_openai.py` | 新建 | OpenAI TTS 适配器（从现有代码提取） |
| `app/config.py` | 修改 | 添加 `tts_default_provider` 等配置 |
| `app/schemas/audio.py` | 修改 | 新增 TtsQueryParams Schema |
| `requirements.txt` | 修改 | 添加 `websockets`（Edge TTS 需要） |

---

## 5. P2-2: Flashcard 抽认卡架构设计

### 5.1 职责

基于错题和 Quiz 数据自动生成复习卡片（Flashcard），用户通过左右划动评价掌握程度，系统据此更新 mastery_score，已掌握（≥0.8）自动归档。

### 5.2 架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Flashcard 抽认卡系统架构                                │
│                                                                             │
│  Android App                                                                │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  FlashcardScreen (Compose)                                           │    │
│  │  ┌────────────────────────────────────────┐                          │    │
│  │  │  顶部: 进度指示 "今日进度 3/10"         │                          │    │
│  │  │                                        │                          │    │
│  │  │  中间: SwipeToDismiss 卡片栈             │                          │    │
│  │  │  ┌──────────────────────────────────┐  │                          │    │
│  │  │  │  FlashcardFront (正面: 题目)      │  │                          │    │
│  │  │  │  - 题干（选择题含选项）             │  │                          │    │
│  │  │  │  - 掌握度进度条 "掌握度 30%"       │  │                          │    │
│  │  │  │  - 点击翻转 → FlashcardBack       │  │                          │    │
│  │  │  └──────────────────────────────────┘  │                          │    │
│  │  │  ┌──────────────────────────────────┐  │                          │    │
│  │  │  │  FlashcardBack (反面: 答案+解析)  │  │                          │    │
│  │  │  │  - 正确答案 (绿色高亮)             │  │                          │    │
│  │  │  │  - 用户当时答案 (红色标注)         │  │                          │    │
│  │  │  │  - 详细解析                       │  │                          │    │
│  │  │  │  - 知识点标签                     │  │                          │    │
│  │  │  └──────────────────────────────────┘  │                          │    │
│  │  │                                        │                          │    │
│  │  │  左划 ← "不熟练" │ 右划 → "已掌握"     │                          │    │
│  │  │  (SwipeToDismiss + 手势检测器)         │                          │    │
│  │  └────────────────────────────────────────┘                          │    │
│  │                                                                      │    │
│  │  底部: "今日复习完成! 🎉" 或 "明天再来"                                  │    │
│  └──────────────────────┬───────────────────────────────────────────────┘    │
│                         │                                                   │
│  FlashcardViewModel                                                         │
│  ├── cards: StateFlow<List<Flashcard>>                                      │
│  ├── todayProgress: StateFlow<Pair<Int, Int>> (已完成/总数)                 │
│  ├── currentCardIndex: StateFlow<Int>                                       │
│  ├── reviewCard(cardId, judgment)                                           │
│  │   └── 本地更新 mastery + 写入 review log + 调后端 API + 加积分          │
│  └── loadTodayCards()                                                       │
│       ├── GET /api/v1/flashcard/today (联网)                                │
│       └── Room flashcards 表缓存 (离线)                                     │
│                         │                                                   │
│  Domain Layer          ▼                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ UseCases:                                                           │    │
│  │  - GetTodayCardsUseCase: 获取今日卡片 (本地优先 → 网络补充)           │    │
│  │  - ReviewCardUseCase: 评价卡片 (更新 mastery + 记录日志 + 同步后端)  │    │
│  │  - SyncFlashcardsUseCase: 批量同步离线评价到后端                     │    │
│  │  - ArchiveMasteredCardsUseCase: 归档已掌握卡片                       │    │
│  │                                                                     │    │
│  │ Flashcard (Domain Model):                                           │    │
│  │  data class Flashcard(                                              │    │
│  │      id: String, sourceType: String, sourceId: String,              │    │
│  │      front: FlashcardFront, back: FlashcardBack,                    │    │
│  │      masteryScore: Float, reviewCount: Int,                         │    │
│  │      isArchived: Boolean, lastReviewedAt: Instant?                  │    │
│  │  )                                                                  │    │
│  └──────────────────────────┬──────────────────────────────────────────┘    │
│                             │                                               │
│  Data Layer                ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ FlashcardRepositoryImpl                                              │    │
│  │  ├── getTodayCards():                                                │    │
│  │  │   联网 → FlashcardApi.getTodayCards() → 缓存到 Room              │    │
│  │  │   离线 → Room flashcards 表 (已缓存) + review_logs 表            │    │
│  │  ├── reviewCard(cardId, judgment):                                   │    │
│  │  │   ├── 本地更新 FlashcardEntity.masteryScore                      │    │
│  │  │   ├── 插入 FlashcardReviewLogEntity (含 synced=false)            │    │
│  │  │   ├── POST /api/v1/flashcard/review (联网时)                     │    │
│  │  │   └── 调用 GamificationApi.syncScore(+2积分)                     │    │
│  │  └── syncOfflineReviews():                                           │    │
│  │       └── POST /api/v1/flashcard/sync                                │    │
│  │                                                                     │    │
│  │ FlashcardDao                                                         │    │
│  │  ├── getTodayCards(limit: Int): List<FlashcardEntity>               │    │
│  │  │   WHERE isArchived=0 ORDER BY lastReviewedAt ASC LIMIT :limit   │    │
│  │  ├── updateMastery(cardId, newScore, ...)                           │    │
│  │  ├── getUnsyncedReviewLogs(): List<FlashcardReviewLogEntity>        │    │
│  │  └── deleteArchivedCards()                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                             │                                               │
└─────────────────────────────┼───────────────────────────────────────────────┘
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
     GET /flashcard/today    POST /flashcard/review
     POST /flashcard/sync              │
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Backend                                                                    │
│                                                                             │
│  routers/flashcard.py                                                       │
│  ├── GET /api/v1/flashcard/today → today 的待复习卡片列表                   │
│  │   算法: 从 wrong_answers 表中查询 mastery_score < 0.8                   │
│  │         按 last_reviewed_at ASC 排序, 取 limit 张                        │
│  │         排除已归档卡片 (is_archived=true)                                │
│  │                                                                          │
│  ├── POST /api/v1/flashcard/review → 提交单卡评价                          │
│  │   算法:                                                                  │
│  │   1. 读取当前 mastery_score                                             │
│  │   2. 计算新 mastery_score:                                              │
│  │      mastered → min(1.0, old + (1.0 - old) * 0.3)                      │
│  │      unfamiliar → max(0.0, old - 0.2)                                  │
│  │   3. 如果 ≥ 0.8 → is_archived=true                                     │
│  │   4. 更新 wrong_answers.mastery_score + review_count + last_reviewed_at│
│  │   5. 发放积分 (调用 game/sync/score 或直接加)                          │
│  │                                                                          │
│  └── POST /api/v1/flashcard/sync → 批量同步离线评价                        │
│       (幂等: 相同 card_id + timestamp 不重复处理)                          │
│                                                                             │
│  services/flashcard_service.py                                              │
│  ├── get_today_cards(user_id, limit):                                      │
│  │   SELECT * FROM wrong_answers                                           │
│  │   WHERE user_id = :uid                                                  │
│  │     AND mastery_score < 0.8                                             │
│  │     AND (is_archived IS NULL OR is_archived = false)                    │
│  │   ORDER BY last_reviewed_at ASC NULLS FIRST                             │
│  │   LIMIT :limit                                                          │
│  │                                                                          │
│  ├── review_card(card_data):                                               │
│  │   用 SQL 原子更新 mastery_score                                          │
│  │   发放积分 → 写入积分流水或调用 user_scores 表更新                      │
│  │                                                                          │
│  └── archive_old_cards():                                                   │
│       批量设置 mastery_score >= 0.8 的 wrong_answers 为已归档               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 数据流

#### 5.3.1 今日卡片加载

```
用户打开 Flashcard 页面
    │
    ▼
FlashcardViewModel.loadTodayCards()
    │
    ├── 联网模式:
    │   ├── GET /api/v1/flashcard/today?limit=10
    │   │   └── 后端查询 wrong_answers 表 (mastery<0.8, 未归档)
    │   │       └── 返回 cards 列表
    │   └── 缓存到 Room:
    │       └── upsert FlashcardEntity (事务)
    │
    ├── 离线模式:
    │   └── 查询 Room flashcards 表:
    │       SELECT * FROM flashcards
    │       WHERE isArchived=0
    │       ORDER BY lastReviewedAt ASC, createdAt DESC
    │       LIMIT 10
    │
    └── 设置: todayProgress = (completed=0, total=cards.size)
```

#### 5.3.2 卡片评价流

```
用户右划卡片 (judgment="mastered")
    │
    ▼
FlashcardViewModel.reviewCard(cardId, "mastered")
    │
    ├── 本地计算新 mastery_score:
    │   newScore = min(1.0, oldScore + (1.0 - oldScore) * 0.3)
    │   isArchived = newScore >= 0.8
    │
    ├── 更新 Room:
    │   ├── FlashcardDao.updateMastery(cardId, newScore, isArchived, now)
    │   └── FlashcardDao.insertReviewLog(cardId, "mastered", oldScore, newScore, synced=false)
    │
    ├── 积分 +2: 本地累加 + 调用 GamificationApi.syncScore 同步
    │
    ├── 联网模式:
    │   └── POST /api/v1/flashcard/review
    │       Body: { card_id, judgment: "mastered", time_spent_seconds }
    │       └── 更新 review_log.synced = true
    │
    ├── 离线模式:
    │   └── 记录到 review_logs (synced=false)
    │       → 网络恢复后自动同步 (SyncFlashcardsUseCase)
    │
    └── UI 更新:
        ├── todayProgress = (completed+1, total)
        ├── "+2 积分" 飘字动画
        └── 切换到下一张卡片 / 全部完成提示
```

### 5.4 类/接口设计

#### 5.4.1 Room Entity

```kotlin
// FlashcardEntity.kt
@Entity(
    tableName = "flashcards",
    indices = [Index("sourceId", unique = true)]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val sourceType: String,                // wrong_answer / quiz_question
    val sourceId: String,                  // 来源 ID
    val frontContent: String,              // 卡片正面（题目）
    val frontType: String,                 // 题目类型
    val frontOptions: String? = null,      // 选项 JSON
    val backCorrectAnswer: String,
    val backUserAnswer: String,
    val backExplanation: String? = null,
    val backMasteryScore: Float = 0.0f,
    val backSubject: String,
    val backTopic: String? = null,
    val masteryScore: Float = 0.0f,
    val reviewCount: Int = 0,
    val isArchived: Boolean = false,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// FlashcardReviewLogEntity.kt
@Entity(
    tableName = "flashcard_review_logs",
    indices = [Index("cardId"), Index("synced")]
)
data class FlashcardReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: String,
    val judgment: String,               // "mastered" / "unfamiliar"
    val masteryBefore: Float,
    val masteryAfter: Float,
    val timeSpentSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false        // 是否已同步到后端
)
```

#### 5.4.2 DAO

```kotlin
@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE isArchived=0 ORDER BY lastReviewedAt ASC, createdAt DESC LIMIT :limit")
    suspend fun getTodayCards(limit: Int = 10): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :cardId")
    suspend fun getById(cardId: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<FlashcardEntity>)

    @Query("""
        UPDATE flashcards SET
            masteryScore = :newScore,
            reviewCount = reviewCount + 1,
            isArchived = :isArchived,
            lastReviewedAt = :reviewedAt,
            updatedAt = :reviewedAt
        WHERE id = :cardId
    """)
    suspend fun updateMastery(cardId: String, newScore: Float, isArchived: Boolean, reviewedAt: Long)

    @Insert
    suspend fun insertReviewLog(log: FlashcardReviewLogEntity)

    @Query("SELECT * FROM flashcard_review_logs WHERE synced=0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedReviewLogs(): List<FlashcardReviewLogEntity>

    @Query("UPDATE flashcard_review_logs SET synced=1 WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<Int>)

    @Query("SELECT COUNT(*) FROM flashcards WHERE isArchived=0")
    fun countPendingCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isArchived=0 AND lastReviewedAt >= :todayStart")
    suspend fun countReviewedToday(todayStart: Long): Int

    @Delete
    suspend fun deleteCards(cards: List<FlashcardEntity>)
}
```

#### 5.4.3 后端关键类型

```python
# backend/app/schemas/flashcard.py (新建)
from pydantic import BaseModel, Field
from typing import Optional

class FlashcardFront(BaseModel):
    content: str
    type: str
    options: list[dict] = Field(default_factory=list)

class FlashcardBack(BaseModel):
    correct_answer: str
    user_answer: str
    explanation: Optional[str] = None
    mastery_score: float
    subject: str
    topic: Optional[str] = None

class FlashcardOut(BaseModel):
    id: str
    source_type: str      # wrong_answer / quiz_question
    source_id: str
    front: FlashcardFront
    back: FlashcardBack
    mastery_score: float
    review_count: int
    is_archived: bool
    last_reviewed_at: Optional[str] = None
    created_at: str

class TodayCardsResponse(BaseModel):
    cards: list[FlashcardOut]
    total_today: int
    completed_today: int
    daily_limit: int

class ReviewRequest(BaseModel):
    card_id: str
    judgment: str = Field(..., pattern="^(mastered|unfamiliar)$")
    time_spent_seconds: Optional[int] = None

class ReviewResponse(BaseModel):
    card_id: str
    new_mastery_score: float
    archived: bool
    earned_points: int
    total_points: int

class BatchReviewRequest(BaseModel):
    reviews: list[ReviewRequest]

class BatchReviewItem(BaseModel):
    card_id: str
    success: bool
    new_mastery: float
    archived: bool

class BatchReviewResponse(BaseModel):
    synced: int
    results: list[BatchReviewItem]
```

### 5.5 mastery_score 更新算法

```python
def calculate_new_mastery(old_score: float, judgment: str) -> float:
    """
    渐进式掌握度更新算法
    Args:
        old_score: 当前掌握度 (0.0 ~ 1.0)
        judgment: "mastered" 或 "unfamiliar"
    Returns:
        新掌握度
    """
    if judgment == "mastered":
        # 已掌握: 向 1.0 趋近 30% 的剩余距离
        # 例: 0.3 → 0.3 + (1.0-0.3)*0.3 = 0.51
        # 例: 0.7 → 0.7 + (1.0-0.7)*0.3 = 0.79
        # 例: 0.9 → 0.9 + (1.0-0.9)*0.3 = 0.93
        return min(1.0, old_score + (1.0 - old_score) * 0.3)
    elif judgment == "unfamiliar":
        # 不熟练: 固定扣减 20%
        # 例: 0.5 → 0.3
        # 例: 0.2 → 0.0
        return max(0.0, old_score - 0.2)
    else:
        raise ValueError(f"Invalid judgment: {judgment}")


def should_archive(mastery_score: float) -> bool:
    """掌握度 ≥ 0.8 自动归档"""
    return mastery_score >= 0.8
```

### 5.6 每日新卡上限逻辑

```
配置:
  FLASHCARD_DAILY_LIMIT: Int = 10 (默认, 可通过后端配置或用户设置)
  FLASHCARD_REVIEW_MAX_PER_CARD_PER_DAY: Int = 3 (单卡同日内最多出现3次)

加载逻辑:
  1. 查询今天已完成的 review 数 (lastReviewedAt >= todayStart)
  2. 如果已完成数 >= dailyLimit → 返回空列表 + "今日已完成"
  3. 查询待复习卡片 (isArchived=false):
     ┌── 从未复习过 → 优先展示 (lastReviewedAt IS NULL)
     ├── 今天已复习 < 3 次 → 加入队列 (同一天内重复出现)
     └── 今天已复习 >= 3 次 → 跳过
  4. 取 min(dailyLimit - completed, 待复习卡片数) 张
```

### 5.7 API 设计

| 方法 | 路径 | 说明 | 工作人日 |
|:----:|:-----|:-----|:--------:|
| GET | `/api/v1/flashcard/today` | 获取今日待复习卡片 | 1 |
| POST | `/api/v1/flashcard/review` | 提交单张卡片评价 | 0.5 |
| POST | `/api/v1/flashcard/sync` | 批量同步离线评价 | 0.5 |

### 5.8 关键决策及理由

| 决策 | 选择 | 备选方案 | 理由 |
|:-----|:----|:---------|:-----|
| 卡片数据源 | wrong_answers 表 (服务端权威) | 本地 Room 全量 | 服务端数据完整，避免 Android 端复杂的数据同步 |
| 左右划动实现 | Compose SwipeToDismiss + AnimatedVisibility | ViewPager / RecyclerView | 原生 Compose 组件，与现有 UI 框架一致 |
| mastery 更新位置 | 后端计算（服务端权威） | Android 端先算后同步 | 防止本地篡改，积分发放和归档逻辑集中控制 |
| 积分发放 | 后端 review API 直接调用 game/sync/score | Android 端独立调用 | 减少网络请求次数，原子性保证积分不丢失 |
| 离线评价 | 本地暂存 review_logs，网络恢复后批量同步 | 只读离线，评价必须有网 | 不阻塞用户操作，网络恢复后一次性同步 |

### 5.9 技术风险与备选方案

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| SwipeToDismiss 与页面滚动冲突 | 低 | 中 | 在卡片区域使用 `detectVerticalDragGestures` + `detectHorizontalDragGestures` 分离 |
| 同一天多次复习同一卡片导致 mastery 震荡 | 中 | 中 | 单卡每日最多复习 3 次；mastery 浮动有下限保护 |
| 大量卡片归档后 Room 表膨胀 | 低 | 低 | 归档卡片超过 30 天自动清理（后台定时任务） |
| 积分发放未成功（后端 review 成功但 game/sync 失败） | 低 | 中 | review 接口使用数据库事务保证积分和 mastery 更新原子性 |

### 5.10 文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/local/entity/FlashcardEntity.kt` | 新建 | Flashcard 实体 |
| `data/local/entity/FlashcardReviewLogEntity.kt` | 新建 | 复习评价日志实体 |
| `data/local/dao/FlashcardDao.kt` | 新建 | 卡片 DAO |
| `data/local/db/AiTutorDatabase.kt` | 修改 | 添加 FlashcardEntity/FlashcardReviewLogEntity |
| `data/local/db/Migration1To2.kt` | 修改 | 合并 Flashcard 表创建（已在 2.5.1 统一处理） |
| `di/DatabaseModule.kt` | 修改 | 提供 FlashcardDao |
| `data/remote/api/FlashcardApi.kt` | 新建 | 卡片 API 定义 (Retrofit) |
| `data/remote/dto/FlashcardDtos.kt` | 新建 | 卡片相关 DTO |
| `data/repository/FlashcardRepositoryImpl.kt` | 新建 | 卡片仓库实现 |
| `domain/repository/FlashcardRepository.kt` | 新建 | 卡片仓库接口 |
| `domain/model/Flashcard.kt` | 新建 | 卡片领域模型 |
| `domain/model/FlashcardReview.kt` | 新建 | 复习评价模型 |
| `domain/usecase/flashcard/GetTodayCardsUseCase.kt` | 新建 | 获取今日卡片用例 |
| `domain/usecase/flashcard/ReviewCardUseCase.kt` | 新建 | 评价卡片用例 |
| `domain/usecase/flashcard/SyncFlashcardsUseCase.kt` | 新建 | 同步卡片评价用例 |
| `ui/flashcard/FlashcardScreen.kt` | 新建 | 卡片主页面（含 SwipeToDismiss） |
| `ui/flashcard/FlashcardFront.kt` | 新建 | 卡片正面 Composable |
| `ui/flashcard/FlashcardBack.kt` | 新建 | 卡片反面 Composable |
| `ui/flashcard/FlashcardViewModel.kt` | 新建 | 卡片状态管理 |
| `ui/navigation/Routes.kt` | 修改 | 添加 Flashcard 路由 |
| `ui/navigation/AppNavGraph.kt` | 修改 | 注册 Flashcard 页面 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/flashcard.py` | 新建 | Flashcard 路由 (today/review/sync) |
| `app/schemas/flashcard.py` | 新建 | Flashcard Schema |
| `app/services/flashcard_service.py` | 新建 | 卡片抽取、评分、归档业务逻辑 |
| `app/main.py` | 修改 | 注册 flashcard router |
| `app/models/wrong_answer.py` | 修改 | 添加 `is_archived` 字段 |

---

## 6. 安全设计

### 6.1 API 鉴权

```
所有 Sprint 2 新增路由统一依赖:
  router = APIRouter(dependencies=[Depends(get_current_user)])
  确保每个路由都需要有效的 JWT Token

| 路由 | 额外安全要求 |
|:-----|:------------|
| POST /sync | 仅允许同步本人的数据; action 中的 target_id 必须属于当前用户 |
| GET /sync | 仅返回当前用户的增量数据 |
| POST /report/export | 配额检查（免费 3 次/日, premium 20 次/日） |
| GET /tts | 无额外要求（TTS 不计费） |
| GET /flashcard/today | 仅返回当前用户的待复习卡片 |
| POST /flashcard/review | card_id 必须属于当前用户 |
| POST /flashcard/sync | 同上 |
```

### 6.2 配额安全

```python
# 报告导出配额
# 使用 Redis 计数: report:export:count:{user_id}:{date}
# TTL: 86400s (自动过期)
# 或使用 daily_quotas 表扩展 feature="report_export"

EXPORT_QUOTA = {
    "free": 3,
    "premium": 20,
}

# 逻辑: POST /report/export 时先查是否超限
def check_export_quota(user_id: str) -> bool:
    count = redis.get(f"report:export:{user_id}:{today()}")
    limit = EXPORT_QUOTA["premium"] if is_premium(user_id) else EXPORT_QUOTA["free"]
    return int(count or 0) < limit
```

### 6.3 数据完整性

- `flashcards.sourceId` UNIQUE 索引 → 同一来源不错题不生成重复卡片
- `flashcard_review_logs` 中的评价记录不删除，仅标记 `synced`
- 同步操作使用 `sync_timestamp` 做乐观锁，防止并发覆盖
- Room 缓存数据不包含敏感信息（无 token，无密码）

---

## 7. 兼容性矩阵

### 7.1 Android 端文件变更清单

| 文件 | 操作 | 模块 | 说明 |
|:-----|:----:|:----:|:-----|
| `data/local/entity/CachedQuestionEntity.kt` | ★新建 | 离线 | 缓存题目实体 |
| `data/local/entity/CachedWrongAnswerEntity.kt` | ★新建 | 离线 | 缓存错题实体 |
| `data/local/entity/CachedConversationEntity.kt` | ★新建 | 离线 | 缓存对话实体 |
| `data/local/entity/OfflineActionEntity.kt` | ★新建 | 离线 | 离线操作日志实体 |
| `data/local/entity/FlashcardEntity.kt` | ★新建 | Flashcard | Flashcard 实体 |
| `data/local/entity/FlashcardReviewLogEntity.kt` | ★新建 | Flashcard | 复习评价日志实体 |
| `data/local/dao/CachedQuestionDao.kt` | ★新建 | 离线 | 题目缓存 DAO |
| `data/local/dao/CachedWrongAnswerDao.kt` | ★新建 | 离线 | 错题缓存 DAO |
| `data/local/dao/CachedConversationDao.kt` | ★新建 | 离线 | 对话缓存 DAO |
| `data/local/dao/OfflineActionDao.kt` | ★新建 | 离线 | 离线操作 DAO |
| `data/local/dao/FlashcardDao.kt` | ★新建 | Flashcard | 卡片 DAO |
| `data/local/db/AiTutorDatabase.kt` | 修改 | 离线+Flashcard | v1→v2 Migration, 添加全部新实体 |
| `data/local/db/Migration1To2.kt` | ★新建 | 离线+Flashcard | Migration 1→2 实现 |
| `data/local/CacheManager.kt` | ★新建 | 离线 | 缓存统计 + LRU 淘汰 |
| `di/DatabaseModule.kt` | 修改 | 离线+Flashcard | 提供新 DAO |
| `di/ReportModule.kt` | ★新建 | 报告 | 报告模块依赖注入 |
| `di/SpeechModule.kt` | 修改 | TTS | CloudTtsEngine / LocalTtsEngine |
| `data/repository/OfflineRepositoryImpl.kt` | ★新建 | 离线 | Cache-first 仓库 |
| `domain/repository/OfflineRepository.kt` | ★新建 | 离线 | 离线仓库接口 |
| `data/repository/SyncRepositoryImpl.kt` | ★新建 | 离线 | 同步仓库 |
| `domain/repository/SyncRepository.kt` | ★新建 | 离线 | 同步仓库接口 |
| `data/repository/ReportRepositoryImpl.kt` | ★新建 | 报告 | 下载+ShareSheet |
| `domain/repository/ReportRepository.kt` | ★新建 | 报告 | 报告仓库接口 |
| `data/repository/FlashcardRepositoryImpl.kt` | ★新建 | Flashcard | 卡片仓库 |
| `domain/repository/FlashcardRepository.kt` | ★新建 | Flashcard | 卡片仓库接口 |
| `data/remote/api/SyncApi.kt` | ★新建 | 离线 | 同步 API |
| `data/remote/api/ReportApi.kt` | ★新建 | 报告 | 导出 API |
| `data/remote/api/FlashcardApi.kt` | ★新建 | Flashcard | 卡片 API |
| `data/remote/api/AiTutorApi.kt` | 修改 | TTS | 添加 `GET /api/v1/tts` |
| `data/remote/dto/SyncDtos.kt` | ★新建 | 离线 | 同步 DTO |
| `data/remote/dto/ReportDtos.kt` | ★新建 | 报告 | 报告 DTO |
| `data/remote/dto/FlashcardDtos.kt` | ★新建 | Flashcard | 卡片 DTO |
| `data/media/CloudTtsEngine.kt` | ★新建 | TTS | 云端 TTS (provider切换+降级) |
| `data/media/LocalTtsEngine.kt` | ★新建 | TTS | 本地 TTS 封装 |
| `domain/repository/VoiceRepository.kt` | 修改 | TTS | 添加 set/getTtsProvider |
| `data/repository/VoiceRepositoryImpl.kt` | 修改 | TTS | provider 切换 + 降级 |
| `domain/model/Flashcard.kt` | ★新建 | Flashcard | 卡片领域模型 |
| `domain/model/FlashcardReview.kt` | ★新建 | Flashcard | 复习评价模型 |
| `domain/usecase/sync/SyncDataUseCase.kt` | ★新建 | 离线 | 同步数据用例 |
| `domain/usecase/cache/GetCachedQuestionsUseCase.kt` | ★新建 | 离线 | 获取缓存题目 |
| `domain/usecase/cache/GetCachedWrongAnswersUseCase.kt` | ★新建 | 离线 | 获取缓存错题 |
| `domain/usecase/cache/GetCachedConversationsUseCase.kt` | ★新建 | 离线 | 获取缓存对话 |
| `domain/usecase/cache/ClearCacheUseCase.kt` | ★新建 | 离线 | 清除缓存 |
| `domain/usecase/cache/GetCacheSizeUseCase.kt` | ★新建 | 离线 | 获取缓存大小 |
| `domain/usecase/flashcard/GetTodayCardsUseCase.kt` | ★新建 | Flashcard | 获取今日卡片 |
| `domain/usecase/flashcard/ReviewCardUseCase.kt` | ★新建 | Flashcard | 评价卡片 |
| `domain/usecase/flashcard/SyncFlashcardsUseCase.kt` | ★新建 | Flashcard | 同步卡片评价 |
| `ui/offline/OfflineBanner.kt` | ★新建 | 离线 | 离线指示器 Banner |
| `ui/offline/OfflineViewModel.kt` | ★新建 | 离线 | 离线状态 VM |
| `ui/settings/CacheManagementSection.kt` | ★新建 | 离线 | 缓存管理界面 |
| `ui/settings/SettingsScreen.kt` | 修改 | 离线+TTS | 添加缓存管理 + TTS Provider 选择 |
| `ui/settings/SettingsViewModel.kt` | 修改 | 离线+TTS | 缓存状态 + TTS provider 状态 |
| `ui/report/ExportReportScreen.kt` | ★新建 | 报告 | 导出设置页面 |
| `ui/report/ExportReportViewModel.kt` | ★新建 | 报告 | 导出状态管理 |
| `ui/flashcard/FlashcardScreen.kt` | ★新建 | Flashcard | 卡片主页面 (SwipeToDismiss) |
| `ui/flashcard/FlashcardFront.kt` | ★新建 | Flashcard | 卡片正面 Composable |
| `ui/flashcard/FlashcardBack.kt` | ★新建 | Flashcard | 卡片反面 Composable |
| `ui/flashcard/FlashcardViewModel.kt` | ★新建 | Flashcard | 卡片状态管理 |
| `ui/navigation/Routes.kt` | 修改 | 报告+Flashcard | 添加报告导出、Flashcard 路由 |
| `ui/navigation/AppNavGraph.kt` | 修改 | Flashcard | 注册 Flashcard 页面 |
| `util/NetworkMonitor.kt` | 修改 | 离线 | 扩展 isOnline 快照方法 |

### 7.2 后端文件变更清单

| 文件 | 操作 | 模块 | 说明 |
|:-----|:----:|:----:|:-----|
| `app/routers/sync.py` | ★新建 | 离线 | GET/POST /api/v1/sync |
| `app/routers/report.py` | ★新建 | 报告 | POST /api/v1/report/export |
| `app/routers/flashcard.py` | ★新建 | Flashcard | today/review/sync 路由 |
| `app/routers/audio.py` | 修改 | TTS | 新增 GET /api/v1/tts |
| `app/schemas/sync.py` | ★新建 | 离线 | Sync 请求/响应 Schema |
| `app/schemas/report.py` | ★新建 | 报告 | 导出请求 Schema |
| `app/schemas/flashcard.py` | ★新建 | Flashcard | Flashcard Schema |
| `app/schemas/audio.py` | 修改 | TTS | 添加 TtsQueryParams |
| `app/services/sync_service.py` | ★新建 | 离线 | 增量查询 + 冲突检测 |
| `app/services/report_service.py` | ★新建 | 报告 | PDF 生成 + CSV 生成 |
| `app/services/report_charts.py` | ★新建 | 报告 | Matplotlib 图表生成 |
| `app/services/tts_base.py` | ★新建 | TTS | TTSProvider 抽象基类 |
| `app/services/tts_edge.py` | ★新建 | TTS | Edge TTS 适配器 |
| `app/services/tts_factory.py` | ★新建 | TTS | TTSProvider 工厂 |
| `app/services/tts_openai.py` | ★新建 | TTS | OpenAI TTS 适配器 |
| `app/services/flashcard_service.py` | ★新建 | Flashcard | 卡片业务逻辑 |
| `app/models/wrong_answer.py` | 修改 | Flashcard | 添加 `is_archived` 字段 |
| `app/config.py` | 修改 | TTS | 添加 tts_default_provider 等 |
| `app/main.py` | 修改 | 全部 | 注册 sync/report/flashcard router |
| `requirements.txt` | 修改 | 报告+TTS | 添加 reportlab, matplotlib, websockets |

### 7.3 Room 数据库版本历史

| 版本 | 添加内容 | 对应 Sprint |
|:----:|:---------|:-----------:|
| v1 | ConversationEntity, MessageEntity | Sprint 0 (基础) |
| v2 | CachedQuestionEntity, CachedWrongAnswerEntity, CachedConversationEntity, OfflineActionEntity, FlashcardEntity, FlashcardReviewLogEntity, SyncMetadata | **Sprint 2** |

---

> **文档结束** — Sprint 2 架构设计覆盖 P1-1 (离线模式)、P1-2 (学习报告导出)、P2-1 (后端 TTS 替换)、P2-2 (Flashcard 抽认卡) 四个核心功能模块。
> 评审通过后由 Coder 按此架构实现。建议先在 sprint review 会议上评审架构，确认无误后分配开发任务。
