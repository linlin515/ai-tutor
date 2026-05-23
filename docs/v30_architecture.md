# v30 仪表盘分页架构方案 — Paging 3

**日期**: 2026-05-23
**状态**: ✅ CFO 确认方案，待 PM 分发 Coder 执行
**任务卡片**: `t_9d5867ed`

---

## 一、现状数据流分析

### 1.1 完整数据链路（Before Paging 3）

```
GamificationApi.getLeaderboard()   ──┐
  [GET api/v1/game/leaderboard]       │  云端 API
  → ApiResponse<LeaderboardResponse>  │  返回全量数据（无 pagination 参数）
                                       │  ⚠️ 调用方 refreshLeaderboard() 丢弃此响应
                                       │
GamificationRepositoryImpl            │
  observeLeaderboard()                │  ─── 转发给 Engine
  refreshLeaderboard()                │  ─── 调用 cloud API 但丢弃 body

GamificationEngine
  getLeaderboard(GLOBAL)              │  → Flow<List<RankEntry>>
    userScoreDao.getScore()            │  仅生成当前用户 1 条 RankEntry
    .map { score →                    │  ⚠️ 不包含排行榜全量数据
      score?.let { listOf(RankEntry(  │  ⚠️ 仅有"我"一条
        rank=1, nickname="我", ...))}
    }

DashboardViewModel                    │
  observeGamificationData()           │  StateFlow<List<RankEntry>>
  _uiState.value.rankings             │

LeaderboardView.kt                    │
  rankings: List<RankEntry>           │  LazyColumn(items)
```

### 1.2 关键问题定位

| 层级 | 问题 |
|---|---|
| **GamificationApi** | `getLeaderboard()` 无 `page`/`pageSize` 参数，服务端返回全量 `LeaderboardResponse(rankings: List...)` |
| **LeaderboardResponse DTO** | 没有分页元信息（total/page/pageSize），不支持 Paging 3 的 RemoteMediator/PagingSource 契约 |
| **GamificationRepositoryImpl** | `refreshLeaderboard()` 调用云端但**丢弃 response.body**，`observeLeaderboard()` 不经过 API，只有本地单用户数据，无全量排行榜 |
| **GamificationEngine** | `getLeaderboard()` 仅映射本地单用户 Entry，无法提供全量列表 |
| **LeaderboardView** | 渲染 `List<RankEntry>` 全量数据，用户量增长时布局负担重 |

### 1.3 有利条件

- 代码库已存在 `PaginatedData<T>` 通用分页 DTO（`ApiResponse.kt:14–20`），包含 `items/total/page/pageSize/totalPages` 字段，后端框架已有分页约定
- `GamificationApi` 与引擎结构清晰，易于在不破坏现有分层的前提下新增分页方法

---

## 二、后端 API 支持结论

### 2.1 当前状态：🔴 不支持

```
GamificationApi.getLeaderboard() 签名：
  @GET("api/v1/game/leaderboard")
  suspend fun getLeaderboard(): Response<ApiResponse<LeaderboardResponse>>

问题：
  1. 无 @Query("page") Int
  2. 无 @Query("page_size") Int
  3. 响应体 LeaderboardResponse 只有 rankings 列表，无分页元数据
  4. refreshLeaderboard() 调用该 endpoint 后丢弃响应体，现有 Bug
```

### 2.2 必须的后端改动

**必须由后端配合完成**，否则 Paging 3 PagingSource 无法按页请求：

| API 改动 | 具体说明 |
|---|---|
| 请求参数 | `GET /api/v1/game/leaderboard?page=1&page_size=20&type=GLOBAL` |
| 响应体 | 从 `LeaderboardResponse(rankings: List<...>)` 改为 `ApiResponse<PaginatedData<LeaderboardItem>>` |
| 分页字段 | 服务端返回 `items[List]、total[int]、page[int]、page_size[int]、total_pages[int]` |
| page 值域 | `page ≥ 1`，超出 `totalPages` 应返回空列表 |

**现有 `PaginatedData<T>` 已声明，后端只需接入该契约。**

---

## 三、Paging 3 分层架构设计

