# v2.2 迭代规划（体验补齐）

> 生成日期: 2026-05-17
> 负责人: PM
> 状态: ✅ 规划完成

---

## 一、依赖分析与并行可行性评估

| 任务 | P1-1 Onboarding | P1-2 Offline | P1-3 ErrorBoundary |
|------|:---:|:---:|:---:|
| P1-1 Onboarding | — | 无依赖 | 无依赖 |
| P1-2 Offline | 无依赖 | — | 无依赖 |
| P1-3 ErrorBoundary | 无依赖 | 无依赖 | — |

**结论：3 个任务完全独立，文件无重叠，可以 100% 并行执行。**

涉及文件分析：
- P1-1: `OnboardingScreen.kt`, `OnboardingViewModel.kt`, `SplashScreen.kt`（微调）
- P1-2: `ChatViewModel.kt`, `ChatUiState.kt`, `NetworkBanner.kt`, 新增 `OfflineQueueManager.kt`
- P1-3: 新增 `ErrorBoundary.kt`, 修改 `NetworkModule.kt`, 整合 `CrashHandler.kt`

3 组文件完全不重叠，无冲突风险。

---

## 二、P1-1 新手引导 Onboarding（2 人日）

### 现状分析（已完成 90%）

| 组件 | 状态 | 备注 |
|------|:----:|------|
| `OnboardingScreen.kt` | ✅ 已完成 | 3 页引导（拍照解题/语音交互/学习进度），HorizontalPager，圆点指示器，跳过按钮，下一步/开始按钮 |
| `OnboardingViewModel.kt` | ✅ 已完成 | 调用 `OnboardingDataStore.setOnboardingDone()` |
| `OnboardingDataStore.kt` | ✅ 已完成 | DataStore Preferences 实现持久化 |
| `Routes.kt` | ✅ 已完成 | `ONBOARDING` 路由已注册 |
| `AppNavGraph.kt` | ✅ 已完成 | `composable(Routes.ONBOARDING)` 已配置，完成后跳转 Login |
| `SplashScreen.kt` | ✅ 已完成 | `SplashViewModel` 读取 `isOnboardingDone`，未完成时导航到 `onNavigateToOnboarding()` |

### 剩余工作项（~0.5 人日）

1. **SplashScreen 默认值修复**（0.1 人日）
   - `SplashViewModel` 中 `_isOnboardingDone = mutableStateOf(true)` 默认值为 true，在 `checkStartupState()` 执行前可能导致闪白屏。
   - 解决方法：改为 `mutableStateOf<Boolean?>(null)` 用 null 表示未加载，避免误跳。

2. **UI 打磨**（0.2 人日）
   - 当前使用 emoji 作为引导页图标，考虑替换为插图资源或矢量图标提高视觉品质。
   - 确认全屏沉浸式效果（状态栏透明/深色模式适配）。

3. **边缘情况**（0.1 人日）
   - 多语言支持（引导页文字是否跟随 `LanguagePreferences` 切换）。
   - 确认 `OnboardingDataStore` 的 Flow 首次读取无延迟问题。

### 涉及文件清单

| 文件 | 操作 | 预估工时 |
|------|------|:--------:|
| `app/.../ui/splash/SplashScreen.kt` | 修改 | 0.1 人日 |
| `app/.../ui/onboarding/OnboardingScreen.kt` | 微调 | 0.2 人日 |
| `app/.../ui/onboarding/OnboardingViewModel.kt` | 不改（已完成） | — |
| `app/.../data/local/OnboardingDataStore.kt` | 不改（已完成） | — |

### 验收标准
- [ ] 首次安装启动 → 显示引导页（3 页滑动）
- [ ] 底部圆点随页码高亮
- [ ] 非末页显示「跳过」按钮 → 点击跳转到登录页
- [ ] 末页显示「开始使用」按钮 → 点击跳转到登录页
- [ ] 完成引导后重新启动 → 不再显示引导页，直接进入登录/主页
- [ ] 横竖屏切换、深色模式适配

---

## 三、P1-2 离线状态处理（1.5 人日）

### 现状分析（已完成 60%）

