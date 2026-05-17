# AI 助手 Android App 项目目录结构规划

> 基于 Clean Architecture + MVVM 模式  
> 包名: `com.aitutor.app` | 语言: Kotlin | UI: Jetpack Compose | DI: Hilt

---

## 完整目录树

```
ai-tutor-android/
├── ARCH.md                                    # 架构设计文档
├── API.md                                     # API 接口设计文档
├── build.gradle.kts                           # 项目级 Gradle
├── settings.gradle.kts                        # 项目设置
├── gradle.properties                          # Gradle 属性
├── gradle/
│   ├── libs.versions.toml                     # 统一版本目录
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── app/
    ├── build.gradle.kts                       # 模块级 Gradle
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── drawable/
        │   │   │   ├── ic_launcher_background.xml
        │   │   │   └── ic_launcher_foreground.xml
        │   │   ├── mipmap-anydpi-v26/
        │   │   │   ├── ic_launcher.xml
        │   │   │   └── ic_launcher_round.xml
        │   │   ├── values/
        │   │   │   ├── strings.xml
        │   │   │   └── themes.xml
        │   │   └── xml/
        │   │       └── network_security_config.xml
        │   │
        │   └── java/com/aitutor/app/
        │       │
        │       ├── AiTutorApp.kt              # [已存在] @HiltAndroidApp Application
        │       ├── MainActivity.kt            # [已存在] Single Activity 入口
        │       │
        │       ├── di/                        # ★ Hilt 依赖注入
        │       │   ├── NetworkModule.kt       #   [已存在] Retrofit, OkHttp, API
        │       │   ├── DatabaseModule.kt      #   [待新建] Room DB + DAO
        │       │   ├── RepositoryModule.kt    #   [待新建] Repository 绑定
        │       │   └── SpeechModule.kt        #   [待新建] TTS/ASR 引擎
        │       │
        │       ├── data/                      # ★ 数据层
        │       │   ├── remote/                #   远程数据源
        │       │   │   ├── api/
        │       │   │   │   ├── AiTutorApi.kt      # [已存在] REST API
        │       │   │   │   └── ChatStreamApi.kt   # [待新建] SSE 流式接口
        │       │   │   ├── dto/                   # 网络传输对象
        │       │   │   │   ├── ApiResponse.kt     # [已存在] 通用包装
        │       │   │   │   ├── AuthDtos.kt        # [待新建] 认证 DTO
        │       │   │   │   ├── ChatDtos.kt        # [待新建] 聊天 DTO
        │       │   │   │   └── SettingsDtos.kt    # [待新建] 设置 DTO
        │       │   │   ├── interceptor/
        │       │   │   │   ├── AuthInterceptor.kt # [已存在]
        │       │   │   │   └── TokenManager.kt    # [已存在]
        │       │   │   └── datastore/
        │       │   │       └── SettingsDataStore.kt # [待新建]
        │       │   │
        │       │   ├── local/                 #   本地数据源 (Room)
        │       │   │   ├── db/
        │       │   │   │   └── AiTutorDatabase.kt # [待新建]
        │       │   │   ├── dao/
        │       │   │   │   ├── ConversationDao.kt # [待新建]
        │       │   │   │   └── MessageDao.kt      # [待新建]
        │       │   │   └── entity/
        │       │   │       ├── ConversationEntity.kt # [待新建]
        │       │   │       └── MessageEntity.kt      # [待新建]
        │       │   │
        │       │   ├── repository/            #   Repository 实现
        │       │   │   ├── ChatRepositoryImpl.kt    # [待新建]
        │       │   │   ├── AuthRepositoryImpl.kt    # [待新建]
        │       │   │   ├── VoiceRepositoryImpl.kt   # [待新建]
        │       │   │   └── SettingsRepositoryImpl.kt # [待新建]
        │       │   │
        │       │   └── mapper/                #   对象映射
        │       │       ├── ChatMapper.kt      #   [待新建]
        │       │       └── UserMapper.kt      #   [待新建]
        │       │
        │       ├── domain/                    # ★ 领域层 (纯 Kotlin)
        │       │   ├── model/
        │       │   │   ├── User.kt            # [已存在] 用户
        │       │   │   ├── ChatMessage.kt     # [已存在] 消息
        │       │   │   ├── Conversation.kt    # [待新建] 会话
        │       │   │   ├── VoiceState.kt      # [待新建] 语音状态
        │       │   │   └── AppSettings.kt     # [待新建] 设置模型
        │       │   ├── repository/            # Repository 接口 (抽象)
        │       │   │   ├── ChatRepository.kt      # [待新建]
        │       │   │   ├── AuthRepository.kt      # [待新建]
        │       │   │   ├── VoiceRepository.kt     # [待新建]
        │       │   │   └── SettingsRepository.kt  # [待新建]
        │       │   └── usecase/               #   业务用例
        │       │       ├── chat/
        │       │       │   ├── SendMessageUseCase.kt     # [待新建]
        │       │       │   ├── StreamChatUseCase.kt      # [待新建]
        │       │       │   └── GetChatHistoryUseCase.kt  # [待新建]
        │       │       ├── conversation/
        │       │       │   ├── CreateConversationUseCase.kt  # [待新建]
        │       │       │   ├── SwitchConversationUseCase.kt  # [待新建]
        │       │       │   └── DeleteConversationUseCase.kt  # [待新建]
        │       │       ├── voice/
        │       │       │   ├── StartListeningUseCase.kt # [待新建]
        │       │       │   └── SpeakTextUseCase.kt      # [待新建]
        │       │       └── settings/
        │       │           └── UpdateSettingsUseCase.kt # [待新建]
        │       │
        │       └── ui/                        # ★ UI 层
        │           ├── navigation/
        │           │   ├── Routes.kt          # [已存在]
        │           │   └── AppNavGraph.kt     # [已存在]
        │           ├── theme/
        │           │   ├── Theme.kt           # [已存在]
        │           │   ├── Color.kt           # [已存在]
        │           │   └── Type.kt            # [已存在]
        │           ├── chat/                  #   聊天模块
        │           │   ├── ChatScreen.kt      #   [已存在]
        │           │   ├── ChatViewModel.kt   #   [重写中]
        │           │   ├── ChatUiState.kt     #   [待新建]
        │           │   └── components/
        │           │       ├── MessageBubble.kt   # [待新建]
        │           │       ├── ChatInputBar.kt    # [待新建]
        │           │       ├── VoiceInputBar.kt   # [待新建]
        │           │       └── StreamingText.kt   # [待新建]
        │           ├── conversation/          #   会话管理
        │           │   ├── ConversationListSheet.kt # [待新建]
        │           │   └── ConversationViewModel.kt # [待新建]
        │           ├── auth/                  #   认证模块
        │           │   ├── LoginScreen.kt     #   [已存在]
        │           │   └── LoginViewModel.kt  #   [已存在]
        │           ├── settings/              #   设置模块 [待新建]
        │           │   ├── SettingsScreen.kt      # [待新建]
        │           │   └── SettingsViewModel.kt   # [待新建]
        │           ├── profile/               #   个人中心
        │           │   ├── ProfileScreen.kt   #   [已存在]
        │           │   └── ProfileViewModel.kt#   [已存在]
        │           ├── camera/                #   拍照解题
        │           │   ├── CameraScreen.kt    #   [已存在]
        │           │   └── CameraViewModel.kt #   [已存在]
        │           ├── subscription/          #   订阅
        │           │   └── SubscriptionScreen.kt  # [已存在]
        │           └── common/                #   通用组件
        │               ├── LoadingIndicator.kt    # [待新建]
        │               ├── ErrorView.kt           # [待新建]
        │               ├── MarkdownRender.kt      # [待新建]
        │               └── EmptyStateView.kt      # [待新建]
        │
        └── test/
            └── java/com/aitutor/app/
                ├── data/
                ├── domain/
                └── ui/
```

