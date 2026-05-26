# @Preview 预览注解 — 需求规格说明

## 1. @Preview 统一风格配置

### 1.1 基础配置模板

所有 Screen 级别的 @Preview 应遵循以下统一模板：

```kotlin
@Preview(
    name = "[ScreenName] 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,  // MaterialTheme 深色背景色 (dark surface)
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO  // 默认浅色模式
)
@Preview(
    name = "[ScreenName] 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,  // MaterialTheme 浅色背景色 (light surface)
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES  // 深色模式
)
```

### 1.2 配置项说明

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `name` | `"{ScreenName} 预览"` | 简体中文命名，便于团队识别。格式：文件名去掉"Screen"后缀 + " 预览" |
| `showBackground` | `true` | 始终显示背景，确保预览能看到实际背景色效果 |
| `backgroundColor` | `0xFF1C1B1F` (浅色) / `0xFFFEFBFF` (深色) | 使用 Material3 默认 surface 色。注意：@Preview 注解要求 compile-time 常量，不能直接引用 `MaterialTheme.colorScheme`，故使用硬编码的 Material3 默认色值 |
| `showSystemUi` | `false` | 默认不显示系统 UI 装饰（状态栏、导航栏），聚焦组件本身。后续可以根据需要开启 |
| `uiMode` | `Configuration.UI_MODE_NIGHT_NO` / `UI_MODE_NIGHT_YES` | 每个 Screen 提供两个 @Preview：浅色模式 + 深色模式 |

### 1.3 引入方式

在每个 Screen 文件的 import 区域添加：

```kotlin
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
```

---

## 2. ViewModel Mock 方案

### 2.1 问题分析

项目中 Screens 的 ViewModel 参数有两种写法：

| 模式 | 示例 | 文件 |
|------|------|------|
| **带默认值（hiltViewModel()）** | `viewModel: XxxViewModel = hiltViewModel()` | FlashcardScreen, ChatScreen, SettingsScreen, DashboardScreen, OnboardingScreen |
| **无默认值** | `viewModel: LoginViewModel` | LoginScreen, SplashScreen (函数体内手动调用) |

**问题**：`hiltViewModel()` 在 @Preview 上下文中无法正常工作，因为 Preview 没有 Hilt Application/Activity 的 CompositionLocal。

### 2.2 推荐方案：函数参数默认值 + Mock State

**统一策略**：为所有带 ViewModel 参数的 Screen 添加一个 `PreviewXxxScreen` 包装函数，或在 Screen 函数中使用默认参数提供 mock 状态。

**方案 A（推荐）— 包装函数 + mock UiState**：

创建一个跟随主 Screen 的预览专用函数，直接传入 mock 的 UiState，绕过 ViewModel。例如：

```kotlin
// === FlashcardScreen.kt 中的预览函数 ===

@Preview(
    name = "Flashcard 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun FlashcardScreenPreview() {
    AITutorTheme {
        FlashcardScreen(
            viewModel = FlashcardViewModel().apply {
                // 注入 mock state (通过 ViewModel 内部暴露的方法或反射)
            }
        )
    }
}
```

**但这种方案仍有侵入性**，更好的方式是：

**方案 B（推荐）— 简化预览，直接在 @Preview 函数中预览关键子组件**。

对于需要完整 Screen 预览的场景：

**方案 C（最推荐）— 创建 `PreviewFlashcardScreen` 内部函数，直接布局 mock UI 内容**：

```kotlin
@Preview(
    name = "Flashcard 卡片预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun PreviewFlashcardScreen() {
    AITutorTheme {
        // 直接使用 mock 数据，不依赖 ViewModel
        FlashcardScreen(
            viewModel = object : FlashcardViewModel() {
                // override uiState to return mock data
                override val uiState = ... 
            }
        )
    }
}
```

**但 Kotlin 中无法 override 非 open 的 property**。

### 2.3 最终推荐方案

**为每个需要 Screen 级别预览的文件，创建独立的 Preview 包装函数**，在函数内部：

