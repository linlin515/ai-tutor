# v2.5 核心功能补齐 — PM 规划

> PM: Hermes Agent | 日期: 2026-05-18 | 版本: v2.5

---

## 一、现状核实

### CEO 指令来源

CEO 功能审计文档 `docs/AUDIT_FEATURES_MAY18.md` 明确 v2.5 需要补齐 4 个 🔴P0 功能：

| # | 功能 | 预估工时 |
|:--|:-----|:--------|
| 1 | 消息长按操作 | 1 天 |
| 2 | AI 回复反馈 👍👎 | 0.5 天 |
| 3 | 消息收藏夹 | 1.5 天 |
| 4 | 学习提醒推送 | 1 天 |

### 现有代码核实结果

| 组件 | 路径 | 状态 | 关键发现 |
|:-----|:-----|:-----|:---------|
| ChatMessage 数据模型 | `domain/model/ChatMessage.kt` | ✅ 存在 | 字段: id, conversationId, content, isUser, contentType, timestamp, status, metadata, agentStepType, toolName, toolQuery, toolResult。**缺少**: feedback 字段、isFavorite 字段 |
| MessageEntity (Room) | `data/local/entity/MessageEntity.kt` | ✅ 存在 | 字段: id, conversationId, content, isUser, contentType, timestamp, status, metadata。**缺少**: agent 字段存于 domain 层(metadata JSON)、**缺少** feedback/isFavorite 列 |
| MessageDao | `data/local/dao/MessageDao.kt` | ✅ 存在 | 已有: insert, update, updateStatus, getByConversationFlow, getByConversationPaged, getById, deleteByConversation, deleteById, searchByContent。**缺少**: updateFeedback, updateFavorite, getFavorites 查询 |
| ChatRepository 接口 | `domain/repository/ChatRepository.kt` | ✅ 存在 | 已有: insertMessage, updateMessageStatus, getMessagesByConversation, streamChat, streamChatWithEvents 等。**缺少**: updateMessageFeedback, toggleFavorite, getFavoriteMessages |
| ChatRepositoryImpl | `data/repository/ChatRepositoryImpl.kt` | ✅ 存在 | 实现完整, 已有 messageDao + conversationDao + API 集成 |
| NotificationChannels | `data/local/NotificationChannels.kt` | ✅ 存在 | 已定义 CHANNEL_MESSAGE / CHANNEL_REVIEW / CHANNEL_SYSTEM, AiTutorApp.onCreate() 中调用 create() |
| NotificationHelper | `data/local/NotificationHelper.kt` | ✅ 存在 | 已有 sendMessageNotification / sendReviewReminder / sendQuotaExceededNotification |
| SettingsScreen | `ui/settings/SettingsScreen.kt` | ✅ 存在 | 502 行，已含 Model/Temperature/Agent/Voice/Theme/Cache/Update 等区域。**缺少**: "每日学习提醒" 开关 |
| SettingsViewModel | `ui/settings/SettingsViewModel.kt` | ✅ 存在 | 管理 AppSettings + cache + language + agent + update。**缺少**: dailyReminder toggle 状态管理 |
| AppSettings | `domain/model/AppSettings.kt` | ✅ 存在 | 字段: modelId, temperature, topP, maxTokens, darkTheme, ttsSpeed, ttsVoice。**缺少**: dailyReminderEnabled, reminderTime |
| SettingsDataStore | `data/remote/datastore/SettingsDataStore.kt` | ✅ 存在 | 用 DataStore Preferences 持久化。**缺少**: reminder 相关 key |
| MessageBubble | `ui/chat/components/MessageBubble.kt` | ✅ 存在 | 194 行，有 onRetry / onSpeak / onImageLoadRetry 回调。**没有** long-press 手势、**没有** 👍👎 按钮 |
| ChatScreen | `ui/chat/ChatScreen.kt` | ✅ 存在 | 651 行，消息列表 351-401 行渲染 MessageBubble。**缺少**: long-press 菜单、反馈回调 |
| ChatUiState | `ui/chat/ChatUiState.kt` | ✅ 存在 | 42 行。**缺少**: feedbackTarget / favorite 相关状态 |
| ChatViewModel | `ui/chat/ChatViewModel.kt` | ✅ 存在 | 762 行。**缺少**: copyMessage, toggleFavorite, submitFeedback 方法 |
| ChatMapper | `data/mapper/ChatMapper.kt` | ✅ 存在 | MessageEntity ↔ ChatMessage 双向映射。**需要更新**: 新增字段映射 |
| AiTutorApp | `AiTutorApp.kt` | ✅ 存在 | onCreate 初始化 CrashHandler + NotificationChannels + AppLifecycleTracker。**需要添加**: WorkManager 调度 |
| Database | `data/local/db/AiTutorDatabase.kt` | ✅ 存在 | version=4, 12 entities。**需要**: migration v4→v5 |
| Routes | `ui/navigation/Routes.kt` | ✅ 存在 | 16 routes。**需要**: 新增 FAVORITES 路由 |
| AppNavGraph | `ui/navigation/AppNavGraph.kt` | ✅ 存在 | 注册全部路由。**需要**: 注册 FavoritesScreen |
| **WorkManager 依赖** | `app/build.gradle.kts` | ❌ **不存在** | 需要添加 `androidx.work:work-runtime-ktx` 依赖 |

