# AI 助手 Android App 架构设计文档

> 文档版本: v1.0  
> 日期: 2026-05-14  
> 架构师: CFO  
> 项目路径: `~/hermes/projects/ai-tutor-android/`

---

## 1. 总体架构概览

### 1.1 架构风格

采用 **MVVM + Clean Architecture** 三层架构，遵循依赖倒置原则：

```
┌─────────────────────────────────────────┐
│            UI LAYER (Compose)            │
│  Screens ── ViewModels ── StateFlow     │
├─────────────────────────────────────────┤
│          DOMAIN LAYER (纯 Kotlin)         │
│  UseCases ── Repository Interfaces       │
│  ── Domain Models                        │
├─────────────────────────────────────────┤
│           DATA LAYER (实现层)             │
│  Repositories ── Remote / Local Sources  │
│  ── DTOs / Entities / Mappers            │
└─────────────────────────────────────────┘
```

**分层原则：**
- **UI Layer**：只依赖 Domain Layer，不直接接触 Data Layer
- **Domain Layer**：不依赖任何 Android 框架，纯 Kotlin 模块
- **Data Layer**：实现 Domain 定义的 Repository 接口，管理远程/本地数据源

### 1.2 数据流架构 (单向数据流)

```
User Action → ViewModel (Event) → UseCase → Repository → DataSource
                                                          ↓
User Sees  ← ViewModel (State) ←  Flow    ←  Result<T>  ←┘
```

- **事件驱动**：UI 通过调用 ViewModel 方法发起操作
- **响应式状态**：ViewModel 通过 `StateFlow<UiState>` 暴露状态
- **副作用隔离**：网络请求、数据库操作、语音等放在 UseCase / Repository 层

---

## 2. 模块划分与包结构

```
com.aitutor.app/
├── AiTutorApp.kt                  # Application (@HiltAndroidApp)
├── MainActivity.kt                # Single Activity (@AndroidEntryPoint)
│
├── di/                            # Hilt 依赖注入模块
│   ├── NetworkModule.kt           #   Retrofit, OkHttp, API 接口
│   ├── DatabaseModule.kt          #   Room 数据库, DAO
│   ├── RepositoryModule.kt        #   Repository 绑定
│   └── SpeechModule.kt            #   TTS/ASR 引擎注入
│
├── data/                          # 数据层
│   ├── remote/                    #   远程数据源
│   │   ├── api/                   #     Retrofit 接口定义
│   │   │   ├── AiTutorApi.kt      #     传统 REST API
│   │   │   └── ChatStreamApi.kt   #     SSE Streaming API
│   │   ├── dto/                   #     网络 DTO (已有 + 新增)
│   │   ├── interceptor/           #     OkHttp 拦截器
│   │   └── datastore/             #     DataStore 偏好存储
│   ├── local/                     #   本地数据源
│   │   ├── db/                    #     Room 数据库定义
│   │   │   └── AiTutorDatabase.kt
│   │   ├── dao/                   #     Room DAO 接口
│   │   │   ├── ConversationDao.kt
│   │   │   └── MessageDao.kt
│   │   └── entity/                #     Room Entity
│   │       ├── ConversationEntity.kt
│   │       └── MessageEntity.kt
│   ├── repository/                #   Repository 实现
│   │   ├── ChatRepositoryImpl.kt
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── VoiceRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   └── mapper/                    #   DTO/Entity <-> Domain 映射
│       ├── ChatMapper.kt
│       └── UserMapper.kt
│
├── domain/                        # 领域层 (纯 Kotlin)
│   ├── model/                     #   领域模型
│   │   ├── User.kt
│   │   ├── ChatMessage.kt
│   │   ├── Conversation.kt        #    会话模型
│   │   ├── VoiceState.kt          #    语音状态模型
│   │   └── AppSettings.kt         #    应用设置模型
│   ├── repository/                #   Repository 接口
│   │   ├── ChatRepository.kt
│   │   ├── AuthRepository.kt
│   │   ├── VoiceRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/                   #   用例
│       ├── chat/
│       │   ├── SendMessageUseCase.kt
│       │   ├── StreamChatUseCase.kt
│       │   └── GetChatHistoryUseCase.kt
│       ├── conversation/
│       │   ├── CreateConversationUseCase.kt
│       │   ├── SwitchConversationUseCase.kt
│       │   └── DeleteConversationUseCase.kt
│       ├── voice/
│       │   ├── StartListeningUseCase.kt
│       │   └── SpeakTextUseCase.kt
│       └── settings/
│           └── UpdateSettingsUseCase.kt
│
├── ui/                            # UI 层
│   ├── navigation/                #   导航
│   │   ├── Routes.kt              #     路由常量
│   │   └── AppNavGraph.kt         #     导航图 + BottomNav
│   ├── theme/                     #   主题 (Material3)
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── chat/                      #   聊天页面
│   │   ├── ChatScreen.kt
│   │   ├── ChatViewModel.kt
│   │   ├── ChatUiState.kt         #    分离的 UI 状态
│   │   └── components/
│   │       ├── MessageBubble.kt   #     消息气泡
│   │       ├── ChatInputBar.kt    #     输入栏 (含语音按钮)
│   │       ├── VoiceInputBar.kt   #     语音输入面板
│   │       └── StreamingText.kt   #     流式文本动画组件
│   ├── conversation/              #   会话管理
│   │   ├── ConversationListSheet.kt
│   │   └── ConversationViewModel.kt
│   ├── auth/                      #   登录/注册
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── settings/                  #   设置页面
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── profile/                   #   个人中心
│   │   ├── ProfileScreen.kt
│   │   └── ProfileViewModel.kt
│   ├── camera/                    #   拍照/图片识别
│   │   ├── CameraScreen.kt
│   │   └── CameraViewModel.kt
│   └── common/                    #   通用组件
│       ├── LoadingIndicator.kt
│       ├── ErrorView.kt
│       ├── MarkdownRender.kt
│       └── EmptyStateView.kt
│
└── util/                          # 工具类
    ├── NetworkMonitor.kt          #   网络状态监听
    ├── AudioUtils.kt              #   音频工具
    └── Extensions.kt              #   Kotlin 扩展
```

