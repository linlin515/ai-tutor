# v2.3 工程化迭代规划

> 生成日期: 2026-05-17
> 负责人: PM
> 状态: ✅ 规划完成

---

## 一、任务总览

| 任务 | 类型 | 预估工时 | 依赖 | 并行可行 |
|------|:----:|:--------:|:----:|:--------:|
| P1-4 CI/CD 自动化构建 | 基础设施 | ~3 人日 | 无 | ✅ 可并行 |
| P2-1 单元测试覆盖 | 质量保障 | ~7 人日 | 无 | ✅ 可并行 |

**结论：两个任务完全独立，文件无重叠，可以 100% 并行执行。**

---

## 二、P1-4 CI/CD 自动化构建（~3 人日）

### 2.1 GitHub Actions Runner Android SDK 支持确认

**结论：GitHub Actions `ubuntu-latest` runner 完全支持 Android SDK。**

- `ubuntu-latest` 预装 Android SDK 命令行工具（`sdkmanager`）
- `ANDROID_HOME` 默认路径: `/usr/local/lib/android/sdk`
- 预装多个 SDK 版本（通常包含 compileSdk=34 所需 platform）
- 环境变量 `ANDROID_SDK_ROOT` 和 `ANDROID_HOME` 均可使用
- Gradle Wrapper 8.5 自带 Gradle 下载能力
- JDK 17 可通过 `actions/setup-java@v4` 精确设置