### 3.1 五层架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation 层                           │
│ LeaderboardView.kt: LazyPagingItems<RankEntry>                   │
│                            ↓ collectAsLazyPagingItems()          │
│  items(lazyPagingItems) { ... }                                  │
│  loadState: loadState.refresh / append / prepend                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Flow<PagingData<RankEntry>>
┌──────────────────────────▼──────────────────────────────────────┐
│                         ViewModel 层                              │
│  DashboardViewModel.kt                                            │
│  observeLeaderboardPaging() → Flow<PagingData<RankEntry>>        │
│  状态由 lazyPagingItems.loadState 驱动，不再维护 isLoading 标志   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Flow<PagingData<RankEntry>>
┌──────────────────────────▼──────────────────────────────────────┐
│                     Domain Repository 层                          │
│  GamificationRepository.kt（interface）                           │
│  observeLeaderboardPaging(type, pageSize): Flow<PagingData<...>>  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Pager(PagingConfig(...)) { pagingSource }
┌──────────────────────────▼──────────────────────────────────────┐
│                       Domain Engine 层                            │
│  GamificationEngine.kt                                            │
│  getLeaderboardPaging(type, pageSize): Flow<PagingData<RankEntry> │
│  ├── Pager 创建（pageSize=20, enablePlaceholders=false）          │
│  └── LeaderboardPagingSource(key=type, load=page→api)             │
└──────────────────────────┬──────────────────────────────────────┘
                           │ load(params: LoadParams<Int>) 
┌──────────────────────────▼──────────────────────────────────────┐
│                        Data PagingSource 层                       │
│  LeaderboardPagingSource(key=type)     ── 内部调用               │
│    load(params: LoadParams<Int>)          └→ GamificationApi      │
│      gamificationApi.getLeaderboard(        getLeaderboard(       │
│        page=key, page_size=pageSize)         type, page, size)   │
│      → PaginatedData<LeaderboardItem>        ↓                    │
│      → List<RankEntry> (map)               PagingData             │
│                                                                   │
│  错误处理：LoadResult.Error → retry，服务端异常 → LoadResult.Error│
│  边界：page > totalPages → LoadResult.Page(emptyList(), prev=null)│
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP GET (page, pageSize)
┌──────────────────────────▼──────────────────────────────────────┐
│                       Remote / 云端 API                            │
│  GET /api/v1/game/leaderboard?page=N&page_size=M&type=GLOBAL     │
│  响应：ApiResponse<PaginatedData<LeaderboardItem>>               │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.2 各层数据转换

```
层级转换链：

第1层（API响应）
  ApiResponse<PaginatedData<LeaderboardItem>>
    └─ data.items: List<LeaderboardItem>  ──[map]───▶
                                              rankEntryList: List<RankEntry>
                                                  ↓
第2层（PagingSource）
  LoadResult.Page(data = rankEntryList, prevKey = null, nextKey = nextPage)
    └─ PagingData<RankEntry>
        ↓
第3层（Pager → Engine）
  Flow<PagingData<RankEntry>>  ──[Operator]───▶
    .cachedIn(viewModelScope)  ──[Operator]───▶
    Flow<PagingData<RankEntry>>  (与 ViewModel 生命周期绑定)
        ↓
第4层（ViewModel）
  val pagingFlow = gamificationRepository.observeLeaderboardPaging()
  val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
        ↓
第5层（Compose UI）
  LeaderboardView(rankings = lazyPagingItems)
    items(lazyPagingItems, key = { it.rank }) { entry ->
      LeaderboardRow(entry)
    }
    lazyPagingItems.loadState
      .refresh   → 首屏加载/下拉刷新
      .append    → 滚动到底部加载更多
      .prepend   → 顶部插入（排行榜通常不需要）
```

---

## 四、每层详细设计

### 4.1 Gap 0：先修复 `refreshLeaderboard()` 数据丢弃 Bug

```kotlin
// GamificationRepositoryImpl.kt — 当前 Bug
override suspend fun refreshLeaderboard() {
    try {
        val response = gamificationApi.getLeaderboard()
        if (response.isSuccessful) {
            response.body()?.let { body ->
                body.data?.let { data ->
                    // ⚠️ 排行榜数据从云端获取，本地引擎负责展示
                    // ⚠️ 目前本地仅存储当前用户数据
                    // BUG: response 被忽略，不填充任何数据
                }
            }
        }
    } catch (_: Exception) {
        // 静默失败，使用本地数据
    }
}
```