### 2.1 现有代码适配说明

现有 `data/repository/AppRepository.kt`（单体仓库）将在架构升级中拆分为多个专一 Repository：

| 现有类 | 拆分为 |
|--------|--------|
| `AppRepository` | `AuthRepositoryImpl`, `ChatRepositoryImpl`, `VoiceRepositoryImpl`, `SettingsRepositoryImpl` |
| `AiTutorApi` | 保留 + 新增 `ChatStreamApi` (SSE) |
| `ChatViewModel` | 拆分出 `ConversationViewModel` (会话管理) + 增强 `ChatViewModel` (消息流) |

---

## 3. 核心模块设计

### 3.1 智能对话模块 (核心)

#### 架构设计

```
ChatScreen ──> ChatViewModel
                  │
                  ├── StreamChatUseCase
                  │       └── ChatRepository
                  │               ├── ChatStreamApi (SSE)    ← 流式响应
                  │               └── MessageDao (Room)       ← 本地持久化
                  │
                  └── GetChatHistoryUseCase
                          └── ChatRepository
                                  └── MessageDao
```

#### 流式输出 (Streaming) 方案

- **协议**：Server-Sent Events (SSE) over HTTP
- **OkHttp 配置**：`readTimeout` 设为 0（无超时），使用 `EventSource` 监听
- **WebSocket 备选**：若后端支持 WebSocket，SSE 可平滑切换
- **UI 渲染**：`StreamingText` 组件逐块追加文本，支持 Markdown 增量渲染

```
[POST /v1/chat/completions]  →  SSE stream
  data: {"choices":[{"delta":{"content":"你好"}}]}
  data: {"choices":[{"delta":{"content":"，我是"}}]}
  data: {"choices":[{"delta":{"content":"AI助手"}}]}
  data: [DONE]
```

#### 关键实现要点

- 使用 `OkHttp EventSource.Factory` 或自定义 `Flow<ChatChunk>` 包装 SSE
- 用 `callbackFlow` 将 SSE 回调转为 Kotlin Flow，ViewModel 通过 `stateFlow` 逐块更新 UI
- 每个流式块到达即写入 Room，保证断网不丢消息

### 3.2 语音交互模块

#### 架构

```
VoiceInputBar ──> ChatViewModel
                     │
                     ├── StartListeningUseCase
                     │       └── VoiceRepository
                     │               ├── Android SpeechRecognizer (ASR, 离线)
                     │               └── 云端 ASR API (备选, 准确率更高)
                     │
                     └── SpeakTextUseCase
                             └── VoiceRepository
                                     ├── Android TTS (离线)
                                     └── Edge-TTS / 云端 TTS API (备选)
```

#### ASR 策略 (双轨)

| 方案 | 适用场景 | 实现方式 |
|------|---------|---------|
| **离线 ASR** | 短句、无网络 | `SpeechRecognizer` + `RecognizerIntent.EXTRA_LANGUAGE` (zh/en) |
| **云端 ASR** | 长句、高准确率 | 上传录音文件到后端 `/v1/audio/transcriptions` |