**已知注意事项：**
- 需显式设置 `ANDROID_HOME` 环境变量（workflow 中通过 `$ANDROID_HOME` 引用）
- Release 签名需通过 GitHub Secrets 注入 `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
- PR 构建可使用 debug signing（debug.keystore 自动生成）
- Gradle cache 使用 `actions/cache@v4` 可大幅加速后续构建

### 2.2 工作流设计

**触发条件：**
- `push` 到 `main` 分支
- `pull_request` 到 `main` 分支

**工作流步骤：**

```
1. checkout → actions/checkout@v4
2. JDK 17 → actions/setup-java@v4 (distribution: 'temurin', java-version: 17)
3. ANDROID_HOME → 设置环境变量
4. Gradle cache → actions/cache@v4 (~/.gradle/caches, ~/.gradle/wrapper)
5. 授权 gradlew → chmod +x gradlew
6. assembleRelease → ./gradlew assembleRelease
7. APK 上传 → actions/upload-artifact@v4
8. （可选）GitHub Release → softprops/action-gh-release
```

### 2.3 文件清单

| 文件 | 操作 | 预估工时 | 说明 |
|------|------|:--------:|------|
| `.github/workflows/android-ci.yml` | **新增** | 1.5 人日 | 完整 CI/CD workflow |
| `android/gradle.properties` | 修改 | 0.2 人日 | 可增加 `org.gradle.parallel=true` 优化构建速度 |
| `android/app/build.gradle.kts` | 微调 | 0.3 人日 | 确保 release signing 环境变量兼容 CI |
| CI 验证调试 | 调试 | 1.0 人日 | 首次触发后调整 cache key/路径/权限问题 |

### 2.4 关键技术决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| JDK 版本 | Temurin JDK 17 | 与 `build.gradle.kts` 中 `JavaVersion.VERSION_17` 一致 |
| Gradle cache key | `gradle-${hashFiles('**/*.gradle*', '**/gradle-wrapper.properties')}` | 依赖文件变更时才失效 |
| APK artifact 命名 | `ai-tutor-v{versionName}-{sha}.apk` | 包含版本号和 commit hash |
| Release 触发 | 手动 workflow_dispatch + 打 Git Tag | 避免每次 push 都创建 Release |
| 构建变体 | assembleRelease + assembleDebug | PR 跑 Debug，push到 main 跑 Release |
| 签名配置 | PR: debug 自动签名；main: 从 Secrets 注入 | 保护 Release key |

### 2.5 验收标准

- [ ] PR 提交到 main → 自动触发 CI 构建
- [ ] push 到 main → 自动触发 CI 构建
- [ ] assembleRelease 编译成功，APK 产出
- [ ] APK 上传为 Actions Artifact，可从 GitHub UI 下载
- [ ] Gradle cache 生效，二次构建显著加速
- [ ] Release 构建使用正确的签名（非 debug）
- [ ] PR 构建使用 debug.keystore 自动签名
- [ ] ANDROID_HOME 环境变量正确传递

---

## 三、P2-1 单元测试覆盖（~7 人日）

### 3.1 现有测试覆盖评估

**已有测试（8 个文件，已覆盖 P1 核心）：**

| 测试文件 | 测试对象 | 行数 | 框架 | 状态 |
|----------|---------|:----:|:----:|:----:|
| `BaseViewModelTest.kt` | 测试基类 | 32 | JUnit4+MockK | ✅ 可用 |
| `MainCoroutineRule.kt` | TestDispatcher Rule | 28 | JUnit4 | ✅ 可用 |
| `ChatViewModelTest.kt` | ChatViewModel | 270 | JUnit4+MockK | ✅ 完整 |
| `LoginViewModelTest.kt` | LoginViewModel | 292 | JUnit4+MockK | ✅ 完整 |
| `ChatRepositoryImplTest.kt` | ChatRepositoryImpl | 200 | JUnit4+MockK | ✅ 完整 |
| `AuthRepositoryImplTest.kt` | AuthRepositoryImpl | 232 | JUnit4+MockK | ✅ 完整 |
| `AgentRepositoryImplTest.kt` | AgentRepositoryImpl | 82 | JUnit4+MockK+Robolectric | ✅ 完整 |
| `RoomMigrationTest.kt` | Room 迁移 | 132 | JUnit4 | ✅ 完整 |

**需要新增测试（按优先级排列）：**

### 3.2 核心测试目标（P0 — 必须覆盖）

#### Engine 层（纯 Kotlin，零 Android 依赖，最高 ROI）

| 引擎 | 源文件行数 | 预估测试行数 | 优先级 | 理由 |
|------|:---------:|:-----------:|:------:|------|
| `ScoreCalculator` | 34 | 60-80 | **P0** | 纯函数，积分计算核心逻辑，零依赖，极低测试成本 |
| `SpacedRepetitionEngine` | 84 | 120-160 | **P0** | 纯 Kotlin 类，SM-2 算法核心，无 Android 依赖 |
| `StreakCalculator` | 93 | 80-100 | **P0** | 纯 Object，连续学习天数计算，已有 StreakResult 模型 |
| `AchievementDetector` | 86 | 80-100 | **P0** | 纯 Object，成就解锁逻辑，边缘情况多 |

**Engine 层小计：~3 人日（4 个引擎，覆盖全部算法逻辑）**

#### ViewModel 层（需 Mock 依赖，中等成本）

| ViewModel | 源文件行数 | 预估测试行数 | 优先级 | 理由 |
|-----------|:---------:|:-----------:|:------:|------|
| `SettingsViewModel` | 203 | 180-220 | **P0** | CEO 指定，功能复杂（缓存清理/更新检测/参数设置/语言切换/Agent 模式切换） |
| `DashboardViewModel` | 122 | 100-140 | **P1** | 首页数据聚合（积分/连续/成就/排行榜） |
| `QuizViewModel` | 176 | 120-160 | **P1** | 测验流程状态机（答题/提交/评分），业务逻辑密集 |

**ViewModel 层小计：~2 人日（3 个 ViewModel）**

#### Repository 层（需 Mock API/DAO，中等成本）

| Repository | 源文件行数 | 预估测试行数 | 优先级 | 理由 |
|------------|:---------:|:-----------:|:------:|------|
| `SolveRepositoryImpl` | 79 | 100-130 | **P0** | CEO 指定，含图片上传/Streaming/步骤重试，错误处理复杂 |
| `SettingsRepositoryImpl` | 52 | 60-80 | **P1** | CEO 指定，但逻辑较简单（DataStore 读写委托） |
| `QuizRepositoryImpl` | 168 | 120-150 | **P1** | 测验 API 调用 + Room 持久化，数据流复杂 |
| `GamificationRepositoryImpl` | 65 | 60-80 | **P2** | 游戏化数据聚合，依赖多个 DAO |

**Repository 层小计：~1.5 人日（4 个 Repository）**

### 3.3 完整优先级矩阵

| 优先级 | 文件 | 层 | 预估工时 | 累积 |
|:------:|------|:--:|:--------:|:----:|
| **P0** | `ScoreCalculatorTest` | Engine | 0.3 人日 | 0.3 |
| **P0** | `StreakCalculatorTest` | Engine | 0.4 人日 | 0.7 |
| **P0** | `SpacedRepetitionEngineTest` | Engine | 0.6 人日 | 1.3 |
| **P0** | `AchievementDetectorTest` | Engine | 0.5 人日 | 1.8 |
| **P0** | `SettingsViewModelTest` | ViewModel | 1.0 人日 | 2.8 |
| **P0** | `SolveRepositoryImplTest` | Repository | 0.7 人日 | 3.5 |
| **P1** | `DashboardViewModelTest` | ViewModel | 0.7 人日 | 4.2 |
| **P1** | `QuizViewModelTest` | ViewModel | 0.8 人日 | 5.0 |
| **P1** | `SettingsRepositoryImplTest` | Repository | 0.4 人日 | 5.4 |
| **P1** | `QuizRepositoryImplTest` | Repository | 0.7 人日 | 6.1 |
| **P2** | `GamificationRepositoryImplTest` | Repository | 0.4 人日 | 6.5 |
| **P2** | `GamificationEngineTest` | Engine | 0.5 人日 | 7.0 |

**总计：~7 人日（含缓冲 0.5 人日）**

### 3.4 框架迁移与依赖升级

现有测试使用 JUnit4 + MockK + kotlinx-coroutines-test。CEO 要求升级框架：

| 框架 | 当前版本 | 目标版本 | 变动 |
|------|:--------:|:--------:|------|
| JUnit | 4.13.2 | **JUnit5 Jupiter 5.10.2** | 需替换 `@Test`(junit4) 为 `@Test`(junit5)，引入 `junit-jupiter` |
| MockK | 1.13.9 | 1.13.9 | 兼容 JUnit5，无需变更 |
| Turbine | ❌ 未引入 | **1.0.0** | 用于 `kotlinx.coroutines.flow` 测试，需新增依赖 |
| kotlinx-coroutines-test | 1.7.3 | 1.7.3 | 兼容，无需变更 |

**build.gradle.kts 依赖变更：**
```kotlin
// 替换 JUnit4
testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")