| 组件 | 状态 | 备注 |
|------|:----:|------|
| `NetworkMonitor.kt` | ✅ 已完成 | `callbackFlow` -> `isOnline: Flow<Boolean>` + `isCurrentlyOnline(): Boolean` |
| `NetworkBanner.kt` | ✅ 已完成 | AnimatedVisibility 动画，橙色横幅，文字提示 |
| `ChatUiState.kt` | ✅ 已完成 | 含 `isOnline: Boolean = true` |
| `ChatViewModel.kt` | ✅ 已完成 | `observeNetworkState()` 收集 `networkMonitor.isOnline` 更新 State |
| `ChatScreen.kt` | ✅ 已完成 | `NetworkBanner` 在 Box 顶部对齐，接收 `state.isOnline` |
| `ChatInputBar.kt` | ✅ 已完成 | 断网时发送按钮禁用 + Toast 提示 |
| `ChatViewModel.sendMessage()` | ⚠️ 部分完成 | 断网时返回 errorMessage，但**没有消息队列** |

### 剩余工作项（~1.0 人日）

1. **新建 `OfflineQueueManager`**（0.4 人日）
   - Room 实体 `PendingMessageEntity`（conversationId, content, timestamp, retryCount）
   - DAO 操作：insert / getAllPending / delete / updateRetryCount
   - 管理类：`OfflineQueueManager` 负责入队、出队、重试逻辑
   - 使用 `NetworkMonitor.isOnline` 监听网络恢复后自动 Flush

2. **修改 `ChatViewModel.sendMessage()`**（0.2 人日）
   - 断网时不显示 errorMessage，改为入队并显示队列状态
   - 网络恢复后 `OfflineQueueManager.flush()` 自动发送
   - 发送成功后删除队内记录，失败则重试（最多 3 次）

3. **队列状态 UI**（0.3 人日）
   - `ChatUiState` 新增 `pendingMessageCount: Int`
   - `ChatScreen` / `NetworkBanner` 显示待发送消息数
   - 横幅区分黄色（有队列）和红色（纯离线无队列）

4. **增强 `NetworkBanner`**（0.1 人日）
   - 支持 pendingCount 参数
   - 黄色 `Color(0xFFFFA000)`：有消息排队中
   - 红色 `Color(0xFFE65100)`：纯离线
   - 等待图标动画（可选）

### 涉及文件清单

| 文件 | 操作 | 预估工时 |
|------|------|:--------:|
| `app/.../util/NetworkMonitor.kt` | 不改（已完成） | — |
| `app/.../ui/components/NetworkBanner.kt` | 修改 | 0.1 人日 |
| `app/.../ui/chat/ChatUiState.kt` | 修改 | 0.05 人日 |
| `app/.../ui/chat/ChatViewModel.kt` | 修改 | 0.2 人日 |
| `app/.../ui/chat/ChatScreen.kt` | 修改 | 0.15 人日 |
| `app/.../ui/chat/components/ChatInputBar.kt` | 微调 | 0.05 人日 |
| **新增** `app/.../data/local/OfflineQueueManager.kt` | 新增 | 0.2 人日 |
| **新增** Room entity + DAO | 新增 | 0.15 人日 |
| DI 模块注册 | 修改 | 0.1 人日 |

### 验收标准
- [ ] 断网后顶部显示橙色/黄色横幅
- [ ] 断网时发送消息显示队列状态（横幅显示待发送 N 条）
- [ ] 恢复网络后横幅自动消失
- [ ] 恢复网络后自动发送排队的消息
- [ ] 发送成功后队列数更新
- [ ] 重试 3 次失败后标记丢弃并提示用户

---

## 四、P1-3 全局错误边界（1 人日）

### 现状分析（已完成 50%）

| 组件 | 状态 | 备注 |
|------|:----:|------|
| `CrashHandler.kt` | ✅ 已完成 | 完整的 `Thread.UncaughtExceptionHandler` 实现，写入 `filesDir/crashes/` |
| `AiTutorApp.kt` | ✅ 已完成 | `CrashHandler.init(this)` 在 `onCreate` 调用 |
| `CrashLogScreen.kt` + `CrashLogViewModel.kt` | ✅ 已完成 | 已有崩溃日志查看和删除功能 |
| Compose ErrorBoundary | ❌ 未实现 | 需要新建 |
| 网络请求统一错误处理 | ❌ 未实现 | 需要新增 Retrofit/OkHttp 拦截器 |
| Error -> UI 可见反馈 | ❌ 未完成 | CrashHandler 只写文件，用户无感知 |

