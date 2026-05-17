# AI 助手 Android App QA 审计报告

> 审计人：PM (审计)  
> 审计日期：2026-05-14  
> 审计版本：v1.0  
> 项目路径：`~/hermes/projects/ai-tutor-android/`  
> 代码总量：92 个文件（73 Kotlin + 8 XML/资源配置 + 5 文档 + 6 Gradle/构建）  

---

## 免责声明

本审计基于静态代码审查 + 测试报告复读确认，**未执行实际编译和运行时测试**。编译阻塞问题的判定基于代码分析。建议修复后由 Tester 回归验证。

---

## 一、需求覆盖矩阵

对照 PRD 定义的 35 项功能（F01-F35），逐项审计实现状态：

### P0 — 核心对话（14项，必须交付）

| ID | 功能 | PRD 章节 | 实现状态 | 验证依据 | 备注 |
|----|------|---------|:--------:|---------|------|
| F01 | 手机号注册 | 2.1/4.2 | ✅ **完成** | LoginScreen + LoginViewModel + AuthRepositoryImpl | 输入校验、API 调用、Token 持久化完整 |
| F02 | 手机号登录 | 2.1/4.2 | ✅ **完成** | LoginScreen + LoginViewModel + AuthRepositoryImpl | 同上 |
| F03 | Token 自动续期 | 5.5 | ⚠️ **部分实现** | AuthRepositoryImpl.refreshToken() 存在，但 AuthInterceptor 没有 401 拦截触发自动刷新逻辑 | 只实现了刷新 API 调用，缺少拦截器层自动触发 |
| F04 | 会话列表管理 | 2.1/4.5 | ❌ **未完成** | 3 个核心文件缺失：ConversationViewModel.kt + ConversationListSheet.kt + ui/conversation/ 包不存在 | **CRITICAL** — 编译阻塞 |
| F05 | 聊天界面 | 2.1/4.4 | ✅ **完成** | ChatScreen（LazyColumn + 气泡布局） | |
| F06 | 文本消息发送 | 2.1 | ✅ **完成** | ChatViewModel.sendMessage() | |
| F07 | SSE 流式输出 | 3.1 | ⚠️ **有 Bug** | ChatStreamApi + callbackFlow，但 trySend 默认 Rendezvous 通道可能导致丢数据 | **BUG-005** |
| F08 | 打字机效果 | 4.4 | ✅ **完成** | StreamingText 组件存在 | |
| F09 | Markdown 渲染 | 4.4 | ✅ **完成** | MarkdownRender 组件存在 | |
| F10 | 多轮对话上下文 | 5.1 | ⚠️ **有 Bug** | ChatViewModel.doSendMessage 中 contextMessages 收集存在竞态条件（BUG-006） | 上下文可能不完整 |
| F11 | 本地消息持久化 | 2.1 | ✅ **完成** | Room + MessageDao + ConversationDao | |
| F12 | 消息发送状态 | 4.4 | ✅ **完成** | SENDING/SENT/FAILED 枚举 + ChatUiState | |
| F13 | 自动会话命名 | 5.4 | ✅ **完成** | 首条消息截取前 20 字 | |
| F14 | 清空当前会话 | 4.8 | ✅ **完成** | ChatViewModel.clearConversation() | |

**P0 完成度：11/14 完成 = 78.5%**（2 项有 Bug 需修复，1 项阻塞编译未完成）

---

### P1 — 语音 + 多模态（14项，增强体验）

