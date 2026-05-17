# AI 学伴 — 版本号管理规范

## 概述

本规范定义了 AI 学伴 Android App 的版本号管理方案，实现版本号的自动化生成与语义化管理，消除手动修改带来的错误风险。

## 版本号组成

| 字段 | 格式 | 示例 | 生成方式 |
|------|------|------|----------|
| **versionCode** | 正整数 | `16` | Git commit count（自动） |
| **versionName** | `major.minor.patch` | `1.0.0` | version.properties（手动管理） |
| **versionName（Debug）** | `major.minor.patch-debug` | `1.0.0-debug` | 自动追加 `-debug` |

### versionCode（构建编号）

- **生成规则**：执行 `git rev-list --count HEAD` 获取当前仓库的提交总数
- **特性**：
  - 每次新的 commit，versionCode 自动 +1，天然递增
  - 不同分支检出时，versionCode 随提交数量变化（release 分支通常只 merge，值稳定）
  - Git 不可用时的回退：使用构建时间戳（Unix 秒数）
- **用户可见性**：对最终用户不可见，仅用于应用商店版本识别
- **查看方式**：`git rev-list --count HEAD`

### versionName（语义版本号）

- **格式**：`MAJOR.MINOR.PATCH`
- **管理文件**：项目根目录 [`version.properties`](./version.properties)
- **更新时机**：

| 版本位 | 递增时机 | 示例 |
|--------|----------|------|
| **MAJOR** | 重大架构重构，破坏性 UI 变更，不兼容 API 改动 | `1.0.0` → `2.0.0` |
| **MINOR** | 新功能发布，但向后兼容 | `1.0.0` → `1.1.0` |
| **PATCH** | Bug 修复，性能优化，微小改进 | `1.0.0` → `1.0.1` |

- **Debug 构建**：versionName 自动附加 `-debug` 后缀（如 `1.0.0-debug`），由 `build.gradle.kts` 中的 `versionNameSuffix = "-debug"` 控制，Release 构建不附加

## 版本管理流程

### 日常开发

```
$ git commit -m "fix: ..."
$ git rev-list --count HEAD   # versionCode 自动 = N（N 递增）
# versionName 保持当前 MAJOR.MINOR.PATCH
```

### 发布新版本

1. **更新 version.properties**：
   ```properties
   VERSION_MAJOR=1
   VERSION_MINOR=1
   VERSION_PATCH=0
   ```

2. **创建 Git Tag**（推荐）：
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```

3. **构建 Release APK**：
   ```bash
   ./gradlew assembleRelease
   ```
   - versionCode = git commit count（自动）
   - versionName = `1.1.0`（从 properties 读取，无 `-debug` 后缀）

## 文件清单

| 文件 | 作用 |
|------|------|
| `app/build.gradle.kts` | Gradle 构建配置，包含 versionCode/versionName 生成逻辑 |
| `version.properties` | 语义版本号管理文件，开发人员手动更新 |
| `VERSIONING.md` | 本规范文档 |

## 技术原理

### build.gradle.kts 中的实现

```
┌─────────────────────────────────────────────────┐
│ 构建阶段                                    │
├─────────────────────────────────────────────────┤
│ 1. 加载 version.properties                      │
│ 2. 执行 git rev-list --count HEAD               │
│ 3. 设置 versionCode = git count                 │
│ 4. 设置 versionName = major.minor.patch        │
│ 5. Debug 模式自动附加 "-debug" 后缀            │
└─────────────────────────────────────────────────┘
```

### 回退策略

| 场景 | versionCode 回退 |
|------|------------------|
| Git 仓库完整（正常情况） | `git rev-list --count HEAD` |
| CI 环境无 .git 目录 | 构建时间戳（Unix 秒数） |
| Git 命令执行失败 | 1（默认兜底） |

## 常见问题

**Q: 为什么 versionCode 不用时间戳而用 Git commit count？**
时间戳在短时间内多次构建时可能重复（秒级精度），而 Git commit count 天然递增、永不重复，且每次 commit 自动 +1。

**Q: 分支切换后 versionCode 会变吗？**
会。不同分支的提交数量不同，versionCode 反映当前检出分支的提交总数。Release 分支的提交数通常稳定。

**Q: 多人协作时，versionCode 冲突吗？**
不冲突。versionCode 仅用于 Google Play 内部版本标识，同一应用上传到商店时只要比前一个版本大即可。不同开发者本地构建的 versionCode 不同是正常行为。

## 参考

- [Android 官方文档：为应用分配版本号](https://developer.android.com/studio/publish/versioning)
- [语义化版本 2.0.0 (SemVer)](https://semver.org/lang/zh-CN/)