**CFO 判定**：此 Bug 在 v30 方案实施前必须一并修复（或将刷新逻辑合并到分页实现中），否则分页数据源无初始数据。

---

### 4.2 Domain Layer — Repository

**文件**: `domain/repository/GamificationRepository.kt`

```kotlin
interface GamificationRepository {
    // 保留旧方法（兼容现有调用，后续内部重定向）
    fun observeLeaderboard(): Flow<List<RankEntry>>

    // [新增 v30] Paging 3 分页流
    fun observeLeaderboardPaging(
        type: LeaderboardType = LeaderboardType.GLOBAL,
        pageSize: Int = 20
    ): Flow<PagingData<RankEntry>>
}
```

**文件**: `data/repository/GamificationRepositoryImpl.kt`

```kotlin
@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val gamificationEngine: GamificationEngine,
    private val gamificationApi: GamificationApi
) : GamificationRepository {

    // 旧方法保留（内部转发到 Paging 3，对外 API 不变）
    override fun observeLeaderboard(): Flow<List<RankEntry>> =
        observeLeaderboardPaging()   // ⬅ Step3 内部整改，Coder 决定是否直接转发
            .map { pagingData -> TODO() } // 暂时保留旧接口适配

    // 新方法
    override fun observeLeaderboardPaging(
        type: LeaderboardType,
        pageSize: Int
    ): Flow<PagingData<RankEntry>> {
        return gamificationEngine.getLeaderboardPaging(type, pageSize)
    }

    // Step0 BugFix: 修复 refreshLeaderboard 数据丢弃
    override suspend fun refreshLeaderboard() = gamificationEngine.refreshLeaderboard()
}
```

---

### 4.3 Domain Layer — Engine

**文件**: `domain/engine/GamificationEngine.kt`

```kotlin
@Singleton
class GamificationEngine @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userScoreDao: UserScoreDao,
    private val scoreLogDao: ScoreLogDao,
    // [新增 v30] 注入云 API（或通过 Repository 封装）
    private val gamificationApi: GamificationApi
) {

    // [新增 v30] Pager 创建方法
    fun getLeaderboardPaging(
        type: LeaderboardType = LeaderboardType.GLOBAL,
        pageSize: Int = 20
    ): Flow<PagingData<RankEntry>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,           // 每页 20 条
                enablePlaceholders = false,    // 排行榜排名精确，无需占位
                prefetchDistance = 3,          // 距底部 3 条时预取下一页
                maxSize = 200,                 // 最近加载上限，防止 RL 循环
                jumpThreshold = 1000           // 超过 1000 条跳转时丢弃调试
            ),
            pagingSourceFactory = {
                LeaderboardPagingSource(
                    gamificationApi = gamificationApi,
                    type = type
                )
            }
        ).flow
    }

    // [新增 v30] 刷新时通知 PagingSource 失效
    suspend fun refreshLeaderboard() {
        // 1）同步分数到云端（现有逻辑保留）
        try {
            gamificationApi.syncScore()
        } catch (_: Exception) {
            // 证书失败不中断刷新
        }
        // 2）PagingSource invalidate() 由 ViewModel 在 refresh 时调用
        //    （通过 mediator.invalidate() 完成）
        // → ViewModel refresh() 改为: pagingItems.refresh()
    }
}
```

**PagingConfig 参数说明**

| 参数 | 选值 | 理由 |
|---|---|---|
| `pageSize` | `20` | 排行榜每屏约 5–8 条可见，20 条留余量，滚动到第二页时已加载较多 |
| `enablePlaceholders` | `false` | `RankEntry` 含有 rank 精确字段，佔位会造成排名空白 |
| `prefetchDistance` | `3` | 距底部还剩 3 条时预加载，用户几乎感觉不到延迟 |
| `maxSize` | `200` | 限制近期内存中缓存条目数，超过时丢弃旧的 |
| `jumpThreshold` | `1000` | 排行榜用户量超千时需优化跳转，此处告警 |

---

### 4.4 Data Layer — PagingSource

**新增文件**: `data/repository/paging/LeaderboardPagingSource.kt`