| ID | 功能 | PRD 章节 | 实现状态 | 验证依据 | 备注 |
|----|------|---------|:--------:|---------|------|
| F15 | 语音输入 (ASR) | 4.6 | ⚠️ **部分实现** | VoiceInputBar 存在 + VoiceRepositoryImpl.createRecognizerIntent()，但**缺少 RECORD_AUDIO 运行时权限申请** | **BUG-008** |
| F16 | 云端 ASR 备选 | 4.6 | ❌ **未实现** | 仅实现了本地 SpeechRecognizer，无云端 API 调用 | 按 PRD 为备选方案，可接受 MVP 降级 |
| F17 | 消息 TTS 朗读 | 4.6 | ✅ **完成** | ChatViewModel.speakText() + VoiceRepositoryImpl.speak() | |
| F18 | 云端 TTS 备选 | 4.6 | ❌ **未实现** | 仅本地 TextToSpeech，无云端 API 调用 | 同上，可接受 |
| F19 | 语音状态指示 | 4.6 | ✅ **完成** | VoiceState.kt 定义状态枚举 + ChatUiState 引用 | |
| F20 | 语音打断 | 4.6 | ⚠️ **部分实现** | stopSpeaking() 存在，但语音状态机打断协调逻辑未集成到 VoiceInputBar | |
| F21 | 拍照解题 | 4.7 | ❌ **未完成** | CameraX 集成但 surfaceProvider 未定义导致预览崩溃（BUG-007）；拍照是模拟调用 viewModel.onPhotoCaptured("temp_photo_uri")，无实际 CameraX 图像采集 | **CRITICAL** |
| F22 | 相册选择图片 | 4.7 | ❌ **未实现** | Gallery 按钮 onClick 为空实现（BUG-013） | |
| F23 | 本地 OCR 实时检测 | 4.7 | ❌ **未实现** | ML Kit 依赖声明了，但 CameraScreen 中无 ML Kit TextRecognition 调用代码 | |
| F24 | 图片消息显示 | 4.4 | ❌ **未实现** | ChatMessage 定义了 IMAGE 类型，但 MessageBubble 未实现图片渲染 | |
| F25 | 模型选择切换 | 4.8 | ⚠️ **显示层完成** | SettingsScreen 显示模型选择（当前禁用状态），DataStore 存储逻辑完备，但后端获取模型列表 API 未集成 | 模型选择实际不可用 |
| F26 | 参数调整 | 4.8 | ✅ **完成** | Temperature/TopP/MaxTokens 滑块均实现 + DataStore 持久化 | |
| F27 | 主题切换 | 4.8 | ✅ **完成** | 浅色/深色/跟随系统 + DataStore 持久化 | |
| F28 | 搜索历史对话 | 4.8 | ✅ **完成** | ConversationDao.searchByTitle() + MessageDao.searchByContent() | |

**P1 完成度：6/14 完成 = 42.9%**（多项未实现或阻塞）

---

### P2 — 体验完善（7项）

| ID | 功能 | PRD 章节 | 实现状态 | 验证依据 | 备注 |
|----|------|---------|:--------:|---------|------|
| F29 | 用户资料编辑 | 4.9 | ✅ **完成** | ProfileScreen 编辑/保存/取消 + API 调用 | |
| F30 | 订阅管理 | 4.10 | ⚠️ **UI 完成** | SubscriptionScreen 有精美 UI 但无真实后端数据绑定，AppNavGraph 中使用占位 SubscriptionPlaceholder | 实为纯展示 |
| F31 | 清除缓存 | 4.8 | ❌ **未实现** | SettingsScreen 通用区域无此功能项 | |
| F32 | 新消息通知 | 2.1 | ❌ **未实现** | 完全缺失 | |
| F33 | 会话列表排序 | 5.4 | ✅ **完成** | DAO 中 ORDER BY updatedAt DESC | |
| F34 | Empty State | 4.4 | ✅ **完成** | EmptyStateView 组件存在 | |
| F35 | 错误重试 | 4.4 | ⚠️ **部分实现** | ChatViewModel.retrySend() 存在，但 ErrorView 组件与 ViewModel 未连接 | |

**P2 完成度：3/7 完成 = 42.9%**

---

### 总体需求覆盖摘要

| 优先级 | 总数 | 完成(✅) | 有Bug(⚠️) | 未完成(❌) | 完成率 |
|:-----:|:----:|:--------:|:---------:|:---------:|:-----:|
| P0 | 14 | 11 | 2 | 1 | 78.6% |
| P1 | 14 | 6 | 3 | 5 | 42.9% |
| P2 | 7 | 3 | 1 | 3 | 42.9% |
| **总计** | **35** | **20** | **6** | **9** | **57.1%** |

