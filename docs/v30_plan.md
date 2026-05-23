# v30 仪表盘分页规划

**日期**: 2026-05-23
**状态**: 规划完成，等待 CEO 分配 CFO

---

## 一、现状分析

### 1.1 数据加载方式

仪表盘整体采用 **一次性全量加载 + Flow 一次性发射** 架构：

- **统计数据** (`DashboardViewModel.loadDashboard` → `AnalyticsRepository.getDashboardStats`): Room Flow
  - 资源: `LearningRecordEntity` 按天聚合，单行数据，量极小
  - **无须分页**

- **趋势图** (`setTrendDays` → `AnalyticsRepository.getTrend`): Room Flow, 按天聚合
  - 默认 7 天，最大 30 天，固定范围
  - **分页无意义**

- **知识点掌握度** (`selectSubject` → `AnalyticsRepository.getKnowledgeGraph`): Room Flow
  - 科目节点树形结构，数量中等（各科目通常 < 50 节点）
  - **目前无须分页**，但若科目极多可考虑懒渲染

- **游戏化数据** (`observeGamificationData`): `GamificationEngine` 内部发射 `List<T>`：
  | 数据类型 | 类型 | 量级评估 |
  |---|---|---|
  | `RankEntry` 排行榜 | `LazyColumn` | **潜在大量**，随用户量增长 |
  | `AchievementWithStatus` 成就徽章 | `FlowRow` | ~8 条固定 |
  | `StreakResult` 连胜指示器 | 单对象 | 单条 |
  | `UserScore` 用户积分 | 单对象 | 单条 |

### 1.2 性能问题根因

- 排行榜数据来自云 API（`GamificationRepositoryImpl.observeLeaderboard` → `GamificationEngine.getLeaderboard(LeaderboardType.GLOBAL)`），当用户量增长后全量下发数千条 `RankEntry` 再一次性发到 Compose，**首屏渲染时间增加，LazyColumn 初始 item 数量大导致布局跳跃**。
- **错题数**仅传 int 无分页问题，但错题列表（`onNavigateToWrongAnswers`）若后续需要展示列表，需对接新数据源。
- 总体设备内存和海量数据是一次性获取 vs 按需加载。

### 1.3 决定性数据

| 组件 | 文件 | 列表维度 | 当前规模 | 规模风险 |
|---|---|---|---|---|
| 排行榜 | `LeaderboardView.kt` | `rankings: List<RankEntry>` | 随用户量增长 | **高** |
| 成就徽章 | `AchievementBadge.kt` | `achievements: List<…>` | ~8 条固定枚举 | 低 |
| 知识节点 | `KnowledgeGraph.kt` | `knowledgeNodes: List<KnowledgeNode>` | ~30–50 条/科目 | 中 |

---

## 二、分页方案选型

### 2.1 方案 A：Paging 3 库

**引入**: `androidx.paging:paging-compose`（当前未引入，在 `build.gradle.kts` 和 `libs.versions.toml` 需各加一处）

**优点**:
- Compose 原生支持（`collectAsLazyPagingItems`），代码简洁
- 支持 `PagingData` Flow，天然适配现有 Repository Flow 架构
- 内置缓存（`PagingCache`），支持位置持久化 & 重组合
- 与 Hilt/Compose 生态完美融合

**缺点**:
- 首次引入需改动 Multiple 层，并增加认知负担
- 集成成本高于手动分页

**适用场景**: 长期维护、有持续扩展预期的列表

### 2.2 方案 B：手动分页（suspend + pageSize）

**优点**:
- 零外部依赖，改造成本最低
- 代码透明易维护

**缺点**:
- 状态管理（加载中/加载更多/列表替换）需自行实现
- 位置持久化和重组合缺失
- 针对 LazyColumn 需自行管理分页参数

### 2.3 选型结论

**选择方案 A（Paging 3）**，理由：
1. 排行榜是唯一有持续增长压力的列表，Paging 3 一次性解决并在其他页面复用
2. 项目已有 Compose + Hilt + Flow 体系，Paging 3 可以自然融入
3. 当前中量架构（版本 ~30），首次引入最佳时机，晚了需重写
4. **首次版本 v30 仅覆盖排行榜（Leaderboard）分页**，其他列表暂不动，实施风险可控

---

## 三、API 设计（Paging 3 方案）

### 3.1 Repository 层

```kotlin
// GamificationRepository (新方法)
fun observeLeaderboardPaging(
    type: LeaderboardType = LeaderboardType.GLOBAL,
    pageSize: Int = 20
): Flow<PagingData<RankEntry>>

// 保留旧方法（后续内部重定向）
fun observeLeaderboard(type: LeaderboardType = LeaderboardType.GLOBAL): Flow<List<RankEntry>>
```

