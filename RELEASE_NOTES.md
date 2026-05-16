# Release Notes — APK Release Build

## 构建信息

| 项目 | 值 |
|------|-----|
| **编译状态** | ✅ BUILD SUCCESSFUL |
| **APK 路径** | `app/build/outputs/apk/release/app-release.apk` |
| **完整路径** | `/home/linruihang0517/hermes/projects/ai-tutor-android/app/build/outputs/apk/release/app-release.apk` |
| **签名方式** | debug.keystore （别名: aitutor, 用于测试签名流程） |
| **ProGuard** | 已启用（`proguard-rules.pro`） |

## Deprecation 警告

- **总数: 13**
- 均为 Material Icons 的 AutoMirrored 迁移（建议替换为 `Icons.AutoMirrored.Filled.*`）以及 CameraX `setTargetResolution` 弃用

### Top 5 Deprecation 警告

1. `CameraScreen.kt:128` — `ArrowBack: ImageVector` → 请改用 `Icons.AutoMirrored.Filled.ArrowBack`
2. `CameraScreen.kt:431` — `setTargetResolution(Size): ImageAnalysis.Builder` — 已在 Java 中弃用
3. `ChatInputBar.kt:84` — `Send: ImageVector` → 请改用 `Icons.AutoMirrored.Filled.Send`
4. `MessageBubble.kt:86` — `VolumeUp: ImageVector` → 请改用 `Icons.AutoMirrored.Filled.VolumeUp`
5. `SocraticQuestionBubble.kt:76` — `HelpOutline: ImageVector` → 请改用 `Icons.AutoMirrored.Filled.HelpOutline`

### 完整列表（13 条）

| # | 文件 | 行号 | 说明 |
|---|------|------|------|
| 1 | CameraScreen.kt | 128 | ArrowBack → Icons.AutoMirrored.Filled.ArrowBack |
| 2 | CameraScreen.kt | 431 | setTargetResolution 弃用 |
| 3 | ChatInputBar.kt | 84 | Send → Icons.AutoMirrored.Filled.Send |
| 4 | MessageBubble.kt | 86 | VolumeUp → Icons.AutoMirrored.Filled.VolumeUp |
| 5 | SocraticQuestionBubble.kt | 76 | HelpOutline → Icons.AutoMirrored.Filled.HelpOutline |
| 6 | SocraticQuestionBubble.kt | 164 | ArrowForward → Icons.AutoMirrored.Filled.ArrowForward |
| 7 | UnderstandingBadge.kt | 60 | TrendingUp → Icons.AutoMirrored.Filled.TrendingUp |
| 8 | EmptyStateView.kt | 28 | Chat → Icons.AutoMirrored.Filled.Chat |
| 9 | AppNavGraph.kt | 68 | Chat → Icons.AutoMirrored.Filled.Chat |
| 10 | AppNavGraph.kt | 71 | MenuBook → Icons.AutoMirrored.Filled.MenuBook |
| 11 | ProfileScreen.kt | 40 | ArrowBack → Icons.AutoMirrored.Filled.ArrowBack |
| 12 | ProfileScreen.kt | 176 | ExitToApp → Icons.AutoMirrored.Filled.ExitToApp |
| 13 | SubscriptionScreen.kt | 41 | ArrowBack → Icons.AutoMirrored.Filled.ArrowBack |

## 配置变更

- **app/build.gradle.kts**: 解除 signing config 注释，使用 `debug.keystore` 测试签名
- 环境变量: `KEYSTORE_PASSWORD=aitutor123`, `KEY_ALIAS=aitutor`, `KEY_PASSWORD=aitutor123`
- Release buildType 已配置 `signingConfig = signingConfigs.getByName("release")`