---

## 二、验收标准（AC）覆盖

对照 PRD §6 验收标准（AC01-AC33），逐项审计：

### P0 验收项（AC01-AC16）

| ID | 验收项 | 状态 | 说明 |
|----|--------|:---:|------|
| AC01 | 用户可注册新账号 | ✅ | F01 完整实现 |
| AC02 | 用户可登录已有账号 | ✅ | F02 完整实现 |
| AC03 | Token 过期自动续期（无感知） | ❌ | F03 缺少拦截器自动触发 |
| AC04 | Token 刷新失败跳转登录 | ❌ | 同上 |
| AC05 | 输入文字发送消息 | ✅ | ChatViewModel.sendMessage |
| AC06 | AI 流式回复 | ⚠️ | 有 TrySend 丢数据风险 |
| AC07 | Markdown 正确渲染 | ✅ | MarkdownRender 组件存在 |
| AC08 | 多轮对话上下文保持 | ⚠️ | 竞态条件风险 |
| AC09 | 本地消息持久化 | ✅ | Room 实现 |
| AC10 | 新建会话 | ❌ | 缺失 ConversationListSheet（无法从 UI 新建） |
| AC11 | 切换会话 | ❌ | 同上 |
| AC12 | 删除会话 | ⚠️ | ChatViewModel.deleteConversation 存在但 UI 入口缺失 |
| AC13 | 会话自动命名 | ✅ | F13 实现 |
| AC14 | 消息发送状态 | ✅ | F12 实现 |
| AC15 | 清空当前会话 | ✅ | F14 实现 |
| AC16 | 冷启动时间 < 2s | ⚠️ | SplashScreen 硬 delay(1500) + 动画 = 约 2.5s，超过目标 |

**P0 验收项：9/16 通过 = 56.3%**

### P1 验收项（AC17-AC28）

| ID | 验收项 | 状态 | 说明 |
|----|--------|:---:|------|
| AC17 | 长按录音 → ASR 转文字 | ❌ | 缺少 RECORD_AUDIO 权限申请 |
| AC18 | ASR 离线可用 | ⚠️ | SpeechRecognizer 存在但无权限无法工作 |
| AC19 | 点击消息 → TTS 朗读 | ✅ | ViewModel.speakText + VoiceRepositoryImpl |
| AC20 | 语音打断 | ⚠️ | 打断逻辑未完整实现 |
| AC21 | 拍照上传解题 | ❌ | CameraX 预览崩溃 |
| AC22 | 相册选择图片 | ❌ | 空实现 |
| AC23 | ML Kit OCR 实时检测 | ❌ | 未集成 |
| AC24 | 模型选择切换 | ❌ | UI 禁用，后端未集成 |
| AC25 | Temperature 参数生效 | ⚠️ | DataStore 存储正确，但 ChatRepositoryImpl.streamChat 有传递但无后端验证 |
| AC26 | 主题切换 | ✅ | 完整实现 |
| AC27 | MD 代码块复制 | ⚠️ | MarkdownRender 有组件但代码块复制功能未审计到 |
| AC28 | 搜索历史对话 | ✅ | DAO 级实现 |

**P1 验收项：3/12 通过 = 25.0%**

### P2 验收项（AC29-AC33）

| ID | 验收项 | 状态 | 说明 |
|----|--------|:---:|------|
| AC29 | 编辑个人资料 | ✅ | ProfileScreen 完整 |
| AC30 | 查看订阅状态 | ⚠️ | UI 存在但无后端数据 |
| AC31 | 退出登录 | ✅ | ProfileScreen 退出确认弹窗 |
| AC32 | 无会话空状态 | ✅ | EmptyStateView |
| AC33 | 网络错误提示 | ⚠️ | ErrorView 存在但未完全集成 |

**P2 验收项：3/5 通过 = 60.0%**

**总体验收项通过率：15/33 = 45.5%** — 不满足发布标准。

---

## 三、Bug 优先级汇总

