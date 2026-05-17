# AI 助手 Android App 测试报告

> 测试人：QA (tester)  
> 测试日期：2026-05-14  
> 项目路径：`~/hermes/projects/ai-tutor-android/`  
> 代码总量：73 个 Kotlin 文件 + 8 个 XML/资源配置  
> 架构：MVVM + Clean Architecture (Kotlin + Jetpack Compose + Hilt + Room)

---

## 一、测试概要

| 测试维度 | 用例数 | 通过 | 失败(BUG) | 阻塞 |
|---------|:-----:|:---:|:---------:|:----:|
| 构建配置检查 | 8 | 6 | 2 | 0 |
| Domain 层 | 10 | 7 | 3 | 0 |
| Data 层 | 15 | 10 | 5 | 0 |
| DI 模块 | 8 | 5 | 3 | 0 |
| UI 层 | 18 | 11 | 6 | 1 |
| 安全与工具 | 5 | 3 | 2 | 0 |
| **总计** | **64** | **42** | **21** | **1** |

**结论：不可发布** — 存在 4 个 CRITICAL 级别编译阻塞 Bug、3 个运行时严重 Bug，必须修复后方可进入 QA 审核。

---

## 二、BUG 清单

### 🔴 CRITICAL（编译阻塞/崩溃）

#### BUG-001：关键文件缺失 — `SettingsViewModel.kt` 不存在
- **严重级别**：🔴 CRITICAL
- **文件**：`app/src/main/java/.../ui/settings/SettingsViewModel.kt`
- **描述**：`SettingsScreen.kt` 第 24 行使用 `hiltViewModel()` 注入 `SettingsViewModel`，但该文件未创建。
- **影响**：编译直接失败。Settings 页面完全无法打开。
- **修复建议**：创建 `SettingsViewModel.kt`，实现 `@HiltViewModel`，注入 `SettingsRepository`，暴露 `settings: StateFlow<AppSettings>` 及各项 update 方法。

---

#### BUG-002：关键文件缺失 — `ConversationListSheet.kt` + `ConversationViewModel.kt` 不存在
- **严重级别**：🔴 CRITICAL
- **文件**：`app/src/main/java/.../ui/conversation/` 目录不存在
- **描述**：`ChatScreen.kt` 第 25/26 行 import `ConversationListSheet`，ARCH.md 规划了 `conversation/` 包下两个文件，实际均缺失。
- **影响**：编译直接失败。会话列表 BottomSheet 功能不可用。
- **修复建议**：创建 `ui/conversation/ConversationListSheet.kt`（含会话列表 UI、搜索、新建/删除）及 `ConversationViewModel.kt`（管理会话列表状态）。

---

#### BUG-003：`AuthInterceptor` 依赖注入配置错误
- **严重级别**：🔴 CRITICAL
- **文件**：`di/NetworkModule.kt` 第 25-27 行
- **代码**：
  ```kotlin
  @Provides @Singleton
  fun provideAuthInterceptor(): AuthInterceptor {
      return AuthInterceptor()  // ← 不传 TokenManager 参数
  }
  ```
- **描述**：`AuthInterceptor` 的构造函数需要 `TokenManager` 参数（`@Inject constructor(private val tokenManager: TokenManager)`），但 `provideAuthInterceptor()` 调用的是无参构造。Hilt 无法解析此绑定，编译失败。此外，此 `@Provides` 方法是冗余的——既然 `AuthInterceptor` 已有 `@Inject` 构造，Hilt 可自动创建。
- **影响**：编译失败，所有 API 请求失去 Token 注入。
- **修复建议**：删除 `provideAuthInterceptor()` 方法，或改为 `fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor`。

---

#### BUG-004：`SettingsRepositoryImpl.getSyncSettings()` 死锁
- **严重级别**：🔴 CRITICAL
- **文件**：`data/repository/SettingsRepositoryImpl.kt` 第 49-58 行
- **代码**：
  ```kotlin
  override fun getSyncSettings(): AppSettings {
      return runBlocking {
          var settings = AppSettings()
          settingsDataStore.settingsFlow.collect { s ->  // ← collect 是无限期操作
              settings = s
              return@collect
          }
          settings
      }
  }
  ```
- **描述**：`collect` 对 DataStore 的 `Flow` 是终端操作，DataStore 的 Flow 永远不会 complete（它是持续观察的）。这意味着此方法会**永久阻塞**调用线程。如果主线程调用则触发 ANR。
- **影响**：运行时调用此方法必定死锁/ANR。
- **修复建议**：改为 `settingsDataStore.settingsFlow.first()` 或直接读 `.data`。

---

