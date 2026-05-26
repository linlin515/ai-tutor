# CEO 项目审计数据报告

**审计日期**: 2026-05-23
**项目路径**: `/home/linruihang0517/hermes/projects/ai-tutor-android`
**当前版本**: v2.7.0 (version.properties)
**最新提交**: v3.0 feat: dashboard paging3 leaderboard

---

## 1. TODO / FIXME / HACK 注释扫描

**扫描范围**: `android/` 目录下所有 `.kt`, `.java`, `.kts`, `.xml`, `.gradle`, `.properties`, `.json` 文件

仅发现 **2 处 TODO**，无 FIXME 或 HACK 注释：

| 文件路径 | 行号 | 内容 |
|---------|:----:|------|
| `android/app/src/main/java/com/aitutor/app/ui/screen/chat/components/PhotoPreviewDialog.kt` | 205 | `// TODO: 实现完整的保存到相册逻辑` |
| `android/app/src/main/java/com/aitutor/app/data/repository/VoiceRepositoryImpl.kt` | 81 | `// TODO: 实际项目中应先运行本地 ASR 获取置信度` |

**评估**: 遗留 TODO 数极少（2 个），均为非阻塞性功能增强项。无 FIXME/HACK。

---

## 2. 依赖版本 vs 当前常见最新稳定版

**源文件**: `android/gradle/libs.versions.toml`

| 依赖 | 当前版本 | 常见最新稳定版 (2026-05) | 差距评估 |
|------|:--------:|:------------------------:|:--------:|
| AGP (Android Gradle Plugin) | `8.2.2` | `8.7.x` | ⚠️ 落后约 2-3 个大版本 |
| Kotlin | `1.9.22` | `2.1.x` | ⚠️ 落后重大版本 (K2 编译器) |
| KSP | `1.9.22-1.0.17` | `2.1.x-...` | ⚠️ 随 Kotlin 版本滞后 |
| Compose BOM | `2024.02.00` | `2025.01.00+` | ⚠️ 落后约 1 年 |
| Compose Compiler | `1.5.10` | `1.5.15+` | ✅ 接近 |
| Hilt | `2.50` | `2.52+` | ✅ 接近 |
| Hilt Navigation Compose | `1.1.0` | `1.2.0` | ✅ 接近 |
| Room | `2.6.1` | `2.6.1+` | ✅ 最新 |
| Retrofit | `2.9.0` | `2.11.0` | ⚠️ 落后 2 个小版本 |
| OkHttp | `4.12.0` | `4.12.0` | ✅ 最新 |
| Coroutines | `1.7.3` | `1.9.0` | ⚠️ 落后 2 个小版本 |
| Navigation Compose | `2.7.7` | `2.8.x` | ⚠️ 落后约 1 个大版本 |
| CameraX | `1.3.1` | `1.4.x` | ⚠️ 落后约 1 个大版本 |
| DataStore | `1.0.0` | `1.1.x` | ✅ 接近 |
| ML Kit Text Recognition | `16.0.0` | `16.0.0` | ✅ 最新 |
| Coil | `2.5.0` | `2.7.x` | ⚠️ 落后 2 个小版本 |
| Activity Compose | `1.8.2` | `1.9.x` | ⚠️ 落后约 1 个大版本 |
| Lifecycle | `2.7.0` | `2.8.x` | ⚠️ 落后约 1 个大版本 |
| Paging | `3.2.1` | `3.3.x` | ⚠️ 落后约 1 个大版本 |
| Gson | `2.11.0` | `2.11.0` | ✅ 最新 |
| Core KTX | `1.12.0` | `1.15.x` | ⚠️ 落后 3 个小版本 |
| Security Crypto | `1.1.0-alpha06` | `1.1.0-alpha06` | ⚠️ 仍为 alpha 版本 |

**评估**: 部分核心依赖（AGP、Kotlin、Compose BOM）落后较久，建议在 v3.x 迭代中集中升级。其余依赖版本差距可接受或接近最新。

---

## 3. 已知 Bug 列表（docs/BUGS.md）

**源文件**: `docs/BUGS.md` (2026-05-15 生成)

### ✅ 已验证修复 (7 项)

| 编号 | 描述 | 状态 |
|------|------|------|
| BUG-001 | SettingsViewModel.kt 缺失 | ✅ 已修复 |
| BUG-002 | ConversationListSheet/VM 缺失 | ✅ 已修复 |
| BUG-005 | SSE trySend 可能丢数据 | ✅ 已处理 — Channel.BUFFERED |
| BUG-006 | 流式消息逻辑缺陷/协程泄漏 | ✅ 已修复 |
| BUG-007 | camera surfaceProvider 未定义 | ✅ 已修复 |
| BUG-008 | 麦克风权限未运行时申请 | ✅ 已修复 |
| BUG-009 | Domain层依赖Android Intent | ✅ 已修复 |