#### TTS 策略 (双轨)

| 方案 | 适用场景 | 实现方式 |
|------|---------|---------|
| **离线 TTS** | 短回复、快速播放 | `TextToSpeech` 引擎 + `setLanguage` |
| **云端 TTS** | 长文本、高音质 | 请求后端 `/v1/audio/speech` 流式播放 |

#### 语音状态机

```
IDLE → LISTENING → PROCESSING → SPEAKING → IDLE
  ↑        ↑            ↑            │
  └────────┴────────────┴────────────┘ (可随时打断)
```

- 语音输入时用户可说「发送」触发自动提交
- 播放 TTS 时可随时打断（重新触发 ASR）
- 所有状态通过 `VoiceUiState` 暴露给 UI

### 3.3 多模态识别模块

#### 现有能力

- `CameraX` 已集成，可拍照
- 拍照后通过 `Multipart` 上传至后端 `/api/v1/solve/photo`

#### 增强方案 (本地 OCR + 云端分析)

```
CameraScreen ──> CameraViewModel
                    │
                    ├── ML Kit OCR (本地, 实时文字检测)
                    │       └── TextRecognitionClient
                    │
                    └── SolvePhotoUseCase (云端, 深度分析)
                            └── ChatRepository
                                    └── POST /api/v1/solve/photo
```

- **本地 OCR**：使用 ML Kit Text Recognition 实时检测取景框内文字
- **云端分析**：拍照后上传图片至后端，由 LLM 分析内容
- **图片选择**：除拍照外，支持从相册选择图片

### 3.4 会话管理模块 (新增)

#### Room 数据库设计

```
┌───────────────────┐       ┌─────────────────────┐
│  Conversation      │       │  Message             │
├───────────────────┤       ├─────────────────────┤
│ id: Long (PK)     │──1:N──│ id: Long (PK)        │
│ title: String     │       │ conversationId: FK   │
│ createdAt: Long   │       │ content: String      │
│ updatedAt: Long   │       │ isUser: Boolean      │
│ modelId: String   │       │ contentType: enum    │
│ systemPrompt: Str │       │   (TEXT/IMAGE/AUDIO) │
└───────────────────┘       │ timestamp: Long      │
                            │ metadata: String?    │
                            └─────────────────────┘
```

#### 会话操作

| 操作 | 实现 |
|------|------|
| **新建会话** | `ConversationDao.insert()` → 导航到新会话 |
| **切换会话** | `ConversationDao.getMessages(id)` → `StateFlow` 切换 |
| **删除会话** | `ConversationDao.delete(id)` + 级联删除消息 |
| **会话列表** | `ConversationDao.getAllFlow()` → `StateFlow<List<Conversation>>` |
| **自动命名** | 首条消息截取前 20 字作为标题 |

#### UI 交互

- 聊天页面左上角点击展开「会话列表」BottomSheet
- 支持长按删除、滑动切换
- 新建按钮 + 当前会话指示器

### 3.5 设置模块

#### 设置项

| 分组 | 设置项 | 存储方式 |
|------|--------|---------|
| **模型** | 模型选择 (列表来自后端) | DataStore |
| **参数** | Temperature (0.0-2.0) | DataStore |
| | Top-P (0.0-1.0) | DataStore |
| | Max Tokens | DataStore |
| **主题** | 深色/浅色/跟随系统 | DataStore + `isSystemInDarkTheme()` |
| **语音** | TTS 语速、音色 | DataStore |
| **通用** | 语言、清除缓存、关于 | DataStore / 系统 |

#### 实现

```kotlin
// domain/model/AppSettings.kt
data class AppSettings(
    val modelId: String = "default",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
    val darkTheme: ThemeMode = ThemeMode.SYSTEM,
    val ttsSpeed: Float = 1.0f,
    val ttsVoice: String = "default"
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

使用 Preferences DataStore 存储，通过 `SettingsRepository` 暴露 `Flow<AppSettings>`。

### 3.6 认证模块 (现有)

保持现有架构不变，仅增强：

- Token 自动续期逻辑 (401 时静默刷新)
- Token 失效时自动跳转登录页

---

## 4. 导航设计

### 路由结构

```
Splash ──> Login ──> Main (BottomNavigation)
                        ├── Chat (默认首页)
                        ├── Camera (拍照解题)
                        └── Settings (设置)
                              └── Profile (个人资料)
                              └── Subscription (订阅)
