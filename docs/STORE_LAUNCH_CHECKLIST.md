# Google Play 商店上架检查清单

> **项目**: AI 学伴 (AI Tutor)  
> **包名**: `com.aitutor.app`  
> **分类**: 教育 (Education)  
> **版本**: v1.0 初始上架  
> **目标市场**: 大陆、港澳台、全球华语用户 + 全球英语市场  
> **定价**: 免费下载 + 应用内订阅  
> **广告声明**: 不含广告  

本文档汇整以上所有商品化任务的产出，按阶段分类列出上架检查项，供发布参考。

---

## A. 商店准备阶段 — Store Listing Assets

| # | 检查项 | 动作 | 负责人 | 状态 | 备注 / 来源 |
|---|--------|------|--------|------|-------------|
| 1 | **应用图标（各分辨率 PNG）** | 确认 `app/src/main/res/mipmap-*` 及 `ai-tutor-android/app/src/main/res/mipmap-*` 均已覆盖 5 种密度 (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) | CFO | ✅ 完成 | t_c6fd8ba6 — 已生成 20 个 PNG (5密度×2形状×2模块) |
| 2 | **圆形图标** | 确认 `ic_launcher_round.png` 已在各 mipmap 目录存在，且 `AndroidManifest.xml` 已配置 `android:roundIcon` | CFO | ✅ 完成 | 同上，`anydpi-v26` XML 自适应图标已保留 |
| 3 | **标题（中文）** | 填写 **「AI 学伴 - AI 辅导拍照解题」** (14字, ≤50字符) | PM | ⬜ 待操作 | store_listing_cn.md |
| 4 | **标题（英文）** | 填写 **「AI Tutor - Homework Helper & Study Buddy」** (39字符, ≤50字符) | PM | ⬜ 待操作 | store_listing_en.md |
| 5 | **短描述（中文）** | 填写 **「AI 拍照解题 + 苏格拉底式教学 + 智能问答，你的专属 AI 学习助手。」** (28字, ≤80字符) | PM | ⬜ 待操作 | store_listing_cn.md |
| 6 | **短描述（英文）** | 填写 **「Snap & Solve homework. AI-powered tutoring, quizzes, and study reports. Your personal AI learning companion.」** (79字符, ≤80字符) | PM | ⬜ 待操作 | store_listing_en.md |
| 7 | **完整描述（中文）** | 填写约 800 字完整介绍，含拍照解题、苏格拉底教学、AI 问答、Agent 模式、仪表盘、测验、错题复习、学习报告、间隔重复等功能 | PM | ⬜ 待操作 | store_listing_cn.md §3 |
| 8 | **完整描述（英文）** | 填写约 900 字英文完整介绍，同上功能要点 | PM | ⬜ 待操作 | store_listing_en.md §3 |
| 9 | **关键词列表（中文）** | 在 Play Console 填写中文关键词（AI学习助手、拍照搜题、作业帮、AI辅导、智能问答等 15 个） | PM | ⬜ 待操作 | store_listing_cn.md §4 — 含热度说明 |
| 10 | **关键词列表（英文）** | 在 Play Console 填写英文关键词（AI homework helper, math solver, photo math solver, AI tutor, study buddy, Socratic learning 等 17 个） | PM | ⬜ 待操作 | store_listing_en.md §4 — 含热度说明 |
| 11 | **功能截图（≥4 张，1080×1920）** | 在 Android Emulator (Pixel 6/7) 中运行 App 并截取 6 张截图：拍照解题 / 苏格拉底教学 / AI Agent 问答 / 学习仪表盘 / 智能测验 / 订阅页面 | Coder | ⬜ 待操作 | store_listing_cn.md §5 — 每张含 UI 元素及中英文 caption 指引 |
| 12 | **截图图片说明文案** | 为每张截图提供中英文图片说明，上传 Play Console 时填写 | PM | ⬜ 待操作 | store_listing_cn.md §5 及 store_listing_en.md §5 已提供完整文案 |
| 13 | **Feature Graphic (1024×500)** | 设计制作 Feature Graphic：渐变背景 + 应用图标 + 标语「AI 学伴」/「AI Tutor — Learn Smarter」 | Designer | ⬜ 待操作 | store_listing_cn.md §7 — 推荐方案已给出 |
| 14 | **应用分类与内容分级** | 在 Play Console 设置：分类 = **教育**，内容分级 = **Everyone (E)**，目标年龄 = 6-18 岁 | PM | ⬜ 待操作 | store_listing_cn.md §6 — 已确定 |

---

## B. 合规阶段 — Compliance & Legal

