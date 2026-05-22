# v2.7 P1 重要优化迭代 — 规划文档

> 生成时间: 2026-05-22
> PM: Hermes Agent
> 状态: ✅ 规划完成，等待分配 Coder

---

## 并行策略总览

```
时间轴 →
┌─────────────────────────────────────────────┐
│ P1-5 错题本 UI        │ P1-6 后端 CI/CD       │  ← 完全独立，可并行
├─────────────────────────────────────────────┤
│ P1-7 ProGuard 验证                          │  ← 独立，可与其他并行
└─────────────────────────────────────────────┘
```

- **P1-5 和 P1-6 完全独立**，可分配给不同 Coder 并行
- **P1-7 完全独立**（编译 APK 不需要等前两者），可与 P1-5/P1-6 并行
- 建议：3 人并行，或 2 人（P1-5 + P1-6 并行，P1-7 在某一完成后加入）

---

## P1-5 错题本 UI

### 现状核查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| WrongAnswerRepository (interface) | ✅ 已存在 | `domain/repository/WrongAnswerRepository.kt` — 5 个查询方法+2 个变更方法 |
| WrongAnswerRepositoryImpl | ✅ 已存在 | `data/repository/WrongAnswerRepositoryImpl.kt` — 完整实现 |
| WrongAnswerItem (domain model) | ✅ 已存在 | `domain/model/WrongAnswerItem.kt` — 17 个字段 |
| WrongAnswerEntity (entity) | ✅ 已存在 | `data/local/entity/WrongAnswerEntity.kt` |
| WrongAnswerDao | ✅ 已存在 | Room DAO |
| WrongAnswerScreen.kt | ❌ 缺失 | 需要新建 |
| WrongAnswerViewModel.kt | ❌ 缺失 | 需要新建 |
| Routes 路由定义 | ❌ 缺失 | Routes.kt 中无 `WRONG_ANSWERS` |
| AppNavGraph 注册 | ❌ 缺失 | 无 composable 注册 |
| DashboardScreen 入口 | ❌ 缺失 | 无错题本卡片 |

### 文件清单

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `android/app/src/main/java/com/aitutor/app/ui/screen/wronganswer/WrongAnswerScreen.kt` | ✏️ 新建 | 错题本 UI 页面，展示全部错题/按科目筛选/已完成/待复习 tab |
| 2 | `android/app/src/main/java/com/aitutor/app/ui/screen/wronganswer/WrongAnswerViewModel.kt` | ✏️ 新建 | ViewModel，注入 WrongAnswerRepository，管理错题列表状态 |
| 3 | `android/app/src/main/java/com/aitutor/app/ui/navigation/Routes.kt` | ✏️ 修改 | 添加 `const val WRONG_ANSWERS = "wrong_answers"` |
| 4 | `android/app/src/main/java/com/aitutor/app/ui/navigation/AppNavGraph.kt` | ✏️ 修改 | 注册 `composable(Routes.WRONG_ANSWERS)`，导入 WrongAnswerScreen |
| 5 | `android/app/src/main/java/com/aitutor/app/ui/screen/dashboard/DashboardScreen.kt` | ✏️ 修改 | 添加错题本入口卡片（含待复习数量角标），接收 `onNavigateToWrongAnswers` 回调 |

### 可复用的已有代码

- `WrongAnswerRepository` — 直接注入，调用 `getAllWrongAnswers()`、`getWrongAnswersBySubject()`、`getDueReviews()`
- `WrongAnswerItem` — 直接使用作为数据模型
- `ReviewCard.kt` — 可复用卡片组件（需要确认是否可通用）
- DashboardScreen 现有模式 — 入口卡片参考 `StatsOverviewCard` 或榜单卡片风格

### 设计要点

1. Screen 布局：顶部 Tabs（全部 / 数学 / 物理 / 化学 / 生物 / 语文 / 英语），列表显示错题
2. 每项显示：题目、正确答案、用户答案、科目、掌握状态、下次复习时间
3. 支持滑动删除（确认弹窗）
4. 支持标记已掌握
5. Dashboard 入口卡片显示「错题本」+ 待复习数量

---

## P1-6 后端 CI/CD

### 现状核查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Dockerfile | ✅ 已存在 | `backend/Dockerfile` — Python 3.12-slim, uvicorn |
| 测试文件数 | ✅ 8 个 | `test_health.py`, `test_auth.py`, `test_user.py`, `test_chat.py`, `test_subscription.py`, `test_models.py`, `conftest.py`, `__init__.py` |
| pytest 配置 | ✅ 已安装 | requirements.txt 已含 `pytest==8.3.4` + `pytest-asyncio==0.24.0` |
| 测试异步 | ✅ 已用 | 所有测试使用 `@pytest.mark.asyncio` + `async` fixtures |
| conftest 质量 | ✅ 良好 | 内存 SQLite + ASGITransport + 自动 Mock 外部服务 |
| GitHub Actions | ⚠️ 仅有 Android | `.github/workflows/android-ci.yml` 已存在，缺后端 CI |