以下基于 Tester 发现的 18 个 Bug，结合我的独立审查，重新分类：

### 🔴 Critical（编译阻塞/运行崩溃）— 6项

| ID | 严重级别 | 文件 | 问题描述 | 影响 |
|:--:|:-------:|------|---------|:----:|
| BUG-001 | 🔴 CRITICAL | `ui/settings/SettingsViewModel.kt` **缺失** | SettingsScreen 第 24 行引用 hiltViewModel() 但文件不存在 | 编译失败 |
| BUG-002 | 🔴 CRITICAL | `ui/conversation/*.kt` **整个包缺失** | ChatScreen 引用 ConversationListSheet，ConversationViewModel 未创建 | 编译失败 |
| BUG-003 | 🔴 CRITICAL | `di/NetworkModule.kt:25` | provideAuthInterceptor() 无参构造，AuthInterceptor 需要 TokenManager 参数 | 编译失败 |
| BUG-004 | 🔴 CRITICAL | `SettingsRepositoryImpl.kt:49-58` | getSyncSettings() 使用 runBlocking + Flow.collect，永久阻塞 | 运行时 ANR/死锁 |
| BUG-007 | 🔴 CRITICAL | `ui/camera/CameraScreen.kt:187` | `surfaceProvider` 变量未在当前作用域定义 | 编译失败 |
| — | 🆕 **新发现** | `AuthInterceptor.kt` | 没有 401 拦截 → 自动刷新逻辑，F03 验收不通过 | Token 过期无感续期不工作 |

### 🟠 Major（功能受损/数据风险）— 5项

| ID | 严重级别 | 文件 | 问题描述 | 影响 |
|:--:|:-------:|------|---------|:----:|
| BUG-005 | 🟠 MAJOR | `ChatStreamApi.kt:29/65` | callbackFlow 默认 Rendezvous 通道，trySend 静默丢数据 | 流式回复不完整 |
| BUG-006 | 🟠 MAJOR | `ChatViewModel.kt:136-170` | doSendMessage 中协程泄漏 + 竞态条件 + 消息重复 | AI 回复内容缺陷/重复 |
| BUG-008 | 🟠 MAJOR | `VoiceInputBar/Manifest` | 无 RECORD_AUDIO 运行时权限申请 | 语音输入无声失败 |
| BUG-011 | 🟠 MAJOR(提升) | `TokenManager.kt` | Token 明文 SharedPreferences 存储 | 安全性不达标，定级从 MINOR 提升至 MAJOR |
| — | 🆕 **新发现** | `ChatScreen.kt:26` | import ConversationListSheet 但文件不存在 | 编译阻塞 |

### 🟡 Minor（代码质量/安全隐患）— 13项

| ID | 严重级别 | 文件 | 问题描述 |
|:--:|:-------:|------|---------|
| BUG-009 | 🟡 MINOR | `domain/repository/VoiceRepository.kt:3` | Domain 层引入 android.content.Intent |
| BUG-010 | 🟡 MINOR | `app/build.gradle.kts:25` | isMinifyEnabled = false |
| BUG-012 | 🟡 MINOR | `di/SpeechModule.kt:20-22` | SpeechRecognizer 应用启动时即创建（资源浪费） |
| BUG-013 | 🟡 MINOR | `CameraScreen.kt:215/247` | 相册选择和切换摄像头为空实现 |
| BUG-014 | 🟡 MINOR | `SplashScreen.kt:59-72` | 两个 LaunchedEffect 可能触发重复导航 |
| BUG-015 | 🟡 MINOR | `ConversationDao:37 / MessageDao:31` | LIKE 搜索可优化为 FTS4 |
| BUG-016 | 🟡 MINOR | `ChatInputBar.kt:45` | 2000 字符限制无用户提示 |
| BUG-017 | 🟡 MINOR | `app/build.gradle.kts` | 无单元测试/UI 测试依赖 |
| BUG-018 | 🟡 MINOR | `app/build.gradle.kts` | Release 无 signingConfig |
| — | 🆕 **新发现** | `AppNavGraph.kt:111-115` | Routes.MAIN → Routes.CHAT 跳转中 immediate navigate 可能导致二次导航 |
| — | 🆕 **新发现** | `AiTutorDatabase.kt:13` | exportSchema = false 不利于数据库版本迁移 |
| — | 🆕 **新发现** | `DatabaseModule.kt:26` | fallbackToDestructiveMigration() 会导致升级时数据丢失 |
| — | 🆕 **新发现** | `ChatScreen.kt` | ConversationListSheet 引用缺失导致 Route.MAIN 导航后 ChatScreen 编译失败 |

