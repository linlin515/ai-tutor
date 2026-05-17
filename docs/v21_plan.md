# v2.1 迭代规划

> 编制人：PM
> 日期：2026-05-17
> 版本：v2.1.0（从 v1.0.0 升级 → MAJOR 提升：因 P0-2 网络安全配置属于架构级变更）
> 构建编号：20 → 21+（git commit count 自动递增）

---

## 1. 迭代范围

| 任务 | 优先级 | CEO 预估 | PM 重估 | 状态 |
|:----|:------:|:--------:|:-------:|:----:|
| P0-1 崩溃监控与异常上报 | P0 | 2 人日 | **1.5 人日** | 🆕 待实现 |
| P0-2 网络安全配置 | P0 | 0.5 人日 | **0 人日** | ✅ **已实现**（检查确认） |
| P0-3 应用内更新检测 | P0 | 2 人日 | **1.5 人日** | 🆕 待实现 |
| **合计** | | **4.5 人日** | **3.0 人日** | |

**核减原因**：P0-2 `network_security_config.xml` 经审计已实现：
- `android/app/src/main/res/xml/network_security_config.xml` → Release: cleartextTrafficPermitted=false（仅 HTTPS）
- `android/app/src/debug/res/xml/network_security_config.xml` → Debug: 允许 10.0.2.2 + localhost 明文
- `AndroidManifest.xml` 已配置 `android:networkSecurityConfig="@xml/network_security_config"`
- 无需任何额外工作

---

## 2. 任务拆解

### 2.1 P0-1 崩溃监控与异常上报（1.5 人日）

#### 方案选择：自建本地存储方案（不引入 Sentry）
- 理由：无 Firebase 账号依赖，无额外网络权限，crash log 本地储存对用户透明
- **若后续需要远程监控**，可在本地存储基础上加一步上传到自有后端即可

#### 子任务

| # | 子任务 | 文件 | 预估工时 | 前置依赖 |
|:-:|:-------|:-----|:--------:|:---------|
| 1 | **全局未捕获异常处理器** — `CrashHandler.kt` | `data/local/CrashHandler.kt` | 2h | 无 |
| 2 | **在 AiTutorApp.onCreate() 中注册** | `AiTutorApp.kt` | 0.5h | #1 |
| 3 | **崩溃日志查看页面** — `CrashLogScreen.kt` + 路由 | `ui/settings/CrashLogScreen.kt` + `Routes.kt` + `AppNavGraph.kt` | 3h | #1 |
| 4 | **设置页面入口** — "崩溃日志" ListItem | `SettingsScreen.kt` | 1h | #3 |
| 5 | **ProGuard 保留规则**（防止混淆后无法获取堆栈）| `proguard-rules.pro` | 0.5h | #1 |
| 6 | **测试验证**：手动触发 NPE 验证写入 + 页面显示 | — | 2h | #1-#5 |
| | **小计** | | **9h (≈1.1人日)** | |

**CrashHandler 设计要点**：
- 文件存储路径：`context.filesDir/crashes/crash_yyyyMMdd_HHmmss.txt`
- 捕获字段：时间、设备信息（Brand/Model/Android Version/API Level）、App 版本名+版本号、异常类型、异常消息、完整堆栈
- 日志上限：最多保留 20 个文件，超限时删除最旧
- 使用 `Thread.setDefaultUncaughtExceptionHandler`，包装原有 handler

**CrashLogScreen 设计要点**：
- 列表显示所有崩溃日志文件（文件名=时间戳，显示友好日期）
- 点击查看崩溃详情（全屏滚动文本）
- 清空全部、单条删除
- 分享崩溃日志（通过 Intent.ACTION_SEND 分享纯文本）

#### 依赖检查：无需新增第三方库

---

### 2.2 P0-2 网络安全配置（0 人日）✅ 已实现

**验证清单**：

| 检查项 | 状态 |
|:-------|:----:|
| `res/xml/network_security_config.xml` 存在 | ✅ |
| Release 版 cleartextTrafficPermitted=false | ✅ |
| `res/debug/res/xml/network_security_config.xml` 存在 | ✅ |
| Debug 版允许 10.0.2.2 + localhost 明文 | ✅ |
| AndroidManifest.xml 引用 `@xml/network_security_config` | ✅ |
| ProGuard 保留规则无影响 | ✅ |