---

## 二、4 个功能的文件清单

### F1: 消息长按操作

**改动说明**: MessageBubble 添加 `combinedClickable` 长按手势, 弹出 DropdownMenu (复制内容 / 收藏消息 / 对AI回答反馈)。复制直接调用系统 ClipboardManager; 收藏和反馈委托给 ChatViewModel。

| 文件 | 类型 | 改动内容 |
|:-----|:-----|:---------|
| `ui/chat/components/MessageBubble.kt` | ✏️ 修改 | 添加 long-press Handler + DropdownMenu (复制/收藏/反馈)，新增 `onCopy`, `onFavorite`, `onFeedback` 回调参数 |
| `ui/chat/ChatScreen.kt` | ✏️ 修改 | 消息 items 渲染处为 MessageBubble 传入 `onCopy`, `onFavorite`, `onFeedback` 回调，连接 ChatViewModel |
| `ui/chat/ChatViewModel.kt` | ✏️ 修改 | 新增 `copyMessage(content: String)`, `toggleFavorite(messageId: Long)`, `setFeedbackTarget(messageId: Long)` 方法 |
| `ui/chat/ChatUiState.kt` | ✏️ 修改 | 新增 `feedbackTargetMessageId: Long? = null` 用于标识当前待反馈消息（长按菜单触发） |

**与 F2/F3 关系**: F1 的菜单项 "收藏" 和 "反馈" 依赖 F2/F3 的底层实现（Repository 方法），但 UI 层可先行开发，留好回调即可。

---

### F2: AI 回复反馈 👍👎

**改动说明**: 每条 AI 消息下方加 👍👎 按钮，点击后将反馈记录到数据库。需要新增 feedback 字段到数据层。

| 文件 | 类型 | 改动内容 |
|:-----|:-----|:---------|
| `domain/model/ChatMessage.kt` | ✏️ 修改 | 新增 `val feedback: FeedbackType? = null` 字段；新增 enum `FeedbackType { POSITIVE, NEGATIVE }` |
| `data/local/entity/MessageEntity.kt` | ✏️ 修改 | 新增 `val feedback: String? = null` 列（存储 "POSITIVE"/"NEGATIVE"/null） |
| `data/local/dao/MessageDao.kt` | ✏️ 修改 | 新增 `@Query("UPDATE messages SET feedback = :feedback WHERE id = :id") suspend fun updateFeedback(id: Long, feedback: String?)` |
| `domain/repository/ChatRepository.kt` | ✏️ 修改 | 新增 `suspend fun updateMessageFeedback(id: Long, feedback: String?)` |
| `data/repository/ChatRepositoryImpl.kt` | ✏️ 修改 | 实现 `updateMessageFeedback()` → 调用 `messageDao.updateFeedback()` |
| `data/mapper/ChatMapper.kt` | ✏️ 修改 | `toDomain()` 解析 feedback 字符串为 FeedbackType; `toEntity()` 序列化 FeedbackType 为字符串 |
| `data/local/db/Migrations.kt` | ✏️ 修改 | 新增 MIGRATION_4_5: `ALTER TABLE messages ADD COLUMN feedback TEXT DEFAULT NULL` |
| `data/local/db/AiTutorDatabase.kt` | ✏️ 修改 | version 5, 添加 migration 4→5 |
| `ui/chat/components/MessageBubble.kt` | ✏️ 修改 | AI 消息底部添加 👍👎 按钮行，新增 `onThumbsUp: () -> Unit`, `onThumbsDown: () -> Unit` 回调 |
| `ui/chat/ChatScreen.kt` | ✏️ 修改 | MessageBubble 传入 `onThumbsUp`/`onThumbsDown` → ChatViewModel |
| `ui/chat/ChatViewModel.kt` | ✏️ 修改 | 新增 `submitFeedback(messageId: Long, positive: Boolean)` 方法, 调用 `chatRepository.updateMessageFeedback()` |