1. 创建一个 `mutableStateOf` 存储 mock UiState
2. 使用 `CompositionLocalProvider` 提供 fake 依赖（可选）
3. 用 `AITutorTheme` 包裹

**简化版实现**（以 FlashcardScreen 为例）：

```kotlin
@Preview(
    name = "Flashcard 复习中",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun FlashcardScreenPreview_Cards() {
    AITutorTheme {
        FlashcardScreen(
            viewModel = FlashcardViewModel()  // hiltViewModel() 在 preview 中会回退到默认构造函数
        )
    }
}
```

> **注意**：`hiltViewModel()` 在 Preview 上下文中如果依赖了默认构造 + 使用 `@HiltViewModel` 注解且构造函数参数有默认值，可能能工作。但最佳实践仍是创建 mock 数据直接渲染。

### 2.4 针对 LoginScreen 的特殊方案

LoginScreen 的 ViewModel 参数**没有默认值** (`viewModel: LoginViewModel`)，因此必须显式传入：

```kotlin
@Preview(
    name = "登录页预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun LoginScreenPreview() {
    AITutorTheme {
        // LoginViewModel 无默认构造函数，需要 mock
        // 方案：创建一个 PreviewLoginViewModel 继承 LoginViewModel
        LoginScreen(
            viewModel = PreviewLoginViewModel(),
            onLoginSuccess = {}
        )
    }
}
```

需要额外创建一个 mock ViewModel 类：

```kotlin
private class PreviewLoginViewModel : LoginViewModel() {
    // 覆盖 uiState 提供 mock 数据
    override val uiState: LoginUiState get() = LoginUiState(
        phone = "13800000000",
        password = "********",
        isLoading = false,
        isSuccess = false,
        errorMessage = null,
        isRegister = false,
        email = ""
    )
}
```

> ⚠️ **注意**：`LoginViewModel.uiState` 如果是普通的 `val`（非 open），则上述方法无法 override。此时需要在 LoginViewModel 中**将 uiState 改为 open val**，或**在 Preview 中直接渲染 UI 内容而不使用 ViewModel**。

### 2.5 Mock 数据约定

对于所有需要提供 mock UiState 的场景，遵循以下数据约定：

| UiState 字段 | Mock 值 | 说明 |
|-------------|---------|------|
| 文本/字符串 | 使用示例中文内容 | 如："示例手机号 13800000000" |
| isLoading | `false` | 预览不应展示加载态（除非专门测试 Loading） |
| 列表/集合 | 2-3 个 mock 条目 | 展示填充状态，避免空列表 |
| 错误信息 | `null` | 预览不应展示错误态（除非专门测试 Error） |
| 回调 lambda | `{}` | 空实现即可 |

---

## 3. 各 Screen 的具体注解配置

### 3.1 FlashcardScreen.kt

**签名**：`FlashcardScreen(viewModel: FlashcardViewModel = hiltViewModel(), onNavigateBack: () -> Unit = {})`

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `Flashcard 复习中` | 卡片翻转中状态，显示卡片内容 | uiMode=NIGHT_NO |
| `Flashcard 复习中 (深色)` | 深色模式下的卡片 | uiMode=NIGHT_YES |
| `Flashcard 已完成` | 今日复习已完成状态（AllDone） | 需要 mock AllDone state |
| `Flashcard 加载中` | Loading 状态 | 需要 mock Loading state |
| `Flashcard 错误态` | Error 状态 | 需要 mock Error state |

### 3.2 ChatScreen.kt

**签名**：`ChatScreen(conversationId: Long? = null, onNavigateToCamera: () -> Unit = {}, onNavigateToProfile: () -> Unit = {}, viewModel: ChatViewModel = hiltViewModel())`

ChatScreen 体量较大（~658 行），**不建议做完整的 Screen 预览**。改为对其内部已提取的子组件做预览（见第 4 节）。

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `Chat 空对话` | 无消息时的空状态 | uiMode=NIGHT_NO |
| `Chat 空对话 (深色)` | 深色模式空状态 | uiMode=NIGHT_YES |