**结论**：P0-2 在 v2.0 阶段已由 Coder 完成，在 v2.1 中标记为关闭，无需 Coder 工时。

---

### 2.3 P0-3 应用内更新检测（1.5 人日）

#### 子任务

| # | 子任务 | 文件 | 预估工时 | 前置依赖 |
|:-:|:-------|:-----|:--------:|:---------|
| 1 | **版本检查工具类** — `AppUpdateChecker.kt` | `domain/usecase/AppUpdateChecker.kt` | 3h | 无 |
| 2 | **设置页面"检查更新"按钮** + ViewModel 扩展 | `SettingsScreen.kt` + `SettingsViewModel.kt` | 2h | #1 |
| 3 | **更新弹窗 UI** — `UpdateDialog.kt` | `ui/settings/UpdateDialog.kt` | 2h | #1 |
| 4 | **通知栏下载**（下载 APK 到 Downloads）| `data/local/UpdateDownloader.kt` | 2h | #1 |
| 5 | **安装 APK**（FileProvider + 安装意图） | 更新 `AndroidManifest.xml` + `UpdateDownloader.kt` | 1h | #4 |
| 6 | **测试验证** | — | 2h | #1-#5 |
| | **小计** | | **12h (≈1.5人日)** | |

**AppUpdateChecker 设计要点**：
- 请求 `https://aitutor.googlecloud.ccwu.cc/download.apk` 的 HTTP HEAD 获取 `Content-Length` 和 `Last-Modified`
- 与本地 `versionCode`（git commit count）对比，或者通过后端 API 返回 `{ "versionCode": 21, "versionName": "2.1.0", "forceUpdate": false }`
- 方案 A（推荐）：在后端新增 `/api/v1/app/version` 端点返回版本信息
  ```json
  {
    "version_code": 21,
    "version_name": "2.1.0",
    "download_url": "https://aitutor.googlecloud.ccwu.cc/download.apk",
    "release_notes": "新增崩溃监控、应用内更新、安全加固",
    "force_update": false
  }
  ```
- 方案 B（独立方案）：解析 download.apk 响应头，通过 `Content-Length` 变化检测是否有新版本
- 失败处理：网络不可用 → Toast "检查更新失败，请检查网络连接"
- 防重复：设置 5 分钟冷却期

**UpdateDialog 设计要点**：
- 标题："发现新版本 v2.1.0"
- 正文：更新说明（release notes）
- 按钮："立即更新" / "稍后再说"
- 强制更新时：隐藏"稍后再说"，仅保留"立即更新"
- 下载中：进度条显示下载百分比

**通知栏下载要点**：
- 使用 `DownloadManager`（系统服务）下载 APK
- 或使用 OkHttp 自定义下载 + 通知栏进度
- 下载完成后打开 `FileProvider` URI 触发安装
- 需要 `REQUEST_INSTALL_PACKAGES` 权限（运行时请求）

---

## 3. 并行策略

```
时间轴（人日）:
Day 1        Day 2        Day 3
├── P0-1 #1 ─┤
│  (2h)       │
├── P0-3 #1 ─┼─ P0-1 #2-6 ──┤
│  (3h)       │  (7h)        │
│             │              │
│  P0-1 #1 与 P0-3 #1 可并行 → 二者均无外部依赖
│  P0-3 #1-6 可连续由同一 Coder 完成
│  P0-1 #2-6 在第 2 位 Coder 完成 #1 后跟进
```

**推荐分配**：
- **Coder A**：P0-1（全部 6 个子任务，约 1 人日集中完成）
- **Coder B**：P0-3（全部 6 个子任务，约 1.5 人日集中完成）

两个任务完全独立，无代码冲突，可最迟并行。

---

## 4. 版本号变更

| 字段 | 当前值 | v2.1 目标值 | 说明 |
|:----|:------:|:-----------:|:-----|
| VERSION_MAJOR | 1 | **2** | 网络安全配置属于架构安全增强，按规范提升 MAJOR |
| VERSION_MINOR | 0 | **1** | 新功能发布 |
| VERSION_PATCH | 0 | **0** | — |
| versionName | 1.0.0 | **2.1.0** | 语义版本 |
| versionCode | 20 | **21+** | git commit count，commit 后自动递增 |

