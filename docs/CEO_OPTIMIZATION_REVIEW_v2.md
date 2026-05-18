# 项目全面优化评估报告 v2 — AI 学伴 (AI Tutor)

> **审查日期**: 2026-05-17
> **项目路径**: ~/hermes/projects/ai-tutor-android
> **前次审查**: CEO_OPTIMIZATION_REVIEW.md (已完成商品化优化，本轮是新维度评估)

---

## 1. 功能完整性 — ⭐⭐⭐⭐⭐ (95%)

| 版本 | 内容 | 状态 |
|:----:|------|:----:|
| v1.0 | 基础功能（拍照解题+对话+语音）| ✅ 完成 |
| v1.1 | 密码认证 + 后端 API | ✅ 完成 |
| v2.0 | AI Agent + 学习报告导出 + 多语言 | ✅ 完成 |
| v2.1 | 崩溃监控 + 网络安全 + 应用更新 | ✅ 完成 |
| v2.2 | 新手引导 + 离线队列 + 错误边界 | ✅ 完成 |
| v2.3 | CI/CD 自动化构建 + 单元测试覆盖 | ⬜ **未开始** |

> 所有 P0-P2 功能需求已实现，功能完整度很高。v2.3 计划已写好但还没实施。

---

## 2. 代码质量 — ⭐⭐⭐⭐ (80%)

### 架构
- ✅ Clean Architecture（data/domain/ui 三层分离）
- ✅ 依赖注入（Hilt）
- ✅ 单 Activity + Compose Navigation
- ✅ Repository 模式 + Retrofit 网络层

### 代码规范
- ✅ 模块划分清晰（247 个 Kotlin 文件）
- ⚠️ 部分 KDoc/注释可能缺失
- ⚠️ `build.gradle.kts` 中版本管理通过 libs.versions.toml 但很多版本严重落后

### ProGuard
- ✅ 290 行详尽规则，覆盖 Gson/Retrofit/Room/Hilt/Compose/CameraX/ML Kit 等
- ⚠️ `-keep class com.aitutor.app.** { *; }` **过于宽松**，应细化到模块级别

---

## 3. 性能优化 — ⭐⭐⭐⭐ (75%)

### 已做
- ✅ ProGuard 混淆 + 日志压缩（release 构建移除 Log 调用）
- ✅ Room 数据库迁移链（v1→v2→v3）
- ✅ OkHttp 拦截器 + 缓存配置
- ✅ Compose BOM 统一版本管理

### 需关注
- ⚠️ AGP 8.2.2 相对于 8.7.3 落后较多，升级后 R8 优化效果更好
- ⚠️ Security Crypto 使用 `1.1.0-alpha06` 仍为 alpha 版本
- ⚠️ 无法在服务器环境验证 APK 体积和冷启动性能（缺构建环境）

---

## 4. 用户体验 — ⭐⭐⭐⭐ (85%)

### 已做
- ✅ 新手引导（3页滑动引导页，v2.2）
- ✅ 离线状态处理（NetworkBanner + OfflineQueueManager，v2.2）
- ✅ 错误边界（全局错误兜底，v2.2）
- ✅ 多语言支持（中/英文，v2.0）
- ✅ 游戏化激励（成就系统 + 积分 + 打卡）

### 需修复
- 🔴 **Bug**: `SplashViewModel` 中 `_isOnboardingDone = mutableStateOf(true)` 默认值为 true，在 `checkStartupState()` 执行前可能**闪白屏直接跳转到主页**
- ⚠️ 引导页使用 emoji 图标，建议替换为矢量插图提升品质
- ⚠️ 多语言切换后引导页文字是否跟随需验证

---

## 5. 技术债务 — ⭐⭐ (45%) ⚠️ 最大缺口

### 依赖版本全面落后（18/27 落后）

