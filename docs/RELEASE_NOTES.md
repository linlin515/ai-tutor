# Release Notes — Release APK 构建 + ProGuard 规则完善

## 构建信息

| 项目 | 值 |
|------|-----|
| **编译状态** | ✅ BUILD SUCCESSFUL |
| **APK 路径** | `app/build/outputs/apk/release/app-release.apk` |
| **完整路径** | `/home/linruihang0517/hermes/projects/ai-tutor-android/app/build/outputs/apk/release/app-release.apk` |
| **Release APK 体积** | 50MB（通用 APK，包含 4 种 CPU 架构） |
| **单架构 APK 估算** | ~28MB（arm64-v8a 仅需约 10MB ML Kit 原生库） |
| **Debug APK 体积** | 59MB（此前记录） |
| **体积缩减** | 约 15%（通用 APK）/ 约 52%（单架构） |
| **签名方式** | debug.keystore（别名: aitutor，用于测试签名流程） |
| **ProGuard** | 已启用（`app/proguard-rules.pro`），含完整保留规则 |

## ProGuard 规则覆盖

| 类别 | 覆盖范围 |
|------|---------|
| **Retrofit/OkHttp/Gson** | 保留 7 个 API 接口、全部 DTO 数据类、@SerializedName 字段、TypeAdapter |
| **Room** | 保留 11 个 Entity、全部 DAO、Database 抽象类 |
| **Hilt/Dagger** | 保留 @Module、@Provides、@Binds、@Inject、Hilt 生成代码 |
| **Kotlin Coroutines** | 保留 MainDispatcherFactory、ExceptionHandler |
| **Compose** | 保留 Compose 运行时、@Composable 函数签名、Navigation |
| **CameraX** | 保留所有 CameraX 类、ImageAnalysis.Analyzer |
| **ML Kit** | 保留文字识别库 |
| **Coil** | 保留图片加载库 |
| **DataStore** | 保留 DataStore 类 |
| **Security Crypto** | 忽略 Tink 缺失类警告（com.google.api.client.http.*） |
| **自定义数据模型** | 保留 dto/、entity/、domain.model/、data.media/ 下所有类 |
| **枚举/Parcelable/Serializable** | 保留 values()/valueOf()、CREATOR、writeObject/readObject |

## Deprecation 警告

- **总数: 13**
- 均为 Material Icons 的 AutoMirrored 迁移（建议替换为 `Icons.AutoMirrored.Filled.*`）以及 CameraX `setTargetResolution` 弃用

### Top 5 Deprecation 警告

| # | 文件 | 行号 | 说明 |
|---|------|------|------|
| 1 | CameraScreen.kt | 128 | ArrowBack → Icons.AutoMirrored.Filled.ArrowBack |
| 2 | CameraScreen.kt | 431 | setTargetResolution 弃用 |
| 3 | ChatInputBar.kt | 84 | Send → Icons.AutoMirrored.Filled.Send |
| 4 | MessageBubble.kt | 86 | VolumeUp → Icons.AutoMirrored.Filled.VolumeUp |
| 5 | SubscriptionScreen.kt | 41 | ArrowBack → Icons.AutoMirrored.Filled.ArrowBack |

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

## APK 体积构成分析

| 组件 | 占用（压缩后） | 说明 |
|------|--------------|------|
| classes.dex + classes2.dex | ~31MB | 混淆后的 DEX 字节码 |
| ML Kit 原生库 (4 arch) | ~38MB | `libmlkit_google_ocr_pipeline.so`（单架构约 10MB） |
| image_processing 原生库 | ~132KB | 跨 4 架构 |
| 其他（资源/arsc/签名） | ~2MB | 布局、图片、字符串等 |

> **建议**: 生产环境使用 **Android App Bundle (AAB)** 发布，可自动按设备架构分发，将实际下载体积降至 ~20-30MB。

## 配置文件变更

- **app/proguard-rules.pro**: 从空文件重写为完整规则（约 270 行），覆盖所有关键库
- **KEYS**: 构建使用 `debug.keystore` 测试签名，正式上线需替换生产签名
- **环境变量**: `KEYSTORE_PASSWORD=aitutor123`, `KEY_ALIAS=aitutor`, `KEY_PASSWORD=aitutor123`
- **security-crypto**: 添加 R8 `-dontwarn` 规则忽略 Tink 可选依赖（Google HTTP Client, Joda-Time）