### 剩余工作项（~0.8 人日）

1. **新建 `ErrorBoundary.kt`**（0.3 人日）
   - Compose `Composable` 函数，使用 `@Composable` 级别的 try-catch + `LaunchedEffect` 兜底
   - 子组件崩溃时显示降级 UI（友好提示 + 重试按钮）
   - `ErrorBoundaryState` 保存错误信息和堆栈摘要
   - 自动上报到 CrashHandler 持久化

2. **新增 Retrofit 错误拦截器 `ErrorInterceptor.kt`**（0.3 人日）
   - 统一处理网络错误码（4xx/5xx）
   - 统一的错误消息格式化（中文友好提示）
   - 超时/无网络等异常分类
   - 注册到 `NetworkModule.provideOkHttpClient()`

3. **整合 CrashHandler**（0.1 人日）
   - `ErrorBoundary` 捕获的 Compose 崩溃也写入 `CrashHandler`
   - `CrashHandler` 暴露 `saveException()` 方法供 ErrorBoundary 调用
   - 用户可从 Settings 查看崩溃历史

4. **ChatStreamApi 增强**（0.1 人日）
   - `streamChat`/`streamChatText` 中的 onFailure 回调已有 error event 但不够详细
   - 增加状态码解析和友好提示映射

### 涉及文件清单

| 文件 | 操作 | 预估工时 |
|------|------|:--------:|
| **新增** `app/.../ui/components/ErrorBoundary.kt` | 新增 | 0.3 人日 |
| **新增** `app/.../data/remote/interceptor/ErrorInterceptor.kt` | 新增 | 0.2 人日 |
| `app/.../data/local/CrashHandler.kt` | 修改（新增 `saveException` 公开方法） | 0.1 人日 |
| `app/.../di/NetworkModule.kt` | 修改（注册 ErrorInterceptor） | 0.1 人日 |
| `app/.../ui/chat/ChatScreen.kt` | 修改（包裹 ErrorBoundary） | 0.1 人日 |
| `app/.../data/remote/api/ChatStreamApi.kt` | 微调（错误信息增强） | 0.1 人日 |
| `app/.../ui/navigation/AppNavGraph.kt` | 微调（全局 ErrorBoundary） | 0.1 人日 |

### 验收标准
- [ ] `ErrorBoundary` 包裹后，子 Composable 抛异常时显示降级 UI（非白屏）
- [ ] 降级 UI 含「重试」按钮和后退按钮
- [ ] 错误自动写入 `CrashHandler` 日志
- [ ] 网络请求 4xx/5xx 返回统一中文提示
- [ ] OkHttp 连接超时、DNS 解析失败等有错误消息
- [ ] 全局 `UncaughtExceptionHandler`（CrashHandler）写文件 + 正常退出流程

---

## 五、并行执行计划

```
时间线:
Day 1   |████████████████████████|
Day 2   |████████████████████████|

P1-1 ──┤░░░░░░░░░░░░░░░░░░░░░▓▓▓▓▓│  2 人日 (实际剩余 0.5)
P1-2 ──┤░░░░░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓│  1.5 人日
P1-3 ──┤░░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓│  1 人日
         Tester 验证窗口 ─────────>

3 名 Coder 并行工作，互不等待。
```

## 六、风险与备忘

1. **P1-1 已基本完成** — 注意不要让 Coder 做重复工作，主要是 UI 打磨和边界修复
2. **P1-2 离线队列** — Room 迁移/新表需要确认 `AiTutorDatabase.kt` 版本号是否需递增
3. **P1-3 全局 ErrorBoundary** — 需注意 Compose 的 Composable 级别异常捕获限制（Compose 运行时不允许跨 Composable 的 try-catch，需使用 SubcomposeLayout 或 component 级的方法）
4. **Tester 验证窗口** — 建议在 Day 2 下午开始集成验证，确保并行分支无冲突

---

## 七、标记完成

✅ 规划完成，已输出至 `~/hermes/projects/ai-tutor-android/docs/v22_plan.md`  
✅ 3 个任务确认完全独立可并行  
✅ 各任务具体文件清单和工时已确认  
✅ 依赖分析完成：零冲突