### 🟠 MAJOR（功能异常/数据风险）

#### BUG-005：SSE 流式响应可能丢数据块
- **严重级别**：🟠 MAJOR
- **文件**：`data/remote/api/ChatStreamApi.kt` 第 29/65 行
- **描述**：`callbackFlow` 默认使用 Rendezvous 通道（buffer=0），`trySend()` 在消费者来不及处理时会**静默丢弃**数据块。SSE 快速推送时，部分 content chunk 可能丢失，导致 AI 回答不完整。
- **影响**：对话流式输出内容丢失，用户体验严重受损。
- **修复建议**：使用 `callbackFlow { ... }.buffer(Channel.BUFFERED)` 或在 `trySend` 失败时改用 `send()` 挂起等待。

---

#### BUG-006：流式聊天完成后消息更新逻辑缺陷
- **严重级别**：🟠 MAJOR
- **文件**：`ui/chat/ChatViewModel.kt` 第 136-170 行
- **描述**：`doSendMessage` 中存在多个问题：
  1. **协程泄漏**：第 136-140 行启动一个 `streamJob` 收集 `allMessages`，但立即在第 142 行被覆盖为另一个协程。第一个协程未被取消，永久运行。
  2. **竞态条件**：第 144 行的 `streamChat` 开始时，`contextMessages` 可能为空（第一个协程还没收集到数据），导致发送给后端的消息上下文不完整。
  3. **消息重复**：第 165 行 `chatRepository.insertMessage(finalMessage)` 创建一条新消息（而非更新占位消息），+ 第 166 行又 `updateMessageStatus`，逻辑矛盾。`insert` 搭配 `autoGenerate=true` 会创建新记录，而不是更新已有的。
- **影响**：流式响应上下文可能丢失；AI 回复消息会重复；协程泄漏。
- **修复建议**：重构流式流程——收集 messages 使用 `first()` 而非持续 collect；AI 消息用 `update` 而非第二次 `insert`。

---

#### BUG-007：`CameraScreen.kt` 中 `surfaceProvider` 未定义
- **严重级别**：🟠 MAJOR
- **文件**：`ui/camera/CameraScreen.kt` 第 187 行
- **代码**：`it.setSurfaceProvider(surfaceProvider)`
- **描述**：变量 `surfaceProvider` 未在当前 Composable 作用域中定义。`PreviewView.setSurfaceProvider()` 需要传入一个 `SurfaceProvider`（从 `Preview` 获取）。
- **影响**：编译失败，相机预览功能完全不可用。
- **修复建议**：`val preview = Preview.Builder().build()` 之后，应使用 `preview.setSurfaceProvider(previewView.surfaceProvider)` 方式，或通过 `preview.setSurfaceProvider(surfaceProvider)` 传入从 `Preview` 构建的 provider。

---

#### BUG-008：语音识别权限未运行时申请
- **严重级别**：🟠 MAJOR
- **文件**：`AndroidManifest.xml` + `VoiceRepositoryImpl.kt`
- **描述**：App 声明了 `RECORD_AUDIO` 权限，但语音输入触发时（`VoiceInputBar`）没有运行时权限检查/申请逻辑。用户在拒绝麦克风权限后，`SpeechRecognizer` 会静默失败。
- **影响**：语音输入在未授权状态下无声失败，无错误提示。
- **修复建议**：在 ChatScreen 添加麦克风运行时权限申请逻辑（类似 CameraScreen 的权限处理）。

---

### 🟡 MINOR（架构违规/代码质量问题）

#### BUG-009：Domain 层引入 Android 框架依赖 — 违反 Clean Architecture
- **严重级别**：🟡 MINOR
- **文件**：
  - `domain/repository/VoiceRepository.kt` 第 3 行：`import android.content.Intent`
  - `domain/usecase/voice/StartListeningUseCase.kt` 第 3 行：`import android.content.Intent`
- **描述**：Domain 层（纯 Kotlin 模块）不应依赖 Android SDK。`VoiceRepository` 接口返回 `Intent`，`StartListeningUseCase` 返回 `Intent`，违反架构设计。
- **修复建议**：将 `Intent` 的创建移至 Data 层，Domain 层定义 `VoiceResult` 等纯 Kotlin 类型，或使用 `suspend fun` 封装。

---

#### BUG-010：Release 构建未开启混淆
- **严重级别**：🟡 MINOR
- **文件**：`app/build.gradle.kts` 第 24-30 行
- **描述**：`release { isMinifyEnabled = false }`——发布构建未开启代码混淆/压缩，APK 体积大且代码可被反编译。
- **修复建议**：设置 `isMinifyEnabled = true` 并配置 ProGuard 规则。