### 文件清单

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `.github/workflows/backend-ci.yml` | ✏️ 新建 | 后端 CI/CD workflow |

### backend-ci.yml 设计

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

### 已知风险

- 测试使用 `app.routers.chat.content_filter` 路径 — 需确保该模块在 CI 环境存在
- 测试注册手机号 `13800138000` 硬编码 — 并发测试可能冲突（目前用内存数据库无影响）
- 如果 pytest 报 `ModuleNotFoundError`，需要在 `backend/` 下运行或配置 `PYTHONPATH`

---

## P1-7 ProGuard 验证

### 现状核查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| proguard-rules.pro | ✅ 已存在 | 290 行，覆盖完整 |
| Retrofit 保留 | ✅ 已覆盖 | lines 63-77 |
| Room 保留 | ✅ 已覆盖 | lines 90-97 |
| Hilt/Dagger 保留 | ✅ 已覆盖 | lines 102-131 |
| OkHttp 保留 | ✅ 已覆盖 | lines 81-86 |
| Compose 保留 | ✅ 已覆盖 | lines 158-167 |
| Kotlin 协程 | ✅ 已覆盖 | lines 135-140 |
| DTO/Entity/Model | ✅ 已覆盖 | lines 273-279 安全网 |
| 完整安全网 | ✅ 已覆盖 | line 44: `-keep class com.aitutor.app.** { *; }` |
| Release APK 编译 | ⚠️ 未验证 | 需要手动编译验证 |
| 混淆效果检查 | ⚠️ 未验证 | 需要解压 APK 检查 mapping.txt |

### 文件清单

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | 编译 Release APK | ⚙️ 执行 | `cd android && export KEYSTORE_PASSWORD=aitutor123 KEY_ALIAS=aitutor KEY_PASSWORD=aitutor123 && ./gradlew assembleRelease` |
| 2 | 检查 mapping.txt | 🔍 检查 | 验证 `android/app/build/outputs/mapping/release/mapping.txt` 是否生成 |
| 3 | 检查 APK 混淆 | 🔍 检查 | 解压 APK，用 `javap` 检查 Retrofit API / Room DAO / Hilt 组件类名是否保留 |
| 4 | proguard-rules.pro (如需) | ✏️ 修改 | 如果发现缺失的保留规则，补充修复 |

### 预期结果

- `mapping.txt` 正常生成
- Retrofit API 接口（`data/remote/api/*`）类名和方法保留
- Room Entity/Dao 类名保留
- Hilt 组件和 Dagger 保留
- Composable 函数保留
- DTO/Entity/Model 数据类保留
- 如果全部通过，无需修改 proguard-rules.pro

---

## 汇总：所有需变更的文件

| 任务 | 文件路径 | 操作类型 |
|------|---------|---------|
| P1-5 | `android/app/src/main/java/com/aitutor/app/ui/screen/wronganswer/WrongAnswerScreen.kt` | ✏️ 新建 |
| P1-5 | `android/app/src/main/java/com/aitutor/app/ui/screen/wronganswer/WrongAnswerViewModel.kt` | ✏️ 新建 |
| P1-5 | `android/app/src/main/java/com/aitutor/app/ui/navigation/Routes.kt` | ✏️ 修改 |
| P1-5 | `android/app/src/main/java/com/aitutor/app/ui/navigation/AppNavGraph.kt` | ✏️ 修改 |
| P1-5 | `android/app/src/main/java/com/aitutor/app/ui/screen/dashboard/DashboardScreen.kt` | ✏️ 修改 |
| P1-6 | `.github/workflows/backend-ci.yml` | ✏️ 新建 |
| P1-7 | Release APK 编译 + 验证 | ⚙️ 执行 |
| P1-7 | `android/app/proguard-rules.pro` (可能) | ✏️ 修改 |

**总计：5 个代码文件变更 + 1 个 CI 文件新建 + 1 次编译验证**

---

## 负责人分配建议

| 任务 | 建议 | 预估工时 |
|------|------|---------|
| P1-5 错题本 UI | Coder A | 1.5 天 |
| P1-6 后端 CI/CD | Coder B | 0.5 天 |
| P1-7 ProGuard 验证 | Coder B (完成 P1-6 后) | 0.5 天 |