---

## 文件状态说明

| 状态 | 含义 | 文件数 |
|------|------|--------|
| [已存在] | 现有文件，无需修改 | 19 |
| [待新建] | 架构规划中需新增 | 38 |
| [重写中] | 需在现有基础上重构 | 1 |

### 现有文件清单 (19个)

```
AiTutorApp.kt
MainActivity.kt
data/api/AiTutorApi.kt
data/api/AuthInterceptor.kt
data/api/TokenManager.kt
data/model/Models.kt (包含所有 DTO)
data/repository/AppRepository.kt
di/NetworkModule.kt
domain/model/Models.kt (包含 User, ChatMessage, SolveResult, etc.)
ui/auth/LoginScreen.kt
ui/auth/LoginViewModel.kt
ui/chat/ChatScreen.kt
ui/chat/ChatViewModel.kt
ui/navigation/AppNavGraph.kt
ui/navigation/Routes.kt
ui/navigation/SplashViewModel.kt
ui/profile/ProfileScreen.kt
ui/profile/ProfileViewModel.kt
ui/solve/SolveScreen.kt
ui/solve/SolveViewModel.kt
ui/subscription/SubscriptionScreen.kt
ui/theme/Theme.kt
build.gradle.kts (project + app)
settings.gradle.kts
gradle.properties
gradle/libs.versions.toml
AndroidManifest.xml
network_security_config.xml, strings.xml, themes.xml
icon resources (drawable, mipmap)
```