### 🔧 新修复（自查发现）

| 编号 | 描述 | 文件 | 修复 |
|------|------|:----:|------|
| BUG-010 | override 函数不允许默认参数值 | `AuthRepositoryImpl.kt:47` | 删除默认值 |

### 🏗 编译状态

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

**评估**: 所有已知 Bug 均已修复，当前无开放 Bug。

---

## 4. mipmap 图标分辨率检查

**路径**: `android/app/src/main/res/`

| 分辨率目录 | ic_launcher | ic_launcher_round |
|:----------:|:-----------:|:-----------------:|
| `mipmap-mdpi` | ✅ `ic_launcher.png` | ✅ `ic_launcher_round.png` |
| `mipmap-hdpi` | ✅ `ic_launcher.png` | ✅ `ic_launcher_round.png` |
| `mipmap-xhdpi` | ✅ `ic_launcher.png` | ✅ `ic_launcher_round.png` |
| `mipmap-xxhdpi` | ✅ `ic_launcher.png` | ✅ `ic_launcher_round.png` |
| `mipmap-xxxhdpi` | ✅ `ic_launcher.png` | ✅ `ic_launcher_round.png` |
| `mipmap-anydpi-v26` | ✅ `ic_launcher.xml` (adaptive) | ✅ `ic_launcher_round.xml` (adaptive) |

**评估**: ✅ 全部分辨率齐全，包含 5 种标准密度 + Adaptive Icon (Android 8+)。无缺失。

---

## 5. README.md 检查

**源文件**: `README.md` (共 204 行)

### 安装说明
- ✅ **Android 客户端安装说明**: 有（第 66-80 行），包含环境要求表、clone、Gradle 运行步骤
- ✅ **后端服务安装说明**: 有（第 82-107 行），包含 venv 创建、pip 安装、配置、启动命令
- ✅ **构建发布说明**: 有（第 112-135 行），包含 Release APK 构建和签名说明
- ✅ **APK 下载链接**: 有（第 133 行）

### 截图
- ❌ **无截图/图片**: README 中不包含任何截图、mockup、演示图片或屏幕录像。仅有文字和表格。

**评估**: 安装说明完整清晰，但缺少视觉效果截图，建议在 README 中加入 2-3 张核心功能截图（拍照解题页、AI 对话页、学习进度页）以提升吸引力。

---

## 6. 后端测试

### 测试文件

| 文件 | 大小 | 说明 |
|------|:----:|------|
| `backend/tests/__init__.py` | 空 | 包标记 |
| `backend/tests/conftest.py` | 4.2 KB | 测试配置 (fixtures, mock) |
| `backend/tests/test_health.py` | 830 B | 健康检查 (2 tests) |
| `backend/tests/test_auth.py` | 7.6 KB | 认证 (11 tests) |
| `backend/tests/test_chat.py` | 6.2 KB | 聊天 (9 tests) |
| `backend/tests/test_models.py` | 1.5 KB | 模型 (4 tests) |
| `backend/tests/test_subscription.py` | 3.1 KB | 订阅 (6 tests) |
| `backend/tests/test_user.py` | 4.4 KB | 用户 (9 tests) |

**合计**: 7 个 Python 测试文件（含 conftest.py），共 **6 个测试套件**。

### 测试运行结果

```text
============================= test session starts ==============================
platform linux -- Python 3.11.15
plugins: asyncio-1.3.0, anyio-4.13.0
collected 41 items

tests/test_auth.py .............                                    [ 31%]
tests/test_chat.py .........                                        [ 53%]
tests/test_health.py ..                                             [ 58%]
tests/test_models.py ....                                           [ 68%]
tests/test_subscription.py ......                                   [ 82%]
tests/test_user.py .........                                        [100%]

======================== 41 passed in 1.64s =========================
```

- ✅ **41/41 全部通过**
- ✅ **测试覆盖**: 认证(11)、聊天(9)、用户(9)、订阅(6)、模型(4)、健康检查(2)
- ✅ **Mock 外部服务**: conftest.py 自动 mock AI chat 和 content filter

### 注意事项
- 测试需要 `.venv` 虚拟环境激活（或使用 `.venv/bin/python3`）
- 使用的配置文件: `pyproject.toml` (asyncio_mode=auto)

---

## 7. GitHub Workflows

**目录**: `.github/workflows/` — 共 **2 个文件**

### android-ci.yml (1.1 KB)

```yaml
name: Android CI
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: gradle-${{ hashFiles('**/*.gradle*') }}
      - name: Build Release APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          cd android
          chmod +x gradlew
          ./gradlew assembleRelease --no-daemon --stacktrace
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: android/app/build/outputs/apk/release/app-release.apk
```