```kotlin
class LeaderboardPagingSource(
    private val gamificationApi: GamificationApi,
    private val type: LeaderboardType
) : PagingSource<Int, RankEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RankEntry> {
        val page = params.key ?: 1           // 默认从第1页开始
        val pageSize = params.loadSize.coerceAtMost(100) // API 上限保护

        return try {
            val response = gamificationApi.getLeaderboard(
                page = page,
                page_size = pageSize,
                type = type.name            // "GLOBAL" 或 "FRIENDS"
            )

            if (!response.isSuccessful) {
                return LoadResult.Error(
                    IOException("HTTP ${response.code()}: ${response.message()}")
                )
            }

            val body = response.body()
                ?: return LoadResult.Error(IOException("Response body is null"))

            val paginated: PaginatedData<LeaderboardItem> = body.data
                ?: return LoadResult.Error(IOException("Response data is null"))

            val rankEntries = paginated.items
                .mapIndexed { index, item ->
                    item.toRankEntry(
                        isMe = /* 需要前端传入 isMeUserId 对比，见下节讨论 */
                        false  // PagingSource 无法感知"我"，仅渲染云端数据
                    )
                }

            LoadResult.Page(
                data = rankEntries,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < paginated.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, RankEntry>): Int? {
        // 刷新后从最接近的 key 重新加载
        return state.anchorPosition?.let { anchorPos ->
            state.closestPageToPosition(anchorPos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPos)?.nextKey?.minus(1)
        }
    }
}
```

#### ⚠️ 设计决策："我的排名"高亮（`isMe=true`）

**问题**：PagingSource 按页加载，有的页包含"我"，有的页不含。逐条对比 `item.userId` 需传入当前用户 ID。

**方案**（二选一，Coder 决定）：

| 方案 | 实现方式 | 优先度 |
|---|---|---|
| **A. PagingSource 持有用户 ID** | `LeaderboardPagingSource` 加 `currentUserId: String` 参数，`load()` 内做 `item.userId == currentUserId` 判断 | 推荐 |
| **B. ViewModel 后处理** | PagingSource 返回全量，ViewModel 额外收集 `observeUserScore` 拿到 myRankEntry，在 Compose 层动态替换 | 简单，但需手动 diff |

**推荐方案 A**，具体改动：

```kotlin
// LeaderboardPagingSource 加参数
class LeaderboardPagingSource(
    private val gamificationApi: GamificationApi,
    private val type: LeaderboardType,
    private val currentUserId: String      // [新增]
) : PagingSource<Int, RankEntry>() {
    // load 里:
    item.toRankEntry(isMe = item.userId == currentUserId)
}
```

---

### 4.5 Data Layer — GamificationApi 改造

**后端配合到位后**，GamificationApi 须改动为：

```kotlin
interface GamificationApi {

    // [改造] 新增分页参数
    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboard(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("type") type: String = "GLOBAL"  // GLOBAL / FRIENDS
    ): Response<ApiResponse<PaginatedData<LeaderboardItem>>>

    // 保留旧方法（内部重定向到分页 page=1，用于 refresh 时需要全量的话）
    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboardAll(): Response<ApiResponse<List<LeaderboardItem>>>

    @POST("api/v1/game/sync/score")
    suspend fun syncScore(): Response<ApiResponse<Unit>>
}
```

> **Note**：若后端确认不提供 `type` 参数，则 `LeaderboardType.FRIENDS` 版本待后续 API 迭代再实现，v30 仅覆盖 `GLOBAL`。

---

### 4.6 ViewModel 层

**文件**: `ui/screen/dashboard/DashboardViewModel.kt`

**改动点**：

1. `DashboardUiState` 中 `rankings` 类型从 `List<RankEntry>` 改为 **移除**（不再由 ViewModel 持有列表）
2. 新增 `observeLeaderboardPaging()` 方法，暴露 `Flow<PagingData<RankEntry>>`
3. 不再维护 `isLoading` 状态给排行榜（由 `LazyPagingItems.loadState` 派生）

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val gamificationRepository: GamificationRepository,
    private val wrongAnswerRepository: WrongAnswerRepository
) : ViewModel() {

    // ──────── 分页排行榜 ────────
    val leaderboardPagingData: Flow<PagingData<RankEntry>> =
        gamificationRepository.observeLeaderboardPaging(
            type = LeaderboardType.GLOBAL,
            pageSize = 20
        )
            .cachedIn(viewModelScope)  // 配置缓存，同一生命周期不重复请求

    // ──────── 其余数据流保持不变 ────────
    // achievements, streak, userScore 等 Flow 不变
}
```

** DashboardScreen 调用方式的调整**：

```kotlin
// DashboardScreen.kt 中的调用方式：
// Before:
// val uiState by viewModel.uiState.collectAsState()
// val rankings = uiState.rankings
// val isLoading = uiState.isLoading