### 待新建文件清单 (38个)

**数据层 (13个)**
- `data/local/db/AiTutorDatabase.kt`
- `data/local/dao/ConversationDao.kt`
- `data/local/dao/MessageDao.kt`
- `data/local/entity/ConversationEntity.kt`
- `data/local/entity/MessageEntity.kt`
- `data/remote/api/ChatStreamApi.kt`
- `data/remote/dto/AuthDtos.kt`
- `data/remote/dto/ChatDtos.kt`
- `data/remote/dto/SettingsDtos.kt`
- `data/remote/datastore/SettingsDataStore.kt`
- `data/repository/ChatRepositoryImpl.kt`
- `data/repository/VoiceRepositoryImpl.kt`
- `data/repository/SettingsRepositoryImpl.kt`

**领域层 (16个)**
- `domain/model/Conversation.kt`
- `domain/model/VoiceState.kt`
- `domain/model/AppSettings.kt`
- `domain/repository/ChatRepository.kt`
- `domain/repository/VoiceRepository.kt`
- `domain/repository/SettingsRepository.kt`
- `domain/usecase/chat/SendMessageUseCase.kt`
- `domain/usecase/chat/StreamChatUseCase.kt`
- `domain/usecase/chat/GetChatHistoryUseCase.kt`
- `domain/usecase/conversation/CreateConversationUseCase.kt`
- `domain/usecase/conversation/SwitchConversationUseCase.kt`
- `domain/usecase/conversation/DeleteConversationUseCase.kt`
- `domain/usecase/voice/StartListeningUseCase.kt`
- `domain/usecase/voice/SpeakTextUseCase.kt`
- `domain/usecase/settings/UpdateSettingsUseCase.kt`

**UI 层 (9个)**
- `ui/chat/ChatUiState.kt`
- `ui/chat/components/MessageBubble.kt`
- `ui/chat/components/ChatInputBar.kt`
- `ui/chat/components/VoiceInputBar.kt`
- `ui/chat/components/StreamingText.kt`
- `ui/conversation/ConversationListSheet.kt`
- `ui/conversation/ConversationViewModel.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/settings/SettingsViewModel.kt`

**DI 模块 (3个)**
- `di/DatabaseModule.kt`
- `di/RepositoryModule.kt`
- `di/SpeechModule.kt`

**通用组件 (4个)**
- `ui/common/LoadingIndicator.kt`
- `ui/common/ErrorView.kt`
- `ui/common/MarkdownRender.kt`
- `ui/common/EmptyStateView.kt`

---

## 架构演进路径

```
当前 (单体 Repository)          →      目标 (Clean Architecture)
──────────────                         ─────────────────────
AppRepository                           AuthRepositoryImpl
  (所有逻辑混在一起)                     ChatRepositoryImpl
                                         VoiceRepositoryImpl
                                         SettingsRepositoryImpl

Domain 模型内联在 data/model             Domain 模型独立
                                         Repository 接口在 domain

ViewModel 直接调 API                     ViewModel → UseCase → Repository
```

详细架构设计参见 [ARCH.md](./ARCH.md)