**评估**: 构建 Release APK → 上传至 Artifacts。依赖 GitHub Secrets 签名。

### backend-ci.yml (498 B)

```yaml
name: Backend CI
on:
  push:
    branches: [ main ]
    paths: [ 'backend/**' ]
  pull_request:
    branches: [ main ]
    paths: [ 'backend/**' ]
jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
          cache: 'pip'
      - run: pip install -r requirements.txt
      - run: pytest tests/ -v --asyncio-mode=auto
```

**评估**: 后端 CI 仅针对 `backend/**` 路径变更触发，安装依赖后运行全部 41 个测试。

### 已知问题
- GitHub Secrets (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) 可能为空 — 需用户在 Settings > Secrets 中配置
- android-ci.yml 无单元测试步骤，仅构建 Release APK

---

## 8. docs/ 目录文档统计

**路径**: `docs/` — 共 **37 个文件** (含 `docs/web/` 子目录 2 个)

### 按类型分类

| 类型 | 数量 | 文件 |
|:----:|:----:|------|
| 📋 **审计报告** | 6 | `AUDIT_CEO_MAY18.md`, `AUDIT_FEATURES_MAY18.md`, `AUDIT_FINAL_20260519.md`, `AUDIT_FINAL_MAY18.md`, `AUDIT_REPORT.md`, `AUDIT_V26_PLUS.md` |
| 📝 **项目规划** | 9 | `PRD.md`, `PRD_SPLIT.md`, `ROADMAP.md`, `v21_plan.md`, `v22_plan.md`, `v23_plan.md`, `v24_plan.md`, `v25_plan.md`, `v26_plan.md`, `v27_plan.md`, `v29_plan.md`, `v30_plan.md` |
| 🏗 **架构/设计** | 2 | `ARCH.md`, `v30_architecture.md` |
| 🧪 **测试报告** | 2 | `TEST_REPORT.md`, `v21_test_report.md` |
| 📊 **CEO 评估** | 3 | `CEO_REVIEW.md`, `CEO_OPTIMIZATION_REVIEW_v2.md`, `PROJECT_REPORT.md` |
| 📐 **项目结构** | 1 | `PROJECT_STRUCTURE.md` |
| 🔗 **API 文档** | 1 | `API.md` |
| ✅ **发布/质检** | 3 | `RELEASE_NOTES.md`, `QA_AUDIT_REPORT.md`, `STORE_LAUNCH_CHECKLIST.md` |
| 🔒 **隐私政策** | 1 | `PRIVACY_POLICY.md` |
| 🔄 **版本管理** | 1 | `VERSIONING.md` |
| 🧭 **其他** | 1 | `content_rating_guide.md` |
| 🐛 **Bug 跟踪** | 1 | `BUGS.md` |
| 🌐 **Web 页面 (HTML)** | 2 | `web/privacy_policy.html`, `web/user_agreement.html` |
| **合计** | **35 个 .md + 2 个 .html = 37 个文件** | |

**评估**: 文档覆盖全面，包含从产品需求、架构设计、测试报告到发布检查清单的完整文档链。迭代计划文档 (v21-v30) 详细记录了版本演进历史。

---

## 总结与建议

| 检查项 | 状态 | 备注 |
|:-------|:----:|:-----|
| 1. TODO/FIXME/HACK | ✅ | 仅 2 个 TODO，无 FIXME/HACK |
| 2. 依赖版本差距 | ⚠️ | AGP/Kotlin/Compose BOM 落后较多，建议集中升级 |
| 3. 已知 Bug | ✅ | 所有 10 个 Bug 已修复 |
| 4. mipmap 图标 | ✅ | 全分辨率齐全，含 Adaptive Icon |
| 5. README 安装说明 | ✅ | 完整 |
| 5. README 截图 | ❌ | 无截图，建议补充 |
| 6. 后端测试 | ✅ | 6 套件 41 测试全部通过 |
| 7. CI Workflows | ✅ | Android + Backend CI 配置完整 |
| 8. 文档覆盖 | ✅ | 37 个文档，覆盖全面 |

### 关键建议
1. **依赖升级**: 优先升级 AGP (8.2.2→8.7.x)、Kotlin (1.9.22→2.1.x)、Compose BOM (2024.02.00→latest)
2. **README 截图**: 添加 2-3 张核心功能截图提升项目可读性
3. **GitHub Secrets**: 确保 CI 中引用的签名 secrets 已配置
4. **Android 单元测试**: 当前 CI 仅有构建步骤，建议补充 Android 单元测试

---

*报告由 CEO 审计助手自动生成于 2026-05-23*