| 依赖 | 当前版本 | 最新稳定版 | 说明 |
|:----:|:--------:|:----------:|:----:|
| **Kotlin** | 1.9.22 | **2.1.0** | 🔴 大版本跨越，需配套升级 Compose Compiler |
| **AGP** | 8.2.2 | 8.7.3 | 🔴 落后 5 个小版本 |
| **KSP** | 1.9.22-1.0.17 | 2.1.0-1.0.29 | 🔴 需随 Kotlin 升级 |
| **Compose BOM** | 2024.02.00 | 2024.12.01 | 🔴 落后 10 个月 |
| **Compose Compiler** | 1.5.10 | 与 Kotlin 绑定 | 🔴 需随 Kotlin 升级 |
| **Hilt** | 2.50 | 2.53.1 | 🔴 落后 |
| **Retrofit** | 2.9.0 | **2.11.0** | 🔴 落后（2.9→2.11） |
| **Coroutines** | 1.7.3 | 1.9.0 | 🔴 落后 |
| **Navigation** | 2.7.7 | 2.8.5 | 🔴 落后 |
| **CameraX** | 1.3.1 | 1.4.1 | 🔴 落后 |
| **DataStore** | 1.0.0 | 1.1.1 | 🔴 落后 |
| **Coil** | 2.5.0 | 2.7.0 | 🔴 落后 |
| **Lifecycle** | 2.7.0 | 2.8.7 | 🔴 落后 |
| **Core KTX** | 1.12.0 | 1.15.0 | 🔴 落后 |
| **Activity Compose** | 1.8.2 | 1.9.3 | 🔴 落后 |
| **Turbine** | 1.0.0 | 1.2.0 | 🔴 落后 |
| **Robolectric** | 4.11.1 | 4.14.1 | 🔴 落后 |

### 其他技术债
- ❌ **没有 Git Tag** — 无法追踪发布版本
- ⚠️ 后端 API 路由前缀不统一（`/api/v1/` vs `/v1/`）
- ⚠️ `version.properties` 中 `VERSION_MAJOR=1, VERSION_MINOR=0, VERSION_PATCH=0` 依然为 `1.0.0`，未随迭代更新

---

## 6. 产品化就绪度 — ⭐⭐⭐ (60%) ⚠️ 上架待办多

### ✅ 已完成
| 项目 | 来源 |
|:----|:----:|
| 隐私政策文档 | PRIVACY_POLICY.md（226 行，含儿童隐私章节）|
| ProGuard 混淆规则 | 290 行全覆盖 |
| Network Security Config | debug/release 分离，release 禁止明文 |
| API 密钥安全审计 | 无硬编码密钥 |
| 版本号管理 | Git commit count 自动生成 versionCode |
| 应用图标 | 20 个 PNG（5密度×2形状×2模块）|
| 合规配置 | AndroidManifest 内容分级已添加 |
| 应用圆形图标 | `ic_launcher_round.png` 各密度已生成 |

### ❌ 待完成（Store 上架清单）
| # | 事项 | 难度 | 说明 |
|:-:|:----|:----:|:----|
| 1 | 🔴 **功能截图**（≥6张，1080×1920） | 中 | 需跑模拟器截图：拍照解题/教学/AI问答/仪表盘/测验/订阅 |
| 2 | 🔴 **隐私政策托管 URL** | 低 | 部署到 GitHub Pages 或自有域名 |
| 3 | 🔴 **IARC 年龄评级问卷** | 低 | Play Console 在线完成，预期 Everyone |
| 4 | 🔴 **Data Safety 表单** | 低 | Play Console 填写数据声明 |
| 5 | 🔴 **应用描述文案中/英文** | 低 | Store Listing 标题/短描述/完整描述（素材已由 CFO 生成） |
| 6 | 🟡 **Feature Graphic 1024×500** | 中 | 商店横幅设计 |
| 7 | 🟡 **Release APK 签名验证** | 低 | 正式 keystore 签名后验证 |
| 8 | 🟡 **Google Play Families Policy** | 低 | 教育类应用声明 |
| 9 | 🟡 **Education app registration** | 低 | Google Play 教育应用登记 |

---

## 7. 测试覆盖 — ⭐⭐⭐ (50%)

### 已做
- ✅ 13 个 Android 单元测试文件（JUnit 5 + MockK + Turbine + Robolectric）
- ✅ 覆盖 ViewModel（LoginViewModel/SettingsViewModel/ChatViewModel）
- ✅ 覆盖 Repository（Auth/Solve/Chat/Agent/SpacedRepetition）
- ✅ 覆盖 Domain Engine（StreakCalculator/ScoreCalculator/AchievementDetector）
- ✅ Room Migration 测试
- ✅ 后端 16 个测试文件