更新文件：`version.properties`

---

## 5. 验收标准

### P0-1 崩溃监控
- [ ] 手动触发 NullPointerException → App 不闪退到桌面，crash 日志写入 filesDir/crashes/
- [ ] 再次打开 App → 设置页面可见"崩溃日志"入口
- [ ] 点击"崩溃日志" → 显示 crash 文件列表，包含时间、异常类型
- [ ] 点击 crash 条目 → 完整堆栈信息可读
- [ ] 支持分享崩溃日志（文本格式）
- [ ] 支持删除单条/清空全部
- [ ] 日志文件上限 20 个，超限自动淘汰最旧

### P0-2 网络安全配置
- [x] Release APK 明文 HTTP 请求被阻断（已在 v2.0 验证）
- [x] Debug APK 可正常连接 10.0.2.2/localhost

### P0-3 应用内更新
- [ ] 设置页面"检查更新"按钮 → 网络可用时发起版本检查
- [ ] 当前版本已是最新 → Toast "已是最新版本"
- [ ] 发现新版本 → 弹窗显示版本号 + 更新说明
- [ ] 点击"立即更新" → 后台下载 APK（通知栏显示进度）
- [ ] 下载完成 → 自动弹出安装界面
- [ ] 强制更新模式 → 弹窗不可关闭，仅"立即更新"按钮
- [ ] 网络不可用 → Toast "检查更新失败"

---

## 6. 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|:----|:----:|:----:|:---------|
| Android 14+ 安装 APK 限制变更 | 低 | 中 | 使用 `PackageInstaller` API 或引导用户手动开启"安装未知应用"权限 |
| DownloadManager 在某些 ROM 上行为不一致 | 中 | 中 | 备选方案：OkHttp 自定义下载 + 通知栏，保证主流机型兼容 |
| 崩溃处理器中访问文件系统导致二次崩溃 | 低 | 高 | 使用 `try-catch` 包裹全部 I/O 操作；写入失败时仅 fallback 到原生 handler |
| 后端版本 API 未及时上线 | 中 | 中 | 方案 B 作为 fallback（解析 download.apk 响应头），确保 demo 可演示 |

---

## 7. 交付物清单

| # | 文件 | 状态 | 负责人 |
|:-:|:-----|:----:|:------:|
| 1 | `app/src/main/java/com/aitutor/app/data/local/CrashHandler.kt` | 🆕 新建 | Coder |
| 2 | `app/src/main/java/com/aitutor/app/AiTutorApp.kt` | 📝 修改 | Coder |
| 3 | `app/src/main/java/com/aitutor/app/ui/settings/CrashLogScreen.kt` | 🆕 新建 | Coder |
| 4 | `app/src/main/java/com/aitutor/app/ui/settings/CrashLogViewModel.kt` | 🆕 新建 | Coder |
| 5 | `app/src/main/java/com/aitutor/app/ui/navigation/Routes.kt` | 📝 修改 | Coder |
| 6 | `app/src/main/java/com/aitutor/app/ui/navigation/AppNavGraph.kt` | 📝 修改 | Coder |
| 7 | `app/src/main/java/com/aitutor/app/ui/settings/SettingsScreen.kt` | 📝 修改 | Coder |
| 8 | `app/proguard-rules.pro` | 📝 修改 | Coder |
| 9 | `app/src/main/java/com/aitutor/app/domain/usecase/AppUpdateChecker.kt` | 🆕 新建 | Coder |
| 10 | `app/src/main/java/com/aitutor/app/ui/settings/UpdateDialog.kt` | 🆕 新建 | Coder |
| 11 | `app/src/main/java/com/aitutor/app/data/local/UpdateDownloader.kt` | 🆕 新建 | Coder |
| 12 | `app/src/main/java/com/aitutor/app/ui/settings/SettingsViewModel.kt` | 📝 修改 | Coder |
| 13 | `version.properties` | 📝 修改 | PM（发布前） |
