# AI 学伴 Android App — 产品迭代路线图

> 编制人：CEO
> 日期：2026-05-16
> 基于：项目已完成 F01-F47 全部 47 项功能，BUILD SUCCESSFUL
> Git 仓库：https://github.com/linlin515/ai-tutor.git

---

## 总体评估

项目核心功能（F01-F47）已全部交付，但距离**生产级发布**还有差距。
以下迭代方向按优先级排列。

---

## P0 — 必须（发版前完成）

### P0-1 崩溃监控与异常上报
- **问题**：App 崩溃无任何上报，用户闪退后开发者无从排查
- **方案**：集成 Firebase Crashlytics 或自建 Sentry 服务
- **依赖**：Firebase 账号 / Sentry 自建
- **预估**：2 人日

### P0-2 网络安全配置
- **问题**：无 `network_security_config.xml`，生产环境应锁定 HTTPS 防抓包
- **方案**：添加 `network_security_config.xml`，正式版仅允许 HTTPS
- **参考**：`app/src/main/res/xml/` 下创建
- **预估**：0.5 人日

### P0-3 应用内更新检测
- **问题**：APK 更新只能手动扫码下载
- **方案**：检查 GitHub Release / 自有版本 API，App 内提示更新
- **预估**：2 人日

---

## P1 — 重要（下一迭代）

### P1-1 新手引导 Onboarding
- **问题**：首次启动无引导页
- **方案**：3 页引导（拍照解题 / 语音交互 / 学习进度）
- **预估**：2 人日

### P1-2 离线状态处理
- **问题**：断网时发送消息直接失败，无友好提示
- **方案**：NetworkMonitor 驱动 UI 状态 → 断网横幅 + 自动重连
- **预估**：1.5 人日

### P1-3 错误边界与全局异常捕获
- **问题**：当前 ViewModel 中 try-catch 散落各处，全局未捕获异常导致闪退
- **方案**：
  - Thread.setDefaultUncaughtExceptionHandler
  - Compose 错误边界 Composable
- **预估**：1 人日

### P1-4 CI/CD 自动化构建
- **问题**：每次发版需手动 gradle build + cp + nginx reload
- **方案**：GitHub Actions 自动打包 + 部署到服务器
- **依赖**：P0-1 Crashlytics
- **预估**：3 人日

---

## P2 — 优化（持续迭代）

### P2-1 单元测试覆盖
- **问题**：仅有 8 个测试文件，核心逻辑无测试
- **重点**：ViewModel / Repository / Engine 核心类
- **框架**：JUnit5 + MockK + Turbine (Flow)
- **预估**：5-8 人日

### P2-2 国际化
- **问题**：全部硬编码中文
- **方案**：抽取 strings.xml，支持中/英
- **预估**：3 人日

### P2-3 深色模式过渡动画
- **问题**：主题切换无过渡
- **方案**：animateColorAsState 等
- **预估**：1 人日

### P2-4 列表性能优化
- **问题**：对话数量增多后 LazyColumn 性能下降
- **方案**：key 优化 + 图片预加载 + 消息分页
- **预估**：2 人日

### P2-5 骨架屏 / Loading 态
- **问题**：数据加载时空白闪烁
- **方案**：Shimmer 骨架屏组件
- **预估**：2 人日

### P2-6 13 个 Deprecation 警告清理
- **问题**：编译时 13 个 deprecation warnings（Material Icons AutoMirrored 等）
- **预估**：1 人日

---

## 已存在的生产就绪项 ✅

| 维度 | 状态 |
|:----|:----:|
| 后端 HTTPS | ✅ Let's Encrypt SSL |
| Nginx 反向代理 | ✅ 已配置 |
| APK 下载分发 | ✅ /download.apk |
| 签名打包 | ✅ debug.keystore |
| Git 版本控制 | ✅ GitHub 托管 |
| 功能完成度 | ✅ 100% (47/47) |
| 构建状态 | ✅ BUILD SUCCESSFUL |

---

## 各阶段目标

| 阶段 | 内容 | 目标完成度 | 预估工时 |
|:---:|------|:---------:|:--------:|
| **v2.1** | P0-1 + P0-2 + P0-3 | 生产可监控 | 4.5 人日 |
| **v2.2** | P1-1 + P1-2 + P1-3 | 体验补齐 | 4.5 人日 |
| **v2.3** | P1-4 + P2-1 | 工程化 | 8-11 人日 |
| **v2.4** | P2-2 到 P2-6 | 体验打磨 | 9 人日 |