---

### F3: 消息收藏

**改动说明**: 新增收藏功能：收藏/取消收藏按钮 + "我的收藏"独立页面。收藏信息存储在消息表中新增的 `isFavorite` 字段。

| 文件 | 类型 | 改动内容 |
|:-----|:-----|:---------|
| `domain/model/ChatMessage.kt` | ✏️ 修改 | 新增 `val isFavorite: Boolean = false` |
| `data/local/entity/MessageEntity.kt` | ✏️ 修改 | 新增 `val isFavorite: Boolean = false` 列 |
| `data/local/dao/MessageDao.kt` | ✏️ 修改 | 新增 `@Query("UPDATE messages SET isFavorite = :isFavorite WHERE id = :id") suspend fun updateFavorite(id: Long, isFavorite: Boolean)` + `@Query("SELECT * FROM messages WHERE isFavorite = 1 ORDER BY timestamp DESC") fun getFavorites(): Flow<List<MessageEntity>>` |
| `domain/repository/ChatRepository.kt` | ✏️ 修改 | 新增 `suspend fun toggleFavorite(id: Long, isFavorite: Boolean)` + `fun getFavoriteMessages(): Flow<List<ChatMessage>>` |
| `data/repository/ChatRepositoryImpl.kt` | ✏️ 修改 | 实现 `toggleFavorite()` 和 `getFavoriteMessages()` |
| `data/mapper/ChatMapper.kt` | ✏️ 修改 | `toDomain()` 映射 isFavorite; `toEntity()` 映射 isFavorite |
| `data/local/db/Migrations.kt` | ✏️ 修改 | MIGRATION_4_5 增加 `ALTER TABLE messages ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0` |
| `data/local/db/AiTutorDatabase.kt` | ✏️ 修改 | version 5, 添加 migration 4→5 |
| `ui/chat/ChatViewModel.kt` | ✏️ 修改 | 新增 `toggleFavorite(messageId: Long)` → 调用 `chatRepository.toggleFavorite()` |
| `ui/favorites/FavoritesScreen.kt` | 🆕 **新建** | 收藏列表页, 显示所有 isFavorite=true 的消息 |
| `ui/favorites/FavoritesViewModel.kt` | 🆕 **新建** | 加载收藏列表, 提供 `removeFavorite(id)` 操作 |
| `ui/navigation/Routes.kt` | ✏️ 修改 | 新增 `const val FAVORITES = "favorites"` |
| `ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 注册 `composable(Routes.FAVORITES) { FavoritesScreen(...) }` |
| `ui/settings/SettingsScreen.kt` | ✏️ 修改 | 新增"我的收藏"入口 ListItem（点击导航到 FavoritesScreen） |
| `ui/settings/SettingsViewModel.kt` | ✏️ 修改 | 无需修改（导航由 Screen 层处理） |

> **DB Migration 共享**: F2 和 F3 共用一次 migration v4→v5，同时添加 `feedback` 和 `isFavorite` 两列。

---

### F4: 学习提醒推送

**改动说明**: SettingsScreen 添加"每日学习提醒"开关，开启后通过 WorkManager 定时（默认每日 20:00）发送 CHANNEL_REVIEW 通知提醒用户学习。

| 文件 | 类型 | 改动内容 |
|:-----|:-----|:---------|
| `app/build.gradle.kts` | ✏️ 修改 | dependencies 添加 `implementation("androidx.work:work-runtime-ktx:2.9.0")` |
| `domain/model/AppSettings.kt` | ✏️ 修改 | 新增 `val dailyReminderEnabled: Boolean = false`, `val reminderHour: Int = 20`, `val reminderMinute: Int = 0` |
| `data/remote/datastore/SettingsDataStore.kt` | ✏️ 修改 | 新增 KEY_DAILY_REMINDER / KEY_REMINDER_HOUR / KEY_REMINDER_MINUTE, 读写新字段 |
| `domain/repository/SettingsRepository.kt` | ✏️ 修改 | 新增 `suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int)` 和 `suspend fun getReminderSettings(): Triple<Boolean, Int, Int>` |
| `data/repository/SettingsRepositoryImpl.kt` | ✏️ 修改 | 实现新增方法 |
| `ui/settings/SettingsScreen.kt` | ✏️ 修改 | 在"通用"区域添加提醒开关 ListItem (Switch + 时间选择器) |
| `ui/settings/SettingsViewModel.kt` | ✏️ 修改 | 新增 `dailyReminderEnabled`, `reminderHour`, `reminderMinute` StateFlow + `toggleDailyReminder(enabled: Boolean)` 和 `setReminderTime(hour: Int, minute: Int)` 方法 |
| `data/local/reminder/DailyReminderWorker.kt` | 🆕 **新建** | WorkManager CoroutineWorker, doWork() 调用 NotificationHelper.sendReviewReminder() + WrongAnswerDao 获取待复习数量 |
| `AiTutorApp.kt` | ✏️ 修改 | onCreate() 中检查设置 → 如果已开启则 `WorkManager.enqueueUniquePeriodicWork()` 调度 DailyReminderWorker |
| `ui/settings/SettingsViewModel.kt` | ✏️ 修改 | `toggleDailyReminder()` 中根据开关状态调用 `WorkManager.enqueue/cancel` |

---

## 三、并行策略

```
数据库层 (F2+F3 共用迁移)
    ├── MessageEntity.kt  ─── 添加 feedback + isFavorite
    ├── ChatMessage.kt    ─── 添加 feedback + isFavorite (domain)
    ├── ChatMapper.kt     ─── 更新映射
    ├── MessageDao.kt     ─── updateFeedback + updateFavorite + getFavorites
    ├── ChatRepository.kt ─── 接口新增方法
    ├── ChatRepositoryImpl.kt ── 实现新增方法
    ├── Migrations.kt     ─── v4→v5
    └── AiTutorDatabase.kt ── version=5
               │
               ├──→ F2 UI: MessageBubble.kt (👍👎) + ChatScreen.kt + ChatViewModel.kt + ChatUiState.kt
               │
               └──→ F3 UI: FavoritesScreen.kt + FavoritesViewModel.kt (🆕)
                          + MessageBubble.kt (⭐ 按钮) + ChatViewModel.kt (toggleFavorite)
                          + Routes.kt + AppNavGraph.kt + SettingsScreen.kt