---

#### BUG-011：Token 明文存储 — 安全隐患
- **严重级别**：🟡 MINOR
- **文件**：`data/remote/interceptor/TokenManager.kt`
- **描述**：JWT Token 通过 `SharedPreferences` 明文存储，未加密。有 root 权限的设备或备份文件中可被读取。项目已引入 DataStore，但 Token 仍用 SharedPreferences。
- **修复建议**：使用 `EncryptedSharedPreferences`（AndroidX Security）或将 Token 移至 DataStore 并加密。

---

#### BUG-012：`SpeechRecognizer` 单例初始化问题
- **严重级别**：🟡 MINOR
- **文件**：`di/SpeechModule.kt` 第 20-22 行
- **描述**：`SpeechRecognizer` 作为 `@Singleton` 在应用启动时创建。`SpeechRecognizer.createSpeechRecognizer(context)` 会触发语音识别服务的绑定，即使尚未使用。`TextToSpeech` 构造是异步的，回调 `onInitListener` 可能在对象返回后尚未执行。
- **影响**：资源浪费；首次使用 TTS 时可能因未初始化而静默失败。
- **修复建议**：按需创建（`@ActivityScoped` 或工厂模式），或确保 TTS 初始化完成后再调用 `speak()`。

---

#### BUG-013：CameraScreen 相册选择和切换摄像头为占位代码
- **严重级别**：🟡 MINOR
- **文件**：`ui/camera/CameraScreen.kt` 第 215-218 / 247-252 行
- **描述**：「相册选择」和「切换摄像头」按钮的 `onClick` 为空实现，显示为不可用 UI 控件。
- **影响**：功能未实现但展示可点击控件，误导用户。
- **修复建议**：实现相册选择（`ActivityResultContracts.GetContent`）和前后摄像头切换逻辑，或在未实现前隐藏按钮。

---

#### BUG-014：`SplashScreen` 导航逻辑可能重复
- **严重级别**：🟡 MINOR
- **文件**：`ui/splash/SplashScreen.kt` 第 59-72 行
- **描述**：存在两个 `LaunchedEffect`：一个监听 `isLoggedIn` 导航，一个用 `delay(2500)` 兜底导航。当登录状态为 false 时，两者可能触发两次导航（菜单重叠）。
- **修复建议**：统一为一个 `LaunchedEffect`，在 `checkAuth` 完成后一次性决定导航目标。

---

#### BUG-015：消息搜索功能使用 LIKE 可能导致 SQLite 注入
- **严重级别**：🟡 MINOR
- **文件**：
  - `data/local/dao/ConversationDao.kt` 第 37 行
  - `data/local/dao/MessageDao.kt` 第 31 行
- **描述**：`WHERE title LIKE '%' || :keyword || '%'`——Room 的参数绑定（`:keyword`）能防止 SQL 注入，但更安全的方式是 Room 的 `@RawQuery` 或使用 FTS4 全文搜索。
- **影响**：当前无直接风险，但搜索可优化。
- **修复建议**：对大量数据可考虑升级为 Room FTS4 全文搜索。

---

#### BUG-016：ChatInputBar 字符限制与 UI 不协调
- **严重级别**：🟡 MINOR
- **文件**：`ui/chat/components/ChatInputBar.kt` 第 45 行
- **描述**：`if (it.length <= 2000) onTextChange(it)` 硬编码 2000 字符限制，未向用户展示剩余字符数。
- **影响**：用户可能不清楚输入被截断。
- **修复建议**：显示字符计数器，或将限制与后端 `max_tokens` 联动。

---

#### BUG-017：无单元测试和 UI 测试
- **严重级别**：🟡 MINOR
- **描述**：`app/build.gradle.kts` 声明了 `testInstrumentationRunner` 但没有任何测试依赖（JUnit、Mockito、Compose UI Test 等）。`app/src/test/` 和 `app/src/androidTest/` 目录为空（如存在）。
- **修复建议**：补充 `libs.versions.toml` 测试库版本，添加 JUnit、MockK、Compose UI Test 依赖。

---

#### BUG-018：Release APK 未配置签名
- **严重级别**：🟡 MINOR
- **文件**：`app/build.gradle.kts`
- **描述**：buildTypes.release 下没有 `signingConfig` 配置。
- **影响**：无法生成正式发布的签名 APK。
- **修复建议**：配置 signingConfigs 并引用到 release buildType。

---

## 三、静态代码审查摘要