// 新增 Turbine
testImplementation("app.cash.turbine:turbine:1.0.0")
```

**注意：新测试统一使用 JUnit5，但**已有测试文件（8 个）保持 JUnit4 不动**，以免大规模回归风险。后续迭代可逐步迁移。

### 3.5 每个测试文件的覆盖目标

#### Engine 层

**ScoreCalculatorTest（30 分）**
- `calculateScore()`: 每种 LearningEvent 类型返回正确积分
- `calculateScore(QuizCompleted)`: 满分时额外奖励
- `calculateScore(AppOpened)`: 返回 0
- `calculateScore(StreakMaintained)`: 返回 0
- `calculateStreakBonus()`: 连续天数 × 5
- `calculateStreakBonus(0)`: 返回 0
- **目标覆盖率: 100% lines**

**StreakCalculatorTest（40 分）**
- `calculateStreak()`: 空列表返回全部 0
- `calculateStreak()`: 仅今天学习 → currentStreak=1
- `calculateStreak()`: 仅昨天学习 → currentStreak=1
- `calculateStreak()`: 连续 7 天 → currentStreak=7
- `calculateStreak()`: 中断后重新开始
- `longestStreak`: 短期连续 vs 长期连续
- `hasLearnedToday`: 今天/昨天/未学 三种情况
- **目标覆盖率: 100% lines**

**SpacedRepetitionEngineTest（50 分）**
- `calculateNextReview()`: 正确 → interval 递增 (1→3→7→14→30→60)
- `calculateNextReview()`: 错误 → interval 重置为 1
- `calculateNextReview()`: 连续 3 次正确 → isMastered=true
- `calculateNextReview()`: 超过 6 次后 → 指数增长 (×2, max 180)
- `getDueItems()`: 到期/未到期/已掌握 过滤
- `createInitialReview()`: 新建时 interval=1, nextReviewAt=明天
- **目标覆盖率: 100% lines**

**AchievementDetectorTest（40 分）**
- `checkAchievements()`: 全部锁定 → 无解锁
- `checkAchievements()`: 满足积分条件 → 解锁成功
- `checkAchievements()`: 满足连续天数 → 解锁
- `checkAchievements()`: 多个成就同时满足 → 批量解锁
- `checkConditionDirectly()`: 6 种条件类型的判定
- 边界值: 刚好满足 / 差一点 / 远超
- **目标覆盖率: 95%+ lines**

#### ViewModel 层

**SettingsViewModelTest（60 分）**
- 初始化状态: settings/cacheSize/agentEnabled/language 默认值
- `updateTemperature()`: 调用 repository 更新
- `updateTopP()`: 调用 repository 更新
- `updateMaxTokens()`: 调用 repository 更新
- `updateThemeMode()`: 调用 repository 更新
- `updateTtsSpeed()`: 调用 repository 更新
- `setLanguage()`: 调用 languagePreferences
- `toggleAgentMode()`: 切换并调用 repository
- `loadCacheSize()`: 从 CacheManager 获取
- `clearCache()`: 清理流程 + progress 更新
- `checkForUpdate()`: 成功/失败/空结果 三种路径
- `clearUpdateResult()`: 重置状态
- `getLanguageDisplayName()`: 中/英文显示
- 防止重复检查: `isCheckingUpdate` guard
- **目标覆盖率: 85%+ lines**

**DashboardViewModelTest（40 分）**
- 初始化状态: 积分/连续/成就/排行榜 默认
- 各 Flow 数据正常加载
- 空数据状态处理
- **目标覆盖率: 80%+ lines**

**QuizViewModelTest（50 分）**
- 初始化状态
- 开始测验: 题目加载
- 答题: 选择/取消选择
- 提交: 正确/错误/部分正确
- 查看解析
- 重新开始
- 超时/异常
- **目标覆盖率: 80%+ lines**

#### Repository 层

**SolveRepositoryImplTest（40 分）**
- `streamSolvePhoto()`: 正确构建 API 调用
- `retryStep()`: 成功路径
- `retryStep()`: API 错误返回
- `retryStep()`: 网络异常
- `getSolveSteps()`: 成功返回
- `getSolveSteps()`: API 错误
- `getSolveSteps()`: 网络异常
- **目标覆盖率: 90%+ lines**

**SettingsRepositoryImplTest（30 分）**
- 各项 settings 的读写委托验证
- Flow 数据正确映射
- 默认值处理
- **目标覆盖率: 85%+ lines**

**QuizRepositoryImplTest（40 分）**
- 题目加载: API → DAO 缓存
- 提交答案: API 调用
- 获取历史记录
- 错误处理
- **目标覆盖率: 80%+ lines**

### 3.6 测试文件组织结构

```
android/app/src/test/java/com/aitutor/app/
├── domain/engine/
│   ├── ScoreCalculatorTest.kt          # P0 NEW
│   ├── StreakCalculatorTest.kt          # P0 NEW
│   ├── SpacedRepetitionEngineTest.kt    # P0 NEW
│   └── AchievementDetectorTest.kt       # P0 NEW
├── domain/repository/
│   ├── SolveRepositoryImplTest.kt       # P0 NEW
│   ├── SettingsRepositoryImplTest.kt    # P1 NEW
│   ├── QuizRepositoryImplTest.kt        # P1 NEW
│   └── GamificationRepositoryImplTest.kt # P2 NEW
├── ui/settings/
│   └── SettingsViewModelTest.kt         # P0 NEW
├── ui/screen/dashboard/
│   └── DashboardViewModelTest.kt        # P1 NEW
├── ui/screen/quiz/
│   └── QuizViewModelTest.kt             # P1 NEW
├── data/repository/                     # 已有
│   ├── ChatRepositoryImplTest.kt        # 已有
│   ├── AuthRepositoryImplTest.kt        # 已有
│   └── AgentRepositoryImplTest.kt       # 已有
├── ui/chat/
│   └── ChatViewModelTest.kt             # 已有
├── ui/auth/
│   └── LoginViewModelTest.kt            # 已有
├── data/local/db/
│   └── RoomMigrationTest.kt             # 已有
└── utils/
    ├── BaseViewModelTest.kt             # 已有
    └── MainCoroutineRule.kt             # 已有