### 3.2 Manager/Engine 层

- `GamificationEngine` 新增 `getLeaderboardPaging(type, pageSize)`，
  内部基于 `Pager(PagingConfig(pageSize)) { leaderboardPagingSource(type) }`

### 3.3 ViewModel 层

- 通过 `Pager.createPagingSourceFactory()` 等卸载分页逻辑到 Engine/Repository
- ViewModel 暴露 `Flow<PagingData<RankEntry>>` 给 Compose
- 随 UI 剩余分页状态：`loadState`: LoadState.NotLoading/Loading/Error

### 3.4 UI 层

- `LeaderboardView` 参数改为 `rankings: LazyPagingItems<RankEntry>`
- 原有 `isLoading` 状态从 `LazyPagingItems.loadState` 派生
- 使用 `items(lazyPagingItems) { ... }` 渲染

---

## 四、影响文件清单

| 层级 | 文件 | 修改类型 | 说明 |
|---|---|---|---|
| 依赖 | `android/gradle/libs.versions.toml` | **新增** | 添加 paging 版本 |
| 依赖 | `android/app/build.gradle.kts` | **新增** | 添加 Paging 3 + Compose artifact |
| Domain | `GamificationRepository` (interface) | **扩展** | 新增 `observeLeaderboardPaging` |
| Data | `GamificationRepositoryImpl.kt` | **修改** | 新方法实现，保留旧方法 |
| Domain | `GamificationLeaderboardPagingSource` | **新增** | Pager 数据源 |
| Domain | `domain/engine/GamificationEngine.kt` | **扩展** | 新增 `getLeaderboardPaging` |
| Domain | `LeaderboardModels.kt` | **不变** | `RankEntry` 不需要改造 |
| Data | Cloud API 需支持 page/pageSize | **可能** | 后端 API 确认是否支持分页参数 |
| UI | `LeaderboardView.kt` | **修改** | `List<RankEntry>` → `LazyPagingItems<RankEntry>` |
| UI | `DashboardViewModel.kt` | **修改** | 暴露分页 Flow |
| UI | `DashboardScreen.kt` | **微调** | 传参略微调整 |
| DI | 如用 Hilt 注入 Pager | **待评估** | 视 Engine 改造范围 |

---

## 五、工作步骤分解

### Step 1 — 依赖引入
1. 在 `libs.versions.toml` `[versions]` 添加 `paging = "3.2.1"`
2. 在 `[libraries]` 添加 `paging-compose` artifact
3. 在 `build.gradle.kts` `dependencies` 添加 `implementation(libs.paging.compose)`

### Step 2 — API 与数据源设计
1. 在 `GamificationRepository` 接口声明 `observeLeaderboardPaging`
2. 在 `GamificationEngine` 实现 `getLeaderboardPaging` 方法：
   - 使用 `Pager(PagingConfig(pageSize = 20, enablePlaceholders = false))`
   - 实现 `PagingSource.load()` 分页逻辑
3. `GamificationRepositoryImpl` 实现接口

### Step 3 — ViewModel 改造
1. `DashboardViewModel` 持有 `observeLeaderboardPaging` 的 Flow
2. 将 `rankings: List<RankEntry>` 改为 `Flow<PagingData<RankEntry>>`
3. Compose 内通过 `collectAsLazyPagingItems()` 收集

### Step 4 — UI 改造
1. `LeaderboardView` 接受 `LazyPagingItems<RankEntry>` 参数
2. 使用 `items(lazyPagingItems, key = ...)` 替换 `items(rankings, key = ...)`
3. `LoadingState` 展示用 `item.loadState` 替代原 `isLoading` 参数

### Step 5 — 回归测试
1. 验证排行榜分页加载（前 20 条，滚动到底部是否加载更多）
2. 验证空状态、加载错误态
3. 验证与 `DashboardScreen` 整体展示兼容

---

## 六、风险与待确认事项

| 风险 | 级别 | 说明 |
|---|---|---|
| 后端 API 不支持分页参数 | 🔴 高 | 若 `getLeaderboard` 无 page 参数，Paging 3 需本地缓存全量再分页，需调整方案 |
| API Key 缺失导致环境配置 | 🟡 中 | Worker/CFO 确认 `.env` 和 Secrets 已配置 |
| Hilt KSP 生成代码出错 | 🟡 中 | Pager 注入需注解验证 |
| AchievementGrid FlowRow 分页无意义 | 🟢 低 | 成就固定 ~8 条，`v30` 不改动 |

---

## 七、已完成文件

| 文件 | 路径 |
|---|---|
| 本规划文件 | `docs/v30_plan.md` |

---

## 八、下一步

**PM 完成，等待 CEO 分配 CFO（coder）执行 Step 1–5。**