### 架构评分

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 包结构清晰度 | ⭐⭐⭐⭐ | 良好的 Clean Architecture 分层，包名规范 |
| 依赖注入 | ⭐⭐⭐ | Hilt 配置整体正确，但 AuthInterceptor 注入有 Bug |
| 单向数据流 | ⭐⭐⭐⭐ | ViewModel + StateFlow + Compose 响应式一致 |
| 错误处理 | ⭐⭐⭐ | Repository 层包装 Result，但 ViewModel 错误处理不统一 |
| 架构合规性 | ⭐⭐⭐ | Domain 层引入了 Android Intent 依赖（VoiceRepository） |
| 模块化程度 | ⭐⭐⭐⭐ | UseCase 拆分合理，职责单一 |

### 文件完整性

| 文件 | 状态 |
|------|:----:|
| `SettingsViewModel.kt` | ❌ **缺失**（编译阻塞） |
| `conversation/ConversationListSheet.kt` | ❌ **缺失**（编译阻塞） |
| `conversation/ConversationViewModel.kt` | ❌ **缺失**（编译阻塞） |
| 其余 70 个 Kotlin 文件 | ✅ 存在 |

### 合规性清单

| 检查项 | 结果 | 备注 |
|-------|:---:|------|
| minSdk 26 兼容性 | ✅ | CameraX 1.3.1 / ML Kit 均兼容 |
| Compose BOM 版本 | ✅ | 2024.02.00 |
| Kotlin 1.9.22 + AGP 8.2.2 + Gradle 8.5 | ✅ | 版本兼容 |
| Room + KSP 配置 | ✅ | Room 2.6.1 + KSP 1.9.22-1.0.17 |
| Hilt 2.50 | ✅ | 含 hilt-navigation-compose |
| DataStore 设置存储 | ✅ | Preferences DataStore |
| 网络安全配置 | ✅ | 允许 10.0.2.2 明文 |
| 边缘到边缘显示 | ✅ | `enableEdgeToEdge()` |
| 深色/浅色主题 | ✅ | Material3 + ThemeMode |
| SSE 流式实现 | ⚠️ | `trySend` 可能丢数据 |
| 运行时权限 | ⚠️ | Camera 已实现，Record Audio 缺失 |
| Token 存储 | ⚠️ | 明文 SharedPreferences |
| 发布构建配置 | ⚠️ | 无混淆、无签名 |
| 单元测试 | ❌ | 完全缺失 |

---

## 四、功能测试用例状态

### 4.1 登录流程

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 手机号输入（11位数字过滤） | 只接受数字，max 11 | 代码正确 | ✅ |
| 密码可见切换 | 点击切换明文/密文 | 代码正确 | ✅ |
| 注册流程调用 | 调用 `authRepository.register()` | 代码正确 | ✅ |
| 登录流程调用 | 调用 `authRepository.login()` | 代码正确 | ✅ |
| 输入校验（手机号<11位） | 提示错误 | 代码正确 | ✅ |
| 密码长度 <6 或 >20 | 提示错误 | 代码正确 | ✅ |
| Token 自动保存 | 登录/注册成功后保存 Token | 代码正确 | ✅ |
| 密码键盘 Done→提交 | 触发 submit() | 代码正确 | ✅ |

### 4.2 对话功能

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 消息发送（基础） | 用户消息插入 DB，触发 streamChat | 逻辑存在但有 Bug | ⚠️ |
| SSE 流式接收 | 逐段接收/显示 | 实现完成但可能丢数据 | ⚠️ |
| 流式完成保存 | 最终完整内容写入 DB | 逻辑有缺陷（BUG-006） | ❌ |
| 首次消息自动命名 | 截取前 20 字作为会话标题 | 代码正确 | ✅ |
| 上下文保持 | 历史消息作为上下文发送 | 存在竞态条件（BUG-006） | ⚠️ |
| 消息重试 | 重发最后失败消息 | 代码正确 | ✅ |
| 空文本阻止发送 | 不发送空白 | 代码正确 | ✅ |
| 流式进行中禁止发送 | 防重复提交 | 代码正确 | ✅ |
| TTS 朗读 AI 回复 | 调用 voiceRepository.speak() | 代码正确 | ✅ |

### 4.3 会话管理

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 新建会话 | Room insert → 选中 | 代码正确 | ✅ |
| 切换会话 | 加载对应消息列表 | Flow 实现正确 | ✅ |
| 删除会话 | Room delete (cascade) | 代码正确 | ✅ |
| 会话列表 BottomSheet | 显示/关闭 | 代码引用缺失（BUG-002） | ❌ |
| 会话搜索 | LIKE 查询 | DAO 实现正确 | ✅ |