```

---

## 四、并行执行计划

```
时间线:
Day 1   |████████████████████████|
Day 2   |████████████████████████|
Day 3   |████████████████████████|

P1-4 ──┤░░░░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓│  3 人日 (CI/CD)
P2-1 ──┤░░░░░░░░░░░░░░░░░░░░▓▓▓▓▓▓▓│  3 人日 (Engine + SettingsVM + SolveRepo)
          └── P2-1 第二阶段 ──────▓▓▓│  2 人日 (DashboardVM + QuizVM + 其余Repo)
          并行第一阶段后 Tester 验证 ─>│
```

**团队安排建议：**
- **Coder A**（CI/CD）：P1-4 workflow 编写 + 调试（3 天）
- **Coder B**（Engine + VM）：P2-1 Engine 层（4 个纯 Kotlin 文件）→ SettingsViewModel → SolveRepository（3 天）
- **Coder C**（剩余 VM + Repo）：P2-1 DashboardViewModel → QuizViewModel → 其余 Repository（2 天，与 Coder B 并行）

---

## 五、风险与备忘

1. **JUnit4 → JUnit5 共存风险**：新测试用 JUnit5，旧测试保持 JUnit4。Gradle 需要配置 `test.useJUnitPlatform()` 但确保旧测试仍能运行。建议在 `app/build.gradle.kts` 中添加 `tasks.withType<Test> { useJUnitPlatform() }`，JUnit4 的 `@RunWith(RobolectricTestRunner::class)` 仍可工作。

2. **Turbine 引入**：需要在 `build.gradle.kts` 新增依赖。注意 Turbine 1.0.0 需要 kotlinx-coroutines 1.7+（当前版本 1.7.3 兼容）。

3. **BaseViewModelTest 兼容性**：当前使用 `UnconfinedTestDispatcher`，某些场景可能需要 `StandardTestDispatcher` + `advanceUntilIdle()`。新测试应按需选择。

4. **Robolectric 依赖**：`AgentRepositoryImplTest` 使用 Robolectric（因为使用 `RuntimeEnvironment`）。新测试应尽量避免 Android 依赖，纯 Kotlin 测试优先。

5. **SettingsViewModel 的 CacheManager**：`CacheManager.calculateSize()` 和 `clearAll()` 可能涉及文件系统操作，需用 MockK mock 或 Robolectric。

6. **GamificationEngine 测试挑战**：依赖 `AchievementDao` / `UserScoreDao` / `ScoreLogDao` 三个 Room DAO，且内部使用 `LocalDate.now()` — 需要 mock DAO + 控制时间。建议使用 MockK 的 `mockkConstructor` 或对象 mock 来处理。

7. **ScoreCalculator 是 object**：MockK 的 `mockkObject()` 可用于 object 类测试。

8. **CI/CD 首次构建时间**：首次构建因下载 Gradle + SDK + dependencies，可能长达 20-30 分钟。Gradle cache 可降低后续构建至 5-10 分钟。

---

## 六、标记完成

✅ 规划完成，已输出至 `~/hermes/projects/ai-tutor-android/docs/v23_plan.md`
✅ 两个任务（P1-4 CI/CD + P2-1 单元测试）确认完全独立可并行
✅ CI/CD 技术方案确认：GitHub Actions ubuntu-latest 完全支持 Android SDK
✅ 核心测试目标已识别：4 个 Engine（P0, ~1.8 人日）+ 3 个 ViewModel（P0-P1, ~2.5 人日）+ 4 个 Repository（P0-P2, ~2.2 人日）
✅ 每个测试文件的覆盖范围和目标已明确
✅ JUnit5 + MockK + Turbine 框架方案已确认
