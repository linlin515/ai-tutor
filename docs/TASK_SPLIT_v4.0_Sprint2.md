# AI 学伴 v4.0 Sprint 2 — 双 Coder 任务包拆分

> **文档版本**: v1.0
> **日期**: 2026-05-26
> **负责人**: PM
> **基准文档**: PRD_v4.0_Sprint2.md, ARCH_v4.0_Sprint2.md, ARCH_v4.0_Sprint1.md
> **拆分原则**: Coder (Android 主攻) + Coder-2 (后端主攻) 并行开发

---

## 目录

1. [Sprint 2 总览](#1-sprint-2-总览)
2. [依赖关系与联调接口](#2-依赖关系与联调接口)
3. [Coder 任务包 (Android 主攻)](#3-coder-任务包-android-主攻)
   - 3.1 离线模式 Android 端
   - 3.2 学习报告导出 Android 端
   - 3.3 Flashcard 抽认卡 Android 端
   - 3.4 TTS 替换 Android 端
4. [Coder-2 任务包 (后端主攻)](#4-coder-2-任务包-后端主攻)
   - 4.1 离线模式 后端 sync API
   - 4.2 学习报告导出 后端 PDF/CSV
   - 4.3 TTS 替换 后端
   - 4.4 Flashcard 后端 API
5. [时间线与里程碑](#5-时间线与里程碑)

---

## 1. Sprint 2 总览

### 1.1 模块与工作量分配

| 模块 | 优先级 | Coder (Android) | Coder-2 (后端) | 总计 |
|:-----|:------:|:---------------:|:--------------:|:----:|
| P1-1 离线模式 | P1 | 4-5 人日 | 1-2 人日 | 5-7 |
| P1-2 学习报告导出 | P1 | 1 人日 | 1-2 人日 | 2-3 |
| P2-1 TTS 替换 | P2 | 1 人日 | 2 人日 | 3 |
| P2-2 Flashcard 抽认卡 | P2 | 2-3 人日 | 1-2 人日 | 3-5 |
| **总计** | | **8-10 人日** | **5-8 人日** | **13-18** |

### 1.2 人力建议

- **Coder（Android）**: 1 人，专注 Room 迁移 + 离线缓存 UI + Flashcard UI + 报告导出 UI + TTS 客户端
- **Coder-2（后端）**: 1 人，专注 4 套后端 API（sync / report / tts / flashcard）
- **联调窗口**: Day 5-6（Coder-2 先完成后端 API，Coder 第 3 天开始对接）
- **建议时长**: 2 周（含测试、联调、Bug fix）

---

## 2. 依赖关系与联调接口

### 2.1 Coder → Coder-2 依赖

| 依赖项 | 说明 | 建议完成时间 |
|:-------|:-----|:------------|
| `GET /api/v1/sync?since=timestamp` | Coder 的 SyncRepository 需要此接口拉取增量数据 | Coder-2 Day 1-2 |
| `POST /api/v1/sync` | Coder 的 SyncWorker 需要推送离线操作 | Coder-2 Day 1-2 |
| `POST /api/v1/report/export` | Coder 的 ReportRepository 需要下载 PDF/CSV | Coder-2 Day 2-3 |
| `GET /api/v1/flashcard/today` | Coder 的 FlashcardRepository 需要获取今日卡片 | Coder-2 Day 2-3 |
| `POST /api/v1/flashcard/review` | Coder 的 FlashcardViewModel 需要提交评价 | Coder-2 Day 2-3 |
| `POST /api/v1/flashcard/sync` | Coder 的 SyncFlashcardsUseCase 需要批量同步 | Coder-2 Day 2-3 |
| `GET /api/v1/tts?provider=edge` | Coder 的 CloudTtsEngine 需要调用新 TTS 端点 | Coder-2 Day 3-4 |

### 2.2 联调 Mock 策略

Coder 可在 Coder-2 API 未就绪时，先用以下方式自测：

- **Sync API**: 本地建 Mock Retrofit 接口，返回硬编码 JSON（从 PRD 附录复制）
- **Report API**: 先实现下载 + ShareSheet 流程，文件来源用本地测试 PDF
- **Flashcard API**: 本地 Room 全量生成模拟卡片，绕过后端
- **TTS API**: 先走本地 LocalTtsEngine 降级路径

### 2.3 Git 分支策略

```
main
└── sprint-2
    ├── coder/   ← Coder 工作分支
    └── coder-2/ ← Coder-2 工作分支
```

联调时合入 `sprint-2` 分支。**Room Migration 文件** (`Migration1To2.kt`) 由 Coder 维护，Coder-2 不修改 Android 代码。

---

## 3. Coder 任务包 (Android 主攻)

### 3.1 离线模式 Android 端

**预估**: 4-5 人日
**优先级**: P1
**说明**: 实现本地 Room 缓存 + 离线浏览 + 网络恢复自动同步的完整客户端链路

#### 3.1.1 Room Entity + DAO（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/local/entity/CachedQuestionEntity.kt` | + | 缓存题目实体（字段见 ARCH 2.5.2） |
| `app/src/main/java/.../data/local/entity/CachedWrongAnswerEntity.kt` | + | 缓存错题实体 |
| `app/src/main/java/.../data/local/entity/CachedConversationEntity.kt` | + | 缓存对话实体（messages 存完整 JSON） |
| `app/src/main/java/.../data/local/entity/OfflineActionEntity.kt` | + | 离线操作日志实体（type/targetId/payload） |
| `app/src/main/java/.../data/local/dao/CachedQuestionDao.kt` | + | `getAll(limit)`, `upsertAll()`, `deleteByIds()`, `deleteAll()`, `count()`, `dataSizeBytes()` |
| `app/src/main/java/.../data/local/dao/CachedWrongAnswerDao.kt` | + | 同上模式 |
| `app/src/main/java/.../data/local/dao/CachedConversationDao.kt` | + | 同上模式 |
| `app/src/main/java/.../data/local/dao/OfflineActionDao.kt` | + | 增删查、按时间排序、清空 |

#### 3.1.2 Room DB Migration（修改 + 新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/local/db/AiTutorDatabase.kt` | ~ | 所有新 Entity 加入 entities 列表；version 1→2；注册 MIGRATION_1_2 |
| `app/src/main/java/.../data/local/db/Migration1To2.kt` | + | 独立的 Migration object，包含全部 CREATE TABLE（见 ARCH 2.5.1） |
| `app/src/main/java/.../data/local/CacheManager.kt` | + | 缓存大小统计 + LRU 淘汰（每表上限见 ARCH 2.3.3） |

**⚠️ 关键约束**: 
- 必须使用 `addMigrations(MIGRATION_1_2)`，不可使用 `fallbackToDestructiveMigration()`
- 修改 `AiTutorDatabase.kt` 时同步添加 Flashcard 端 Entity（见 3.3.1），统一完成 v1→v2 Migration
- Migration SQL 中的 `SyncMetadata` 表也在此统一创建

#### 3.1.3 DI 模块（修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../di/DatabaseModule.kt` | ~ | 提供新 DAO 实例（CachedQuestionDao, CachedWrongAnswerDao, CachedConversationDao, OfflineActionDao） |

#### 3.1.4 Repository 层（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../domain/repository/OfflineRepository.kt` | + | 接口: `getCachedQuestions()`, `getCachedWrongAnswers()`, `getCachedConversations()`, `getCacheSizeInfo()`, `clearAllCache()` |
| `app/src/main/java/.../data/repository/OfflineRepositoryImpl.kt` | + | Cache-first → Network-fallback 实现（见 ARCH 2.3.1 数据流） |
| `app/src/main/java/.../domain/repository/SyncRepository.kt` | + | 接口: `pullIncremental(since)`, `pushOfflineActions()`, `syncAll()`, `getLastSyncTimestamp()`, `recordOfflineAction()` |
| `app/src/main/java/.../data/repository/SyncRepositoryImpl.kt` | + | 调用 SyncApi + Room 事务写入（见 ARCH 2.3.2 同步流） |
| `app/src/main/java/.../data/remote/api/SyncApi.kt` | + | Retrofit 接口: `@GET("sync")` + `@POST("sync")` |
| `app/src/main/java/.../data/remote/dto/SyncDtos.kt` | + | SyncRequest, SyncResponse, SyncAction DTO |

#### 3.1.5 UseCase 层（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../domain/usecase/sync/SyncDataUseCase.kt` | + | 网络恢复后自动同步（pull + push + 更新 SyncMetadata） |
| `app/src/main/java/.../domain/usecase/cache/GetCachedQuestionsUseCase.kt` | + | 从 Room 获取缓存题目，包裹 Resource |
| `app/src/main/java/.../domain/usecase/cache/GetCachedWrongAnswersUseCase.kt` | + | 同上 |
| `app/src/main/java/.../domain/usecase/cache/GetCachedConversationsUseCase.kt` | + | 同上 |
| `app/src/main/java/.../domain/usecase/cache/ClearCacheUseCase.kt` | + | 调用 CacheManager.clearAllCache() |
| `app/src/main/java/.../domain/usecase/cache/GetCacheSizeUseCase.kt` | + | 调用 CacheManager.getCacheSizeInfo() |

#### 3.1.6 UI 层（新建 + 修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../ui/offline/OfflineBanner.kt` | + | 顶部黄色/橙色 Banner Composable："你当前处于离线模式" |
| `app/src/main/java/.../ui/offline/OfflineViewModel.kt` | + | 管理 isOnline 状态（监听 NetworkMonitor）+ Banner 显隐 |
| `app/src/main/java/.../ui/settings/CacheManagementSection.kt` | + | 缓存大小展示 + 各分类明细 + "清除缓存"按钮 |
| `app/src/main/java/.../ui/settings/SettingsScreen.kt` | ~ | 添加 "缓存管理" 区域入口 |
| `app/src/main/java/.../ui/settings/SettingsViewModel.kt` | ~ | 缓存状态管理（大小、清除操作状态） |

#### 3.1.7 基础设施（修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../util/NetworkMonitor.kt` | ~ | 扩展 `getOnlineSnapshot()` 快照方法 + `connectivityState` 细化状态（ONLINE/OFFLINE/WIFI/CELLULAR/VPN） |

#### 3.1.8 SyncWorker（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/worker/SyncWorker.kt` | + | WorkManager CoroutineWorker：网络恢复后触发 syncAll()，需注册到 AndroidManifest |

#### 3.1.9 验收要点

- [ ] 飞行模式下打开题目列表/对话历史/错题本，显示缓存数据
- [ ] 无缓存数据时显示空状态提示
- [ ] 顶部离线 Banner 在断网时显示，恢复网络后自动消失
- [ ] 设置页缓存大小显示准确，"清除缓存"后归零
- [ ] SyncWorker 在网络恢复后自动触发增量同步
- [ ] Room Migration v1→v2 不丢失已有数据（单独写单元测试验证）

---

### 3.2 学习报告导出 Android 端

**预估**: 1 人日
**优先级**: P1
**说明**: 导出范围选择 + 触发后端生成 + 下载到 Downloads 目录 + ShareSheet 分享

#### 3.2.1 Repository + API（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/remote/api/ReportApi.kt` | + | Retrofit `@Streaming @POST("report/export")` 返回 ResponseBody |
| `app/src/main/java/.../data/remote/dto/ReportDtos.kt` | + | ExportRequest DTO（format, period, subject, includeCharts, startDate, endDate） |
| `app/src/main/java/.../domain/repository/ReportRepository.kt` | + | 接口: `exportReport(request)`, `shareFile(context, file)`, `getCachedReportFile(request)` |
| `app/src/main/java/.../data/repository/ReportRepositoryImpl.kt` | + | 下载 → 写入 Downloads/StudyReports/ → 返回 File |

#### 3.2.2 UI + ViewModel（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../ui/report/ExportReportScreen.kt` | + | 范围选择（今日/本周/本月/全部）+ 格式选择（PDF/CSV）+ 导出按钮 + 进度显示 |
| `app/src/main/java/.../ui/report/ExportReportViewModel.kt` | + | ExportState 状态机: IDLE → GENERATING → DOWNLOADING → SHARING → DONE / ERROR |

#### 3.2.3 DI + 路由（新建 + 修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../di/ReportModule.kt` | + | Hilt 模块，提供 ReportRepository 绑定 |
| `app/src/main/java/.../ui/navigation/Routes.kt` | ~ | 添加报告导出路由 `report_export` |

#### 3.2.4 FileProvider 配置

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/AndroidManifest.xml` | ~ | 注册 FileProvider（若尚未注册） |
| `app/src/main/res/xml/file_paths.xml` | ~ | 添加 Downloads 目录路径 |

#### 3.2.5 验收要点

- [ ] 点击"导出"后调用 POST /api/v1/report/export
- [ ] 下载完成后自动弹出系统 ShareSheet（微信/邮件/保存到文件）
- [ ] 进度提示：生成中 → 下载中 → 分享 → 完成/失败
- [ ] 缓存检查：同参数不重复下载
- [ ] 配额不足时显示 429 错误提示

---

### 3.3 Flashcard 抽认卡 Android 端

**预估**: 2-3 人日
**优先级**: P2
**说明**: 左右划动复习卡片 UI + 本地 Room 缓存 + mastery 更新 + 积分同步

#### 3.3.1 Room Entity + DAO（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/local/entity/FlashcardEntity.kt` | + | `sourceId` UNIQUE 索引（见 ARCH 5.4.1） |
| `app/src/main/java/.../data/local/entity/FlashcardReviewLogEntity.kt` | + | 复习日志（含 `synced` 标记） |
| `app/src/main/java/.../data/local/dao/FlashcardDao.kt` | + | `getTodayCards()`, `updateMastery()`, `insertReviewLog()`, `getUnsyncedReviewLogs()`, `markLogsSynced()`, `countPendingCards()`, `countReviewedToday()` |

**注意**: AiTutorDatabase.kt 的 Entity 添加和 Migration 已在 3.1.2 统一完成，此处只需新增 Entity 类和 DAO。

#### 3.3.2 Repository + API（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/remote/api/FlashcardApi.kt` | + | `GET flashcard/today`, `POST flashcard/review`, `POST flashcard/sync` |
| `app/src/main/java/.../data/remote/dto/FlashcardDtos.kt` | + | TodayCardsResponse, ReviewRequest/Response, BatchReviewRequest/Response |
| `app/src/main/java/.../domain/repository/FlashcardRepository.kt` | + | 接口: `getTodayCards()`, `reviewCard()`, `syncOfflineReviews()` |
| `app/src/main/java/.../data/repository/FlashcardRepositoryImpl.kt` | + | 联网→API; 离线→Room; review 时本地更新 mastery + 记录日志 + 调后端 |

#### 3.3.3 领域模型 + UseCase（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../domain/model/Flashcard.kt` | + | 卡片领域模型（含 FlashcardFront, FlashcardBack） |
| `app/src/main/java/.../domain/model/FlashcardReview.kt` | + | 复习评价模型 |
| `app/src/main/java/.../domain/usecase/flashcard/GetTodayCardsUseCase.kt` | + | 获取今日卡片（本地优先 → 网络补充） |
| `app/src/main/java/.../domain/usecase/flashcard/ReviewCardUseCase.kt` | + | 评价卡片（更新 mastery + 记录日志 + 同步后端 + 积分） |
| `app/src/main/java/.../domain/usecase/flashcard/SyncFlashcardsUseCase.kt` | + | 批量同步离线评价到后端 |

#### 3.3.4 UI + ViewModel（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../ui/flashcard/FlashcardScreen.kt` | + | 主页面：顶部进度条 + SwipeToDismiss 卡片栈 + 底部完成提示 |
| `app/src/main/java/.../ui/flashcard/FlashcardFront.kt` | + | 卡片正面 Composable：题干 + 选项 + 掌握度进度条 |
| `app/src/main/java/.../ui/flashcard/FlashcardBack.kt` | + | 卡片反面 Composable：正确答案（绿色）+ 用户答案（红色）+ 解析 + 知识点标签 |
| `app/src/main/java/.../ui/flashcard/FlashcardViewModel.kt` | + | 状态管理: `cards`, `todayProgress`, `currentCardIndex`, `reviewCard()`, `loadTodayCards()` |

#### 3.3.5 路由导航（修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../ui/navigation/Routes.kt` | ~ | 添加 Flashcard 路由 |
| `app/src/main/java/.../ui/navigation/AppNavGraph.kt` | ~ | 注册 FlashcardScreen |

#### 3.3.6 验收要点

- [ ] 初始加载展示今日卡片（默认 10 张），显示进度 "0/10"
- [ ] 左划 → "不熟练" → mastery 下降；右划 → "已掌握" → mastery 上升
- [ ] 掌握度 ≥ 0.8 → 自动归档，从复习队列移除
- [ ] 完成全部卡片后显示 "今日复习已完成！🎉"
- [ ] 每次评价后飘 "+2 积分" 动画
- [ ] 离线模式也能复习，评价记录暂存本地，联网后批量同步
- [ ] 每日上限可配置（默认 10 张），单卡每日最多复习 3 次

---

### 3.4 TTS 替换 Android 端

**预估**: 1 人日
**优先级**: P2
**说明**: CloudTtsEngine + LocalTtsEngine + provider 切换 + 三级降级

#### 3.4.1 TTS 引擎（新建）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/media/CloudTtsEngine.kt` | + | 云端 TTS：支持 provider 参数 + 三级降级（Edge→OpenAI→本地） |
| `app/src/main/java/.../data/media/LocalTtsEngine.kt` | + | 本地 TTS 封装（基于 android.speech.tts.TextToSpeech） |

#### 3.4.2 Repository + API 扩展（修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../data/remote/api/AiTutorApi.kt` | ~ | 添加 `@GET("tts") suspend fun tts(...): Response<ResponseBody>` |
| `app/src/main/java/.../domain/repository/VoiceRepository.kt` | ~ | 添加 `setTtsProvider(provider)` / `getTtsProvider(): Flow<String>` |
| `app/src/main/java/.../data/repository/VoiceRepositoryImpl.kt` | ~ | 集成 CloudTtsEngine + LocalTtsEngine，实现 provider 切换和降级链路 |

#### 3.4.3 DI + UI（修改）

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/src/main/java/.../di/SpeechModule.kt` | ~ | 提供 CloudTtsEngine / LocalTtsEngine |
| `app/src/main/java/.../ui/settings/SettingsScreen.kt` | ~ | 添加 TTS Provider 选择器（Edge / OpenAI / 系统默认） |
| `app/src/main/java/.../ui/settings/SettingsViewModel.kt` | ~ | TTS provider 选择状态管理 |

#### 3.4.4 验收要点

- [ ] GET /api/v1/tts?provider=edge 调用成功时播放 Edge TTS 音频
- [ ] Edge 不可用时自动降级到 OpenAI（日志记录 "TTS降级"）
- [ ] OpenAI 也不可用时降级到本地 TextToSpeech（Toast 提示）
- [ ] 设置页可切换 TTS Provider，选择后持久化
- [ ] 原有 POST /v1/audio/speech 路径不受影响

---

## 4. Coder-2 任务包 (后端主攻)

### 4.1 离线模式 后端 sync API

**预估**: 1-2 人日
**优先级**: P1
**说明**: 提供增量同步接口，支持拉取增量数据和推送离线操作

#### 4.1.1 文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `backend/app/schemas/sync.py` | + | SyncRequest, SyncAction, SyncResponse Pydantic 模型（见 ARCH 2.4.2） |
| `backend/app/services/sync_service.py` | + | 增量查询逻辑 + 冲突检测 + 操作回放 |
| `backend/app/routers/sync.py` | + | `GET /api/v1/sync?since=timestamp` + `POST /api/v1/sync` |
| `backend/app/main.py` | ~ | 注册 sync router |

#### 4.1.2 API 端点详情

**GET /api/v1/sync?since={timestamp}**
- 返回: `updated_questions`, `updated_conversations`, `updated_wrong_answers`, `deleted_ids`, `sync_timestamp`
- 查询: quiz_questions, conversations, wrong_answers 表中 updated_at > since 的记录
- 安全: JWT 认证，仅返回当前用户数据

**POST /api/v1/sync**
- Body: `{ actions: [{ type, target_id, data, timestamp }] }`
- 处理: 逐条回放操作，更新对应表的 mastery_score / review_count
- 幂等: (type, target_id, timestamp) 组合 1 分钟内去重
- 冲突: 服务端优先，冲突详情记录在 conflicts 列表

#### 4.1.3 验收要点

- [ ] GET /sync?since=... 返回增量数据，空数据时返回空列表
- [ ] POST /sync 正确处理离线操作，返回 synced_actions 计数
- [ ] 仅返回当前用户的数据（JWT 过滤）
- [ ] 冲突检测逻辑正确，不丢失服务端更新

---

### 4.2 学习报告导出 后端 PDF/CSV

**预估**: 1-2 人日
**优先级**: P1
**说明**: ReportLab 生成 PDF（含 Matplotlib 图表）+ CSV 导出（UTF-8 BOM）

#### 4.2.1 文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `backend/app/schemas/report.py` | + | ExportRequest Pydantic 模型 |
| `backend/app/services/report_service.py` | + | PDF 生成（ReportLab 7 章节）+ CSV 生成 |
| `backend/app/services/report_charts.py` | + | Matplotlib 图表: 每日时长折线图、正确率曲线、雷达图、错题分布饼图 |
| `backend/app/routers/report.py` | + | `POST /api/v1/report/export` |
| `backend/app/main.py` | ~ | 注册 report router |
| `backend/requirements.txt` | ~ | 添加 `reportlab`, `matplotlib` |
| `backend/Dockerfile` 或 `docker-compose.yml` | ~ | 预装 Noto Sans CJK 中文字体（确保 PDF 中文显示） |

#### 4.2.2 配额控制

- 免费用户每日 3 次，premium 用户 20 次
- 用 Redis 或 daily_quotas 表计数: `report:export:count:{user_id}:{date}`
- TTL: 86400s
- 超配额返回 429 + message

#### 4.2.3 PDF 报告章节

1. **报告概要**: 日期范围、总学习时长、解题总数、正确率
2. **每日学习时长柱状图**: Matplotlib 生成
3. **正确率趋势曲线**: Matplotlib 生成
4. **知识点掌握度雷达图**: 按学科分类
5. **错题分布饼图**: 按学科/知识点
6. **薄弱知识点列表**: 掌握度 < 0.6
7. **学习建议**: 基于薄弱点自动生成

#### 4.2.4 CSV 格式

- 编码: UTF-8 BOM（兼容 Excel）
- 列: `date, subject, topic, question_content, correct_answer, user_answer, is_correct, time_spent_seconds, mastery_score`
- 限制: 最多返回 365 天数据

#### 4.2.5 验收要点

- [ ] PDF 包含全部 7 章节，中文显示正常
- [ ] CSV 可被 Excel 正常打开（UTF-8 BOM）
- [ ] 配额检查正确：免费 3 次/日，premium 20 次/日，超额返回 429
- [ ] 30 秒超时返回 504，文件大于 10MB 时压缩图表质量
- [ ] 无数据时返回空报告提示

---

### 4.3 TTS 替换 后端

**预估**: 2 人日
**优先级**: P2
**说明**: Provider 抽象层 + Edge TTS 集成 + OpenAI 适配 + 配置化切换

#### 4.3.1 文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `backend/app/services/tts_base.py` | + | `TTSProvider` 抽象基类: `synthesize()`, `get_supported_voices()`, `name()` |
| `backend/app/services/tts_edge.py` | + | EdgeTTSProvider: 通过 WebSocket 调用微软 Edge TTS API |
| `backend/app/services/tts_openai.py` | + | OpenAITTSProvider: 封装现有 OpenAI TTS 调用（从 audio router 提取） |
| `backend/app/services/tts_factory.py` | + | `TTSProviderFactory`: 注册 + 获取 provider + 默认 provider |
| `backend/app/schemas/audio.py` | ~ | 添加 `TtsQueryParams`（text, provider, voice, speed） |
| `backend/app/routers/audio.py` | ~ | 新增 `GET /api/v1/tts` 端点（保留原有 POST /v1/audio/speech） |
| `backend/app/config.py` | ~ | 添加 TTS 配置: `tts_default_provider`, `tts_edge_enabled`, `tts_edge_voice_cn`, `tts_edge_voice_en` |
| `backend/requirements.txt` | ~ | 添加 `websockets`（Edge TTS WebSocket 协议需要） |

#### 4.3.2 TTS Provider 架构

```
TTSProvider (ABC)
    ├── EdgeTTSProvider  — 微软 Edge TTS（免费，默认）
    ├── OpenAITTSProvider — OpenAI TTS（付费，备选）
    └── (预留) FishSpeechProvider — 自部署 TTS
```

- 通过 `TTSProviderFactory` 工厂获取实例
- 默认 provider 由环境变量 `tts_default_provider` 控制（默认 `edge`）

#### 4.3.3 GET /api/v1/tts 端点

- 参数: `text`（必填，≤1024 字符）, `provider`, `voice`, `speed`（0.5-2.0）
- 响应: `audio/mpeg` 二进制流 + 响应头 `X-TTS-Provider`
- 降级: 后端不做降级（降级由 Android 端 CloudTtsEngine 实现）
- 长文本: 分段处理，每段 ≤1024 字符，连续返回

#### 4.3.4 验收要点

- [ ] GET /api/v1/tts?text=你好&provider=edge 返回可播放的 MP3
- [ ] provider 切换生效（edge ↔ openai 音色不同）
- [ ] 无效 provider 回退到默认 provider
- [ ] 原有 POST /v1/audio/speech 完全兼容
- [ ] 响应头包含 X-TTS-Provider
- [ ] 文本超过 1024 字符时分段处理

---

### 4.4 Flashcard 后端 API

**预估**: 1-2 人日
**优先级**: P2
**说明**: 基于 wrong_answers 表生成每日复习卡片 + 评价更新 mastery + 积分发放

#### 4.4.1 文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `backend/app/schemas/flashcard.py` | + | FlashcardOut, TodayCardsResponse, ReviewRequest/Response, BatchReviewRequest/Response |
| `backend/app/services/flashcard_service.py` | + | `get_today_cards()`, `review_card()`, `archive_old_cards()` |
| `backend/app/routers/flashcard.py` | + | `GET /api/v1/flashcard/today`, `POST /api/v1/flashcard/review`, `POST /api/v1/flashcard/sync` |
| `backend/app/main.py` | ~ | 注册 flashcard router |
| `backend/app/models/wrong_answer.py` | ~ | 添加 `is_archived` 字段（Boolean, default False） |

#### 4.4.2 API 端点详情

**GET /api/v1/flashcard/today?limit=10**
- 查询: wrong_answers 表，mastery_score < 0.8 AND is_archived = false
- 排序: last_reviewed_at ASC NULLS FIRST（未复习过的优先）
- 返回: `cards[]`, `total_today`, `completed_today`, `daily_limit`

**POST /api/v1/flashcard/review**
- Body: `{ card_id, judgment: "mastered"|"unfamiliar", time_spent_seconds }`
- mastery 算法:
  - `mastered` → `min(1.0, old + (1.0 - old) * 0.3)`
  - `unfamiliar` → `max(0.0, old - 0.2)`
  - ≥ 0.8 → `is_archived = true`
- 积分: 每次 review 发放 +2 分（调用 game/sync/score 或直接在事务中更新 user_scores）
- 返回: `new_mastery_score`, `archived`, `earned_points: 2`, `total_points`

**POST /api/v1/flashcard/sync**
- Body: `{ reviews: [{ card_id, judgment, timestamp }] }`
- 幂等: 相同 card_id + timestamp 1 分钟内不重复处理
- 返回: `synced` 计数 + `results[]`

#### 4.4.3 验收要点

- [ ] GET /flashcard/today 返回 mastery < 0.8 且未归档的错题
- [ ] POST /flashcard/review 正确计算新 mastery，≥ 0.8 自动归档
- [ ] review 成功后积分 +2，total_points 累计正确
- [ ] POST /flashcard/sync 批量处理，幂等正确
- [ ] 错题不足 limit 时返回实际数量
- [ ] 所有卡均已归档时返回空列表 + 提示信息

---

### 4.5 后端公共修改

所有 Coder-2 子任务共享的修改：

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `backend/app/main.py` | ~ | 注册 sync router + report router + flashcard router（共 3 个新 router） |
| `backend/requirements.txt` | ~ | 添加 `reportlab`, `matplotlib`, `websockets` |

---

## 5. 时间线与里程碑

### 5.1 建议排期（2 周 = 10 工作日）

```
Day 1-2:  Coder  Room Entity + DAO + Migration (3.1.1-3.1.2)
           Coder-2  Sync API (4.1) + Flashcard API (4.4)

Day 3-4:  Coder  Repository + UseCase + Offline UI (3.1.3-3.1.8)
                  Flashcard UI 开始 (3.3)
           Coder-2  Report 后端 (4.2) + TTS 后端 (4.3)

Day 5-6:  Coder  Flashcard UI 完成 (3.3) + Report UI (3.2) + TTS Android (3.4)
           Coder-2  TTS 后端完成 (4.3) + 联调准备
           ★ 联调窗口 ★

Day 7-8:  联调 + Bug fix
           Coder  对接真实后端 API（替换 Mock）
           Coder-2  修复联调发现的问题

Day 9-10: 测试 + 回归 + 文档
           Android Unit Test 覆盖新增 DAO/UseCase
           pytest 覆盖新增后端路由
           飞行模式端到端验证
```

### 5.2 里程碑

| 里程碑 | 时间 | 交付物 |
|:-------|:-----|:-------|
| M1: Room v1→v2 迁移完成 | Day 2 | Migration1To2.kt + AiTutorDatabase.kt 修改 |
| M2: 后端 7 个 API 全上线 | Day 5 | sync(2), report(1), tts(1), flashcard(3) |
| M3: 端到端联调完成 | Day 7 | Coder + Coder-2 合入 sprint-2 分支 |
| M4: 全量验收通过 | Day 10 | 飞行模式验证 + QA 回归测试 |

---

> **文档结束** — 本拆分文档由 PM 基于 PRD 和 ARCH 编写，Coder 和 Coder-2 各司其职，并行开发。
> Coder 拿到本包即可开始 Android 端开发；Coder-2 拿到本包即可开始后端 API 开发。
> 联调期间建议每日站会同步进度，及时暴露阻塞问题。