| # | 检查项 | 动作 | 负责人 | 状态 | 备注 / 来源 |
|---|--------|------|--------|------|-------------|
| 15 | **隐私政策文档** | PRIVACY_POLICY.md 已撰写完成（226 行，含数据收集表、AI 数据处理、儿童隐私章节、Data Safety 映射） | CFO | ✅ 完成 | t_42922911 — 文件位于项目根目录 |
| 16 | **隐私政策托管 URL** | 将 PRIVACY_POLICY.md 部署到可公开访问的 HTTPS URL，并在 Play Console 中填写 | PM | ⬜ 待操作 | 选项：GitHub Pages / 自有域名 / Google Drive。`web/privacy_policy.html` 已同步生成 |
| 17 | **内容分级配置 (AndroidManifest)** | `AndroidManifest.xml` 已添加 `androidx.tvprovider.media.content_rating` meta-data 及 `com.google.android.gms.games.APP_ID` 占位 | CFO | ✅ 完成 | t_42922911 |
| 18 | **IARC 年龄评级问卷** | 在 Play Console → Store presence → Store settings → Content rating 中完成 IARC 问卷 | PM | ⬜ 待操作 | content_rating_guide.md (§3-11) 已提供逐项指南；预期结果：Everyone (E) |
| 19 | **Data Safety 表单** | 在 Play Console 填写 Data Safety section，需声明：用户提供数据（账号/学习内容/照片）、自动收集数据（设备信息/使用日志/性能数据）、第三方 SDK 共享（Firebase/ML Kit/Google Billing） | PM | ⬜ 待操作 | PRIVACY_POLICY.md §1-4 已整理完整映射 |
| 20 | **儿童隐私声明 (COPPA / GDPR-K)** | 确认应用面向 K-12 用户，需在隐私政策中声明符合 COPPA/GDPR-K，且不收集 13 岁以下儿童个人信息除非获得父母/监护人同意 | PM | ⬜ 待操作 | PRIVACY_POLICY.md §8 已包含儿童隐私章节；content_rating_guide.md §10 含补充检查项 |
| 21 | **家庭教育应用登记** | Google Play 要求教育类应用需完成「Education app registration」声明 | PM | ⬜ 待操作 | content_rating_guide.md §11 含指引 |
| 22 | **家庭政策合规 (Google Play Families Policy)** | 如应用向家庭/儿童投放，需完成 Families Policy 声明及额外 IARC 问卷 | PM | ⬜ 待操作 | content_rating_guide.md §10 含检查项 |

---

## C. 技术准备阶段 — Technical Readiness

| # | 检查项 | 动作 | 负责人 | 状态 | 备注 / 来源 |
|---|--------|------|--------|------|-------------|
| 23 | **Release APK 签名验证** | 运行 `./gradlew assembleRelease`，确认 APK 已使用正式签名密钥 (keystore) 签名，`jarsigner -verify` 通过 | Coder | ⬜ 待操作 | 需确保 keystore 已配置于 `signingConfigs` |
| 24 | **ProGuard 混淆生效** | 确认 `app/proguard-rules.pro` 已加载，运行 Release 构建后 `mapping.txt` 生成正常，关键路径（Gson/Retrofit/协程）混淆规则正确 | CFO | ✅ 完成 | t_2ca9bc9e — 已新增日志清理规则 (assumenosideeffects) |
| 25 | **Network Security Config — 禁止明文 HTTP** | Release network_security_config.xml 已配置 `cleartextTrafficPermitted="false"`，仅 HTTPS 通信 | CFO | ✅ 完成 | t_2ca9bc9e — 采用 debug/release 变体分离策略 |
| 26 | **API 密钥安全审计** | 已扫描全部源码目录，未发现硬编码 API Key / Token / 密码。TokenManager 使用 EncryptedSharedPreferences (AES256_GCM) 加密存储 | CFO | ✅ 完成 | t_2ca9bc9e — security_audit_report.md |
| 27 | **版本号确认 (versionCode)** | `app/build.gradle.kts` 已配置 Git commit count 生成 versionCode。上架前须确认 versionCode > 上一版本 | CFO | ✅ 完成 | t_4796f001 — VERSIONING.md |
| 28 | **版本号确认 (versionName)** | 在 `version.properties` 中确认 `VERSION_MAJOR`、`VERSION_MINOR`、`VERSION_PATCH` 正确。v1.0 上架值为 `1.0.0` | CFO | ✅ 完成 | t_4796f001 — version.properties |
| 29 | **日志输出禁用 (Release)** | Release 构建中所有 `Log.()` 调用通过 ProGuard `assumenosideeffects` 移除。Debug 构建日志正常保留 | CFO | ✅ 完成 | t_2ca9bc9e — proguard-rules.pro 已配置 |
| 30 | **SSL Pinning 评估** | 当前未启用 SSL Pinning。建议评估是否需要（非必须，API 后端仅有固定地址时可考虑） | CFO | ⬜ 待讨论 | security_audit_report.md |
| 31 | **WebView 安全检查** | 应用未使用 WebView，无 WebView 相关风险 | CFO | ✅ 完成 | security_audit_report.md |
| 32 | **权限最小化检查** | 确认 AndroidManifest 中的应用权限均在合理范围内且按需使用：CAMERA（拍照解题）、RECORD_AUDIO（语音输入）、POST_NOTIFICATIONS（学习提醒）等 | CFO | ⬜ 待确认 | PRIVACY_POLICY.md §1.3 — 权限列表已整理 |