### 3.3 SettingsScreen.kt

**签名**：`SettingsScreen(onNavigateToProfile: () -> Unit = {}, ..., viewModel: SettingsViewModel = hiltViewModel())`

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `设置页预览` | 完整设置页面 | uiMode=NIGHT_NO |
| `设置页预览 (深色)` | 深色模式 | uiMode=NIGHT_YES |

### 3.4 LoginScreen.kt → auth/LoginScreen.kt

**签名**：`LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit)`

> **注意**：此文件在 `auth/` 子目录，不在 `ui/` 根目录。

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `登录页预览` | 登录模式（默认态） | uiMode=NIGHT_NO |
| `登录页预览 (深色)` | 深色模式 | uiMode=NIGHT_YES |
| `注册页预览` | 注册模式，显示邮箱输入框 | 需要 mock isRegister=true |
| `登录页加载中` | 提交中的 loading 状态 | 需要 mock isLoading=true |

**⚠️ 特殊处理**：LoginScreen 的 ViewModel 参数无默认值，需要一个 mock ViewModel。建议：
1. 在 LoginViewModel 中将 `uiState` 改为 `open val` 以便 override
2. 或在 Preview 中用一个简单的 Composable 重新绘制表单 UI

### 3.5 DashboardScreen.kt → screen/dashboard/DashboardScreen.kt

**签名**：`DashboardScreen(viewModel: DashboardViewModel = hiltViewModel(), onNavigateToQuiz: () -> Unit = {}, onNavigateToWrongAnswers: () -> Unit = {})`

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `仪表盘预览` | 有数据的仪表盘 | uiMode=NIGHT_NO |
| `仪表盘预览 (深色)` | 深色模式 | uiMode=NIGHT_YES |
| `仪表盘空态` | 无数据空状态 | 需要 mock isEmpty=true |
| `仪表盘加载中` | Skeleton 加载态 | 需要 mock isLoading=true |

### 3.6 SplashScreen.kt → splash/SplashScreen.kt

**签名**：`SplashScreen(onNavigateToLogin: () -> Unit, onNavigateToMain: () -> Unit, onNavigateToOnboarding: () -> Unit = {})`

ViewModel 在函数体内手动调用：`val viewModel: SplashViewModel = hiltViewModel()`

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `闪屏页预览` | 默认动画展示状态 | uiMode=NIGHT_NO |
| `闪屏页预览 (深色)` | 深色模式 | uiMode=NIGHT_YES |

### 3.7 OnboardingScreen.kt → onboarding/OnboardingScreen.kt

**签名**：`OnboardingScreen(onOnboardingComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel())`

| @Preview 名称 | 说明 | 特殊参数 |
|--------------|------|---------|
| `引导页预览` | 第一页引导页 | uiMode=NIGHT_NO |
| `引导页预览 (深色)` | 深色模式 | uiMode=NIGHT_YES |

### 3.8 其余 Screen 文件（使用相同模式）

所有带 `= hiltViewModel()` 默认值的 Screen 均按上述模式添加 @Preview，命名格式一致：

- `ProfileScreen.kt` → `Profile 预览`
- `FavoritesScreen.kt` → `收藏页 预览`
- `SubscriptionScreen.kt` → `订阅页 预览`
- `CameraScreen.kt` → `相机页 预览`
- `ExportReportScreen.kt` / `ReportExportScreen.kt` → `报告导出 预览`
- `WrongAnswerScreen.kt` → `错题本 预览`
- `ReviewScreen.kt` → `复习页 预览`
- `QuizScreen.kt` → `测验页 预览`
- `CrashLogScreen.kt` → `崩溃日志 预览`
- `PrivacyPolicyScreen.kt` → `隐私政策 预览`
- `UserAgreementScreen.kt` → `用户协议 预览`

---

## 4. 需要额外添加子 Composable 预览的文件

### 4.1 需提取子 Composable 并添加 @Preview 的文件

以下文件包含较大的 Composable 函数，内部有可提取的 UI 区块，建议先提取为独立的 Composable，再添加 @Preview：