```

### 路由表

| Route | Screen | 说明 |
|-------|--------|------|
| `splash` | SplashScreen | 启动页，检查 Token |
| `login` | LoginScreen | 登录/注册 |
| `main` | MainScreen | 主容器 (BottomNav) |
| `chat` | ChatScreen | 聊天 (底部 Tab) |
| `chat/{conversationId}` | ChatScreen | 指定会话 |
| `solve` | CameraScreen | 拍照解题 (底部 Tab) |
| `settings` | SettingsScreen | 设置 (底部 Tab) |
| `profile` | ProfileScreen | 个人资料 |
| `subscription` | SubscriptionScreen | 订阅管理 |

---

## 5. 关键技术决策

### 5.1 网络层

| 组件 | 版本范围 | 用途 |
|------|---------|------|
| Retrofit 2.x | 2.9+ | 传统 REST API |
| OkHttp 4.x | 4.12+ | HTTP 客户端 + SSE 支持 |
| Gson / Moshi | - | JSON 序列化 (当前用 Gson，可转 Moshi) |
| Room | 2.6+ | 本地持久化 |

### 5.2 协程与 Flow

- **ViewModel**：`viewModelScope` + `StateFlow`
- **Repository**：`suspend fun` 返回 `Result<T>` + `Flow<T>` 观察
- **流式聊天**：`callbackFlow<ChatChunk>` → `StateFlow<List<ChatMessage>>`
- **数据库观察**：Room DAO 返回 `Flow<List<T>>`

### 5.3 DI 策略 (Hilt)

```
@Singleton
├── NetworkModule       → OkHttpClient, Retrofit, AiTutorApi, ChatStreamApi
├── DatabaseModule      → AiTutorDatabase, ConversationDao, MessageDao
├── RepositoryModule    → ChatRepository, AuthRepository, VoiceRepository
└── SpeechModule        → TextToSpeech, SpeechRecognizer (Application 绑定)
```

---

## 6. 当前项目差距分析

| 需求 | 现有状态 | 需新增/改造 | 优先级 |
|------|---------|------------|-------|
| 智能对话 (文本) | ✅ 基础聊天 | 增强流式 SSE | P0 |
| 流式输出 | ❌ | 新增 `ChatStreamApi` + `StreamingText` 组件 | P0 |
| 语音输入 (ASR) | ❌ | 新增 `VoiceRepository` + `VoiceInputBar` | P1 |
| 语音输出 (TTS) | ❌ | 新增 TTS 引擎 + `SpeakTextUseCase` | P1 |
| 多模态 (拍照) | ✅ CameraX + 上传 | 增强 ML Kit OCR | P1 |
| 历史记录 (本地) | ❌ (仅服务端) | 新增 Room + DAO + Entity | P0 |
| 会话管理 | ❌ | 新增 `Conversation` 模型 + DAO + UI | P0 |
| 模型/参数设置 | ❌ | 新增 `SettingsScreen` + DataStore | P1 |
| 主题切换 | ✅ 深色/浅色 | 完善 `ThemeMode.SYSTEM` | P2 |
| 用户设置页 | ❌ | 新增 `SettingsScreen` + `SettingsViewModel` | P1 |
| 个人中心 | ✅ 基础 | 保持 | P2 |
| 订阅管理 | ✅ 基础 | 保持 | P2 |
| Clean Architecture | ⚠️ 部分 | 拆分 `AppRepository` → 多个专一 Repository，新增 UseCase | P1 |

---

## 7. 目录结构汇总

```
~/hermes/projects/ai-tutor-android/
├── ARCH.md                        # 本文档
├── API.md                         # API 接口设计
├── build.gradle.kts               # 项目级 Gradle
├── settings.gradle.kts            # 项目设置
├── gradle.properties              # Gradle 属性
├── gradle/
│   ├── libs.versions.toml         # 版本目录
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts           # 模块级 Gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── drawable/
        │   ├── mipmap-anydpi-v26/
        │   ├── values/
        │   │   ├── strings.xml
        │   │   └── themes.xml
        │   └── xml/
        │       └── network_security_config.xml
        └── java/com/aitutor/app/
            ├── AiTutorApp.kt
            ├── MainActivity.kt
            ├── di/
            │   ├── NetworkModule.kt
            │   ├── DatabaseModule.kt
            │   ├── RepositoryModule.kt
            │   └── SpeechModule.kt
            ├── data/
            │   ├── remote/
            │   │   ├── api/
            │   │   │   ├── AiTutorApi.kt
            │   │   │   └── ChatStreamApi.kt
            │   │   ├── dto/
            │   │   │   ├── ApiResponse.kt
            │   │   │   ├── AuthDtos.kt
            │   │   │   ├── ChatDtos.kt
            │   │   │   └── SettingsDtos.kt
            │   │   ├── interceptor/
            │   │   │   ├── AuthInterceptor.kt
            │   │   │   └── TokenManager.kt
            │   │   └── datastore/
            │   │       └── SettingsDataStore.kt
            │   ├── local/
            │   │   ├── db/
            │   │   │   └── AiTutorDatabase.kt
            │   │   ├── dao/
            │   │   │   ├── ConversationDao.kt
            │   │   │   └── MessageDao.kt
            │   │   └── entity/
            │   │       ├── ConversationEntity.kt
            │   │       └── MessageEntity.kt
            │   ├── repository/
            │   │   ├── ChatRepositoryImpl.kt
            │   │   ├── AuthRepositoryImpl.kt
            │   │   ├── VoiceRepositoryImpl.kt
            │   │   └── SettingsRepositoryImpl.kt
            │   └── mapper/
            │       ├── ChatMapper.kt
            │       └── UserMapper.kt
            ├── domain/
            │   ├── model/
            │   │   ├── User.kt
            │   │   ├── ChatMessage.kt
            │   │   ├── Conversation.kt
            │   │   ├── VoiceState.kt
            │   │   └── AppSettings.kt
            │   ├── repository/
            │   │   ├── ChatRepository.kt
            │   │   ├── AuthRepository.kt
            │   │   ├── VoiceRepository.kt
            │   │   └── SettingsRepository.kt
            │   └── usecase/
            │       ├── chat/
            │       │   ├── SendMessageUseCase.kt
            │       │   ├── StreamChatUseCase.kt
            │       │   └── GetChatHistoryUseCase.kt
            │       ├── conversation/
            │       │   ├── CreateConversationUseCase.kt
            │       │   ├── SwitchConversationUseCase.kt
            │       │   └── DeleteConversationUseCase.kt
            │       ├── voice/
            │       │   ├── StartListeningUseCase.kt
            │       │   └── SpeakTextUseCase.kt
            │       └── settings/
            │           └── UpdateSettingsUseCase.kt
            └── ui/
                ├── navigation/
                │   ├── Routes.kt
                │   └── AppNavGraph.kt
                ├── theme/
                │   ├── Theme.kt
                │   ├── Color.kt
                │   └── Type.kt
                ├── chat/
                │   ├── ChatScreen.kt
                │   ├── ChatViewModel.kt
                │   ├── ChatUiState.kt
                │   └── components/
                │       ├── MessageBubble.kt
                │       ├── ChatInputBar.kt
                │       ├── VoiceInputBar.kt
                │       └── StreamingText.kt
                ├── conversation/
                │   ├── ConversationListSheet.kt
                │   └── ConversationViewModel.kt
                ├── auth/
                │   ├── LoginScreen.kt
                │   └── LoginViewModel.kt
                ├── settings/
                │   ├── SettingsScreen.kt
                │   └── SettingsViewModel.kt
                ├── profile/
                │   ├── ProfileScreen.kt
                │   └── ProfileViewModel.kt
                ├── camera/
                │   ├── CameraScreen.kt
                │   └── CameraViewModel.kt
                └── common/
                    ├── LoadingIndicator.kt
                    ├── ErrorView.kt
                    ├── MarkdownRender.kt
                    └── EmptyStateView.kt
```

---

## 8. 实施建议 (按阶段)

### Phase 1 — 核心对话 (P0)
1. 新增 Room Database (Conversation + Message Entity/DAO)
2. 新增 `ChatStreamApi` SSE 流式接口
3. 拆分 `AppRepository` 为专一 Repository
4. 新增 `ConversationViewModel` 会话管理
5. 重写 `ChatViewModel` 支持流式
6. 新增 `StreamingText` Markdown 增量组件

### Phase 2 — 语音 + 多模态 (P1)
1. 新增 `VoiceRepositoryImpl` (ASR + TTS)
2. 新增 `VoiceInputBar` 组件
3. 新增 `SpeechModule` Hilt Module
4. 集成 ML Kit OCR 到 CameraScreen
5. 新增相册图片选择

### Phase 3 — 设置 + 增强 (P1/P2)
1. 新增 `SettingsScreen` + `SettingsViewModel`
2. DataStore 保存模型参数
3. 主题切换完善 (跟随系统)
4. 新增 `SettingsRepository` + `UpdateSettingsUseCase`