---

## 四、架构合规性审查

### 4.1 Clean Architecture 分层检查

| 维度 | 评分 | 评估 |
|:----|:---:|------|
| **包结构** | ⭐⭐⭐⭐ | 严格遵循 domain/data/ui 三层，包命名规范 |
| **依赖方向** | ⭐⭐⭐⭐ | UI→Domain, Data→Domain, Domain 纯 Kotlin（除一处违规） |
| **Repository 模式** | ⭐⭐⭐⭐ | 接口在 Domain，实现在 Data，Hilt Binds 绑定 |
| **UseCase 拆分** | ⭐⭐⭐⭐ | 8 个 UseCase，单一职责 |
| **单向数据流** | ⭐⭐⭐⭐ | ViewModel(StateFlow) → UI 状态驱动 |
| **DI 注入** | ⭐⭐⭐ | 整体正确，但 AuthInterceptor 注入配置错误 |
| **错误处理** | ⭐⭐⭐ | Repository 返回 Result<T>，但 ViewModel 层处理不统一 |
| **模型设计** | ⭐⭐⭐⭐ | Domain Model ↔ Entity/DTO 层次清晰 |

### 4.2 架构违规项

1. **❌ Domain 层依赖 Android SDK** — `VoiceRepository.kt` 返回 `android.content.Intent`（BUG-009）
2. **❌ AuthInterceptor 未实现 401 自动刷新** — 架构设计 F03 要求 Token 自动续期，但当前 Interceptor 只做 Bearer 注入，无 401 拦截回调
3. **⚠️ SettingsViewModel 缺失** — ARCH.md 规划了 settings 包下有 ViewModel，但未实现
4. **⚠️ ConversationViewModel 缺失** — ARCH.md 规划了 conversation 包下两个文件，都未实现

### 4.3 关键架构决策一致性

| 决策项 | ARCH.md 规划 | 实际实现 | 一致性 |
|-------|-------------|---------|:-----:|
| MVVM + Clean Architecture | 三层 | 三层 | ✅ |
| Hilt DI | 4 个 Module | 4 个 Module | ✅ |
| Room (本地持久化) | 2 DAO + 2 Entity | 2 DAO + 2 Entity | ✅ |
| DataStore (设置) | SettingsDataStore | 完整实现 | ✅ |
| SSE via callbackFlow | ChatStreamApi | 实现但有 Bug | ⚠️ |
| ConversationViewModel | conversation/ 包 | **缺失** | ❌ |
| SettingsViewModel | settings/ 包 | **缺失** | ❌ |
| Token 自动刷新 | AuthInterceptor + TokenManager | 只有 TokenManager | ❌ |
| ML Kit OCR | CameraScreen | **未集成** | ❌ |
| Voice State Machine | VoiceUiState | VoiceState 存在但未完全连接 UI | ⚠️ |

---

## 五、代码规范审查

### 5.1 命名规范

- ✅ **Kotlin 文件名**: `PascalCase.kt` — 所有文件符合
- ✅ **包名**: `com.aitutor.app.*` — 统一
- ✅ **类名**: `PascalCase` — 正确
- ✅ **函数/变量**: `camelCase` — 正确
- ✅ **常量**: UPPER_SNAKE_CASE — Companion object 常量符合
- ⚠️ **Compose 函数**: 以组件名命名（正确），但缺少 `@Composable` 注释

### 5.2 注释完整性