### 4.4 语音功能

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 离线 ASR 初始化 | SpeechRecognizer 创建 | 单例存在风险（BUG-012） | ⚠️ |
| TTS 朗读 | speak() 调用 | 可能未初始化（BUG-012） | ⚠️ |
| 停止朗读 | stopSpeaking() | 代码正确 | ✅ |
| 语音输入权限 | 运行时申请 | 缺失（BUG-008） | ❌ |

### 4.5 拍照解题

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 相机权限申请 | 运行时请求 | 代码正确 | ✅ |
| CameraX 预览 | PreviewView 显示 | `surfaceProvider` 未定义（BUG-007） | ❌ |
| 拍照模拟 | 触发 onPhotoCaptured | 占位实现 | ⚠️ |
| 确认使用→分析 | 模拟 2s 延迟后显示结果 | 占位实现 | ⚠️ |
| 相册选择 | 打开相册 | 空实现（BUG-013） | ❌ |
| 切换摄像头 | 切换前后摄 | 空实现（BUG-013） | ❌ |

### 4.6 设置页面

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 设置页加载 | 显示当前设置 | ViewModel 缺失（BUG-001） | ❌ |
| Temperature 滑块 | DataStore 持久化 | DataStore 逻辑正确 | ✅ |
| Top-P 滑块 | DataStore 持久化 | DataStore 逻辑正确 | ✅ |
| Max Tokens 滑块 | DataStore 持久化 | DataStore 逻辑正确 | ✅ |
| 主题切换 | 控制 dark theme | DataStore 逻辑正确 | ✅ |
| TTS 语速 | DataStore 持久化 | DataStore 逻辑正确 | ✅ |

### 4.7 异常场景

| 用例 | 预期 | 实际 | 状态 |
|------|------|------|:----:|
| 网络断开 | errorMessage 显示 | 实现中未连接 NetworkMonitor | ⚠️ |
| Token 过期（401） | 自动刷新或跳转登录 | AuthInterceptor 实现中无 401 拦截 | ⚠️ |
| 空会话列表 | 显示 "开始新对话" | 代码正确 | ✅ |
| 快速连续点击发送 | 防重复（isStreaming 检查） | 代码正确 | ✅ |
| 拍照后返回 | 重置 CameraState | 代码正确 | ✅ |

---

## 五、初步修复优先级建议

### P0（编译阻塞 — 必须优先修复）
1. **BUG-001**：创建 `SettingsViewModel.kt`
2. **BUG-002**：创建 `ConversationListSheet.kt` + `ConversationViewModel.kt`
3. **BUG-003**：修复 `AuthInterceptor` 注入 — 删除冗余 `@Provides` 方法
4. **BUG-004**：修复 `getSyncSettings()` 死锁 — 改用 `first()`
5. **BUG-007**：修复 `surfaceProvider` 未定义 — 修正 CameraX Preview 绑定

### P1（功能受损 — 必须修复）
6. **BUG-005**：SSE `trySend` 丢数据 — 加 buffer
7. **BUG-006**：流式消息逻辑缺陷 — 重构 `doSendMessage`
8. **BUG-008**：麦克风权限运行时申请

### P2（质量提升 — 建议修复）
9. **BUG-009**：Domain 层移除 Android 依赖
10. **BUG-010**：Release 开启混淆
11. **BUG-011**：Token 加密存储
12. **BUG-012**：TTS/Speech 按需初始化
13. **BUG-013**：实现/隐藏占位按钮
14. **BUG-014**：统一 Splash 导航逻辑
15. **BUG-015**：搜索功能优化
16. **BUG-016**：输入字符计数器
17. **BUG-017**：添加测试依赖和用例
18. **BUG-018**：Release 签名配置

---

## 六、总结

本项目架构设计优良（MVVM + Clean Architecture + 单向数据流），包结构清晰，技术选型合理（Compose + Hilt + Room + Retrofit + SSE）。**代码质量整体较好，但存在 5 个编译阻塞问题（4 个 CRITICAL + 1 个 MAJOR），导致项目当前无法通过编译和运行**。

**核心问题集中在：**
1. **3 个文件缺失**（SettingsViewModel、ConversationListSheet、ConversationViewModel）- 从 ARCH.md 规划来看应为未完成代码
2. **依赖注入配置错误**（AuthInterceptor 的 @Provides 方法不传参）
3. **运行时死锁**（getSyncSettings 使用 runBlocking + collect）
4. **CameraX 预览绑定错误**（surfaceProvider 未定义）
5. **SSE 流式丢数据和消息重复**（影响核心对话体验）

**判定：不可发布。** 建议 CODER 优先修复 P0 级别的 5 个 Bug，然后回归验证后再进入 PM(QA审核) 阶段。

---

*测试报告结束*
