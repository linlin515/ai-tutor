# AI 学伴 v4.0 Sprint 1 — 产品需求文档 (PRD)

> **文档版本**: v1.0  
> **日期**: 2026-05-26  
> **负责人**: PM  
> **状态**: 待评审  
> **基准文档**: PRD.md v1.2, CEO_FEATURE_GAP_ANALYSIS.md, CEO_FEATURE_GAP_DATA.md  
> **Sprint 目标**: 补齐 P0 核心断裂点，达成生产就绪度 80%+

---

## 目录

1. [Sprint 概述](#1-sprint-概述)
2. [P0-1: Google Play Billing 集成](#2-p0-1-google-play-billing-集成)
3. [P0-2: 后端补齐 5 个路由](#3-p0-2-后端补齐-5-个路由)
4. [P0-3: ASR 路径对齐](#4-p0-3-asr-路径对齐)
5. [风险与依赖](#5-风险与依赖)
6. [附录：参考数据](#6-附录参考数据)

---

## 1. Sprint 概述

### 1.1 背景

AI 学伴 Android 项目（v2.7.0）已完成五位一体功能体系（拍照解题 + AI 对话 + 语音交互 + 智能出题 + 游戏化），在功能完整性上领先竞品。但 CEO 审计发现 **6 个接口未对接 + 1 个支付闭环断裂** 的核心问题，生产就绪度仅 **55%**。

Sprint 1 聚焦 **3 个 P0 功能**，目标是将生产就绪度从 55% 提升至 80%+，使 App 达到可上架发布的最低要求。

### 1.2 Sprint 范围

| 优先级 | 功能 | 类型 | 预估工作量 | 依赖 |
|:------:|:-----|:----:|:----------:|:----:|
| P0-1 | Google Play Billing 集成 | Android | 3-5 人日 | 无 |
| P0-2 | 后端补齐 5 个路由 | 后端 | 5-7 人日 | 无 |
| P0-3 | ASR 路径对齐 | Android + 后端 | 1-2 人日 | 无 |

**总预估工作量**: 9-14 人日  
**建议人力**: 1 Android Coder + 1 Backend Coder 并行

### 1.3 验收全局原则

1. 所有代码需通过现有测试套件（`pytest` / Android Unit Test）
2. 新增 API 路由需在 Swagger docs 中可浏览和调试
3. 支付流程需在 Sandbox 环境端到端验证通过
4. ASR 路径对齐后，旧路径 `api/v1/voice/asr` 需保留兼容或返回 301

---

## 2. P0-1: Google Play Billing 集成

### 2.1 功能描述

集成 Google Play Billing Library 5.x，打通 Android 应用内购买 → 后端 verify → 配额消耗的完整支付闭环。当前状态：后端 `POST /api/v1/subscription/verify` 已实现、Android 端 `SubscriptionScreen` UI 已有、`SubscriptionRepository` 和 `SubscriptionApi` 已定义，但 **中间支付环节断裂**——Android 端未集成 BillingClient，无法发起购买。

### 2.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-BILL-01 | 免费版用户 | 在订阅页面看到 premium 套餐价格并点击购买 | 我可以升级到付费版获得无限提问和语音交互功能 |
| US-BILL-02 | 订阅用户 | 完成 Google Play 支付后自动激活 premium 权益 | 我不需要手动输入兑换码或联系客服 |
| US-BILL-03 | 订阅用户 | 每日使用 AI 功能后消耗配额 | 系统能准确记录我的使用量，防止超额使用 |
| US-BILL-04 | 免费版用户 | 在配额用完后看到购买引导 | 我知道需要升级才能继续使用 |
| US-BILL-05 | 后端运维 | 验证 purchase_token 的真实性后再激活订阅 | 防止伪造购买凭证绕过支付 |

### 2.3 验收标准

#### AC-BILL-01: BillingClient 连接建立

```
Given  用户打开订阅页面 (SubscriptionScreen)
When   页面初始化时调用 BillingClient.startConnection()
Then   BillingClient 连接状态应为 CONNECTED
And    连接失败时显示 "支付服务连接失败，请检查 Google Play 服务" 错误提示
And    连接成功后记录 BillingClient 实例到 Application 级别单例
```

#### AC-BILL-02: 查询订阅商品 SKU

```
Given  BillingClient 已连接
When  调用 queryProductDetailsAsync(ProductType.SUBS)
Then  成功返回 premium 订阅商品列表（含 productId、title、description、price）
And   查询失败时显示 "无法获取商品信息" 并引导重试
And   商品信息渲染到 SubscriptionScreen 的方案卡片上
```

**商品定义**（需在 Google Play Console 配置）：

| Product ID | 类型 | 周期 | 建议定价 |
|:-----------|:----:|:----:|:--------:|
| `premium_monthly` | Subs (订阅) | 月付 | ¥30/月（或等价本地货币） |
| `premium_yearly` | Subs (订阅) | 年付 | ¥198/年（≈ 月付的 55 折） |

#### AC-BILL-03: 发起购买流程

```
Given  用户已登录且 BillingClient 已获取商品列表
When   用户点击 premium 套餐的 "立即订阅" 按钮
Then   调用 BillingClient.launchBillingFlow(activity, billingFlowParams)
And   Google Play 支付弹窗正常弹出
And   用户可选择已绑定的支付方式完成支付
And   购买失败时（取消/支付错误）显示对应提示，不产生扣费
```

#### AC-BILL-04: 处理购买回调

```
Given  用户完成 Google Play 支付
When   BillingClient 的 PurchasesUpdatedListener 收到 onPurchasesUpdated
Then   purchaseToken 与 productId 不为空
And    purchaseState 为 PURCHASED
And    acknowledgePurchase() 在 3 秒内被调用
And    若 acknowledge 失败，重试 3 次（指数退避）
And    Android 端不自行处理消耗（非消耗品订阅）
```

#### AC-BILL-05: 购买成功后调后端 verify API

```
Given  购买回调成功（purchaseToken + productId 有效）
When   调用 POST /api/v1/subscription/verify
       Body: {
         "purchase_token": "purchasetokenabc123",
         "product_id": "premium_monthly"
       }
Then   后端返回 200 OK + SubscriptionStatus (plan_type=premium, is_active=true)
And   Android 端收到成功响应后更新本地订阅缓存（Room）
And   UI 刷新显示 "订阅成功！" 提示，订阅状态变为 premium
And   verify 失败时（后端验证不通过），显示 "订阅验证失败，请联系客服" 并标记问题
```

> **后端 subscribe/verify 改造说明**：当前 `verify` 实现为简化版（不做真实 Google Play API 校验）。Sprint 1 中需要增加对 purchase_token 的本地缓存检查（防止同一 token 重复使用），并记录购买日志。**生产环境加固**（调用 Google Play Developer API 真实校验）留到 Sprint 2。

#### AC-BILL-06: 配额消耗调后端 consume API

```
Given  用户为 premium 或 free 身份
When   用户发起一次 AI 对话/拍照解题/出题等消耗配额的请求
Then   在业务请求前/后调用 POST /api/v1/subscription/consume
       Body: {
         "feature": "text_chat" | "photo_solve" | "quiz"
       }
And   后端返回当前剩余配额 (remaining_quota)
And   Android 端更新本地配额缓存
And   剩余配额为 0 时（仅 free 用户），阻止后续请求并显示 "今日配额已用完，升级解锁无限使用"
```

#### AC-BILL-07: 恢复购买

```
Given  用户已登录且之前购买过 premium 订阅
When   用户点击 "恢复购买" 按钮
Then   调用 BillingClient.queryPurchasesAsync(ProductType.SUBS)
And   若有有效订阅 → 调用 verify 同步到后端 → 恢复 premium 权限
And   若无有效订阅 → 提示 "未找到可恢复的订阅"
```

#### AC-BILL-08: 订阅状态自动同步

```
Given  用户已激活 premium 订阅
When   App 启动时 / 订阅页面进入时
Then   调用 GET /api/v1/subscription/status
And   本地缓存（Room）同步更新
And   若订阅已过期（end_date < now），自动降级为 free 权限
```

### 2.4 依赖库

```kotlin
// build.gradle.kts (app)
implementation("com.android.billingclient:billing:5.2.1")
implementation("com.android.billingclient:billing-ktx:5.2.1")  // Kotlin 扩展
```

### 2.5 涉及修改文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/build.gradle.kts` | 修改 | 添加 billing 依赖 |
| `data/billing/BillingManager.kt` | 新建 | BillingClient 生命周期管理、连接/购买/回调/恢复 |
| `data/billing/BillingProduct.kt` | 新建 | 商品模型 (productId, title, price, type) |
| `data/remote/api/SubscriptionApi.kt` | 修改 | 添加 verifyPurchase() 方法（若缺少） |
| `data/remote/dto/SubscriptionDtos.kt` | 修改 | 添加/确认 VerifyRequest DTO |
| `data/repository/SubscriptionRepositoryImpl.kt` | 修改 | 集成 BillingManager，购买后自动调 verify |
| `domain/repository/SubscriptionRepository.kt` | 修改 | 添加 verifyPurchase()、consumeQuota() 接口 |
| `ui/subscription/SubscriptionScreen.kt` | 修改 | 绑定真实商品数据，接入购买流程，显示购买结果 |
| `ui/subscription/SubscriptionViewModel.kt` | 修改 | 添加购买/恢复/状态同步逻辑 |
| `di/BillingModule.kt` | 新建 | Hilt Module 提供 BillingManager |
| `AndroidManifest.xml` | 修改 | 添加 `com.android.vending.BILLING` 权限 |

---

## 3. P0-2: 后端补齐 5 个路由

### 3.1 功能描述

Android 端已定义 5 个 API 调用（`QuizApi.kt`, `AnalyticsApi.kt`, `GamificationApi.kt`, `SubscriptionApi.kt`），但后端缺少对应路由，导致功能"假死"——用户点击后拿不到数据。此 P0 任务要求后端在现有 FastAPI 项目中补充这 5 个路由。

### 3.2 路由总览

| # | 方法 | 路由路径 | Android 来源 | 用途 | 预估工作量 |
|:-:|:----:|:---------|:-------------|:-----|:----------:|
| 1 | POST | `/api/v1/quiz/generate` | `QuizApi.kt: generateQuiz()` | AI 生成练习题 | 2-3 人日 |
| 2 | POST | `/api/v1/quiz/submit` | `QuizApi.kt: submitQuiz()` | 提交答案 + AI 批改 | 1-2 人日 |
| 3 | GET | `/api/v1/analytics/stats` | `AnalyticsApi.kt: getStats()` | 学习统计数据 | 1-2 人日 |
| 4 | POST | `/api/v1/game/sync/score` | `GamificationApi.kt: syncScore()` | 同步游戏积分 | 1 人日 |
| 5 | POST | `/api/v1/subscription/consume` | `SubscriptionApi.kt: consumeQuota()` | 消耗配额 | 1 人日 |

### 3.3 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-API-01 | 学生用户 | 选择学科和知识点后生成练习题 | 我可以针对薄弱环节进行针对性训练 |
| US-API-02 | 学生用户 | 提交作答后获得 AI 批改和错因解析 | 我知道错在哪里并针对性改进 |
| US-API-03 | 学生用户 | 在仪表盘查看学习统计数据 | 我能了解自己的学习进度和薄弱点 |
| US-API-04 | 游戏化用户 | 我的学习积分能同步到云端 | 排行榜能反映真实的学习成果 |
| US-API-05 | 免费版用户 | 每次使用后配额被准确消耗 | 我不会无意中超额使用 |
| US-API-06 | 后端管理员 | 所有路由都统一错误码和日志 | 我能在出问题时快速定位 |

### 3.4 API 详细设计

#### 3.4.1 POST /api/v1/quiz/generate

**功能**: 根据指定的学科、知识点、难度和题目数量，调用 AI 生成练习题。

**Request**:
```json
{
  "subject": "math",
  "topic": "二次函数",
  "difficulty": "medium",
  "count": 5,
  "question_types": ["multiple_choice", "fill_blank"],
  "grade": "初三"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| subject | string | 是 | — | 学科: math/physics/chemistry/biology/chinese/english |
| topic | string | 否 | "通用" | 知识点（留空则 AI 自动选择） |
| difficulty | string | 否 | "medium" | 难度: easy/medium/hard |
| count | int | 否 | 5 | 题目数量, 范围 1-10 |
| question_types | string[] | 否 | ["multiple_choice"] | 题型: multiple_choice/fill_blank/true_false/essay |
| grade | string | 否 | 从用户 profile 读取 | 年级: 小学/初中/高中/大学 |

**Response**:
```json
{
  "code": 0,
  "data": {
    "quiz_id": "uuid",
    "questions": [
      {
        "id": 1,
        "type": "multiple_choice",
        "content": "二次函数 y = ax² + bx + c 的顶点坐标是？",
        "options": [
          {"key": "A", "value": "(-b/2a, (4ac-b²)/4a)"},
          {"key": "B", "value": "(b/2a, (4ac-b²)/4a)"},
          {"key": "C", "value": "(-b/a, (4ac-b²)/4a)"},
          {"key": "D", "value": "(-b/2a, (b²-4ac)/4a)"}
        ],
        "correct_answer": "A",
        "explanation": "二次函数的顶点坐标公式为 (-b/2a, (4ac-b²)/4a)...",
        "difficulty": "medium",
        "points": 10
      }
    ],
    "total_questions": 5,
    "subject": "math",
    "topic": "二次函数",
    "generated_at": "2026-05-26T10:00:00Z"
  }
}
```

**错误码**:

| code | message | 说明 |
|:----:|:--------|:-----|
| 0 | 成功 | — |
| 400 | 参数错误 | 学科/题数不合法 |
| 403 | 配额不足 | 免费用户每日出题次数已用完 |
| 429 | 请求过于频繁 | 30 秒内不可重复出题 |
| 500 | AI 生成超时 | AI 响应超过 30 秒 |

**业务规则**:
- 免费用户每日最多生成 3 次测验（需配额检查）
- 超时后（AI > 30s）返回 500，客户端提示重试
- questions 中的 correct_answer 在返回给客户端时需要保留（客户端在提交前不展示正确答案）
- 生成的 quiz 需持久化到数据库（quiz_results 或新表）

#### 3.4.2 POST /api/v1/quiz/submit

**功能**: 提交用户作答结果，AI 进行批改评分，返回结果和错因解析。

**Request**:
```json
{
  "quiz_id": "uuid",
  "answers": [
    {
      "question_id": 1,
      "selected_answer": "A",
      "time_spent_seconds": 30
    },
    {
      "question_id": 2,
      "selected_answer": "C",
      "time_spent_seconds": 45
    }
  ]
}
```

**Response**:
```json
{
  "code": 0,
  "data": {
    "quiz_id": "uuid",
    "score": 80,
    "total_points": 50,
    "earned_points": 40,
    "results": [
      {
        "question_id": 1,
        "is_correct": true,
        "correct_answer": "A",
        "user_answer": "A",
        "explanation": "正确！顶点坐标公式为 (-b/2a, (4ac-b²)/4a)",
        "mastery_score": 0.9
      },
      {
        "question_id": 2,
        "is_correct": false,
        "correct_answer": "B",
        "user_answer": "C",
        "explanation": "注意判别式 Δ = b² - 4ac，顶点纵坐标应为 (4ac-b²)/4a...",
        "mastery_score": 0.3,
        "weakness": "对判别式符号理解不清晰"
      }
    ],
    "summary": {
      "total": 5,
      "correct": 4,
      "accuracy": 0.8,
      "mastery_level": "良好",
      "suggestions": ["建议复习二次函数顶点坐标推导过程"]
    },
    "wrong_answer_ids": ["uuid1"]  // 答错题目自动记录的 ID
  }
}
```

**业务规则**:
- 每道题给出 mastery_score（0-1），低于 0.6 标记为薄弱点
- 答错题目自动记录到 wrong_answers 表（供错题本和间隔复习使用）
- 更新用户的 daily_quota 使用计数
- 评分可纯后端逻辑完成（无需调用 AI），内置判题规则

#### 3.4.3 GET /api/v1/analytics/stats

**功能**: 返回用户学习统计数据，供 Dashboard 展示。

**Request**: 无需 Body，通过 Token 识别用户。

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| period | string | 否 | "weekly" | 统计周期: daily/weekly/monthly/all |
| subject | string | 否 | "all" | 学科过滤（可选） |

**Response**:
```json
{
  "code": 0,
  "data": {
    "overview": {
      "study_minutes_today": 25,
      "questions_solved_today": 12,
      "accuracy_today": 0.75,
      "streak_days": 7,
      "total_knowledge_points": 42,
      "mastered_points": 28,
      "mastery_rate": 0.67
    },
    "daily_stats": [
      {
        "date": "2026-05-20",
        "study_minutes": 30,
        "questions_solved": 10,
        "accuracy": 0.8
      },
      {
        "date": "2026-05-21",
        "study_minutes": 45,
        "questions_solved": 15,
        "accuracy": 0.73
      }
    ],
    "subject_breakdown": [
      {
        "subject": "math",
        "questions_solved": 80,
        "accuracy": 0.75,
        "study_minutes": 200
      },
      {
        "subject": "physics",
        "questions_solved": 30,
        "accuracy": 0.83,
        "study_minutes": 90
      }
    ],
    "weak_areas": [
      {"topic": "二次函数", "mastery": 0.4, "subject": "math"},
      {"topic": "牛顿第二定律", "mastery": 0.5, "subject": "physics"}
    ]
  }
}
```

**数据来源**: 聚合以下数据库表
- `chat_history` / `question_records` — 解题数、正确率
- `wrong_answers` — 薄弱点分析
- `quiz_results` — 测验正确率
- `daily_quota` — 学习时长估算
- `learning_records` (若存在) — 知识点掌握度

**业务规则**:
- 首次使用用户 → 返回空数据 + "开始学习吧！" 引导标记
- 数据从本地用户行为表聚合计算，不需要外部 AI 调用
- `streak_days` (连续学习天数) 算法：从今天往前，每天至少有一次有效学习行为
- 性能要求：响应时间 < 200ms（通过数据库聚合查询或 Redis 缓存）

#### 3.4.4 POST /api/v1/game/sync/score

**功能**: 同步用户的游戏积分（学习积分）到云端，更新排行榜数据。

**Request**:
```json
{
  "score_data": {
    "total_points": 1250,
    "daily_points": 50,
    "streak_days": 7,
    "achievements_unlocked": ["first_solve", "week_streak"]
  },
  "sync_timestamp": "2026-05-26T10:30:00Z"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:-----|:----:|:----:|:-----|
| total_points | int | 是 | 总积分（客户端累积值） |
| daily_points | int | 是 | 当日获取积分 |
| streak_days | int | 否 | 连续学习天数 |
| achievements_unlocked | string[] | 否 | 本地上新解锁的成就 ID 列表 |
| sync_timestamp | string | 是 | 同步时间戳 (ISO 8601) |

**Response**:
```json
{
  "code": 0,
  "data": {
    "synced": true,
    "server_total_points": 1250,
    "rank": 42,
    "total_users": 1024,
    "new_achievements": [],
    "conflict": false
  }
}
```

**业务规则**:
- 以服务端积分为准，客户端同步触发服务端更新
- 如果客户端 `total_points` 低于服务端已有值 → 返回服务端值，`conflict=true`
- achievements_unlocked 中的成就 ID 若已在服务端记录则忽略，新成就记录到成就表
- 更新排行榜数据（leaderboard 表）
- 频率限制：每分钟最多同步 1 次（防刷分）

#### 3.4.5 POST /api/v1/subscription/consume

**功能**: 消耗用户的一次配额调用。后端验证配额是否足够，扣除并返回剩余。

**Request**:
```json
{
  "feature": "text_chat"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:-----|:----:|:----:|:-----|
| feature | string | 是 | 功能标识: text_chat / photo_solve / quiz_generate / voice_asr / tts |

**Response** (成功):
```json
{
  "code": 0,
  "data": {
    "consumed": true,
    "quota_remaining": 4,
    "plan_type": "free",
    "is_premium": false
  }
}
```

**Response** (配额不足):
```json
{
  "code": 403,
  "message": "今日配额已用完",
  "data": {
    "consumed": false,
    "quota_remaining": 0,
    "plan_type": "free",
    "upgrade_url": null
  }
}
```

**业务规则**:
- premium 用户：不消耗配额（但记录调用次数用于分析），`quota_remaining` 返回 -1 表示无限
- free 用户：每次调用消耗 1 次配额
- 每日配额重置逻辑：基于 UTC 日期，每天 00:00 UTC 重置为 `daily_quota_free`（默认 5）
- 并发安全：使用 SQL 原子更新或加锁防止超卖
- 配额检查也可在业务 API 层先调用此路由校验，或由业务 API 内部集成

### 3.5 后端实现要求

#### 3.5.1 路由注册

在 `backend/app/main.py` 中注册新增路由：
```python
app.include_router(quiz.router)          # 新增
app.include_router(analytics.router)     # 新增
app.include_router(game.router)          # 已存在，需扩展
```

**路由文件创建**:
- `backend/app/routers/quiz.py` — quiz/generate + quiz/submit
- `backend/app/routers/analytics.py` — analytics/stats
- 扩展 `backend/app/routers/game.py` — 添加 sync/score
- 扩展 `backend/app/routers/subscription.py` — 添加 consume

#### 3.5.2 数据模型扩展

需要新建/扩展的 SQLAlchemy 模型：

| 模型 | 表名 | 说明 |
|:-----|:-----|:-----|
| `QuizRecord` | `quiz_records` | 测验记录 (id, user_id, subject, topic, score, created_at) |
| `QuizQuestion` | `quiz_questions` | 题目记录 (id, quiz_id, content, correct_answer, is_correct, user_answer) |
| `AnalyticsSnapshot` | `analytics_snapshots` | 统计数据快照（可选，用于加速查询） |
| `UserScore` | `user_scores` | 用户积分 (user_id, total_points, rank, updated_at) |
| `AchievementRecord` | `achievement_records` | 成就记录 (user_id, achievement_id, unlocked_at) |
| 扩展 `DailyQuota` | `daily_quotas` | 增加 feature 维度或完善现有 |

#### 3.5.3 统一错误码

所有新增路由使用现有 `app/schemas/common.py` 的统一响应格式。

### 3.6 验收标准

#### AC-API-01: POST /api/v1/quiz/generate

```
Given  用户已登录且具有有效 Token
When   发送 POST /api/v1/quiz/generate
       Body: {"subject": "math", "topic": "二次函数", "difficulty": "medium", "count": 3}
Then   返回 200 OK
And    data.quiz_id 不为空
And    data.questions 长度等于请求的 count (3)
And    每个 question 包含 id, type, content, options, correct_answer, explanation
And    correct_answer 的值是有效的选项 key
```

```
Given  免费用户当日已用完出题配额
When   发送同上请求
Then   返回 403 错误码 + message 提示配额不足
```

```
Given  请求参数不合法（subject 为空、count=0 或 count>10）
When   发送请求
Then   返回 400 错误码 + message 提示参数错误
```

#### AC-API-02: POST /api/v1/quiz/submit

```
Given  用户有一个有效的 quiz_id
When   发送 POST /api/v1/quiz/submit
       Body: {"quiz_id": "valid-uuid", "answers": [...]}
Then   返回 200 OK
And    data.score 在 0-100 之间
And    data.results 长度与 answers 一致
And    每个 result 包含 is_correct, correct_answer, user_answer, explanation
And    答错的题目自动写入 wrong_answers 表
```

```
Given  quiz_id 不存在或不属于当前用户
When   发送提交请求
Then   返回 404 错误码
```

#### AC-API-03: GET /api/v1/analytics/stats

```
Given  用户有至少 7 天的学习记录
When   发送 GET /api/v1/analytics/stats?period=weekly
Then   返回 200 OK
And    data.overview.study_minutes_today >= 0
And    data.daily_stats 数组长度 <= 7
And    data.subject_breakdown 包含至少一个学科
```

```
Given  新注册用户无任何学习记录
When   发送 GET /api/v1/analytics/stats
Then   返回 200 OK
And    data.overview.study_minutes_today = 0
And    data.daily_stats 为空数组 []
And    客户端可根据空数据展示引导提示
```

#### AC-API-04: POST /api/v1/game/sync/score

```
Given  用户本地有 1250 积分
When   发送 POST /api/v1/game/sync/score
       Body: {"score_data": {"total_points": 1250, "daily_points": 50, "streak_days": 7}, "sync_timestamp": "..."}
Then   返回 200 OK
And    data.synced = true
And    data.server_total_points >= 1250
```

```
Given  客户端积分低于服务端已有积分（冲突场景）
When   发送同步请求
Then   返回 data.conflict = true
And    data.server_total_points 返回服务端更高值
And    Android 端收到冲突标记后以服务端值为准
```

#### AC-API-05: POST /api/v1/subscription/consume

```
Given  用户为 free 身份，当日已使用 3 次，总配额 5
When   发送 POST /api/v1/subscription/consume
       Body: {"feature": "text_chat"}
Then   返回 200 OK
And    data.consumed = true
And    data.quota_remaining = 1
```

```
Given  用户为 free 身份，当日配额已用完 (used=5, quota=5)
When   发送消耗请求
Then   返回 403
And    data.consumed = false
And    data.quota_remaining = 0
```

```
Given  用户为 premium 身份
When   发送消耗请求
Then   返回 200 OK
And    data.consumed = true
And    data.quota_remaining = -1  (表示无限)
```

---

## 4. P0-3: ASR 路径对齐

### 4.1 功能描述

当前 Android 端调用 ASR 的路径为 `api/v1/voice/asr`，但后端实际 ASR 端点位于 `/v1/audio/transcriptions`（遵循 OpenAI 兼容格式）。路径不一致导致 ASR 云端降级功能无法正常工作。同时需要增加后端 TTS 开关选项，使 Android 端可选择使用本地 TTS 或云端 TTS。

### 4.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-ASR-01 | 用户（弱网环境） | 语音输入能自动使用本地 ASR | 即使网络不好也能用语音提问 |
| US-ASR-02 | 用户（高质量需求） | 长语音能自动降级到云端 ASR | 获得更高的识别准确率 |
| US-ASR-03 | 用户 | 在设置中选择使用云端 TTS 或本地 TTS | 我可以根据流量和音质偏好选择 |
| US-ASR-04 | 开发者 | ASR 路径对齐统一 | 维护时不需要记两套路径 |

### 4.3 详细设计

#### 4.3.1 Android 端修改

**现状**: Android `AiTutorApi.kt` 中定义：
```kotlin
@POST("api/v1/voice/asr")
suspend fun voiceAsr(@Body request: VoiceAsrRequest): Response<ApiResponse<TranscriptionResponse>>
```

**修改为**:
```kotlin
@POST("v1/audio/transcriptions")
@Headers("Content-Type: multipart/form-data")
suspend fun transcribeAudio(@Part file: MultipartBody.Part): Response<TranscriptionResponse>
```

**兼容性要求**:
- 旧路径 `api/v1/voice/asr` 必须在 Android 端删除或注释
- 后端若收到旧路径请求，返回 301 重定向或 404（由后端决定）
- 接口协议从 JSON body 改为 multipart/form-data（与后端现有实现一致）

#### 4.3.2 后端 TTS 开关选项

**后端新增配置**:
```python
# backend/app/config.py 新增
class Settings(BaseSettings):
    # ... 现有配置 ...
    tts_enabled: bool = True           # 是否启用云端 TTS
    tts_engine: str = "openai"         # openai / azure / edge（预留）
    tts_default_voice: str = "alloy"   # 默认音色
    tts_quota_per_day: int = 50        # 免费用户每日 TTS 次数限制
```

**新增后端 API**: 返回 TTS 配置给客户端
```
GET /api/v1/audio/config
Response:
{
  "code": 0,
  "data": {
    "tts_enabled": true,
    "default_voice": "alloy",
    "supported_voices": ["alloy", "echo", "fable", "onyx", "nova", "shimmer"],
    "tts_quota_remaining": 50
  }
}
```

#### 4.3.3 Android 端 TTS 开关设置

在 `SettingsScreen.kt` 或语音设置区域新增：

| 设置项 | 控件 | 说明 |
|:-------|:-----|:-----|
| TTS 引擎 | 切换开关 | 本地 TTS / 云端 TTS（后端） |
| 默认音色 | 下拉选择器 | 仅云端模式下可用，列表来自 `GET /api/v1/audio/config` |

**VoiceRepository 修改**:
```kotlin
// VoiceRepository 接口新增
interface VoiceRepository {
    // ... 现有方法 ...
    suspend fun getTtsConfig(): TtsConfig
    fun setTtsMode(mode: TtsMode)  // LOCAL / CLOUD
    fun getTtsMode(): TtsMode
}
```

**TTS 降级逻辑**:
```
When TTS 模式 = 云端:
  → 短文本(≤500字) && 网络良好 → 调用 POST /v1/audio/speech
  → 长文本(>500字) → 分段调用云端 TTS
  → 网络不可用 → 自动降级到本地 TTS
When TTS 模式 = 本地:
  → 直接使用 android.speech.tts.TextToSpeech
```

### 4.4 验收标准

#### AC-ASR-01: Android ASR 路径修正

```
Given  用户触发云端 ASR 识别
When   语音录制完成后发送请求
Then   请求 URL 为 POST <base>/v1/audio/transcriptions
And    Content-Type 为 multipart/form-data
And    body 包含 file 字段（音频文件）和 model/language 参数
And   响应成功解析为 TranscriptionResponse(text="...")
And   旧路径 api/v1/voice/asr 不再被调用
```

#### AC-ASR-02: 后端响应 ASR

```
Given  后端接收 POST /v1/audio/transcriptions 请求
When   请求携带有效的音频文件和 JWT Token
Then   返回 200 OK
And   响应格式为 {"text": "识别后的文字"}
```

#### AC-ASR-03: 后端 TTS 开关配置 API

```
Given  用户已登录
When   发送 GET /api/v1/audio/config
Then   返回 200 OK
And   data.tts_enabled 为 true/false
And   data.supported_voices 包含至少 "alloy"
And   data.default_voice 为有效的音色名称
```

#### AC-ASR-04: Android TTS 模式切换

```
Given  用户进入设置页面
When   查看语音设置区域
Then   显示 "TTS 引擎" 切换开关（本地 / 云端）
And   切换为云端时显示音色选择下拉
And   切换为本地时隐藏音色选择
```

```
Given  用户已切换 TTS 模式为云端
When   点击消息气泡的 TTS 播放按钮
Then   调用 POST /v1/audio/speech
And   成功返回音频数据并播放
And   网络不可用时自动降级到本地 TTS 播放
```

### 4.5 涉及修改文件清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| **Android**: | | |
| `data/remote/api/AiTutorApi.kt` | 修改 | ASR 路径改为 `v1/audio/transcriptions`，改为 multipart |
| `data/remote/dto/AudioDtos.kt` | 修改 | 确认/调整 ASR 请求 DTO |
| `data/repository/VoiceRepositoryImpl.kt` | 修改 | 实现云端 TTS 调用 + 本地/云端切换逻辑 |
| `data/media/CloudAsrEngine.kt` | 新建/修改 | 确保 ASR 使用新的路径 |
| `data/media/CloudTtsEngine.kt` | 新建 | 实现云端 TTS 流式播放 |
| `domain/repository/VoiceRepository.kt` | 修改 | 添加 TTS 模式切换、getTtsConfig 接口 |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加 TTS 引擎切换开关 + 音色选择 |
| `ui/settings/SettingsViewModel.kt` | 修改 | TTS 设置状态管理 |
| `data/remote/interceptor/TokenManager.kt` | 修改 | 确保 ASR multipart 请求携带 Token |
| **后端**: | | |
| `app/routers/audio.py` | 修改 | 添加 TTS 配额检查、日志增强 |
| `app/config.py` | 修改 | 添加 tts_enabled 等配置项 |
| `app/schemas/audio.py` | 修改 | 添加 TTS Config 响应 Schema |
| `app/main.py` | 修改 | 注册新增路由（若需要） |

---

## 5. 风险与依赖

### 5.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| Google Play Console 开发者账号审核未通过 | 中 | 高 | 提前准备资质材料；使用 Internal Test Track 先行验证 |
| 后端 5 个路由涉及 AI 调用延迟过高 | 中 | 中 | Quiz generate 设置 30s 超时，客户端显示加载动画 |
| ASR 路径修改导致现有本地 ASR 功能异常 | 低 | 高 | 保留本地 ASR 兜底，修改后回归测试长按录音功能 |
| BillingClient 5.x API 变更 | 低 | 中 | 参考 Google 官方文档最新版本，使用 billing-ktx |
| 同步积分冲突导致数据不一致 | 低 | 中 | 服务端始终为权威源，冲突时返回服务端数据 |

### 5.2 依赖关系

| 任务 | 依赖 | 说明 |
|:-----|:-----|:-----|
| P0-1 (Billing) | Google Play Console 完成商品配置 | 需提前创建 premium_monthly / premium_yearly 订阅商品 |
| P0-1 (Billing) | 后端 `POST /api/v1/subscription/verify` 已就绪 | ✅ 已有，但需要加固 |
| P0-2 (路由) | 无外部依赖 | 纯后端实现 |
| P0-2 (consume) | 无外部依赖 | 基于现有 subscription 模型扩展 |
| P0-3 (ASR) | 后端 ASR 端点已就绪 | ✅ 已有 `/v1/audio/transcriptions` |
| P0-3 (TTS) | 后端 TTS 端点已就绪 | ✅ 已有 `/v1/audio/speech` |

### 5.3 不纳入 Sprint 1 的内容

以下功能在 CEO 报告中列为 P1/P2，**不在本 Sprint 范围内**：

- 离线模式（P1）
- Flashcard 闪卡（P2）
- 学习报告导出（P2）
- 家长监控面板（P3）
- 手写 OCR 识别（P4）
- 后端 TTS 替换本地 TTS（P1，仅添加开关选项，完整替换留到 Sprint 2）

---

## 6. 附录：参考数据

### 6.1 现状摘要（来自 CEO 审计）

| 维度 | 当前值 | Sprint 1 目标值 |
|:-----|:------:|:---------------:|
| 功能闭环率 | 55% | 80% |
| 支付/商业模式就绪度 | 30% | 90% |
| 总体生产就绪度 | 60% | 80% |

### 6.2 后端现有路由（Sprint 1 基线）

| # | 方法 | 路径 | 状态 |
|:-:|:----:|:-----|:----:|
| 1 | POST | `/api/v1/auth/register` | ✅ |
| 2 | POST | `/api/v1/auth/login` | ✅ |
| 3 | POST | `/api/v1/auth/refresh` | ✅ |
| 4 | GET | `/api/v1/user/profile` | ✅ |
| 5 | PATCH | `/api/v1/user/profile` | ✅ |
| 6 | POST | `/api/v1/chat/ask` | ✅ |
| 7 | GET | `/api/v1/chat/history` | ✅ |
| 8 | POST | `/api/v1/chat/completions` (SSE) | ✅ |
| 9 | POST | `/api/v1/solve/photo` | ✅ |
| 10 | POST | `/api/v1/solve/step/retry` | ✅ |
| 11 | POST | `/api/v1/solve/steps` | ✅ |
| 12 | GET | `/api/v1/subscription/status` | ✅ |
| 13 | POST | `/api/v1/subscription/verify` | ✅ |
| 14 | GET | `/api/v1/game/leaderboard` | ✅ |
| 15 | POST | `/v1/audio/transcriptions` | ✅ |
| 16 | POST | `/v1/audio/speech` | ✅ |
| 17 | GET | `/api/v1/models` | ✅ |
| 18 | GET | `/health` | ✅ |

### 6.3 Sprint 1 新增路由一览

| # | 方法 | 路径 | 工作量 | 对应 P0 |
|:-:|:----:|:-----|:------:|:-------:|
| 1 | POST | `/api/v1/quiz/generate` | 2-3 人日 | P0-2 |
| 2 | POST | `/api/v1/quiz/submit` | 1-2 人日 | P0-2 |
| 3 | GET | `/api/v1/analytics/stats` | 1-2 人日 | P0-2 |
| 4 | POST | `/api/v1/game/sync/score` | 1 人日 | P0-2 |
| 5 | POST | `/api/v1/subscription/consume` | 1 人日 | P0-2 |
| 6 | GET | `/api/v1/audio/config` | 0.5 人日 | P0-3 |
| — | — | ASR 路径修正 (Android) | 0.5 人日 | P0-3 |
| — | — | TTS 开关 (Android + 后端) | 0.5 人日 | P0-3 |

### 6.4 参考文档

- [CEO 功能差距分析报告](CEO_FEATURE_GAP_ANALYSIS.md)
- [CEO 功能差距数据报告](CEO_FEATURE_GAP_DATA.md)
- [主 PRD 文档](PRD.md)
- [架构文档](ARCH.md)
- [API 文档](API.md)
- [需求拆分表](PRD_SPLIT.md)

---

> **文档结束** — 本 PRD 由 PM 基于 CEO 审计报告和现有代码库分析生成。
> 评审后由 Coder 拆分为具体开发任务。