- ❌ **KDoc**: 公共 API 无 KDoc 注释（SubscriptionScreen 除外）
- ❌ **复杂逻辑注释**: ChatViewModel.doSendMessage 多步操作缺乏注释
- ✅ **TODO/FIXME**: 适量使用（CameraScreen 中的空实现注释）

### 5.3 资源文件规范

- ✅ `strings.xml` ❌ **缺失** — 搜索发现不存在 strings.xml 文件中的实际内容；硬编码中文文本在 Compose 组件中
- ⚠️ **硬编码问题**: 大量中文字符串直接写在 Composable 中，未抽取到 strings.xml，不利于多语言
- ✅ **主题**: Color.kt / Type.kt / Theme.kt — 结构清晰，Material3 规范

### 5.4 代码重复与复杂度

- ⚠️ **SettingsRepositoryImpl**: getSyncSettings() 方法整体应该移除或用 `first()` 替代
- ⚠️ **AuthInterceptor provide 方法**: 冗余 — AuthInterceptor 已有 @Inject 构造，Hilt 可自动创建
- ✅ **UseCase 层**: 简洁，单一职责
- ✅ **数据映射**: Mapper 模式使用正确（toDomain / toEntity）

---

## 六、性能与安全评估

### 6.1 性能风险

| 问题 | 严重程度 | 说明 |
|------|:-------:|------|
| getSyncSettings() 死锁 | 🔴 | 主线程调用将触发 ANR |
| SpeechRecognizer 单例 | 🟡 | 应用启动即绑定语音服务 |
| SplashScreen delay(2500) | 🟡 | 冷启动时间 > 2s |
| SSE 丢数据 | 🟠 | 影响流式响应完整性 |
| No LazyColumn pagination | 🟡 | 数百条消息可能卡顿 |
| fallbackToDestructiveMigration | 🟡 | 数据库升级清空所有数据 |

### 6.2 安全风险

| 问题 | 严重程度 | 说明 |
|------|:-------:|------|
| Token 明文 SharedPreferences | 🟠 | 无加密，root 设备可读取 |
| Release 无混淆 | 🟡 | APK 可被反编译 |
| Release 无签名 | 🟡 | 无法生成生产用 APK |
| SQL LIKE 注入 | ⚠️ | Room 参数绑定可防范，但最佳实践是 FTS4 |
| 网络安全配置允许明文 | ⚠️ | 开发阶段可接受，发布前应限制 |

---

## 七、修复优先级建议

### P0 — 编译阻塞（必须立即修复）

| 优先级 | Bug | 修复内容 | 工作量估计 | 依赖 |
|:-----:|:---:|---------|:---------:|:----:|
| P0-A | BUG-001 | 创建 `SettingsViewModel.kt`（@HiltViewModel，注入 SettingsRepository，暴露 settings StateFlow + 各 update 方法） | ~30 行 | — |
| P0-B | BUG-002 | 创建 `ui/conversation/` 包，包含 ConversationListSheet.kt + ConversationViewModel.kt | ~200 行 | — |
| P0-C | BUG-003 | NetworkModule.provideAuthInterceptor() 删除或改为接收 TokenManager 参数 | 1 行 | — |
| P0-D | BUG-004 | getSyncSettings() 改用 `settingsDataStore.settingsFlow.first()` | 2 行 | — |
| P0-E | BUG-007 | CameraScreen: `surfaceProvider` → `preview.setSurfaceProvider(surfaceProvider)` 修正 | 2 行 | — |

### P1 — 功能受损（必须修复）

| 优先级 | Bug | 修复内容 | 工作量估计 |
|:-----:|:---:|---------|:---------:|
| P1-A | BUG-005 | callbackFlow 加 `.buffer(Channel.BUFFERED)` 或改用 `send()` | 1 行 |
| P1-B | BUG-006 | 重构 doSendMessage：取消重复协程 → 用 `first()` 替代持续 collect → 用 update 替代 insert | ~20 行 |
| P1-C | BUG-008 | 添加 RECORD_AUDIO 运行时权限申请逻辑（参考 CameraScreen 权限处理） | ~30 行 |
| P1-D | F03缺失 | AuthInterceptor 添加 401 拦截 → TokenManager 刷新 → 重试原请求 | ~40 行 |
| P1-E | BUG-011 | Token 迁移至 EncryptedSharedPreferences 或 DataStore + 加密 | ~20 行 |