---

## D. 发布检查 — Release Readiness

| # | 检查项 | 动作 | 负责人 | 状态 | 备注 / 来源 |
|---|--------|------|--------|------|-------------|
| 33 | **选定发布渠道** | 确定首次上架渠道：正式版 (Production) / 内测版 (Internal Testing) / Alpha / Beta | PM | ⬜ 待决定 | 建议先 **内部测试 (Internal Testing)** → **封闭 Alpha** → 转 **正式版** |
| 34 | **确认目标国家/地区** | 在 Play Console 设置发布地区。建议首批：中国大陆、港澳台、美国、加拿大、英国、澳大利亚、新加坡、日本、韩国 | PM | ⬜ 待操作 | store_listing_cn.md §6 — 目标市场已明确 |
| 35 | **设定定价** | 免费下载 (Free)，应用内订阅 (IAP) | PM | ⬜ 待操作 | 已确定 |
| 36 | **广告声明** | 在 Play Console 关于广告的选项中选择 **「不包含广告」** | PM | ⬜ 待操作 | 已确定不含广告 |
| 37 | **评分引导策略** | 在用户完成 5 次以上解题或学习 3 天后弹出评分引导 | PM | ⬜ 待实现 | store_listing_cn.md §7 — 已在 Listing 中提及 |
| 38 | **预注册评估** | 考虑在 Google Play 开启预注册 (Pre-registration)，积累早期用户口碑 | PM | ⬜ 待决定 | store_listing_cn.md §7 |
| 39 | **本地化优先级** | 首批上架：简体中文 + 英文。后续本地化优先顺序：日语 → 韩语 → 西班牙语 → 印地语 | PM | ⬜ 待规划 | store_listing_en.md §7 |
| 40 | **Git Tag 发布** | 上架前创建 Git Tag: `git tag -a v1.0.0 -m "Release v1.0.0"` | Coder | ⬜ 待操作 | VERSIONING.md §2 |
| 41 | **Release APK 构建** | 运行 `./gradlew clean assembleRelease`，确认构建通过，APK 正常生成 | Coder | ⬜ 待操作 | VERSIONING.md |
| 42 | **内测验证** | 上传 APK 至 Internal Testing 渠道，在至少 2 台不同 Android 设备上验证安装和核心功能 | QA / Coder | ⬜ 待操作 | — |

---

## 汇总统计

| 阶段 | 总项数 | ✅ 已完成 | ⬜ 待操作 | 依赖 |
|------|--------|----------|----------|------|
| **A. 商店准备** | 14 | 2 | 12 | Coder（截图）、Designer（Feature Graphic）、PM（填入 Console） |
| **B. 合规** | 8 | 2 | 6 | PM（操作 Play Console 表单 + 部署隐私政策 URL） |
| **C. 技术准备** | 10 | 7 | 3 | Coder（签名 APK）、PM（SSL Pinning 决策） |
| **D. 发布检查** | 10 | 0 | 10 | PM（策略决策）、Coder（构建 + 签名） |
| **合计** | **42** | **11** | **31** | |

---

## 依赖任务产出索引

| 父任务 | 产出文件 | 关键交付物 |
|--------|----------|-----------|
| t_c6fd8ba6 — 应用图标 | `app/src/main/res/mipmap-*` (×10), `ai-tutor-android/.../mipmap-*` (×10) | 5 密度方形+圆形应用图标 |
| t_17a93d97 — Store Listing 素材 | `store_listing_cn.md`, `store_listing_en.md` | 中英文标题/描述/关键词/截图脚本/分级信息 |
| t_42922911 — 合规配置 | `PRIVACY_POLICY.md`, `web/privacy_policy.html`, `docs/content_rating_guide.md`, `AndroidManifest.xml` | 隐私政策、IARC 问卷指南、内容分级配置 |
| t_2ca9bc9e — 安全审计与网络加固 | `security_audit_report.md`, `network_security_config.xml` (Release+Debug), `proguard-rules.pro` | 安全审计报告、HTTPS-only 配置、日志清理规则 |
| t_4796f001 — 版本号管理规范 | `VERSIONING.md`, `version.properties`, `app/build.gradle.kts` (已修改) | 自动 versionCode、语义 versionName 方案 |

---

## 快速启动指南

1. **需 Coder 操作**：截图生成（Emulator 运行 App 截取 6 张 1080×1920 截图）、Release APK 构建与签名
2. **需 PM 操作**：在 Play Console 中填入 Listing 素材（标题/描述/关键词/截图）、完成 IARC 问卷、填写 Data Safety 表单、部署隐私政策 URL、设置分发地区与定价
3. **需 Designer 操作**：设计 Feature Graphic (1024×500)
4. **上架后**：监控 Crashlytics 空指针问题、验证订阅流程端到端、关注首个版本的 7 日留存数据