F1 消息长按 (纯 UI)          ← 可与 F2/F3 并行
    ├── MessageBubble.kt  ── long-press + DropdownMenu
    ├── ChatScreen.kt     ── 回调连接
    ├── ChatViewModel.kt  ── copyMessage / toggleFavorite / setFeedbackTarget
    └── ChatUiState.kt    ── feedbackTargetMessageId

F4 学习提醒 (独立模块)        ← 完全独立，可与其他全部并行
    ├── build.gradle.kts  ── WorkManager 依赖
    ├── AppSettings.kt    ── 新增字段
    ├── SettingsDataStore.kt ── 新 key
    ├── SettingsRepository.kt ── 接口
    ├── SettingsRepositoryImpl.kt ── 实现
    ├── SettingsScreen.kt ── UI toggle
    ├── SettingsViewModel.kt ── toggle 逻辑 + WorkManager 调度
    ├── DailyReminderWorker.kt ── 🆕 Worker
    └── AiTutorApp.kt     ── 初始化调度
```

```
时间线（3 位 Coder）:
Coder A: F2 数据库层 + F2 UI ────────────────────┐
Coder B: F3 数据库层(同迁移) + F3 UI ─────────────┤
                        │                          ├─ 集成编译
Coder C: F4 学习提醒 ─────────────── F1 长按 UI ──┘
```

**并行要点**:
- F2 和 F3 共享数据库迁移，建议由同一人或紧密协作完成
- F4 完全独立于聊天功能，可全程并行
- F1 最灵活：可在 F2/F3 数据层完成后作为收尾（因为长按菜单的"收藏"/"反馈"按钮依赖 F2/F3 的 Repository 方法）；也可以先做 UI 骨架（只复制功能可用）

---

## 四、执行步骤（建议顺序）

### Phase 1: 基础设施（并行可行）
| Step | 功能 | 文件 | 依赖 |
|:-----|:-----|:-----|:-----|
| 1.1 | 添加 WorkManager 依赖 | `build.gradle.kts` | 无 |
| 1.2 | 数据库 Migration v4→v5 | `Migrations.kt`, `AiTutorDatabase.kt` | 无 |
| 1.3 | Entity/Domain 模型扩展 | `MessageEntity.kt`, `ChatMessage.kt`, `AppSettings.kt` | 1.2 |
| 1.4 | DAO + Repository 接口 | `MessageDao.kt`, `ChatRepository.kt`, `SettingsRepository.kt` | 1.3 |
| 1.5 | Repository 实现 | `ChatRepositoryImpl.kt`, `SettingsRepositoryImpl.kt`, `ChatMapper.kt`, `SettingsDataStore.kt` | 1.4 |

### Phase 2: 功能开发（高度并行）
| Step | 功能 | 文件 | 可并行 |
|:-----|:-----|:-----|:------:|
| 2.1 | F1 长按菜单 UI | `MessageBubble.kt`, `ChatScreen.kt` | ✅ |
| 2.2 | F2 👍👎 按钮 UI | `MessageBubble.kt`, `ChatScreen.kt` | ✅ |
| 2.3 | F3 收藏列表页 | `FavoritesScreen.kt` 🆕, `FavoritesViewModel.kt` 🆕, `Routes.kt`, `AppNavGraph.kt`, `SettingsScreen.kt` | ✅ |
| 2.4 | F4 提醒开关 UI | `SettingsScreen.kt`, `SettingsViewModel.kt`, `DailyReminderWorker.kt` 🆕, `AiTutorApp.kt` | ✅ |

### Phase 3: 连线 + 集成
| Step | 功能 | 文件 | 
|:-----|:-----|:-----|
| 3.1 | ChatViewModel 连线 | `ChatViewModel.kt`, `ChatUiState.kt` |
| 3.2 | 编译验证 | `./gradlew assembleRelease` |
| 3.3 | 版本号更新 | `version.properties` → 2.5.0 |

---

## 五、风险与注意事项

| 风险 | 等级 | 缓解措施 |
|:-----|:----:|:---------|
| DB migration 冲突 | 🟡 中 | F2+F3 合并为一次 migration (v4→v5)，由同一 coder 负责 |
| MessageBubble.kt 多人修改冲突 | 🟡 中 | F1 和 F2 都修改此文件, 建议 F2 先做反馈按钮, F1 后做长按手势, 或提前协商修改区域 |
| WorkManager 依赖缺失 | 🟢 低 | build.gradle.kts 添加即可，与其他依赖无冲突 |
| FavoritesScreen 空状态处理 | 🟢 低 | 需设计空收藏列表的 placeholder UI |
| 提醒时间选择器 | 🟢 低 | 使用 Material3 TimePicker 或简单的时间输入组件 |
| 编译环境限制 | 🟡 中 | 无 Android SDK 环境，无法本地编译验证；建议 CI/CD 验证 |

---

## 六、PM 阶段完成标记

- [x] CEO 指令已读取 (`AUDIT_FEATURES_MAY18.md` + `AUDIT_FINAL_MAY18.md`)
- [x] 现有代码已核实 (ChatMessage, MessageEntity, MessageDao, ChatRepository, ChatRepositoryImpl, NotificationChannels, NotificationHelper, SettingsScreen, SettingsViewModel, MessageBubble, ChatScreen, ChatMapper, AppSettings, SettingsDataStore, Database, Routes, AppNavGraph, ChatUiState, ChatViewModel)
- [x] 4 功能文件清单已拆分
- [x] 并行策略已识别
- [x] 规划文件已输出 → `docs/v25_plan.md`
- [ ] 等待 CEO 分配 Coder

---

> **PM 阶段完成**。v2.5 规划已就绪，总计涉及 **25 个文件**（20 个修改 + 3 个新建 + 1 个 migration + 1 个依赖添加），新增代码量预估 ~800 行，数据库 schema 变化 1 次（v4→v5）。