### P2 — 质量提升（建议修复）

| 优先级 | Bug | 修复内容 |
|:-----:|:---:|---------|
| P2-A | BUG-009 | VoiceRepository 接口改造，Intent 创建移至 Data 层 |
| P2-B | BUG-010 | release 开启 isMinifyEnabled = true + 配置 ProGuard |
| P2-C | BUG-012 | SpeechRecognizer / TextToSpeech 改为 @ActivityScoped 按需创建 |
| P2-D | BUG-013 | 实现相册选择和切换摄像头，或隐藏按钮 |
| P2-E | BUG-014 | 统一 Splash 导航为一个 LaunchedEffect |
| P2-F | BUG-017 | 添加测试依赖（JUnit/MockK/Compose UI Test） |
| P2-G | BUG-018 | 配置 release signingConfig |
| P2-H | BUG-015 | 搜索升级为 FTS4 |
| P2-I | BUG-016 | 添加字符计数器 |
| P2-J | 新增 | exportSchema = true + 编写 Migration 替代 fallbackToDestructiveMigration |

---

## 八、最终结论

### 判定：❌ **不通过 — 不可发布**

### 核心原因

1. **编译阻塞**：6 个 CRITICAL 级别问题导致项目无法编译（3 文件缺失、2 引用编译错误、1 死锁 ANR）
2. **需求覆盖严重不足**：35 项功能仅 20 项完整实现（57.1%），P0 核心功能有 1 项完全缺失（会话管理）、2 项有严重 Bug
3. **验收项通过率仅 45.5%**：33 项验收标准仅 15 项通过
4. **架构违规**：Domain 层依赖 Android SDK、Token 自动刷新未实现架构设计意图
5. **安全合规不达标**：Token 明文存储、无混淆、无签名

### 修复后必须回归验证项

1. 重新编译并验证所有路由导航正常
2. 会话管理（新建/切换/删除）全流程验证
3. SSE 流式消息发送 → 接收 → 持久化全链路验证
4. 设置页面参数调整 + DataStore 持久化验证
5. Token 过期自动刷新 401 场景验证
6. 语音输入/朗读流程验证
7. 相机预览 + 拍照流程验证

---

## 附录：文件清单变更摘要

### 需新增的文件

```
app/src/main/java/com/aitutor/app/ui/settings/SettingsViewModel.kt
app/src/main/java/com/aitutor/app/ui/conversation/ConversationListSheet.kt
app/src/main/java/com/aitutor/app/ui/conversation/ConversationViewModel.kt
```

### 需修复的文件

```
app/src/main/java/com/aitutor/app/di/NetworkModule.kt          # BUG-003
app/src/main/java/com/aitutor/app/data/repository/SettingsRepositoryImpl.kt  # BUG-004
app/src/main/java/com/aitutor/app/ui/camera/CameraScreen.kt    # BUG-007, BUG-013
app/src/main/java/com/aitutor/app/ui/chat/ChatViewModel.kt     # BUG-006
app/src/main/java/com/aitutor/app/data/remote/api/ChatStreamApi.kt  # BUG-005
app/src/main/java/com/aitutor/app/data/remote/interceptor/AuthInterceptor.kt  # F03缺失
app/src/main/java/com/aitutor/app/data/remote/interceptor/TokenManager.kt  # BUG-011
app/src/main/java/com/aitutor/app/ui/splash/SplashScreen.kt    # BUG-014
app/src/main/java/com/aitutor/app/domain/repository/VoiceRepository.kt  # BUG-009
app/src/main/java/com/aitutor/app/di/SpeechModule.kt           # BUG-012
app/src/main/java/com/aitutor/app/ui/chat/components/ChatInputBar.kt  # BUG-016
app/build.gradle.kts                                            # BUG-010, BUG-017, BUG-018
```

---

*审计报告结束 — PM 确认签字*