### 缺口
- ❌ **没有 androidTest** — 无集成测试 / UI 测试 / Instrumented 测试
- ❌ **CI 中没有自动跑测试** — `android-ci.yml` 只 build 不 test
- ❌ **端到端测试缺失** — 拍照→OCR→解题的完整流程无自动化验证
- ⚠️ 后端测试覆盖了 auth/chat/health/models/subscription，但 solve/audio/steps 路由缺少测试

---

## 8. 优先级汇总

### P0（上线前必须修复）
| # | 问题 | 类别 | 分配 | 工作量 |
|:-:|:----|:----:|:----:|:-----:|
| 1 | `SplashViewModel` 默认值 `true` 导致闪白屏 | Bug | Coder | 0.1 人日 |
| 2 | Store 商品化截图+上架待办（清单9项） | 产品化 | PM + Coder | 2 人日 |
| 3 | CI/CD 添加 test 步骤（当前只 build 不 test） | 基础设施 | Coder | 0.5 人日 |

### P1（尽早完成）
| # | 问题 | 类别 | 分配 | 工作量 |
|:-:|:----|:----:|:----:|:-----:|
| 4 | 18/27 依赖版本批量升级（含 Kotlin 2.0） | 技术债务 | CFO + Coder | 3 人日 |
| 5 | androidTest 集成测试框架搭建 + 核心路径用例 | 测试 | Tester | 2 人日 |
| 6 | Docker 密码明文暴露 → .env 方案 | 安全 | Coder | 0.3 人日 |
| 7 | ProGuard `-keep class **` 过于宽松 → 细化 | 代码质量 | Coder | 0.5 人日 |
| 8 | 后端 API 路由前缀统一（`/api/v1/`） | 一致性 | Coder | 0.3 人日 |
| 9 | Git 版本发布加 Tag | 版本管理 | Coder | 0.1 人日 |

### P2（体验优化）
| # | 问题 | 类别 | 分配 | 工作量 |
|:-:|:----|:----:|:----:|:-----:|
| 10 | 引导页 emoji 替换为矢量插图 | UI | Coder | 0.5 人日 |
| 11 | 多语言引导页文字跟随验证 | 国际化 | Tester | 0.2 人日 |
| 12 | `AndroidManifest` 中 `allowBackup` 安全处理 | 安全 | Coder | 0.1 人日 |
| 13 | `version.properties` 更新为 v1.2.0 | 版本管理 | Coder | 0.05 人日 |
| 14 | v2.3 CI/CD 增加后端 CI + Docker 镜像构建 | 基础设施 | Coder | 1 人日 |

---

## 9. 总体评分

| 维度 | 评分 | 趋势 |
|:----|:----:|:----:|
| 功能完整性 | ⭐⭐⭐⭐⭐ (95%) | ✅ 已完成全部 5 个版本迭代 |
| 代码质量 | ⭐⭐⭐⭐ (80%) | ➡️ 稳定，ProGuard 规则可再优化 |
| 性能优化 | ⭐⭐⭐⭐ (75%) | ➡️ 依赖升级后可改善 R8 效果 |
| 用户体验 | ⭐⭐⭐⭐ (85%) | ⚠️ 有一个闪白屏 Bug 需立即修复 |
| 技术债务 | ⭐⭐ (45%) | 🔴 最大短板 — 18/27 依赖落后 |
| 产品化就绪度 | ⭐⭐⭐ (60%) | ➡️ 核心配置已就绪，上架操作待完成 |
| 测试覆盖 | ⭐⭐⭐ (50%) | ➡️ 单元测试基础好，缺集成/UI 测试 |

**综合评级**: ⭐⭐⭐⭐ (72/100)

---

## 10. 下一步行动建议

1. **立即**: 修复 SplashViewModel 闪白屏 Bug（P0）
2. **本周**: 完成 Store 上架操作（截图+描述+IARC+Data Safety）
3. **本周**: 批量升级依赖版本（CFO 先做升级方案评估，避免 Kotlin 2.0 兼容性问题）
4. **短期**: CI/CD 添加 test 步骤 + 搭建 androidTest 框架
5. **持续**: 完善后端测试 + 细化 ProGuard 规则 + 统一 API 前缀