// After:
val lazyPagingItems = viewModel.leaderboardPagingData
    .collectAsLazyPagingItems()   // ⬅ Paging 3 Compose Operator

LeaderboardView(
    rankings = lazyPagingItems,  // ⬅ List<RankEntry> → LazyPagingItems<RankEntry>
    // ⬅ isLoading 参数移除，由 loadState 内部驱动
)
```

---

### 4.7 UI 层 — LeaderboardView.kt 改造

**Before**:

```kotlin
@Composable
fun LeaderboardView(
    rankings: List<RankEntry>,
    isLoading: Boolean = false,     // ⬅ v30 移除
    modifier: Modifier = Modifier
)
```

**After**:

```kotlin
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun LeaderboardView(
    rankings: LazyPagingItems<RankEntry>,   // ⬅ 改为 LazyPagingItems
    modifier: Modifier = Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "排行榜",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // [改造] 用 loadState 替换 isLoading 判断
            when (val loadState = rankings.loadState.refresh) {
                is LoadState.Loading -> {
                    if (rankings.itemCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // 已经有数据，本次是刷新中，可省略骨架或做增量提示
                    }
                }
                is LoadState.Error -> {
                    ErrorRow(error = loadState.error) { rankings.retry() }
                }
                is LoadState.NotLoading -> {
                    if (rankings.itemCount == 0) {
                        Text(
                            text = "暂无排行榜数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // [改造] LazyColumn 改用 Paging Items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    count = rankings.itemCount,     // ⬅ count 模式
                    key = { index -> rankings[index]?.rank ?: index }
                ) { index ->
                    rankings[index]?.let { entry ->
                        LeaderboardRow(entry = entry)
                    }
                }

                // [新增] 加载更多状态
                when (val appendState = rankings.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            ErrorRetryRow(
                                error = appendState.error,
                                onRetry = { rankings.retry() }
                            )
                        }
                    }
                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}
```

---

## 五、数据流信号流图

```
 ┌──────────────────────────────────────────────────────────────┐
 │                   Paging 3 数据流（运行时）                    │
 └──────────────────────────────────────────────────────────────┘

 [首屏加载]
  DashboardScreen 首次 Composed
    └─ collectAsLazyPagingItems(leaderboardPagingData)
         └─ Pager 驱动 PagingSource.load(key=null → key=1)
              └─ gamificationApi.getLeaderboard(page=1, pageSize=20)
                   └─ Response<PaginatedData<LeaderboardItem>>
                        └─ PagingSource 映射为 RankEntry
                             └─ LoadResult.Page(data, nextKey=2)
                                  └─ PagingData<RankEntry> → Compose
                                       └─ LazyColumn 渲染 20 条

 [滚动到底部]
  LazyColumn 倒数第 3 条可见（prefetchDistance=3）
    └─ PagingSource.load(key=2, loadSize=20)
         └─ gamificationApi.getLeaderboard(page=2, pageSize=20)
              └─ LoadResult.Page(data, nextKey=3/nextKey=null)
                   └─ LazyColumn 渐入 20 条

 [下拉刷新]
  DashboardScreen pull-to-refresh
    └─ lazyPagingItems.refresh()          ⬅ 不触发新 API key，参数不变
         └─ PagingSource.invalidate()
              └─ 新 PagingSource 实例
                   └─ gamificationApi.getLeaderboard(page=1, pageSize=20)
                        └─ 更新 UI

 [配置变更]
  viewModelScope 生命周期结束
    └─ cachedIn(viewModelScope) 自动释放
         └─ 页面移出栈时数据失效

 [错误重试]
  LoadResult.Error → lazyPagingItems.retry()
    └─ PagingSource 重新 load(key=当前 page)
         └─ 单次重试，不跳过页面

 [加载预取控制]
  PagingConfig.maxSize=200   → 内存最多保留 200 条
  PagingConfig.prefetch=3    → 底部剩3条时自动加载下一页
  PagingConfig.jump=1000     → 滚动超过1000条时报警告，提示需优化跳转列表
```

---

## 六、后端 API 配合需求（CFO 确认要点）

### 6.1 必须改动（Blocking）

| 检查项 | 当前 | 要求 |
|---|---|---|
| `getLeaderboard` 是否支持 `page` 参数 | ❌ 无 | ✅ 必须新增 |
| `getLeaderboard` 是否支持 `page_size` 参数 | ❌ 无 | ✅ 必须新增 |
| 响应体是否含分页元数据（total/totalPages） | ❌ 无 | ✅ 必须新增 |
| 最大 `page_size` 上限（防滥用） | — | ⚠️ 建议设置最大 100 |

### 6.2 推荐改动（Nice to Have）

| 检查项 | 建议 |
|---|---|
| 支持 `type=GLOBAL\|FRIENDS` | FRIENDS 后续迭代可复用同一 PagingSource |
| 返回 `HTTP 404/503` 时携带可读 `message` | Paging 3 已支持 LoadResult.Error，但友好提示需后端配合 |
| 排行榜数据热更新（ETag/Last-Modified） | Paging 3 可扩展 RemoteMediator，高级版本支持 |
| `getLeaderboard` 响应结构 | `ApiResponse<PaginatedData<LeaderboardItem>>`（与已存在的 `PaginatedData<T>` 对齐） |

---

## 七、Coder 执行步骤（PM 派发）

### Step 1 — 依赖引入
**执行者**: Coder Depend
1. `android/gradle/libs.versions.toml` → 在 `[versions]` 加 `paging = "3.2.1"`
2. `android/gradle/libs.versions.toml` → 在 `[libraries]` 加 `paging-compose` artifact（`androidx.paging:paging-compose`）
3. `android/app/build.gradle.kts` → `dependencies { implementation(libs.paging.compose) }`
4. 同步 Gradle，验证编译通过

### Step 2 — API + PagingSource
**执行者**: Coder Data / Coder Domain
1. 修改 `GamificationApi.kt` — `getLeaderboard()` 加 `@Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 20`
2. 改造 `LeaderboardResponse.kt` — 或改 API 响应为 `PaginatedData<LeaderboardItem>`
3. 新增 `LeaderboardPagingSource.kt`（含 `currentUserId` 方案）
4. 在 `GamificationEngine.kt` 新增 `getLeaderboardPaging()`，装配 Pager + PagingSource
5. 在 `GamificationRepository` interface 声明 `observeLeaderboardPaging()`

### Step 3 — Repository + ViewModel
**执行者**: Coder Domain / Coder ViewModel
1. `GamificationRepositoryImpl` 实现 `observeLeaderboardPaging()`
2. **BugFix**：修复 `refreshLeaderboard()` 数据丢弃
3. `DashboardViewModel` 改用 `leaderboardPagingData: Flow<PagingData<RankEntry>>`

### Step 4 — UI 改造
**执行者**: Coder UI
1. `LeaderboardView.kt` 参数改为 `LazyPagingItems<RankEntry>`
2. 新增 `ErrorRow`、`ErrorRetryRow` composable（提取共用的错误展示组件）
3. `DashboardScreen.kt` 改为 `collectAsLazyPagingItems()` 调用方式
4. 移除 `DashboardUiState.isLoading` 对排行榜的依赖

### Step 5 — 测试验证
**执行者**: Coder QA
1. Unit: `LeaderboardPagingSource` — 边界 page=1、page=lastPage、空响应、异常
2. Integration: `GamificationEngine.getLeaderboardPaging()` — 发射 `PagingData` 验证
3. Compose: `LeaderboardView` — 分页滚动、加载更多、错误重试状态

---

## 八、影响范围总览

| 层级 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 依赖 | `android/gradle/libs.versions.toml` | **新增** | `paging = "3.2.1"` + `paging-compose` artifact |
| 依赖 | `android/app/build.gradle.kts` | **新增** | `implementation(libs.paging.compose)` |
| API | `GamificationApi.kt` | **修改** | `getLeaderboard()` 加 page/pageSize 参数 |
| DTO | `LeaderboardResponse.kt` / 新增 | **修改** | 返回 `PaginatedData<LeaderboardItem>` |
| DTO | `ApiResponse.kt` | **不变** | `PaginatedData<T>` 已存在 |
| Domain | `GamificationRepository.kt` | **扩展** | 新增 `observeLeaderboardPaging()` |
| Domain | `GamificationRepositoryImpl.kt` | **修改** | BugFix + 新方法实现 |
| Domain | `GamificationEngine.kt` | **扩展** | 新增 `getLeaderboardPaging()` |
| Domain | **新增** `LeaderboardPagingSource.kt` | **新建** | `PagingSource<Int, RankEntry>` |
| UI | `LeaderboardView.kt` | **修改** | `List<RankEntry>` → `LazyPagingItems<RankEntry>` |
| UI | `DashboardViewModel.kt` | **修改** | 暴露 Paging Flow |
| UI | `DashboardScreen.kt` | **微调** | `collectAsLazyPagingItems()` |
| UI | **新增**共享错误组件 | **新建** | `LoadingErrorRetryRow.kt`（复用 UI） |
| DAO | 无 | — | `GamificationEngine` DAO 注入不变 |
| Hilt | 如为 Engine 注入 GamificationApi | **待评估** | 若 Engine 不含 API，则在 RepositoryImpl 内封装 |

---

## 九、风险矩阵

| 风险 | 级别 | 影响 | 缓解措施 |
|---|---|---|---|
| **后端不支持 page/pageSize** | 🔴 高 | 阻塞整个 Paging 3 方案 | 必须后端配合。替代方案：本地缓存全量后 PagingSource 虚拟分页（不推荐） |
| **GamificationEngine 不含 API 依赖** | 🟡 中 | Pager 装配位置需定 | Coder 决定：方案 A（Engine 注入 API）或方案 B（RepositoryImpl 内封装 Pager，Engine 只做旧接口） |
| **isMe 高亮跨页定位** | 🟡 中 | 当前 userId 可能不在第一页 | LeaderboardPagingSource 必须持有 currentUserId，方案 A 已覆盖 |
| **PagingSource keyInval 时刷新重载** | 🟢 低 | invalidate 触发不必要请求 | `getRefreshKey()` 实现已覆盖，Paging 3 默认行为阻尼 |
| **KSP/Hilt 生成代码出错** | 🟡 中 | Pager 注入需注解验证 | Step 1 依赖引入后必做 KSP 检查 |
| **Leaderboard Paging Source 与 Engine getLeaderboard 冲突** | 🟡 中 | 两套排行榜数据不一致 | Paging 方案上线后，Meter `getLeaderboard()` 可标注 `@Deprecated("预先迁移，后续删除")` |

---

## 十、架构决策记录（ADR）

| # | 决策 | 原因 |
|---|---|---|
| ADR-1 | PagingSource 部署在 Domain Engine | 遵循 PM 规划：分页逻辑归属 Engine/Repository，ViewModel 只持有 Flow |
| ADR-2 | 枚举 `LeaderboardType.FRIENDS` v30 暂不实现 | 后端 API 可能不支持 type 参数；先跑通 GLOBAL 验证链路 |
| ADR-3 | `enablePlaceholders = false` | 排行榜排名精确，占位符渲染排名 #N 无意义，且额外增加 RecyclerView item 浪费 |
| ADR-4 | `prefetchDistance = 3` | 排行榜对战式场景，接近底部时才加载，减少不必要请求 |
| ADR-5 | `maxSize = 200` | 排行榜 Top200 具有价值再捕获，限制内存 footprint |
| ADR-6 | BugFix refreshLeaderboard 与 Step 2/3 并行 | 数据丢弃 Bug 影响现有 `refresh()` 行为，必须同步修复 |
| ADR-7 | ViewModel 持有 `Flow<PagingData>` 而非 State/StateFlow | PagingData 非线程安全对象，由 Pager flow 与 Compose collector 直接绑定，无需 ViewModel 中转 State 流 |

---

*CFO 签核：数据流分析完成 ✅，API 风险标记 🔴，需求依赖项均已标注。等待 PM 分发 Coder 执行 Step 1–5。*
