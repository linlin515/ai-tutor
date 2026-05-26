# CEO 新增功能分析报告 — v4.0 规划

**分析日期**: 2026-05-23
**项目**: AI 学伴 (ai-tutor-android) v2.7.0
**审计人**: CEO

## 1. 当前功能全景

项目已是"五位一体"综合型 AI 学伴（拍照解题 + AI 对话 + 语音交互 + 智能出题 + 游戏化），在功能完整性上远超 Photomath / Gauthmath / Quizlet 等单一竞品。但存在 **6 个接口未对接 + 1 个支付闭环断裂** 的核心问题。

```
功能覆盖矩阵（与竞品对比）：

                 Photomath  Quizlet  Gauthmath  AI Tutor
拍照解题            ✅        ❌       ✅        ✅
AI 对话教学          ❌       ✅(Q-Chat) ❌       ✅
分步讲解            ✅        ❌       ✅        ✅
自适应讲解           ❌        ❌       ❌        ✅ ← 独家
ASR 语音输入         ❌        ❌       ❌        ✅ ← 独家
智能出题             ❌        ❌       ❌        ✅ ← 独家
游戏化激励           ❌        ✅       ❌        ✅
订阅支付闭环         ❌        ❌       ❌        🟡 ← 未完成
离线模式             🟡       ❌       ❌        ❌
Flashcard 闪卡       ❌        ✅       ❌        ❌
```

**项目当前处于**: 功能完整性 70%，但生产就绪度 55%（支付未闭环是致命短板）

---

## 2. 必须补齐的核心断裂点（P0）

### P0-1: Google Play Billing 集成 — 订阅支付闭环 ⚠️ 最高优先级

```
当前状态: 后端 verify 已实现 | Android UI 已有 | 中间支付环节断裂
影响: 无法产生收入，App 本质是"免费体验版"
工作量: 3-5 天
```

**行动项**: 集成 Google Play Billing Library → 购买触发 verify API → 消耗配额

### P0-2: 后端补齐 4 个路由

| API | 用途 | Android 端状态 | 后端状态 | 工作量 |
|:----|:-----|:--------------|:--------:|:-----:|
| `POST /api/v1/quiz/generate` | 智能出题生成 | QuizScreen 有调用 | ❌ 无路由 | 2-3 天 |
| `POST /api/v1/quiz/submit` | 提交答案评分 | QuizScreen 有调用 | ❌ 无路由 | 1-2 天 |
| `GET /api/v1/analytics/stats` | 学习统计数据 | DashboardScreen 有调用 | ❌ 无路由 | 1-2 天 |
| `POST /api/v1/game/sync/score` | 积分同步 | GamificationApi 有定义 | ❌ 无路由 | 1 天 |
| `POST /api/v1/subscription/consume` | 消耗配额 | SubscriptionApi 有定义 | ❌ 无路由 | 1 天 |

这 5 个路由 Android 端 UI/调用已就绪但后端没实现，导致功能点"假死"——用户点进去了但拿不到数据。

### P0-3: 路径不一致修正

| Android 调用 | 后端实际地址 | 问题 |
|:------------|:------------|:-----|
| `api/v1/voice/asr` | `/v1/audio/transcriptions` | ASR 无法工作 |
| `api/v1/audio/speech` (TTS) | `/v1/audio/speech` | Android 用本地 TTS 替代，建议用后端 TTS 替换 |

---

## 3. 新功能建议（P1-P2）

### P1: 离线模式（5-7 天）

**理由**: 对标 Photomath 离线缓存，国内用户常处于弱网/地铁环境。
**方案**: 
- Room 缓存已解题目（SolveRecord 已有表）
- 本地预置常见题型库（SQLite 预填充）
- 离线 ASR 降级（ML Kit on-device 已有）

### P1: 后端 TTS 替换本地 TTS

**理由**: 后端 `/v1/audio/speech` 已实现（OpenAI TTS），音质远超 Android 原生 TTS。
**方案**: VoiceRepository 加开关：在线→调后端 TTS / 离线→本地 TTS

### P2: Flashcard 闪卡模式（3-5 天）

**理由**: Quizlet 核心功能，适配国内学生背单词/公式需求。
**方案**: 基于现有 QuizResult 数据，自动生成闪卡 + 间隔复习

### P2: 学习报告导出增强

**理由**: Dashboard 已有学习统计 UI，但无 PDF/图片分享导出。
**方案**: 用 Compose 渲染为 Bitmap 分享

### P2: 学习计划 & 提醒（3-5 天）

**理由**: NotificationService 已有但 UI 不完整。设定每日学习提醒。
**方案**: 完善 Settings 中通知配置 UI

### P3: 家长监控面板

**理由**: 家长控制是 K12 教育 App 上架硬性要求（Google Play 家庭政策）。
**方案**: 使用时长限制、内容过滤、学习报告推送

---

## 4. 建议路线图

```
v4.0 Sprint 1 — 支付闭环（1 周）
├── P0-1: Google Play Billing 集成（Coder）
├── P0-2: 后端 5 个路由补齐（Coder）
└── P0-3: ASR 路径对齐（Coder）

v4.0 Sprint 2 — 用户体验提升（1 周）
├── P1: 离线模式（Coder）
├── P1: 后端 TTS 替换（Coder）
└── P1: 学习报告导出（Coder）

v4.1 — 差异化功能（1 周）
├── P2: Flashcard 闪卡（Coder）
├── P2: 学习计划 & 提醒（Coder）
└── P3: 家长监控（CFO 先出方案）
```

---

## 5. 总体评分

| 维度 | 评分 | 关键发现 |
|:----|:---:|:---------|
| 现有功能完整性 | ⭐⭐⭐⭐ 70% | 五位一体领先竞品 |
| 功能闭环率 | ⭐⭐ 55% | 6 个 API + 支付断裂 |
| 竞争力 vs 竞品 | ⭐⭐⭐⭐⭐ 强 | 综合功能无人能及 |
| 支付/商业模式 | ⭐⭐ 30% | 核心短板 |
| 总体 v4.0 就绪度 | ⭐⭐⭐ 60% | 补齐闭环后可发布 |

---

## 6. 行动指令

@PM 收到后 → 拆解为 kanban 任务 → 分配 CFO 做架构/方案 → Coder 实现 → Tester 验证

**P0 优先级判定**: 支付闭环 > 后端路由补齐 > 路径对齐
（没有支付闭环的 App 没有任何上架意义）

**注意**: 所有 P0 任务必须先创建 kanban 记录再执行。
