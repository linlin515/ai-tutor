# AI 学伴 v4.0 Sprint 1 — 架构设计文档

> **文档版本**: v1.0  
> **日期**: 2026-05-26  
> **负责人**: CFO（首席技术架构师）  
> **状态**: 待评审  
> **基准文档**: 
>   - PRD_v4.0_Sprint1.md (PM 产品需求文档)
>   - CEO_FEATURE_GAP_ANALYSIS.md (CEO 功能差距分析)
>   - ARCH.md (现有架构文档)
>   - API.md (API 接口设计)
> **Sprint 目标**: 补齐 P0 核心断裂点，生产就绪度 55% → 80%+

---

## 目录

1. [Sprint 1 架构总览](#1-sprint-1-架构总览)
2. [P0-1: Google Play Billing 架构设计](#2-p0-1-google-play-billing-架构设计)
3. [P0-2: 后端 5 个路由架构设计](#3-p0-2-后端-5-个路由架构设计)
4. [P0-3: ASR 路径对齐架构设计](#4-p0-3-asr-路径对齐架构设计)
5. [数据流全景图](#5-数据流全景图)
6. [安全设计](#6-安全设计)
7. [兼容性矩阵](#7-兼容性矩阵)
8. [附录：模块接口图](#8-附录模块接口图)

---

## 1. Sprint 1 架构总览

### 1.1 核心架构原则

1. **渐进式演进**: 基于现有 Clean Architecture + MVVM 模式扩展，不重构存量代码
2. **后端优先**: 所有 5 个缺失路由在后端实现，Android 端仅做路径对齐和 Billing 集成
3. **服务端权威**: 积分、配额、订阅状态的权威数据源始终在后端
4. **安全兜底**: 支付验证不可跳过后端校验，本地仅做缓存

### 1.2 模块依赖关系

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Android App                                 │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐  │
│  │ SubscriptionScreen│    │  QuizScreen      │    │  VoiceInputBar │  │
│  │ (现有UI需改造)    │    │  (现有UI已对接)  │    │  (现有UI)      │  │
│  └────────┬─────────┘    └────────┬─────────┘    └───────┬────────┘  │
│           │                       │                       │          │
│  ┌────────▼─────────┐    ┌────────▼─────────┐    ┌───────▼────────┐  │
│  │ SubscriptionVM   │    │  QuizRepository  │    │ VoiceRepository │  │
│  │ ★改造: 集成Billing│   │  (已有接口实现)  │    │ ★改造: TTS开关  │  │
│  └────────┬─────────┘    └────────┬─────────┘    └───────┬────────┘  │
│           │                       │                       │          │
│  ┌────────▼─────────┐    ┌────────▼─────────┐    ┌───────▼────────┐  │
│  │ BillingManager   │    │  QuizApi         │    │ CloudAsrEngine │  │
│  │ ★新建: 生命周期   │    │  (已有定义)      │    │ ★改造: 路径修正│  │
│  └────────┬─────────┘    └────────┬─────────┘    └───────┬────────┘  │
└───────────┼───────────────────────┼───────────────────────┼──────────┘
            │                       │                       │
            │ POST /api/v1/         │ POST /api/v1/         │ POST /v1/audio/
            │ subscription/verify   │ quiz/generate         │ transcriptions
            │ subscription/consume  │ quiz/submit           │ (路径修正后)
            ▼                       ▼                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Backend (FastAPI)                               │
│                                                                       │
│  routers/subscription.py    routers/quiz.py ★新建     routers/audio.py│
│  ★扩展: 添加consume        └── POST /generate       ├── /transcriptions│
│  ├── /status (现有)             POST /submit         ├── /speech       │
│  ├── /verify (现有，需加固)                          └── ★GET /config  │
│  └── ★/consume (新增)                                                 │
│                                                                       │
│  routers/analytics.py ★新建     routers/game.py                       │
│  └── GET /stats               ★扩展: 添加 sync/score                 │
│                                                                       │
│  ┌────────────────────────────────────────────────┐                    │
│  │  数据库层 (SQLAlchemy)                          │                   │
│  │  ├── users (现有)                                │                  │
│  │  ├── subscriptions (现有)                        │                  │
│  │  ├── daily_quotas (现有, 需扩展feature维度)      │                  │
│  │  ├── user_scores (现有)                          │                  │
│  │  ├── question_records (现有)                     │                  │
│  │  ├── ★quiz_records (新建)                        │                 │
│  │  ├── ★quiz_questions (新建)                      │                 │
│  │  ├── ★wrong_answers (新建)                       │                 │
│  │  ├── ★achievement_records (新建)                 │                 │
│  │  └── ★subscription_records (新建)                │                 │
│  └────────────────────────────────────────────────┘                    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. P0-1: Google Play Billing 架构设计

### 2.1 BillingManager 类设计

```
┌──────────────────────────────────────────────────────────────────┐
│  BillingManager @Singleton                                       │
│  ├── client: BillingClient (生命周期绑定 ApplicationContext)    │
│  ├── connectionState: StateFlow<BillingConnectionState>          │
│  │   ├── DISCONNECTED                                          │
│  │   ├── CONNECTING                                            │
│  │   ├── CONNECTED                                             │
│  │   └── CLOSED                                                │
│  ├── purchaseState: StateFlow<PurchaseState>                     │
│  │   ├── IDLE                                                  │
│  │   ├── QUERYING_PRODUCTS                                     │
│  │   ├── PRODUCTS_READY                                        │
│  │   ├── PURCHASING                                            │
│  │   ├── PURCHASED                                             │
│  │   ├── VERIFYING (调后端 verify)                              │
│  │   ├── VERIFIED                                              │
│  │   ├── FAILED                                                │
│  │   └── RESTORING (恢复购买)                                   │
│  │                                                               │
│  ├── suspend fun connect()                                      │
│  │   └── startConnection → CONNECTING → CONNECTED / FAILED     │
│  ├── suspend fun queryProducts(): List<BillingProduct>          │
│  │   └── queryProductDetailsAsync(ProductType.SUBS)            │
│  ├── suspend fun purchase(activity, productId): PurchaseResult  │
│  │   └── launchBillingFlow → PurchasesUpdatedListener 回调     │
│  ├── suspend fun acknowledgePurchase(purchaseToken)             │
│  │   └── acknowledgePurchase() → 重试3次(指数退避)             │
│  ├── suspend fun restorePurchases(): List<PurchaseResult>       │
│  │   └── queryPurchasesAsync(ProductType.SUBS)                 │
│  └── fun disconnect()                                           │
│       └── endConnection() → DISCONNECTED → CLOSED               │
└──────────────────────────────────────────────────────────────────┘
```

#### 2.1.1 生命周期管理

| 生命周期事件 | BillingManager 行为 |
|:------------|:-------------------|
| Application.onCreate() | 初始化 BillingClient，不自动连接 |
| SubscriptionScreen 进入 | 调用 connect() → CONNECTING → queryProducts() |
| SubscriptionScreen 退出 | 调用 disconnect() → DISCONNECTED (不释放实例) |
| onBillingServiceDisconnected() | 自动重连（指数退避 1s/2s/4s，最多 3 次） |
| App 进入后台 | 保持连接（BillingClient 会维护心跳） |
| App 被系统回收 | 下次进入 SubscriptionScreen 时重新连接 |

#### 2.1.2 连接状态机

```
                startConnection()
DISCONNECTED ──────────────────► CONNECTING
    ▲                               │
    │                               │ onBillingSetupFinished
    │                               ├──成功──► CONNECTED
    │                               │
    │                               ├──失败──► DISCONNECTED (显示错误)
    │                               │
    │                               │ onBillingServiceDisconnected
    │                               ├──重连──► CONNECTING (指数退避)
    │                               │
    │               disconnect()    │
    └───────────────────────────────┘
                                        endConnection()
    CONNECTED ───────────────────────────────────► CLOSED
```

### 2.2 购买流程状态转换图

```
用户点击"立即订阅"按钮
    │
    ▼
┌─────────┐
│ IDLE    │ ──► queryProducts() ──► PRODUCTS_READY
└─────────┘                              │
                                         │ 用户点击购买按钮
                                         ▼
                                    ┌──────────┐
                                    │PURCHASING│ ──► launchBillingFlow()
                                    └─────┬────┘
                                          │
                           ┌──────────────┼──────────────┐
                           ▼              ▼              ▼
                     ┌─────────┐   ┌──────────┐   ┌─────────┐
                     │ 用户取消 │   │ 支付成功  │   │ 支付错误│
                     │ → IDLE  │   │ (PURCHASED)│  │ → FAILED│
                     └─────────┘   └─────┬────┘   └─────────┘
                                         │
                                         ▼
                                   ┌──────────┐
                                   │acknowledge│ ──► acknowledgePurchase()
                                   │ ...重试3次 │         │
                                   └─────┬────┘          │
                                         │               │
                                         ├──成功──────────┘
                                         │
                                         ▼
                                   ┌──────────┐
                                   │VERIFYING │ ──► POST /api/v1/subscription/verify
                                   └─────┬────┘
                                         │
                           ┌──────────────┼──────────────┐
                           ▼              ▼              ▼
                     ┌─────────┐   ┌──────────┐   ┌─────────┐
                     │ verify  │   │ verify   │   │ 网络错误│
                     │ 成功    │   │ 失败     │   │ → FAILED│
                     │→VERIFIED│   │→FAILED   │   │ 可重试  │
                     └────┬────┘   └──────────┘   └─────────┘
                          │
                          ▼
                    ┌───────────┐
                    │ UI 刷新:   │
                    │ 订阅成功!  │
                    │ → IDLE    │
                    └───────────┘

恢复购买流程:
    ┌─────────┐
    │RESTORING│ ──► queryPurchasesAsync() ──► 找到有效订阅?
    └─────────┘              │
                     ┌───────┴───────┐
                     ▼               ▼
               ┌─────────┐     ┌──────────┐
               │ 有订阅   │     │ 无订阅   │
               │ → verify│     │→ "未找到" │
               └─────────┘     └──────────┘
```

### 2.3 数据表设计

#### 2.3.1 subscription_records 表（新建 — 后端）

用于记录每次订阅购买的完整审计日志，补充现有 `subscriptions` 表。

```sql
CREATE TABLE subscription_records (
    id              VARCHAR(36) PRIMARY KEY,          -- UUID
    user_id         VARCHAR(36) NOT NULL,              -- 用户 ID, FK->users.id
    product_id      VARCHAR(50) NOT NULL,              -- Google Play 商品 ID (premium_monthly / premium_yearly)
    purchase_token  VARCHAR(500) NOT NULL,             -- Google Play purchase_token
    purchase_time   DATETIME NOT NULL,                 -- Google Play 记录的购买时间
    order_id        VARCHAR(100),                      -- Google Play order ID
    plan_type       VARCHAR(20) NOT NULL,              -- premium
    start_date      DATETIME NOT NULL,                 -- 订阅开始时间
    end_date        DATETIME,                          -- 订阅结束时间
    auto_renewing   BOOLEAN DEFAULT FALSE,             -- 是否自动续期
    verified        BOOLEAN DEFAULT FALSE,             -- 是否已通过后端验证
    verify_source   VARCHAR(20) DEFAULT 'sprint1',     -- 验证方式: sprint1(本地缓存检重) / google_api(调用Google API)
    consumption     INT DEFAULT 0,                     -- 该订阅周期内已消耗次数
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_purchase_token (purchase_token),    -- 防止同一token重复使用
    INDEX idx_user_id (user_id)
);
```

#### 2.3.2 现有 subscriptions 表扩展

```sql
-- 现有字段: id, user_id, plan_type, start_date, end_date, is_active, created_at
-- Sprint 1 扩展字段:
ALTER TABLE subscriptions ADD COLUMN purchase_token VARCHAR(500) AFTER is_active;
ALTER TABLE subscriptions ADD COLUMN auto_renewing BOOLEAN DEFAULT FALSE AFTER purchase_token;
ALTER TABLE subscriptions ADD COLUMN last_synced_at DATETIME AFTER auto_renewing;
```

#### 2.3.3 现有 daily_quotas 表扩展

```sql
-- 现有字段: id, user_id, date, questions_used, questions_limit, created_at
-- Sprint 1 扩展: 增加 feature 维度细化
ALTER TABLE daily_quotas ADD COLUMN feature VARCHAR(20) DEFAULT 'general'
    COMMENT '功能维度: general / text_chat / photo_solve / quiz_generate / voice_asr / tts';
-- 对应的 unique constraint 需要调整为 (user_id, date, feature)
-- 或者维持原有每日总配额不变，新增 feature 日志表
```

**设计决策**: 维持 `daily_quotas` 按用户+日期+feature 的粒度，兼容现有按用户+日期的总限额逻辑。对于 consume API，按 feature 维度扣减。

### 2.4 调后端 verify/consume API 的数据流

#### 2.4.1 Verify 数据流

```
┌─────────────────────────────────────────────────────────────────────────┐
│  步骤 1-3: Google Play 侧购买流程                                       │
│                                                                          │
│  Android                                  Google Play                    │
│  BillingManager                              │                          │
│    .purchase(activity, productId) ──────────► launchBillingFlow()       │
│                                              │                          │
│    ◄── PurchasesUpdatedListener ──────────── 用户完成支付               │
│         (purchaseToken, productId)                                      │
│                                                                          │
│  步骤 4: Android 调后端 verify                                            │
│                                                                          │
│  SubscriptionRepositoryImpl                 Backend                      │
│    .verifyPurchase(token, productId) ──────► POST /api/v1/subscription/verify│
│                                              │ Body:                     │
│                                              │ {                        │
│                                              │   "purchase_token": "...",│
│                                              │   "product_id": "..."    │
│                                              │ }                        │
│                                              │                          │
│                                              │ 1. 检查 purchase_token   │
│                                              │    是否在 records 中已存在│
│                                              │ 2. 如果已存在 → 409 拒绝 │
│                                              │ 3. 如果不存在:           │
│                                              │    a. 记录到             │
│                                              │       subscription_records│
│                                              │    b. 更新 subscriptions  │
│                                              │       表激活 premium     │
│                                              │    c. 返回 200 + 订阅状态 │
│                                              │                          │
│  ◄──── 200 OK ◄─────────────────────────────  {                       │
│         SubscriptionStatus                    "plan_type": "premium",  │
│         is_active=true                        "is_active": true       │
│       }                                      }                        │
│                                                                          │
│  步骤 5: Android 更新本地缓存 (Room / DataStore)                        │
│  ┌────────────────────────────┐                                         │
│  │ SubscriptionCacheEntity    │ ← 更新 isPremium, dailyQuota            │
│  └────────────────────────────┘                                         │
│                                                                          │
│  步骤 6: UI 刷新                                                        │
│  SubscriptionScreen / Dashboard                                        │
│  └── 显示"订阅成功！" toast                                              │
│  └── 订阅状态切换为 premium                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 2.4.2 Consume 数据流

```
┌─────────────────────────────────────────────────────────────────────────┐
│  触发条件: 用户发起 AI 对话 / 拍照解题 / 出题                           │
│                                                                          │
│  UseCase / Repository                      Backend                       │
│    .execute(...)                            │                           │
│       │                                    │                           │
│       ├── 先调用 consumeQuota(feature) ────► POST /api/v1/subscription/consume│
│       │                                    │ Body: {"feature": "text_chat"} │
│       │                                    │                           │
│       │                                    │ 1. 查询用户 plan_type     │
│       │                                    │ 2. 如果是 premium:        │
│       │                                    │    → 记录调用次数供分析    │
│       │                                    │    → quota_remaining = -1  │
│       │                                    │ 3. 如果是 free:           │
│       │                                    │    → 查 daily_quotas 表    │
│       │                                    │    → 比较 questions_used  │
│       │                                    │       vs questions_limit   │
│       │                                    │    → 如果可用:             │
│       │                                    │       UPDATE ... SET      │
│       │                                    │       questions_used+=1    │
│       │                                    │    → 如果不可用:           │
│       │                                    │      返回 403             │
│       │                                    │                           │
│       │  ◄── 200 / 403 ◄──────────────────  {                        │
│       │      ConsumeQuotaResponse            "consumed": true,         │
│       │                                     "quota_remaining": 4       │
│       │                                   }                           │
│       │                                    │                           │
│       ├── 如果 consumed=true → 继续执行业务请求                         │
│       └── 如果 consumed=false → 显示"今日配额已用完"                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.5 安全设计：为什么后端验证不可跳过

| 攻击场景 | 仅客户端验证 | 后端验证 | 说明 |
|:---------|:-----------|:---------|:-----|
| 伪造 purchase_token | ❌ 攻击者可构造假 token 本地激活 premium | ✅ 后端查 DB 检重 + 可调 Google API 验证真伪 | purchase_token 由 Google Play 签名，后端可向 Google API 校验 |
| 重放攻击（同 token 重复使用） | ❌ 无状态记录，反复使用 | ✅ subscription_records 表 unique key 拒绝重复 | 唯一索引 uk_purchase_token 杜绝重放 |
| 篡改本地配额数据 | ❌ 攻击者可修改 Room 数据库 | ✅ 后端为配额权威源 | 配额始终以服务端 daily_quotas 为准 |
| 绕过支付直接激活 | ❌ 修改 SharedPreferences 即可 | ✅ 无有效 purchase_token 无法激活 | 必须经过 Google Play 真实购买 → acknowledge → verify |

**结论**: 即使 Sprint 1 的 verify 实现是简化版（不做真实 Google Play API 校验），**后端验证环节仍然不可跳过**。因为：
1. purchase_token 本地缓存的唯一性检查（`uk_purchase_token`）已经能阻断重放攻击
2. 配额消耗由后端原子更新，杜绝本地数据篡改
3. Sprint 2 升级到调用 Google Play Developer API 后，安全性进一步提升

### 2.6 与现有 SubscriptionViewModel / SubscriptionApi 的对接方式

#### 2.6.1 接口变更

```kotlin
// SubscriptionApi.kt — 新增 verifyPurchase 方法
interface SubscriptionApi {
    @GET("api/v1/subscription/status")
    suspend fun getStatus(): Response<ApiResponse<SubscriptionStatusDto>>

    @POST("api/v1/subscription/consume")
    suspend fun consumeQuota(@Body request: ConsumeQuotaRequest): Response<ApiResponse<ConsumeQuotaResponse>>

    // ★ Sprint 1 新增:
    @POST("api/v1/subscription/verify")
    suspend fun verifyPurchase(@Body request: VerifyPurchaseRequest): Response<ApiResponse<SubscriptionStatusDto>>
}
```

```kotlin
// ★ Sprint 1 新增 DTO:
data class VerifyPurchaseRequest(
    val purchase_token: String,
    val product_id: String
)

data class VerifyPurchaseResponse(
    val plan_type: String,
    val is_active: Boolean,
    val start_date: String?,
    val end_date: String?,
    val days_remaining: Int?
)
```

#### 2.6.2 ViewModel 对接

```
SubscriptionViewModel (现有) ─── 改造后:
├── init:
│   ├── getSubscriptionStatus()  ──► GET /api/v1/subscription/status
│   └── BillingManager.connect() ──► 连接 BillingClient
│
├── 用户点击"立即订阅":
│   └── purchase(productId) ──► BillingManager.purchase(activity, productId)
│       └── 回调 onPurchasesUpdated:
│           ├── acknowledgePurchase(purchaseToken) ──► 3次重试
│           └── verifyPurchase(purchaseToken, productId) ──► POST /api/v1/subscription/verify
│               └── 成功 → 更新本地缓存 → UI 刷新
│               └── 失败 → 显示错误
│
├── 用户点击"恢复购买":
│   └── restorePurchases() ──► BillingManager.restorePurchases()
│       └── 有有效订阅 → verifyPurchase(...) → 恢复 premium
│       └── 无有效订阅 → 提示"未找到可恢复的订阅"
│
├── consumeQuota(feature):
│   └── POST /api/v1/subscription/consume ──► 更新本地缓存
│
└── subscriptionStatus: StateFlow<SubscriptionState>
    ├── 来源: GET /api/v1/subscription/status 轮询/定时同步
    └── 本地兜底: Room SubscriptionCacheEntity
```

#### 2.6.3 DI 注册

```kotlin
// BillingModule.kt (新建 — Hilt Module)
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context
    ): BillingManager = BillingManager(context)
}
```

```kotlin
// RepositoryModule.kt — 修改 SubscriptionRepositoryImpl 注入 BillingManager
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository
}
```

---

## 3. P0-2: 后端 5 个路由架构设计

### 3.1 路由总览与注册位置

| # | 方法 | 路径 | 路由文件 | 说明 |
|:-:|:----:|:------|:---------|:-----|
| 1 | POST | `/api/v1/quiz/generate` | `routers/quiz.py` ★新建 | AI 生成练习题 |
| 2 | POST | `/api/v1/quiz/submit` | `routers/quiz.py` ★新建 | 提交答案 + AI 批改 |
| 3 | GET | `/api/v1/analytics/stats` | `routers/analytics.py` ★新建 | 学习统计数据 |
| 4 | POST | `/api/v1/game/sync/score` | `routers/game.py` ★扩展 | 同步游戏积分 |
| 5 | POST | `/api/v1/subscription/consume` | `routers/subscription.py` ★扩展 | 消耗配额 |

在 `app/main.py` 中注册：

```python
# 新建路由
from app.routers import quiz, analytics

# 扩展现有路由
from app.routers import game, subscription

app.include_router(quiz.router)         # 新增
app.include_router(analytics.router)    # 新增
# game.router 和 subscription.router 已注册，直接扩展
```

---

### 3.2 路由 1: POST /api/v1/quiz/generate

#### 3.2.1 数据库表设计

```sql
-- ★ quiz_records 表（新建）
CREATE TABLE quiz_records (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL,
    subject         VARCHAR(20) NOT NULL COMMENT '学科',
    topic           VARCHAR(100) COMMENT '知识点',
    difficulty      VARCHAR(10) DEFAULT 'medium',
    total_questions INT DEFAULT 5,
    score           INT COMMENT '测验得分(若已批改)',
    total_points    INT COMMENT '总分',
    status          VARCHAR(10) DEFAULT 'pending' COMMENT 'pending / completed',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- ★ quiz_questions 表（新建）
CREATE TABLE quiz_questions (
    id              VARCHAR(36) PRIMARY KEY,
    quiz_id         VARCHAR(36) NOT NULL COMMENT '关联测验ID',
    question_index  INT NOT NULL COMMENT '题号(1-based)',
    question_type   VARCHAR(20) NOT NULL COMMENT 'multiple_choice / fill_blank / true_false / essay',
    content         TEXT NOT NULL COMMENT '题目内容',
    options         JSON COMMENT '选项(选择题用)',
    correct_answer  VARCHAR(500) NOT NULL COMMENT '正确答案',
    explanation     TEXT COMMENT '解析',
    difficulty      VARCHAR(10) DEFAULT 'medium',
    points          INT DEFAULT 10,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_quiz_id (quiz_id),
    FOREIGN KEY (quiz_id) REFERENCES quiz_records(id)
);
```

#### 3.2.2 请求/响应 BaseModel

```python
# backend/app/schemas/quiz.py (新建)
from pydantic import BaseModel, Field
from typing import Optional

class QuizGenerateRequest(BaseModel):
    subject: str = Field(..., description="学科: math/physics/chemistry/biology/chinese/english")
    topic: Optional[str] = Field(default="通用", description="知识点")
    difficulty: str = Field(default="medium", description="难度: easy/medium/hard")
    count: int = Field(default=5, ge=1, le=10, description="题目数量 1-10")
    question_types: list[str] = Field(default=["multiple_choice"], description="题型列表")
    grade: Optional[str] = Field(default=None, description="年级, 默认从用户profile读取")

class QuizQuestionOut(BaseModel):
    id: int
    type: str
    content: str
    options: list[dict] = Field(default_factory=list)
    correct_answer: str
    explanation: str
    difficulty: str
    points: int

class QuizGenerateData(BaseModel):
    quiz_id: str
    questions: list[QuizQuestionOut]
    total_questions: int
    subject: str
    topic: str
    generated_at: str

class QuizSubmitRequest(BaseModel):
    quiz_id: str
    answers: list[dict]  # [{question_id, selected_answer, time_spent_seconds}]

class QuestionResult(BaseModel):
    question_id: int
    is_correct: bool
    correct_answer: str
    user_answer: str
    explanation: str
    mastery_score: float

class QuizSummary(BaseModel):
    total: int
    correct: int
    accuracy: float
    mastery_level: str
    suggestions: list[str]

class QuizSubmitData(BaseModel):
    quiz_id: str
    score: int
    total_points: int
    earned_points: int
    results: list[QuestionResult]
    summary: QuizSummary
    wrong_answer_ids: list[str]
```

#### 3.2.3 业务逻辑

```
quiz/generate 处理流程:
1. 参数校验 (subject合法性, count范围) → 400 不合法
2. 配额检查 (免费用户每日3次出题) → 403 配额不足
3. 频率限制 (30秒内不可重复出题) → 429 过频
4. 调用 AI (超时30秒) → 500 超时
5. 构造 Question 列表 (含 options, correct_answer, explanation)
6. 持久化 quiz_records + quiz_questions
7. 返回 QuizGenerateResponse

AI 调用: 通过 new-api 代理调用 OpenAI GPT-4o-mini
Prompt: 使用 system prompt 指定出题格式、学科、年级、难度
Response format: JSON structured output (强制固定字段)
```

---

### 3.3 路由 2: POST /api/v1/quiz/submit

#### 3.3.1 数据库表设计

```sql
-- ★ wrong_answers 表（新建）
CREATE TABLE wrong_answers (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL COMMENT '用户ID',
    quiz_id         VARCHAR(36) COMMENT '来源测验ID',
    subject         VARCHAR(20) NOT NULL COMMENT '学科',
    topic           VARCHAR(100) COMMENT '知识点',
    question_content TEXT NOT NULL COMMENT '题目原文',
    correct_answer  VARCHAR(500) NOT NULL COMMENT '正确答案',
    user_answer     VARCHAR(500) NOT NULL COMMENT '用户填写的答案',
    explanation     TEXT COMMENT '解析/错因',
    mastery_score   FLOAT DEFAULT 0 COMMENT '掌握度 0-1',
    weakness        VARCHAR(200) COMMENT '薄弱环节标签',
    review_count    INT DEFAULT 0 COMMENT '复习次数',
    last_reviewed_at DATETIME COMMENT '上次复习时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_subject (subject)
);
```

#### 3.3.2 业务规则

```
quiz/submit 处理流程:
1. 校验 quiz_id 是否存在且属于当前用户 → 404 不存在
2. 校验 answers 长度与题目数一致
3. 逐题判分:
   - 选择题: 比较 selected_answer == correct_answer
   - 填空题: 字符串归一化后比较（去除空格、大小写等）
   - 判断题: 直接比较
4. 计算得分:
   - 每题 points = 100 / total_questions
   - score = sum(correct ? points : 0)
5. 计算 mastery_score:
   - correct → 0.8~1.0 (根据用时, 用时短=掌握好)
   - incorrect → 0.0~0.5 (根据是否接近正确答案)
6. mastery_score < 0.6 → 记录到 wrong_answers 表
7. 更新 quiz_records.score
8. 更新用户的 daily_quota (出题消耗配额)
9. 返回完整批改结果
```

---

### 3.4 路由 3: GET /api/v1/analytics/stats

#### 3.4.1 数据来源（聚合现有表）

| 数据项 | 来源表 | 聚合方式 |
|:-------|:-------|:---------|
| study_minutes_today | question_records, quiz_records | 按当天记录条数估算（每记录≈3分钟） |
| questions_solved_today | question_records | COUNT(*) WHERE date=today |
| accuracy_today | question_records | SUM(is_correct)/COUNT(*) |
| streak_days | question_records | 从今天往前连续有记录的日期数 |
| total_knowledge_points | quiz_questions | DISTINCT topic 计数 |
| mastered_points | wrong_answers | mastery_score >= 0.6 的 topic 数 |
| daily_stats | question_records | 按日期分组聚合 |
| subject_breakdown | question_records | 按学科分组聚合 |
| weak_areas | wrong_answers | 按 topic 聚合平均 mastery_score |

#### 3.4.2 响应 BaseModel

```python
class DailyStat(BaseModel):
    date: str
    study_minutes: int
    questions_solved: int
    accuracy: float

class SubjectBreakdown(BaseModel):
    subject: str
    questions_solved: int
    accuracy: float
    study_minutes: int

class WeakArea(BaseModel):
    topic: str
    mastery: float
    subject: str

class Overview(BaseModel):
    study_minutes_today: int
    questions_solved_today: int
    accuracy_today: float
    streak_days: int
    total_knowledge_points: int
    mastered_points: int
    mastery_rate: float

class AnalyticsStatsData(BaseModel):
    overview: Overview
    daily_stats: list[DailyStat]
    subject_breakdown: list[SubjectBreakdown]
    weak_areas: list[WeakArea]
```

#### 3.4.3 性能优化

```
性能要求: <200ms 响应

策略:
1. 主查询: single SQL query with GROUP BY + 子查询聚合
2. Redis 缓存: 
   key = f"analytics:stats:{user_id}:{period}:{subject}"
   ttl = 300s (5分钟)
   当有新的 question_record / quiz_record 写入时，清除该用户缓存
3. 大周期(all)数据: 限制返回最近90天
4. streak_days 算法:
   - 从今天往前遍历
   - 每天至少有一次 question_record 或 quiz_record
   - 首次断裂则停止
```

---

### 3.5 路由 4: POST /api/v1/game/sync/score

#### 3.5.1 数据库表设计

已有 `user_scores` 表可复用：
```sql
-- user_scores (现有)
-- id, user_id (UNIQUE), score, created_at, updated_at
```

新增 `achievement_records` 表（可选，Sprint 1 最小化实现）：

```sql
-- ★ achievement_records 表（新建 — Sprint 1 可选）
CREATE TABLE achievement_records (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL,
    achievement_id  VARCHAR(50) NOT NULL COMMENT '成就ID',
    unlocked_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    INDEX idx_user_id (user_id)
);
```

#### 3.5.2 请求/响应 BaseModel

```python
class ScoreData(BaseModel):
    total_points: int
    daily_points: int
    streak_days: Optional[int] = 0
    achievements_unlocked: list[str] = Field(default_factory=list)

class SyncScoreRequest(BaseModel):
    score_data: ScoreData
    sync_timestamp: str  # ISO 8601

class SyncScoreResponse(BaseModel):
    synced: bool
    server_total_points: int
    rank: int
    total_users: int
    new_achievements: list[str] = Field(default_factory=list)
    conflict: bool = False
```

#### 3.5.3 冲突处理逻辑

```
sync/score 处理流程:

1. 获取客户端 total_points (C)
2. 查询服务端 UserScore.score (S)
3. 比较:
   ├── C > S → 更新 S = C, conflict = false
   ├── C == S → 不做更新, conflict = false
   └── C < S → 不更新, 返回 S, conflict = true
4. 处理 achievements_unlocked:
   ├── 逐个查询 achievement_records 是否已存在
   ├── 不存在 → 写入新记录, 添加到 new_achievements 列表
   └── 已存在 → 跳过
5. 计算排名:
   SELECT COUNT(*) + 1 FROM user_scores WHERE score > S
6. 频率限制:
   ├── 每分钟最多同步1次
   └── 存储 last_sync_at 到 user_scores 表或 Redis
```

#### 3.5.4 Android GamificationApi.kt 兼容性

```kotlin
// 当前 GamificationApi.kt 定义:
@POST("api/v1/game/sync/score")
suspend fun syncScore(): Response<ApiResponse<Unit>>  // 无请求体!

// ⚠️ 不兼容! 需要修改为:
@POST("api/v1/game/sync/score")
suspend fun syncScore(@Body request: SyncScoreRequest): Response<ApiResponse<SyncScoreData>>
```

**兼容性检查问题**: Android 端 `GamificationApi.syncScore()` 当前定义为无参数方法，返回 `ApiResponse<Unit>`。后端需要接收 `syncScoreRequest` 体。此接口必须同步修改——Android 端增加 `@Body request` 参数；后端返回结构体改为 `SyncScoreResponse`。

---

### 3.6 路由 5: POST /api/v1/subscription/consume

#### 3.6.1 数据库表

复用现有 `daily_quotas` 表，扩展 feature 维度：

```sql
-- daily_quotas (现有, 需扩展)
-- Sprint 1 扩展: 移除 user_id + date 唯一约束
-- 替换为 user_id + date + feature 唯一约束
ALTER TABLE daily_quotas DROP INDEX uq_daily_quota_user_date;
ALTER TABLE daily_quotas ADD UNIQUE KEY uq_daily_quota_user_date_feature (user_id, date, feature);
ALTER TABLE daily_quotas MODIFY COLUMN questions_used INT DEFAULT 0 COMMENT '已使用次数(指定feature)';
ALTER TABLE daily_quotas MODIFY COLUMN questions_limit INT DEFAULT 5 COMMENT '每日上限(指定feature)';
ALTER TABLE daily_quotas ADD COLUMN feature VARCHAR(20) DEFAULT 'general' COMMENT '功能类型' AFTER questions_limit;
```

**决策说明**: 不新建表，而是扩展现有 `daily_quotas` 表。初始化时，对于尚未有记录的 (user_id, date, feature) 组合，INSERT ON DUPLICATE KEY UPDATE 处理。

#### 3.6.2 请求/响应 BaseModel

```python
class ConsumeQuotaRequest(BaseModel):
    feature: str = Field(..., description="功能标识: text_chat / photo_solve / quiz_generate / voice_asr / tts")

class ConsumeQuotaResponse(BaseModel):
    consumed: bool
    quota_remaining: int  # -1 表示 premium 无限, 0 表示已用完
    plan_type: str
    is_premium: bool
```

#### 3.6.3 业务逻辑

```
consume 处理流程:

1. 获取当前用户 plan_type
2. 如果是 premium:
   ├── 不扣减配额
   ├── 记录调用日志（用于分析）
   └── 返回 {"consumed": true, "quota_remaining": -1}
3. 如果是 free:
   ├── 查找 daily_quotas WHERE user_id=?, date=today, feature=?
   ├── 如果不存在 → INSERT (user_id, date, feature, questions_used=0, questions_limit=5)
   ├── 比较 questions_used < questions_limit:
   │   ├── 是 → UPDATE questions_used += 1 (原子操作)
   │   │       返回 {"consumed": true, "quota_remaining": limit - used - 1}
   │   └── 否 → 返回 403 {"consumed": false, "quota_remaining": 0}
4. 并发安全:
   └── 使用 SQL 原子更新: UPDATE daily_quotas
       SET questions_used = questions_used + 1
       WHERE user_id=? AND date=? AND feature=?
       AND questions_used < questions_limit
       RETURNING questions_used, questions_limit
```

---

### 3.7 错误码规划

| code | HTTP Status | message | 适用路由 |
|:----:|:-----------:|:--------|:---------|
| 0 | 200 | 成功 | 全部 |
| 400 | 400 | 参数错误 | quiz/generate (subject, count不合法) |
| 401 | 401 | 未授权/Token过期 | 全部 |
| 403 | 403 | 配额不足 | quiz/generate (免费用户出题超限), subscription/consume (免费用户当日配额用完) |
| 404 | 404 | 资源不存在 | quiz/submit (quiz_id不存在或不属当前用户) |
| 409 | 409 | 重复请求 | subscription/verify (purchase_token已使用) |
| 429 | 429 | 请求过于频繁 | quiz/generate (30秒内重复出题), game/sync/score (1分钟内重复同步) |
| 500 | 500 | AI生成超时 | quiz/generate (AI响应超过30秒) |
| -1 | 500 | 服务器内部错误 | 全部 |

统一使用 `app/schemas/common.py` 的 `success()` / `error()` 响应函数。

---

### 3.8 与 Android 端接口声明的兼容性检查

| Android 接口 | 路径 | 参数 | 返回值 | 兼容性 |
|:-------------|:-----|:-----|:-------|:------:|
| `QuizApi.generateQuiz(QuizGenerateRequest)` | `POST api/v1/quiz/generate` | ✅ 兼容 | ✅ `ApiResponse<QuizGenerateResponse>` | ✅ |
| `QuizApi.submitQuiz(QuizSubmitRequest)` | `POST api/v1/quiz/submit` | ✅ 兼容 | ✅ `ApiResponse<QuizSubmitResponse>` | ✅ |
| `AnalyticsApi.getAnalyticsStats()` | `GET api/v1/analytics/stats` | ⚠️ 无query参数 | ✅ `ApiResponse<AnalyticsDto>` | ⚠️ Android 端未传 period/subject 参数，后端应使用默认值 |
| `GamificationApi.syncScore()` | `POST api/v1/game/sync/score` | ❌ 无请求体参数 | ❌ 返回 `ApiResponse<Unit>` | **❌ 不兼容** — 需修改 Android 端 |
| `SubscriptionApi.consumeQuota(ConsumeQuotaRequest)` | `POST api/v1/subscription/consume` | ✅ `ConsumeQuotaRequest` | ✅ `ApiResponse<ConsumeQuotaResponse>` | ✅ |

**需要修改的 Android 端**:
1. `GamificationApi.syncScore()` — 增加 `@Body request: SyncScoreRequest` 参数，修改返回值类型为 `ApiResponse<SyncScoreData>`
2. `AnalyticsApi.getAnalyticsStats()` — 增加可选 query 参数 `@Query("period") period: String = "weekly"` 和 `@Query("subject") subject: String = "all"`

---

## 4. P0-3: ASR 路径对齐架构设计

### 4.1 Android VoiceRepositoryImpl.kt 路径修改方案

#### 4.1.1 当前状态

```
CloudAsrEngine.kt 调用路径:
  aiTutorApi.cloudAsr(part)  →  POST api/v1/voice/asr  (旧路径 ❌)

后端实际路径:
  POST /v1/audio/transcriptions  (新路径 ✅)

AiTutorApi.kt 当前定义:
  @Multipart
  @POST("api/v1/voice/asr")
  suspend fun cloudAsr(@Part audio: MultipartBody.Part): Response<ApiResponse<CloudAsrResponse>>
```

#### 4.1.2 修改方案

```kotlin
// AiTutorApi.kt — 修改 ASR 路径
@Multipart
@POST("v1/audio/transcriptions")     // ★ 路径修正
suspend fun transcribeAudio(
    @Part file: MultipartBody.Part     // ★ 参数名从 audio 改为 file (与后端一致)
): Response<TranscriptionResponse>    // ★ 返回值从 ApiResponse<CloudAsrResponse> 改为直接用后端原始响应
```

```kotlin
// CloudAsrEngine.kt — 对应修改
class CloudAsrEngine @Inject constructor(
    private val aiTutorApi: AiTutorApi
) {
    fun recognize(audioFile: File, config: AsrConfig = AsrConfig()): Flow<AsrResult> = flow {
        val requestBody = audioFile.readBytes().toRequestBody("audio/wav".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", audioFile.name, requestBody)  // ★ 字段名改为 "file"

        val response = withTimeout(config.cloudTimeoutMs) {
            aiTutorApi.transcribeAudio(part)  // ★ 调用新方法
        }

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                emit(AsrResult(
                    text = body.text,           // ★ 直接从 TranscriptionResponse 取 .text
                    confidence = 1.0f,
                    isFromCloud = true,
                    durationMs = config.durationThresholdMs
                ))
            }
        } else {
            throw CloudAsrException(code = response.code(), message = "云端 ASR 网络错误: ${response.code()}")
        }
    }.flowOn(Dispatchers.IO)
}
```

#### 4.1.3 VoiceRepositoryImpl 的 TTS 开关修改

```kotlin
// domain/repository/VoiceRepository.kt — 新增接口
interface VoiceRepository {
    // ... 现有方法 ...

    // ★ Sprint 1 新增:
    suspend fun getTtsConfig(): TtsConfig
    fun setTtsMode(mode: TtsMode)  // LOCAL / CLOUD
    fun getTtsMode(): TtsMode
}

enum class TtsMode { LOCAL, CLOUD }

data class TtsConfig(
    val ttsEnabled: Boolean,
    val defaultVoice: String,
    val supportedVoices: List<String>,
    val ttsQuotaRemaining: Int
)
```

```kotlin
// VoiceRepositoryImpl.kt — speak() 方法改造
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val textToSpeech: TextToSpeech,
    private val cloudTtsEngine: CloudTtsEngine,
    private val ttsAudioPlayer: TtsAudioPlayer,
    // ★ 新增依赖:
    private val settingsDataStore: SettingsDataStore
) : VoiceRepository {

    // ★ 当前 speak() 方法: 长文本(>500)→云端, 短文本→本地
    // ★ Sprint 1 改造: 增加 TTS 模式开关判断

    override fun speak(text: String, speed: Float) {
        when (getTtsMode()) {
            TtsMode.CLOUD -> {
                if (isNetworkAvailable()) {
                    scope.launch { speakCloud(text, speed) }
                } else {
                    // 网络不可用 → 自动降级为本地 TTS
                    speakLocal(text, speed)
                }
            }
            TtsMode.LOCAL -> {
                speakLocal(text, speed)
            }
        }
    }

    private fun speakLocal(text: String, speed: Float) {
        if (text.length > 500) {
            // 本地 TTS 也分段
            textToSpeech.setSpeechRate(speed)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            textToSpeech.setSpeechRate(speed)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override suspend fun getTtsConfig(): TtsConfig {
        // 调后端 GET /api/v1/audio/config
        // ... 实现 ...
    }
}
```

---

### 4.2 后端 TTS config API 设计

#### 4.2.1 新增路由

```
GET /api/v1/audio/config
```

**Response**:
```json
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

**BaseModel**:

```python
# app/schemas/audio.py — 扩展
class TtsConfigResponse(BaseModel):
    tts_enabled: bool
    default_voice: str = "alloy"
    supported_voices: list[str] = ["alloy", "echo", "fable", "onyx", "nova", "shimmer"]
    tts_quota_remaining: int = Field(default=0, description="当日剩余 TTS 次数")
```

#### 4.2.2 后端配置扩展

```python
# app/config.py — 扩展
class Settings(BaseSettings):
    # ... 现有配置 ...

    # ★ Sprint 1 新增:
    tts_enabled: bool = True                           # 是否启用云端 TTS
    tts_engine: str = "openai"                         # openai / azure / edge（预留）
    tts_default_voice: str = "alloy"                   # 默认音色
    tts_quota_per_day: int = 50                        # 免费用户每日 TTS 次数限制
```

#### 4.2.3 路由注册

```python
# 在 backend/app/routers/audio.py 中添加:
@router.get("/api/v1/audio/config")   # 注意: router prefix 为 /v1, 所以完整路径为 /v1/../api/v1/audio/config
```

⚠️ **路由前缀注意**: `audio.router` 的 prefix 是 `/v1`，因此 GET `/api/v1/audio/config` 需要单独处理 prefix。建议在 `main.py` 中直接注册此路由：

```python
# main.py
from app.routers.audio import router as audio_router
from app.schemas.audio import TtsConfigResponse

@app.get("/api/v1/audio/config", response_model=ApiResponse[TtsConfigResponse])
async def get_audio_config(current_user: User = Depends(get_current_user)):
    # ... 实现 ...
```

**更优方案**: 将 `/api/v1/audio/config` 注册到单独的 router 或直接在 `main.py` 中实现。

---

### 4.3 TTS 引擎切换架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│  VoiceRepositoryImpl                                                        │
│                                                                           │
│  speak(text, speed)                                                        │
│     │                                                                     │
│     ├── getTtsMode()                                                      │
│     │    │                                                                │
│     │    ├── LOCAL ──► speakLocal(text, speed)                           │
│     │    │              └── android.speech.tts.TextToSpeech.speak()      │
│     │    │                                                                │
│     │    └── CLOUD ──► isNetworkAvailable()                              │
│     │                    │                                                │
│     │                    ├── 有网络 ──► speakCloud(text, speed)          │
│     │                    │                └── POST /v1/audio/speech       │
│     │                    │                    (CloudTtsEngine)            │
│     │                    │                    └── TtsAudioPlayer 播放    │
│     │                    │                                                │
│     │                    └── 无网络 ──► 降级到 speakLocal(text, speed)   │
│     │                                                                     │
│     └── 配额检查: 如果是免费用户云端TTS, 先调 consume(feature="tts")     │
│                                                                           │
│  getTtsConfig()                                                           │
│     └── GET /api/v1/audio/config                                         │
│         └── 返回配置: tts_enabled, supported_voices, default_voice       │
│                                                                           │
│  TTS 引擎选择链:                                                          │
│  ┌────────┐    ┌────────┐    ┌──────────┐    ┌───────────┐              │
│  │ 用户设置 │──►│ 云端/   │──►│ 网络检查  │──►│ 云端TTS   │              │
│  │ TTS模式 │    │ 本地   │    │ OK?     │    │ (流式播放) │              │
│  └────────┘    └────────┘    ├──────────┤    └───────────┘              │
│                               │ 网络不可用│──►│ 本地TTS   │              │
│                               └──────────┘    └───────────┘              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 数据流全景图

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                              完整端到端数据流                                        │
│                                                                                     │
│  [用户操作]                                                                        │
│      │                                                                             │
│      ├── 打开订阅页面 ──► SubscriptionScreen ──► SubscriptionVM ──► BillingManager │
│      │                           │                   │                │             │
│      │                           │                   │                ├── connect() │
│      │                           │                   │                ├── queryProducts()│
│      │                           │                   │                └── purchase() │
│      │                           │                   │                     │        │
│      │                           │                   │              PurchasesUpdated  │
│      │                           │                   │                     │        │
│      │                           │                   ◄── purchaseToken ────┘        │
│      │                           │                   │                              │
│      │                           │              SubscriptionApi                     │
│      │                           │                   │                              │
│      │                           │                   ├── POST /verify               │
│      │                           │                   ├── GET  /status               │
│      │                           │                   └── POST /consume              │
│      │                           │                         │                        │
│      │                           ▼                         ▼                        │
│      │                    ┌─────────────────────────────────────┐                   │
│      │                    │         Backend Server               │                   │
│      │                    │  routers/subscription.py             │                   │
│      │                    │  ├── /verify  → subscription_records │                   │
│      │                    │  ├── /status  → subscriptions        │                   │
│      │                    │  └── /consume → daily_quotas         │                   │
│      │                    └─────────────────────────────────────┘                   │
│      │                                                                             │
│      ├── 打开出题页面 ──► QuizScreen ──► QuizRepository ──► QuizApi                │
│      │                           │                   │          │                   │
│      │                           │                   │          ├── POST /generate │
│      │                           │                   │          └── POST /submit   │
│      │                           │                   │                │             │
│      │                           ▼                   ▼                ▼             │
│      │                    ┌─────────────────────────────────────┐                   │
│      │                    │         Backend Server               │                   │
│      │                    │  routers/quiz.py                     │                   │
│      │                    │  ├── /generate → quiz_records        │                   │
│      │                    │  │             → quiz_questions      │                   │
│      │                    │  │             → AI (new-api)        │                   │
│      │                    │  └── /submit   → quiz_records(score) │                   │
│      │                    │                → wrong_answers       │                   │
│      │                    └─────────────────────────────────────┘                   │
│      │                                                                             │
│      ├── 打开仪表盘 ──► DashboardScreen ──► AnalyticsRepository ──► AnalyticsApi   │
│      │                           │                   │               │             │
│      │                           │                   │               └── GET /stats│
│      │                           ▼                   ▼                   ▼         │
│      │                    ┌─────────────────────────────────────┐                   │
│      │                    │         Backend Server               │                   │
│      │                    │  routers/analytics.py                │                   │
│      │                    │  └── /stats (聚合多表)               │                   │
│      │                    │      ├── question_records            │                   │
│      │                    │      ├── quiz_records                │                   │
│      │                    │      ├── wrong_answers               │                   │
│      │                    │      └── daily_quotas                │                   │
│      │                    └─────────────────────────────────────┘                   │
│      │                                                                             │
│      ├── 同步积分 ──► GamificationRepository ──► GamificationApi                   │
│      │                           │                   │                             │
│      │                           │                   └── POST /sync/score          │
│      │                           ▼                       ▼                         │
│      │                    ┌─────────────────────────────────────┐                   │
│      │                    │         Backend Server               │                   │
│      │                    │  routers/game.py                     │                   │
│      │                    │  └── /sync/score → user_scores       │                   │
│      │                    └─────────────────────────────────────┘                   │
│      │                                                                             │
│      ├── 语音输入 ──► VoiceInputBar ──► VoiceRepository                           │
│      │                           │                   │                             │
│      │                           │                   ├── ASR (本地/云端)           │
│      │                           │                   │    └── CloudAsrEngine        │
│      │                           │                   │         └── POST /v1/audio/ │
│      │                           │                   │             transcriptions   │
│      │                           │                   │                              │
│      │                           │                   └── TTS (本地/云端)           │
│      │                           │                        └── CloudTtsEngine        │
│      │                           │                             └── POST /v1/audio/ │
│      │                           │                                 speech           │
│      │                           ▼                                                   │
│      └── 设置TTS ──► SettingsScreen ──► SettingsVM ──► VoiceRepository             │
│                              │                   │         │                        │
│                              │                   │         └── GET /api/v1/audio/  │
│                              │                   │             config               │
│                              │                   └── 保存 TtsMode 到 DataStore     │
│                              ▼                                                      │
└────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 安全设计

### 6.1 支付安全

| 层次 | 措施 | 说明 |
|:-----|:-----|:-----|
| Google Play | BillingClient 5.x API | Google 官方 SDK，保障支付过程安全 |
| 传输层 | HTTPS + Bearer Token | 所有 API 请求强制 HTTPS |
| 后端验证层 | purchase_token 唯一性检查 | subscription_records.uk_purchase_token 阻止重放 |
| 后端验证层 | acknowledge 必须先于 verify | 确保 Google Play 侧已确认购买 |
| 配额层 | 服务端原子更新 | 使用 SQL atomic UPDATE 防止并发超卖 |
| 积分层 | 服务端权威 | 客户端积分低于服务端时，以服务端为准 |

### 6.2 API 安全

```python
# 所有路由统一依赖:
router = APIRouter(dependencies=[Depends(get_current_user)])
# 确保每个路由都需要有效的 JWT Token
```

| 路由 | 额外安全要求 |
|:-----|:------------|
| POST /quiz/generate | 配额检查 + 频率限制（30s） |
| POST /quiz/submit | 仅允许本人提交自己的 quiz |
| GET /analytics/stats | 仅返回本人的统计数据 |
| POST /game/sync/score | 频率限制（1次/分钟） |
| POST /subscription/consume | 配额原子更新 + 防并发 |
| POST /subscription/verify | purchase_token 唯一校验 + Sprint 2 接入 Google API |

### 6.3 数据完整性

- `subscription_records.purchase_token` UNIQUE 约束 → 防重放
- `daily_quotas` (user_id, date, feature) UNIQUE 约束 → 防重复插入
- `achievement_records` (user_id, achievement_id) UNIQUE 约束 → 防重复解锁
- 所有数据库操作在事务中执行 → 保证原子性

---

## 7. 兼容性矩阵

### 7.1 Android 端文件变更清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/billing/BillingManager.kt` | ★新建 | BillingClient 生命周期管理 |
| `data/billing/BillingProduct.kt` | ★新建 | 商品模型 |
| `data/remote/api/AiTutorApi.kt` | 修改 | ASR 路径修正 + 添加 verify API |
| `data/remote/api/SubscriptionApi.kt` | 修改 | 添加 verifyPurchase() 方法 |
| `data/remote/api/GamificationApi.kt` | 修改 | syncScore() 增加请求体参数 |
| `data/remote/api/AnalyticsApi.kt` | 修改 | getAnalyticsStats() 增加 query 参数 |
| `data/remote/dto/SubscriptionDtos.kt` | 修改 | 添加 VerifyPurchaseRequest |
| `data/remote/dto/AudioDtos.kt` | 修改 | 确认/调整 ASR 响应 DTO |
| `data/repository/SubscriptionRepositoryImpl.kt` | 修改 | 集成 BillingManager |
| `data/repository/VoiceRepositoryImpl.kt` | 修改 | TTS 模式切换 |
| `data/media/CloudAsrEngine.kt` | 修改 | 使用新路径 |
| `domain/repository/SubscriptionRepository.kt` | 修改 | 添加 verifyPurchase 接口 |
| `domain/repository/VoiceRepository.kt` | 修改 | 添加 TTS 模式接口 |
| `ui/subscription/SubscriptionScreen.kt` | 修改 | 绑定真实商品数据 |
| `ui/subscription/SubscriptionViewModel.kt` | 修改 | 添加购买/恢复逻辑 |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加 TTS 开关 |
| `ui/settings/SettingsViewModel.kt` | 修改 | TTS 设置状态管理 |
| `di/BillingModule.kt` | ★新建 | Hilt BillingManager |
| `di/RepositoryModule.kt` | 修改 | 绑定 BillingManager |

### 7.2 后端文件变更清单

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/quiz.py` | ★新建 | POST /generate + POST /submit |
| `app/routers/analytics.py` | ★新建 | GET /stats |
| `app/schemas/quiz.py` | ★新建 | Quiz Generate/Submit 请求响应模型 |
| `app/schemas/analytics.py` | ★新建 | Analytics 请求响应模型 |
| `app/models/quiz_record.py` | ★新建 | quiz_records 表 |
| `app/models/quiz_question.py` | ★新建 | quiz_questions 表 |
| `app/models/wrong_answer.py` | ★新建 | wrong_answers 表 |
| `app/models/subscription_record.py` | ★新建 | subscription_records 表 |
| `app/models/achievement_record.py` | ★新建 | achievement_records 表 |
| `app/routers/subscription.py` | 修改 | 添加 POST /consume; verify 加固 |
| `app/routers/game.py` | 修改 | 添加 POST /sync/score |
| `app/routers/audio.py` | 修改 | 添加 GET /api/v1/audio/config |
| `app/schemas/subscription.py` | 修改 | 添加 Consume 请求/响应 |
| `app/schemas/audio.py` | 修改 | 添加 TtsConfigResponse |
| `app/config.py` | 修改 | 添加 TTS 配置项 |
| `app/models/daily_quota.py` | 修改 | 添加 feature 字段 |
| `app/models/__init__.py` | 修改 | 导出新增模型 |

---

## 8. 附录：模块接口图

### 8.1 Android BillingManager → Backend 接口

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────────┐
│ BillingManager│         │ SubscriptionApi  │         │   Backend        │
│              │         │ (Retrofit)       │         │ subscription.py  │
│  connect()   │         │                  │         │                  │
│  queryProducts│         │ verifyPurchase()│──HTTP──►│ POST /verify    │
│  purchase()  │────────►│ (★新增)          │         │                  │
│  acknowledge │         │                  │         │ POST /consume    │
│  restore()   │         │ consumeQuota()   │──HTTP──►│ (★新增)          │
│              │         │ (已有)           │         │                  │
│              │         │                  │         │ GET /status      │
│              │         │ getStatus()      │──HTTP──►│ (已有)           │
│              │         │ (已有)           │         │                  │
└──────────────┘         └──────────────────┘         └──────────────────┘
```

### 8.2 后端 Quiz → Analytics 数据依赖

```
quiz_records ──────┐
                   ├──► analytics/stats (聚合)
quiz_questions ────┤
                   │
wrong_answers ─────┤
                   │
question_records ──┤
                   │
daily_quotats ─────┘
```

### 8.3 ASR/TTS 路径图

```
Android CloudAsrEngine       Backend audio.py
      │                           │
      │  POST /v1/audio/          │
      │  transcriptions ──────────►│───► new-api /audio/transcriptions (Whisper)
      │    (multipart)            │
      │                           │
      │  ◄── TranscriptionResponse│
      │       {"text": "..."}     │
      │                           │
Android CloudTtsEngine           │
      │                           │
      │  POST /v1/audio/speech ──►│───► new-api /audio/speech (OpenAI TTS)
      │    (JSON body)            │
      │                           │
      │  ◄── 二进制音频数据 ───────┘
      │       (audio/mpeg)
      │                           │
Android VoiceRepository           │
      │                           │
      │  GET /api/v1/audio/       │
      │  config ──────────────────►│
      │                           │
      │  ◄── TtsConfigResponse    │
      │       {tts_enabled,       │
      │        default_voice, ...}│
```

### 8.4 游戏化数据流

```
Android                         Backend
GamificationApi                  game.py

syncScore(scoreData) ──────────► POST /game/sync/score
  │                                │
  │                                ├── 查询 user_scores (服务端当前值)
  │                                ├── 比较: C > S → 更新; C < S → conflict
  │                                ├── 处理新成就 → achievement_records
  │                                └── 计算排名
  │                                │
  ◄── SyncScoreResponse ──────────┘
  {                               {
    synced: true,                    synced: true,
    server_total_points: 1250,       server_total_points: 1250,
    rank: 42,                        rank: 42,
    conflict: false                  conflict: false
  }                               }

getLeaderboard(page, size) ────► GET /game/leaderboard
  ◄── PaginatedData<LeaderboardItem>
```

---

> **文档结束** — Sprint 1 架构设计覆盖 P0-1 (Billing)、P0-2 (后端5路由)、P0-3 (ASR路径对齐) 三个核心功能模块。
> 评审通过后由 Coder 按此架构实现。