| 文件 | 需要提取的子 Composable | 原因 |
|------|------------------------|------|
| **SettingsScreen.kt** | `SettingsSlider`（第 454 行，当前为 private） | 独立的设置滑块组件，提取后可为不同配置做预览（Temperature、Top-P、Max Tokens、TTS Speed）。建议改为 internal 或提取到 `components/` |
| **SettingsScreen.kt** | `CacheManagementSection`（第 412 行，外部引用） | 如果该组件尚未添加 @Preview，建议追加 |
| **DashboardScreen.kt** | `WrongAnswerEntryCard`（第 139 行，当前为 private） | 独立的错题本入口卡片，包含 BadgedBox + Icon + Text 组合逻辑，适合独立预览不同 count 状态 |
| **LoginScreen.kt** | 登录表单内容（phone + password + email 输入域） | 如果在 Preview 中需要渲染不同模式（登录/注册）而不依赖 ViewModel，建议提取为 `LoginFormContent` 组件 |

### 4.2 已有子组件但未添加 @Preview 的文件

以下文件已提取了子组件在 `components/` 目录，但可能缺少各自的 @Preview。需要开发者在实现时补充：

| 模块 | 组件目录 | 状态 |
|------|---------|------|
| **ChatScreen** | `ui/chat/components/*` | 已提取十几个组件（MessageBubble, ChatInputBar, AgentStatusIndicator 等） |
| **DashboardScreen** | `ui/screen/dashboard/components/*` | 已提取（StatsOverviewCard, TrendChart, KnowledgeGraph 等） |

**建议**：上述 components 目录下的每个文件，如果其主 Composable 函数没有 @Preview，应逐一补充。

### 4.3 可提取但非必须的文件

| 文件 | 候选子 Composable | 优先级 |
|------|------------------|--------|
| FlashcardScreen.kt | `FlashcardFront` 和 `FlashcardBack` 已提取（约第 93-95 行引用） | 不强制，已提取 |
| OnboardingScreen.kt | `OnboardingPage` 的单页内容（emoji + title + description） | 低，但可提升开发效率 |
| DashboardScreen.kt | `getSubjectKey()` 工具函数 | 不需要提取 UI |

### 4.4 提取子 Composable 的 @Preview 配置

提取后的子 Composable 使用相同的 @Preview 配置模板，仅 `name` 改为组件名称：

```kotlin
@Preview(
    name = "SettingsSlider 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SettingsSliderPreview() {
    AITutorTheme {
        SettingsSlider(
            label = "Temperature",
            value = "0.7",
            onValueChange = {},
            valueRange = 0f..2f,
            steps = 19,
            currentValue = 0.7f
        )
    }
}
```

---

## 5. 实现步骤总结

1. **全局准备**：确认项目中存在 `AITutorTheme` 主题包装函数（用于 @Preview 中的主题环境）
2. **为 LoginViewModel 添加 open 修饰符**（如需要 override uiState），或其构造函数提供默认参数
3. **按 Screen 逐一添加**：
   - 无 ViewModel 的 Screen：直接添加两份 @Preview（浅色 + 深色）包裹在 `AITutorTheme` 中
   - 有 `= hiltViewModel()` 的 Screen：添加 @Preview 函数，传入默认 ViewModel（Preview 会尝试使用默认构造）
   - 无默认 ViewModel 的 Screen（LoginScreen）：创建 mock ViewModel 或提取子组件预览
4. **子组件提取**：对 SettingsSlider、WrongAnswerEntryCard 等组件提取并添加 @Preview
5. **验证**：在 Android Studio 中打开每个 Screen 文件，确认 Split/Design 面板可以正常显示预览

### 附录：@Preview 完整配置常量

```kotlin
// Preview 统一背景色
const val PREVIEW_BG_LIGHT = 0xFFFEFBFF   // light surface color (Material3)
const val PREVIEW_BG_DARK  = 0xFF1C1B1F   // dark surface color (Material3)
```
