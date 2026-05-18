# v2.4 体验打磨迭代 — PM 规划

> PM: Hermes Agent | 日期: 2026-05-18 | 版本: v2.4

---

## 一、现状分析

| 任务 | 当前状态 | 关键发现 |
|:---|:---|:---|
| P2-2 国际化 | strings.xml 已有 175 条 + 英文翻译存在，但 **所有 Kotlin 文件大量硬编码中文** | 需逐文件替换 stringResource() |
| P2-3 深色动画 | theme.xml 仅 1 个浅色主题，无 animateColorAsState | 需在 App 层 + 各页面添加动画 |
| P2-4 列表性能 | ChatScreen LazyColumn 已有 `key`，但 Dashboard 用 Column+verticalScroll | 需改为 LazyColumn + key |
| P2-5 骨架屏 | 无骨架屏，加载时空白 | 需创建 Shimmer + LoadingState 组件 |
| P2-6 Deprecation | 发现 1 处 `@Suppress("DEPRECATION")` (versionCode) | 替换为 PackageInfoCompat |
| Version | version.properties = 1.0.0 | 更新为 2.4.0 |

---

## 二、并行策略

```
P2-2(国际化) ──────────────────────────────────────┐
P2-3(动画) ─┬─ P2-4(性能) ─┬─ 可并行组 ─────────┤
P2-5(骨架屏)─┘              │                     │
                            └─ Version 更新 ──────┤
                                                  ├─ 编译测试
P2-6(Deprecation) ──────────────────────────────────┘
```

- **并行组 A**: P2-3 + P2-4 + P2-5 (不重叠，分属不同文件)
- **并行组 B**: P2-2 (涉及大部分文件，建议在 P2-3/4/5 之后做，避免冲突)
- **串行末**: P2-6 (可在并行组完成后做)
- **随处**: Version 更新

---

## 三、执行计划

### Step 1: Version 更新
- 文件: `version.properties`
- 改动: VERSION_MAJOR=2, VERSION_MINOR=4, VERSION_PATCH=0

### Step 2: P2-6 Deprecation 清理
- 文件: `SettingsScreen.kt`
- 改动: 用 `PackageInfoCompat.getLongVersionCode()` 替换 `@Suppress("DEPRECATION")`

### Step 3: P2-3 深色模式动画
- 文件: 新增 `ui/theme/Theme.kt` (含 animateColorAsState)
- 文件: 修改 `MainActivity.kt` / 主题应用逻辑
- 文件: `themes.xml` 添加 dark 主题

### Step 4: P2-4 列表性能
- 文件: `DashboardScreen.kt` → LazyColumn + key
- 文件: `AchievementBadge.kt` / `AchievementGrid` 检查 key
- 文件: `LeaderboardView.kt` 检查 key
- 文件: `ChatViewModel.kt` / `ChatRepositoryImpl.kt` 消息分页 limit 50

### Step 5: P2-5 骨架屏
- 新增: `ui/components/ShimmerLoading.kt`
- 修改: `DashboardScreen.kt`, `SettingsScreen.kt` 添加 LoadingState

### Step 6: P2-2 国际化 (大规模)
- 涉及 50+ 文件，逐步替换硬编码中文为 stringResource()
- 检查 strings.xml 是否覆盖所有字符串

### Step 7: 编译 + 测试验证

---

## 四、风险
- P2-2 工作量大(3 天预估)，需分批次处理
- 无 Android SDK 验证环境，编译验证受限
