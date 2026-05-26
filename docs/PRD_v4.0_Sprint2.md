# AI 学伴 v4.0 Sprint 2 — 产品需求文档 (PRD)

> **文档版本**: v1.0  
> **日期**: 2026-05-26  
> **负责人**: PM  
> **状态**: 待评审  
> **基准文档**: PRD.md v1.2, PRD_v4.0_Sprint1.md  
> **Sprint 目标**: 补齐 P1/P2 核心体验断裂点，提升离线场景可用性和学习闭环完整性

---

## 目录

1. [Sprint 概述](#1-sprint-概述)
2. [P1-1: 离线模式](#2-p1-1-离线模式)
3. [P1-2: 学习报告导出](#3-p1-2-学习报告导出)
4. [P2-1: 后端 TTS 替换](#4-p2-1-后端-tts-替换)
5. [P2-2: Flashcard 抽认卡](#5-p2-2-flashcard-抽认卡)
6. [风险与依赖](#6-风险与依赖)
7. [附录：参考数据](#7-附录参考数据)

---

## 1. Sprint 概述

### 1.1 背景

AI 学伴 Android 项目（v4.0）经 Sprint 1 已补齐 P0 核心断裂点（支付闭环、5 个后端路由、ASR 路径对齐），生产就绪度从 55% 提升至 80%+。但用户调研和 QA 审计显示以下 **P1/P2 痛点** 仍需解决：

- **离线场景缺失**：学生在教室/地铁等无网络环境下完全无法使用 App，目前已缓存的对话历史和解题数据无法浏览
- **学习数据无法导出**：用户没有途径将学习统计导出为报告（PDF/CSV），无法向家长/老师展示学习成果
- **TTS 成本偏高**：当前使用 OpenAI TTS API，成本较高（$0.015/1K 字符），需要替换为免费/低成本方案
- **错题复习机制薄弱**：用户答错题目后没有系统性的复习回顾工具，错题本仅支持浏览，缺乏间隔重复机制

Sprint 2 聚焦 **2 个 P1 功能 + 2 个 P2 功能**，目标是将用户体验从"能跑"提升到"好用"，满足日常学习场景的完整闭环。

### 1.2 Sprint 范围

| 优先级 | 功能 | 类型 | 预估工作量 | 依赖 |
|:------:|:-----|:----:|:----------:|:----:|
| P1-1 | 离线模式 | Android + 后端 | 5-7 人日 | Room DB 现有架构, NetworkMonitor |
| P1-2 | 学习报告导出 | 后端 + Android | 2 人日 | analytics/stats 路由 (Sprint 1) |
| P2-1 | 后端 TTS 替换 | 后端 + Android | 2 人日 | 无 |
| P2-2 | Flashcard 抽认卡 | Android + 后端 | 3-5 人日 | wrong_answers 表, gamification 积分体系 |

**总预估工作量**: 12-16 人日  
**建议人力**: 1 Android Coder + 1 Backend Coder 并行  
**建议时长**: 2 周（含测试和联调）

### 1.3 验收全局原则

1. 所有代码需通过现有测试套件（`pytest` / Android Unit Test）
2. 新增 API 路由需在 Swagger docs 中可浏览和调试
3. 离线模式需在飞行模式下端到端验证通过（模拟真实无网络场景）
4. 数据库迁移需向后兼容，不丢失现有用户数据
5. 新功能需有对应的错误处理和用户提示文案（中英文）

---

## 2. P1-1: 离线模式

### 2.1 功能描述

用户在无网络环境下（飞行模式/地铁/教室）可以浏览已缓存的学习内容。当前 App 完全依赖网络请求获取数据，一旦断网则所有页面显示空白或加载失败。通过本地 Room DB 缓存关键数据 + 网络状态监听，实现"先缓存后浏览"的离线体验。

**核心缓存数据集**:
- 已解题目（题目原文 + 选项 + 正确答案 + 用户答案 + 解析）
- AI Tutor 对话历史（JSON 格式，含系统回复和用户提问）
- 错题本（wrong_answers 表数据，含 mastery_score 和复习记录）

**约束**: 不涉及离线出题（需要 AI API），仅浏览已缓存内容。离线状态下不产生新的学习记录，所有操作在恢复网络后提交。

### 2.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-OFF-01 | 学生（地铁通勤） | 在飞行模式下打开 App 能看到之前做过的题目 | 我可以在地铁上复习已学的题目 |
| US-OFF-02 | 学生（教室无信号） | 离线浏览 AI Tutor 的对话历史 | 我可以回顾 AI 老师的讲解内容 |
| US-OFF-03 | 学生 | 打开错题本时看到错题列表，无论是否有网络 | 我可以随时复习我的薄弱知识点 |
| US-OFF-04 | 学生 | 在 App 顶部看到一个离线状态指示器 | 我知道当前处于离线模式，避免误操作 |
| US-OFF-05 | 学生 | 在设置中查看缓存占用大小并一键清除 | 我可以管理手机存储空间 |
| US-OFF-06 | 学生 | 网络恢复后自动同步离线期间的操作 | 我不需要手动操作，学习记录自动更新 |
| US-OFF-07 | 后端运维 | 获得增量同步接口，减少不必要的数据传输 | 同步带宽和延迟都在可控范围内 |

### 2.3 功能列表

| # | 功能点 | 模块 | 优先级 | 说明 |
|:-:|:-------|:----:|:------:|:-----|
| F1 | 本地 Room DB 缓存已解题目 | Android | P1 | 新增 `SolvedQuestionEntity` 表，缓存 quiz_questions + 用户答案 + 解析 |
| F2 | 本地缓存对话历史 JSON | Android | P1 | 扩展 `MessageEntity` 支持 metadata 字段存储完整 JSON，或新增 `CachedConversationEntity` |
| F3 | 本地缓存错题本数据 | Android | P1 | 新增 `CachedWrongAnswerEntity` 表，镜像后端 wrong_answers 表结构 |
| F4 | 飞行模式浏览缓存题目列表 | Android | P1 | 题目列表页从本地 DB 读取，联网时同时拉取云端 |
| F5 | 飞行模式浏览对话历史 | Android | P1 | 会话列表 + 消息列表均支持本地读取 |
| F6 | 飞行模式浏览错题本 | Android | P1 | 错题本页面从本地 DB 读取 |
| F7 | 离线状态指示器 | Android | P1 | 顶部 Banner 或 Snackbar 提示"你当前处于离线模式" |
| F8 | 网络恢复自动同步 | Android + 后端 | P1 | 增量同步策略：上传离线操作日志（若有），拉取最新云端数据 |
| F9 | 缓存管理设置 | Android | P1 | 设置页显示缓存大小 + "清除缓存"按钮 + "刷新缓存"按钮 |
| F10 | 后端增量同步 API 端点 | 后端 | P1 | 新增 `GET/POST /api/v1/sync` 端点，返回增量变更数据 |
| F11 | 数据迁移：Room DB 版本升级 | Android | P1 | 从 v1 到 v2 的 Migration，添加新表 |

### 2.4 验收标准

#### AC-OFF-01: 本地缓存已解题目

```
Given  用户已联网完成至少 3 道解题（拍照解题或测验）
When   切换到飞行模式 / 关闭 WiFi
And    打开"我的解题"页面
Then   页面展示已缓存的 3 道题目
And   每道题目显示：题干、选项（如有）、正确答案、用户答案、解析
And   首次加载速度 < 500ms（从 Room 读取）
```

#### AC-OFF-02: 本地缓存对话历史

```
Given  用户有至少 5 轮 AI 对话历史
When   切换到飞行模式
And    进入会话列表页面
Then   显示所有本地缓存的会话（按时间排序）
When   点击其中一个会话
Then   显示该会话的所有消息（用户提问 + AI 回复）
And   消息内容完整展示，含 Markdown 格式
```

#### AC-OFF-03: 本地缓存错题本

```
Given  用户有至少 3 条错题记录（wrong_answers）
When   切换到飞行模式
And    打开错题本页面
Then   显示缓存的错题列表（按创建时间排序）
And   每条错题显示：题目、正确答案、用户答案、解析、掌握度
And   间隔复习按钮在离线状态下显示但点击提示"需要网络连接"
```

#### AC-OFF-04: 离线状态指示器

```
Given  用户当前网络正常
When   开启飞行模式
Then   页面顶部显示黄色/橙色 Banner："你当前处于离线模式，仅可查看已缓存内容"
And   Banner 可手动关闭（关闭后不再显示直至下次断网重连）
When   关闭飞行模式（恢复网络）
Then   Banner 自动消失
And   触发自动同步流程
```

#### AC-OFF-05: 缓存管理设置

```
Given  用户已缓存若干数据
When   进入设置 → 缓存管理
Then   显示"缓存大小：X.XX MB"
And   显示各分类缓存明细（题目缓存、对话缓存、错题缓存）
When   点击"清除缓存"
Then   弹出确认对话框："确定清除所有本地缓存？此操作不可恢复。"
And   确认后所有缓存表清空
And   缓存大小显示为"0.00 MB"
```

#### AC-OFF-06: 网络恢复自动同步

```
Given  用户处于离线模式并有本地缓存
When   网络恢复（从飞行模式切回）
Then   自动触发同步流程（无需用户操作）
And   调用 POST /api/v1/sync 上传离线期间产生的操作日志（若有）
And   调用 GET /api/v1/sync?since=<last_sync_timestamp> 拉取增量数据
And   同步完成后本地缓存更新
And   UI 刷新显示最新数据
And   同步过程在后台进行，不阻塞 UI
```

#### AC-OFF-07: 后端增量同步 API

```
Given  用户已登录
When   发送 GET /api/v1/sync?since=2026-05-25T10:00:00Z
Then   返回 200 OK
And   data 包含:
       - updated_questions: [...]   # 新增/更新的解题记录
       - updated_conversations: [...]  # 新增/更新的对话
       - updated_wrong_answers: [...]  # 新增/更新的错题
       - deleted_ids: {...}            # 各分类下需删除的 ID 列表
       - sync_timestamp: "2026-05-26T10:00:00Z"

Given  用户离线期间产生了操作日志
When   发送 POST /api/v1/sync
       Body: {
         "actions": [
           {"type": "wrong_answer_review", "wrong_answer_id": "uuid", "new_mastery": 0.6, "timestamp": "..."},
           {"type": "viewed_question", "question_id": "uuid", "timestamp": "..."}
         ]
       }
Then   返回 200 OK
And   data.synced_actions = 2
And   data.conflicts = []  # 无冲突
```

### 2.5 数据模型设计

#### 2.5.1 Android Room 新增实体

**CachedQuestionEntity** — 缓存已解题目
```kotlin
@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,           // 服务端 quiz_question.id
    val quizId: String? = null,           // 关联测验 ID
    val subject: String,                   // 学科
    val topic: String? = null,             // 知识点
    val questionType: String,              // multiple_choice / fill_blank / true_false / essay
    val content: String,                   // 题目原文
    val options: String? = null,           // 选项 JSON
    val correctAnswer: String,             // 正确答案
    val userAnswer: String? = null,        // 用户答案
    val explanation: String? = null,       // 解析
    val isCorrect: Boolean? = null,        // 是否正确
    val masteryScore: Float = 0.0f,        // 掌握度
    val cachedAt: Long = System.currentTimeMillis()  // 缓存时间
)
```

**CachedWrongAnswerEntity** — 缓存错题
```kotlin
@Entity(tableName = "cached_wrong_answers")
data class CachedWrongAnswerEntity(
    @PrimaryKey val id: String,           // 服务端 wrong_answer.id
    val subject: String,
    val topic: String? = null,
    val questionContent: String,
    val correctAnswer: String,
    val userAnswer: String,
    val explanation: String? = null,
    val masteryScore: Float = 0.0f,
    val weakness: String? = null,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
```

**CachedConversationEntity** — 缓存对话（可选，也可复用现有 MessageEntity 加索引）
```kotlin
@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val id: String,           // 会话 ID
    val title: String,
    val messages: String,                  // 完整对话 JSON 数组
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)
```

#### 2.5.2 后端 Sync Schema

```python
class SyncRequest(BaseModel):
    """增量同步请求"""
    actions: list[SyncAction] = Field(default_factory=list)
    
class SyncAction(BaseModel):
    """同步操作"""
    type: str  # wrong_answer_review / viewed_question / ...
    target_id: str
    data: dict = Field(default_factory=dict)
    timestamp: str

class SyncResponse(BaseModel):
    """增量同步响应"""
    updated_questions: list[dict] = Field(default_factory=list)
    updated_conversations: list[dict] = Field(default_factory=list)
    updated_wrong_answers: list[dict] = Field(default_factory=list)
    deleted_ids: dict[str, list[str]] = Field(default_factory=dict)
    synced_actions: int = 0
    conflicts: list[dict] = Field(default_factory=list)
    sync_timestamp: str
```

### 2.6 涉及修改文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/local/entity/CachedQuestionEntity.kt` | 新建 | 缓存题目实体 |
| `data/local/entity/CachedWrongAnswerEntity.kt` | 新建 | 缓存错题实体 |
| `data/local/entity/CachedConversationEntity.kt` | 新建 | 缓存对话实体 |
| `data/local/dao/CachedQuestionDao.kt` | 新建 | 题目缓存 DAO（CRUD + 查询） |
| `data/local/dao/CachedWrongAnswerDao.kt` | 新建 | 错题缓存 DAO |
| `data/local/dao/CachedConversationDao.kt` | 新建 | 对话缓存 DAO |
| `data/local/db/AiTutorDatabase.kt` | 修改 | 添加新实体，v1→v2 Migration |
| `di/DatabaseModule.kt` | 修改 | 提供新 DAO 实例 |
| `data/repository/OfflineRepositoryImpl.kt` | 新建 | 离线缓存读写 + 同步逻辑 |
| `domain/repository/OfflineRepository.kt` | 新建 | 离线仓库接口 |
| `data/repository/SyncRepositoryImpl.kt` | 新建 | 网络同步实现 |
| `domain/repository/SyncRepository.kt` | 新建 | 同步仓库接口 |
| `data/remote/api/SyncApi.kt` | 新建 | 同步 API 接口定义 |
| `data/remote/dto/SyncDtos.kt` | 新建 | 同步请求/响应 DTO |
| `util/NetworkMonitor.kt` | 修改 | 扩展离线状态事件（如添加 isOnline 快照） |
| `ui/offline/OfflineBanner.kt` | 新建 | 离线状态指示器 Composable |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加"缓存管理"区域 |
| `ui/settings/SettingsViewModel.kt` | 修改 | 缓存管理相关状态 |
| `ui/settings/CacheManagementSection.kt` | 新建 | 缓存大小展示 + 清除按钮 |
| `domain/usecase/sync/SyncDataUseCase.kt` | 新建 | 同步数据用例 |
| `domain/usecase/cache/GetCachedQuestionsUseCase.kt` | 新建 | 获取缓存题目用例 |
| `domain/usecase/cache/GetCachedWrongAnswersUseCase.kt` | 新建 | 获取缓存错题用例 |
| `domain/usecase/cache/ClearCacheUseCase.kt` | 新建 | 清除缓存用例 |
| `domain/usecase/cache/GetCacheSizeUseCase.kt` | 新建 | 获取缓存大小用例 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/sync.py` | 新建 | `/api/v1/sync` 路由（GET + POST） |
| `app/schemas/sync.py` | 新建 | Sync Schema 定义 |
| `app/services/sync_service.py` | 新建 | 同步业务逻辑：增量查询 + 冲突检测 |
| `app/main.py` | 修改 | 注册 sync router |

### 2.7 缓存策略

| 数据类型 | 缓存时机 | 过期策略 | 大小上限 |
|:---------|:---------|:---------|:--------:|
| 已解题目 | 每次解题完成后立即缓存 | 保留最近 200 题 | ~5MB |
| 对话历史 | 每次 AI 回复完成后 | 保留最近 50 个会话 | ~10MB |
| 错题本 | 登录时全量拉取 + 每次新增错题 | 服务端同步更新 | ~2MB |

### 2.8 边界条件

| 场景 | 预期行为 |
|:-----|:---------|
| 用户首次使用（无缓存数据） | 离线模式提示"暂无缓存内容，请在联网后使用" |
| 缓存数据达上限 | 自动淘汰最旧数据（LRU 策略），设置页展示缓存已满提示 |
| 同步冲突（服务端数据更新但本地有修改） | 服务端优先，本地覆盖；冲突列表记录在 sync_log |
| 大文件/大量数据同步 | 后台分页同步，显示同步进度条，支持暂停/取消 |
| 同步过程中断网 | 同步暂停，恢复网络后续传 |
| 清除缓存后立即断网 | 正常展示空状态，下次联网后重新缓存 |
| 飞行模式中收到推送通知 | 忽略推送（无网络），联网后统一处理 |

---

## 3. P1-2: 学习报告导出

### 3.1 功能描述

用户可以将自己的学习统计数据导出为可分享/打印的格式，满足向家长汇报、自我复盘、数据备份等需求。支持 PDF（可视化报告）和 CSV（原始数据）两种格式，导出范围可自定义。

**后端生成 PDF，前端下载**：避免 Android 端复杂且不可靠的 PDF 渲染库，PDF 由后端服务端生成（使用 ReportLab / WeasyPrint 等 Python 库），Android 端仅触发下载并通过 ShareSheet 分享。

### 3.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-EXP-01 | 学生 | 将本周的学习报告导出为 PDF | 我可以发给家长证明我在认真学习 |
| US-EXP-02 | 学生 | 导出 CSV 格式的做题记录 | 我可以在 Excel/Notion 中做进一步分析 |
| US-EXP-03 | 学生 | 选择"今日/本周/本月/全部"范围导出 | 我只需要我关心的时间段的数据 |
| US-EXP-04 | 学生 | 导出后直接分享到微信/邮件 | 我不需要先保存文件再手动分享 |
| US-EXP-05 | 家长 | 收到 PDF 报告后能看到正确率曲线和雷达图 | 我一眼就能看出孩子的优势和薄弱学科 |

### 3.3 功能列表

| # | 功能点 | 模块 | 优先级 | 说明 |
|:-:|:-------|:----:|:------:|:-----|
| F1 | PDF 报告生成（后端） | 后端 | P1 | 包含每日学习时长、正确率曲线、知识点雷达图、错题分布 |
| F2 | CSV 数据导出（后端） | 后端 | P1 | 做题记录原始数据，按行排列 |
| F3 | 导出范围选择 | Android | P1 | 今日/本周/本月/全部 四个选项 |
| F4 | 后端 `/api/v1/report/export` 端点 | 后端 | P1 | 支持 format=pdf|csv, period=... |
| F5 | Android 端触发下载 + ShareSheet | Android | P1 | 下载完成后调用 Android ShareSheet |
| F6 | 导出进度提示 | Android | P1 | 生成中 → 下载中 → 完成/失败 |
| F7 | 后端配额校验 | 后端 | P1 | 免费用户每日导出上限 3 次，premium 用户 20 次 |

### 3.4 API 详细设计

#### POST /api/v1/report/export

**功能**: 生成并返回学习报告文件。

**Request**:
```json
{
  "format": "pdf",
  "period": "weekly",
  "subject": "all",
  "include_charts": true,
  "start_date": "2026-05-19",
  "end_date": "2026-05-26"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| format | string | 是 | — | `pdf` 或 `csv` |
| period | string | 是 | — | `today` / `weekly` / `monthly` / `all` / `custom` |
| subject | string | 否 | `all` | 学科过滤 |
| include_charts | bool | 否 | `true` | 仅 PDF 有效，是否包含图表 |
| start_date | string | 否 | 由 period 推断 | 自定义范围的开始日期 (ISO 8601) |
| end_date | string | 否 | 由 period 推断 | 自定义范围的结束日期 (ISO 8601) |

**Response (PDF 格式)**:
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="study_report_20260519_20260526.pdf"
```

**Response (CSV 格式)**:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="study_records_20260519_20260526.csv"
```

**业务规则**:
- PDF 报告包含以下章节：
  1. 报告概要（日期范围、总学习时长、解题总数、正确率）
  2. 每日学习时长柱状图
  3. 正确率趋势曲线
  4. 知识点掌握度雷达图（按学科分类）
  5. 错题分布饼图（按学科/知识点）
  6. 薄弱知识点列表（掌握度 < 0.6）
  7. 学习建议（基于薄弱点自动生成）
- CSV 包含字段：`date, subject, topic, question_content, correct_answer, user_answer, is_correct, time_spent_seconds, mastery_score`
- PDF 生成使用 ReportLab 库，支持中文显示（需包含中文字体）
- 免费用户每日最多导出 3 次，premium 用户 20 次
- 后端生成超时限制 30 秒，超时返回 504
- 文件最大 10MB（PDF 含图表时），超出则压缩图表质量

### 3.5 验收标准

#### AC-EXP-01: PDF 报告生成

```
Given  用户有至少 7 天的学习数据
When   发送 POST /api/v1/report/export
       Body: {"format": "pdf", "period": "weekly"}
Then   返回 200 OK
And   Content-Type 为 application/pdf
And   文件内容包含以下章节页：
       - 报告概要（有日期范围、总学习时长、正确率）
       - 每日时长柱状图
       - 正确率曲线
       - 雷达图（显示当前已学习学科）
       - 错题分布
And   PDF 文件开箱即用，支持中文显示
And   文件大小 < 10MB
```

#### AC-EXP-02: CSV 数据导出

```
Given  用户有做题记录
When   发送 POST /api/v1/report/export
       Body: {"format": "csv", "period": "monthly"}
Then   返回 200 OK
And   Content-Type 为 text/csv
And   CSV 包含标题行：date, subject, topic, question_content, correct_answer, user_answer, is_correct, time_spent_seconds, mastery_score
And   数据行数 >= 当月做题记录数
And   CSV 可通过 Excel 正常打开（UTF-8 BOM 编码）
```

#### AC-EXP-03: 导出范围选择

```
Given  用户打开"学习报告"页面
When   查看导出范围选项
Then   显示四个选项：今日 / 本周 / 本月 / 全部
And   选择"今日"时，period = "today"
And   选择"本周"时，period = "weekly"
And   选择"本月"时，period = "monthly"
And   选择"全部"时，period = "all"
```

#### AC-EXP-04: Android 下载 + ShareSheet

```
Given  用户点击"导出 PDF"
When   报告生成完成并开始下载
Then   显示下载进度条
When   下载完成
Then   自动弹出系统 ShareSheet
And   ShareSheet 包含分享选项：微信、邮件、保存到文件等
And   分享失败时显示"分享失败"提示，但不影响文件保存
```

#### AC-EXP-05: 导出配额限制

```
Given  免费用户今日已导出 3 次
When   再次发送导出请求
Then   返回 429 错误码 + message "今日导出次数已用完，升级 Premium 可获得更多导出次数"
And   premium 用户每日可导出 20 次
```

### 3.6 涉及修改文件清单

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/report.py` | 新建 | `/api/v1/report/export` 路由 |
| `app/schemas/report.py` | 新建 | 导出请求/响应 Schema |
| `app/services/report_service.py` | 新建 | PDF 生成逻辑（ReportLab）+ CSV 生成 |
| `app/main.py` | 修改 | 注册 report router |
| `requirements.txt` | 修改 | 添加 `reportlab`, `matplotlib`, `svg` 等依赖 |

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/remote/api/ReportApi.kt` | 新建 | 导出 API 定义 |
| `data/remote/dto/ReportDtos.kt` | 新建 | 导出请求/响应 DTO |
| `data/repository/ReportRepositoryImpl.kt` | 新建 | 下载 + 文件保存 + ShareSheet 触发 |
| `domain/repository/ReportRepository.kt` | 新建 | 报告仓库接口 |
| `ui/report/ExportReportScreen.kt` | 新建 | 导出设置页面（范围选择 + 格式选择） |
| `ui/report/ExportReportViewModel.kt` | 新建 | 导出状态管理 |
| `di/ReportModule.kt` | 新建 | 报告相关依赖注入 |
| `ui/navigation/Routes.kt` | 修改 | 添加报告导出路由 |

### 3.7 边界条件

| 场景 | 预期行为 |
|:-----|:---------|
| 用户无任何学习数据 | 返回空报告，PDF 显示"暂无学习数据，开始学习吧！" |
| 生成过程中网络中断 | 后端任务异步处理（Celery/BackgroundTask），客户端轮询状态 |
| 生成的 PDF 文件过大 | 压缩图表质量至 72dpi，截断过长文本，限制最大 10MB |
| 选择的日期范围跨度过大（如全部） | 后端限制最多返回 365 天数据，超出部分忽略 |
| 雷达图只有 1 个学科 | 正常显示单点雷达图 |

---

## 4. P2-1: 后端 TTS 替换

### 4.1 功能描述

当前后端 TTS 引擎使用 OpenAI TTS API（`v1/audio/speech`），成本约为 $0.015/1K 字符。对于日活用户数千的学伴应用，TTS 调用费用正在快速增长。需要调研并替换为更高性价比的 TTS 方案，同时保持 Android 端 CloudTtsEngine 的兼容性和降级逻辑。

**候选方案**:
1. **Edge TTS** (免费, 高质量) — 微软 Edge 浏览器内置的 TTS API，通过非官方 API 调用，完全免费，中文音质优秀
2. **Fish Speech** (自部署) — 开源 TTS 模型，支持自部署到 GPU 服务器，完全控制权，但需要额外的运维成本

**初步推荐**: Edge TTS（零成本，集成简单），Fish Speech 作为中长期备选。

### 4.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-TTS-01 | 后端运维 | 能通过 provider 参数切换 TTS 引擎 | 我可以快速在不同 TTS 服务间切换，对比效果和成本 |
| US-TTS-02 | 产品经理 | 使用免费 TTS 替换付费 TTS | 降低运营成本，让免费用户也能享受语音朗读 |
| US-TTS-03 | Android 用户 | 云端 TTS 不可用时自动使用本地 TTS | 无论如何都能听到语音朗读 |
| US-TTS-04 | Android 开发者 | CloudTtsEngine 支持 provider 配置 | 不需要修改业务代码即可切换 TTS 源 |

### 4.3 功能列表

| # | 功能点 | 模块 | 优先级 | 说明 |
|:-:|:-------|:----:|:------:|:-----|
| F1 | 调研 Edge TTS / Fish Speech | 后端 | P2 | 输出对比报告，确认集成方案 |
| F2 | TTS 端点新增 provider 参数 | 后端 | P2 | `GET /api/v1/tts?text=&provider=edge` |
| F3 | Edge TTS 适配器实现 | 后端 | P2 | 调用 Edge TTS API，返回音频流 |
| F4 | Android CloudTtsEngine 支持切换 provider | Android | P2 | 请求时传递 provider 参数 |
| F5 | 降级逻辑：provider 不可用 → 系统 TTS | Android | P2 | 云端失败时回退到本地 TextToSpeech |
| F6 | SSE 流式响应保持兼容 | 后端 | P2 | 响应格式不变，仅后端内部替换引擎 |
| F7 | 后端配置 TTS 默认 provider | 后端 | P2 | `TTS_DEFAULT_PROVIDER=openai|edge|fish` |

### 4.4 API 详细设计

#### GET /api/v1/tts (新增端点，兼容现有 `POST /v1/audio/speech`)

**功能**: 文本转语音，支持切换 provider。

**Request**:
```
GET /api/v1/tts?text=你好世界&provider=edge&voice=zh-CN-XiaoxiaoNeural&speed=1.0
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| text | string | 是 | — | 要合成的文本，URL 编码，最大 1024 字符 |
| provider | string | 否 | 从环境变量读取 | `openai` / `edge` / `fish` |
| voice | string | 否 | 各 provider 的默认值 | 发音人标识 |
| speed | float | 否 | 1.0 | 语速 (0.5-2.0) |

**Response**: SSE 流式音频数据（与现有格式兼容）
```
Content-Type: audio/mpeg
X-TTS-Provider: edge
```

**SSE 流式兼容说明**:
- 保持音频二进制流返回格式（与现有 `POST /v1/audio/speech` 相同）
- 响应头增加 `X-TTS-Provider` 标识使用的引擎
- 长文本分段处理：每段分别调用 TTS，连续返回音频流

#### Edge TTS 集成方案

```python
# backend/app/services/tts_edge.py
import httpx
import uuid

EDGE_TTS_URL = "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

async def synthesize_edge(text: str, voice: str = "zh-CN-XiaoxiaoNeural", speed: float = 1.0) -> bytes:
    """调用 Edge TTS API 生成语音"""
    # 1. 获取 TTS 授权 Token
    # 2. 构造 SSML 请求
    # 3. 解析返回的二进制音频流
    # 4. 返回 mp3 音频数据
    ...
```

#### 后端配置

```python
# backend/app/config.py 新增
class Settings(BaseSettings):
    # ... 现有配置 ...
    
    # TTS 配置
    tts_default_provider: str = "edge"  # openai / edge / fish
    tts_edge_enabled: bool = True
    tts_edge_voice_cn: str = "zh-CN-XiaoxiaoNeural"  # 中文女声
    tts_edge_voice_en: str = "en-US-JennyNeural"      # 英文女声
    tts_fish_enabled: bool = False
    tts_fish_api_url: str = ""  # 自部署 Fish Speech API URL
```

### 4.5 验收标准

#### AC-TTS-01: TTS 端点新增 provider 参数

```
Given  后端 TTS 端点就绪
When   发送 GET /api/v1/tts?text=你好世界&provider=edge
Then   返回 200 OK
And   Content-Type 为 audio/mpeg
And   返回的音频文件为有效 MP3（可播放）
And   语音内容为"你好世界"
```

#### AC-TTS-02: provider 切换

```
Given  后端配置了 openai 和 edge 两个 TTS provider
When   发送 GET /api/v1/tts?text=测试&provider=openai
Then   返回的音频使用 OpenAI TTS 引擎
When   发送 GET /api/v1/tts?text=测试&provider=edge
Then   返回的音频使用 Edge TTS 引擎
And   两种返回的音频音色明显不同
```

#### AC-TTS-03: Android 端 provider 切换

```
Given  Android CloudTtsEngine 支持 provider 配置
When   用户进入语音设置
Then   显示 "TTS Provider" 下拉选择（OpenAI / Edge TTS / 系统默认）
When   用户选择 "Edge TTS"
Then   后续 TTS 请求携带 provider=edge 参数
When   用户选择 "系统默认（本地 TTS）"
Then   使用 android.speech.tts.TextToSpeech 本地合成
```

#### AC-TTS-04: 降级逻辑

```
Given  Android 端请求云端 TTS，provider=edge
When   Edge TTS API 返回 503（服务不可用）
Then   自动降级：调用 POST /v1/audio/speech（使用 OpenAI TTS）作为第一次降级
When   OpenAI TTS 也返回错误
Then   二次降级：使用本地 TextToSpeech 合成
And   UI 显示"语音服务降级"提示（Toast/Snackbar）
```

#### AC-TTS-05: SSE 流式兼容性

```
Given  现有客户端使用 POST /v1/audio/speech
When   发送相同的请求
Then   返回的响应格式与 Sprint 1 保持一致（二进制音频）
And   不影响现有功能的正常运行
When   新客户端使用 GET /api/v1/tts?provider=edge
Then   也返回二进制音频流，格式相同
```

### 4.6 涉及修改文件清单

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/audio.py` | 修改 | 新增 `GET /api/v1/tts` 端点，添加 provider 参数路由 |
| `app/services/tts_edge.py` | 新建 | Edge TTS 服务适配器 |
| `app/services/tts_base.py` | 新建 | TTS 抽象基类 + 工厂方法 |
| `app/config.py` | 修改 | 添加 TTS provider 配置项 |
| `app/schemas/audio.py` | 修改 | 新增 TtsQueryParams schema |
| `requirements.txt` | 修改 | 添加 httpx（已有）、websockets（Edge TTS 可能需要） |

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/remote/api/AiTutorApi.kt` | 修改 | 添加 `GET /api/v1/tts` 接口 |
| `data/media/CloudTtsEngine.kt` | 新建 | 云端 TTS 引擎（支持 provider 切换 + 降级） |
| `data/media/LocalTtsEngine.kt` | 新建 | 本地 TTS 封装（基于 android.speech.tts.TextToSpeech） |
| `domain/repository/VoiceRepository.kt` | 修改 | 添加 `setTtsProvider()` `getTtsProvider()` 接口 |
| `data/repository/VoiceRepositoryImpl.kt` | 修改 | 实现 TTS provider 切换逻辑 |
| `ui/settings/SettingsScreen.kt` | 修改 | 添加 TTS Provider 选择器 |
| `ui/settings/SettingsViewModel.kt` | 修改 | TTS provider 状态管理 |
| `di/SpeechModule.kt` | 修改 | 提供 CloudTtsEngine / LocalTtsEngine |

### 4.7 边界条件

| 场景 | 预期行为 |
|:-----|:---------|
| provider 参数为空或无效值 | 使用默认 provider（后端配置 `tts_default_provider`） |
| Edge TTS API 限流/封禁 | 后端捕获异常，返回 503，Android 端触发降级 |
| 文本超过 1024 字符 | 分段处理（每段 ≤ 1024 字符），依次合成后拼接返回 |
| 特殊字符（HTML/XML 标签） | 对文本进行 HTML 转义后传入 Edge TTS SSML 模板 |
| 并发请求过多 | 后端 TTS 服务限流，返回 429 |

---

## 5. P2-2: Flashcard 抽认卡

### 5.1 功能描述

基于错题和薄弱知识点生成复习卡片（Flashcard），帮助用户通过间隔重复（Spaced Repetition）巩固记忆。卡片从 `wrong_answers` 表和 `quiz_questions` 表中抽取错题生成，用户通过左右划动评价掌握程度，系统据此更新 `mastery_score`，已掌握卡片（score ≥ 0.8）自动归档。

**与 gamification 的关系**: 每次复习卡片获得 2 积分（gamification 积分体系），连续复习 7 天触发 streak 成就。

### 5.2 用户故事

| ID | As a... | I want to... | So that... |
|:---|:--------|:-------------|:-----------|
| US-FC-01 | 学生 | 每天看到系统为我生成的错题复习卡片 | 我不用手动整理，系统自动帮我复习薄弱点 |
| US-FC-02 | 学生 | 左右划动评价"不熟练"或"已掌握" | 我可以用最自然的操作完成复习反馈 |
| US-FC-03 | 学生 | 卡片正面显示题目，反面显示答案和解析 | 我可以先思考再看答案，加深记忆 |
| US-FC-04 | 学生 | 看到每张卡的掌握度数值 | 我知道哪些知识点还需要加练 |
| US-FC-05 | 学生 | 复习完今日卡片后获得积分 | 学习过程有成就感，持续保持动力 |
| US-FC-06 | 后端运维 | 已掌握卡片自动归档减少冗余 | 数据库不会无限膨胀 |

### 5.3 功能列表

| # | 功能点 | 模块 | 优先级 | 说明 |
|:-:|:-------|:----:|:------:|:-----|
| F1 | 从 wrong_answers / quiz_questions 抽取错题生成卡片 | Android | P2 | 本地查询 + 服务端兜底 |
| F2 | 卡片正面：题目（选择题显示题干） | Android | P2 | UI 设计 |
| F3 | 卡片反面：正确答案 + 解析 + 用户当时答案 + 掌握度 | Android | P2 | UI 设计 |
| F4 | 左划"不熟练" / 右划"已掌握"滑动评价 | Android | P2 | 手势交互 |
| F5 | 每日新卡数量上限（可配置，默认 10 张） | Android | P2 | 防止过量复习 |
| F6 | 已掌握卡片自动归档（mastery_score ≥ 0.8） | Android + 后端 | P2 | 归档后不再出现在复习队列 |
| F7 | 更新 mastery_score 到后端 | Android + 后端 | P2 | 同步到 wrong_answers.mastery_score |
| F8 | 复习获积分（2 分/卡） | Android | P2 | 复用 gamification 积分体系 |
| F9 | 今日完成进度指示（3/10 已完成） | Android | P2 | 进度条或计数 |
| F10 | 后端 Flashcard 批量获取/同步 API | 后端 | P2 | 新增路由 |

### 5.4 API 详细设计

#### GET /api/v1/flashcard/today

**功能**: 获取今日待复习卡片列表。

**Request**: 无需 Bod，通过 Token 识别用户。

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|:-----|:----:|:----:|:------:|:-----|
| limit | int | 否 | 10 | 返回卡片数量上限 |
| include_archived | bool | 否 | false | 是否包含已归档卡片 |

**Response**:
```json
{
  "code": 0,
  "data": {
    "cards": [
      {
        "id": "card-uuid",
        "source_type": "wrong_answer",
        "source_id": "wrong-answer-uuid",
        "front": {
          "content": "二次函数 y = ax² + bx + c 的顶点坐标是？",
          "type": "multiple_choice",
          "options": [
            {"key": "A", "value": "(-b/2a, (4ac-b²)/4a)"},
            {"key": "B", "value": "(b/2a, (4ac-b²)/4a)"}
          ]
        },
        "back": {
          "correct_answer": "A",
          "user_answer": "B",
          "explanation": "顶点坐标公式为 (-b/2a, (4ac-b²)/4a)...",
          "mastery_score": 0.3,
          "subject": "math",
          "topic": "二次函数"
        },
        "mastery_score": 0.3,
        "review_count": 1,
        "last_reviewed_at": null,
        "created_at": "2026-05-25T10:00:00Z"
      }
    ],
    "total_today": 10,
    "completed_today": 3,
    "daily_limit": 10
  }
}
```

#### POST /api/v1/flashcard/review

**功能**: 提交单张卡片的复习评价。

**Request**:
```json
{
  "card_id": "card-uuid",
  "judgment": "mastered",
  "time_spent_seconds": 15
}
```

| 字段 | 类型 | 必填 | 说明 |
|:-----|:----:|:----:|:-----|
| card_id | string | 是 | 卡片 ID |
| judgment | string | 是 | `mastered`（已掌握） 或 `unfamiliar`（不熟练） |
| time_spent_seconds | int | 否 | 复习用时 |

**Response**:
```json
{
  "code": 0,
  "data": {
    "card_id": "card-uuid",
    "new_mastery_score": 0.85,
    "archived": false,
    "earned_points": 2,
    "total_points": 1252
  }
}
```

**mastery_score 更新算法**:
```
如果 judgment == "mastered":
  新 mastery = min(1.0, 旧 mastery + (1.0 - 旧 mastery) * 0.3)
如果 judgment == "unfamiliar":
  新 mastery = max(0.0, 旧 mastery - 0.2)
如果 mastery >= 0.8:
  标记为 archived (已归档)
```

#### POST /api/v1/flashcard/sync

**功能**: 批量同步本地卡片评价到服务端（网络恢复时调用）。

**Request**:
```json
{
  "reviews": [
    {"card_id": "uuid1", "judgment": "mastered", "timestamp": "..."},
    {"card_id": "uuid2", "judgment": "unfamiliar", "timestamp": "..."}
  ]
}
```

**Response**:
```json
{
  "code": 0,
  "data": {
    "synced": 2,
    "results": [
      {"card_id": "uuid1", "success": true, "new_mastery": 0.85, "archived": false},
      {"card_id": "uuid2", "success": true, "new_mastery": 0.1, "archived": false}
    ]
  }
}
```

### 5.5 数据模型设计

#### Android Room 新增实体

```kotlin
@Entity(
    tableName = "flashcards",
    indices = [Index("source_id", unique = true)]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,           // 卡片 ID
    val sourceType: String,                // wrong_answer / quiz_question
    val sourceId: String,                  // 来源 ID（关联的 wrong_answer/quiz_question ID）
    val frontContent: String,              // 卡片正面内容（题目）
    val frontType: String,                 // 题目类型
    val frontOptions: String? = null,      // 选项 JSON（选择题）
    val backCorrectAnswer: String,         // 正确答案
    val backUserAnswer: String,            // 用户答案
    val backExplanation: String? = null,   // 解析
    val backMasteryScore: Float = 0.0f,    // 掌握度
    val backSubject: String,               // 学科
    val backTopic: String? = null,          // 知识点
    val masteryScore: Float = 0.0f,        // 当前掌握度
    val reviewCount: Int = 0,              // 复习次数
    val isArchived: Boolean = false,       // 是否已归档
    val lastReviewedAt: Long? = null,      // 上次复习时间
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 5.6 验收标准

#### AC-FC-01: 卡片生成

```
Given  用户有至少 5 条错题记录（wrong_answers 表中 mastery_score < 0.8）
When   打开 Flashcard 页面
Then   展示今日待复习卡片（默认 10 张）
And   卡片来源为 wrong_answers 表中的错题（mastery_score < 0.8）
And   每张卡片正面显示题干（选择题含选项）
And   卡片显示当前掌握度（如"掌握度 30%"）
```

#### AC-FC-02: 左右划动评价

```
Given  用户正在复习一张卡片
When   手指向左划动
Then   卡片移出屏幕，伴随"不熟练"标签动画
And   调用 POST /api/v1/flashcard/review，judgment="unfamiliar"
And   掌握度下降（或不变）
And   该卡片稍后再次出现（同日内最多 3 次）

Given  用户正在复习一张卡片
When   手指向右划动
Then   卡片移出屏幕，伴随"已掌握"标签动画
And   调用 POST /api/v1/flashcard/review，judgment="mastered"
And   掌握度上升
```

#### AC-FC-03: 每日新卡上限

```
Given  用户有 30 条待复习错题
When   打开 Flashcard 页面
Then   今日仅展示 10 张新卡（默认上限）
And   页面显示"今日进度：0/10"
When   完成 10 张卡片复习
Then   显示"今日复习已完成！明天再来吧 🎉"
And   "新卡片"按钮变为不可点击
```

#### AC-FC-04: 已掌握卡片自动归档

```
Given  用户某卡片 mastery_score = 0.75
When   用户右划评价"已掌握"
Then   mastery_score 更新至 0.85 (≥ 0.8)
And   卡片标记为已归档 (isArchived = true)
And   不再出现在今日待复习列表
And   归档记录保留在数据库中（可手动查看）
```

#### AC-FC-05: 积分激励

```
Given  用户完成一张卡片复习
When   提交评价后
Then   返回 earned_points = 2
And   用户的 gamification 总积分增加 2
And   UI 显示"+2 积分"飘字动画
```

#### AC-FC-06: 离线卡片复习

```
Given  用户处于离线模式
When   打开 Flashcard 页面
Then   从本地 Room DB 读取 Flashcards 表
And   显示缓存的今日卡片
When   左右划动评价
Then   评价记录写入本地待同步队列
And   网络恢复后自动调用 POST /api/v1/flashcard/sync 同步
```

### 5.7 涉及修改文件清单

#### Android 端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `data/local/entity/FlashcardEntity.kt` | 新建 | Flashcard 实体 |
| `data/local/dao/FlashcardDao.kt` | 新建 | 卡片 DAO（CRUD + 查询今日卡片） |
| `data/local/db/AiTutorDatabase.kt` | 修改 | 添加 FlashcardEntity，v1→v2 Migration |
| `di/DatabaseModule.kt` | 修改 | 提供 FlashcardDao |
| `data/remote/api/FlashcardApi.kt` | 新建 | 卡片 API 定义 |
| `data/remote/dto/FlashcardDtos.kt` | 新建 | 卡片相关 DTO |
| `data/repository/FlashcardRepositoryImpl.kt` | 新建 | 卡片仓库实现 |
| `domain/repository/FlashcardRepository.kt` | 新建 | 卡片仓库接口 |
| `domain/model/Flashcard.kt` | 新建 | 卡片领域模型 |
| `domain/usecase/flashcard/GetTodayCardsUseCase.kt` | 新建 | 获取今日卡片用例 |
| `domain/usecase/flashcard/ReviewCardUseCase.kt` | 新建 | 评价卡片用例 |
| `domain/usecase/flashcard/SyncFlashcardsUseCase.kt` | 新建 | 同步卡片评价用例 |
| `ui/flashcard/FlashcardScreen.kt` | 新建 | 卡片主页面（含左右划动手势） |
| `ui/flashcard/FlashcardFront.kt` | 新建 | 卡片正面 Composable |
| `ui/flashcard/FlashcardBack.kt` | 新建 | 卡片反面 Composable |
| `ui/flashcard/FlashcardViewModel.kt` | 新建 | 卡片状态管理 |
| `ui/navigation/Routes.kt` | 修改 | 添加 Flashcard 路由 |
| `ui/navigation/AppNavGraph.kt` | 修改 | 注册 Flashcard 页面 |

#### 后端

| 文件 | 操作 | 说明 |
|:-----|:----:|:-----|
| `app/routers/flashcard.py` | 新建 | Flashcard 路由（today/review/sync） |
| `app/schemas/flashcard.py` | 新建 | Flashcard Schema |
| `app/services/flashcard_service.py` | 新建 | 卡片业务逻辑（抽取、评分、归档） |
| `app/main.py` | 修改 | 注册 flashcard router |
| `app/models/wrong_answer.py` | 修改 | 添加/确认 mastery_score 字段（已有） |

### 5.8 边界条件

| 场景 | 预期行为 |
|:-----|:---------|
| 用户错题不足每日上限（如仅 3 条） | 实际展示 3 张，剩余进度标记为 3/10 |
| 所有错题均已归档 | 显示"所有错误均已掌握！🎉"，提供"查看已归档卡片"入口 |
| 用户重复评价同一张卡片（网络延迟重试） | 幂等设计：相同 card_id + judgment + 时间戳（1 分钟内）不重复处理 |
| 每日上限配置为 0 | 不显示卡片，提示"复习卡片已关闭" |
| 卡片来源的错题被删除 | 卡片标记为 `source_deleted`，自动移出复习队列 |
| 极端情况：用户 1 秒内快速划动多张卡 | 异步队列处理，UI 不卡顿，所有评价最终一致 |

---

## 6. 风险与依赖

### 6.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:----:|:----:|:---------|
| Edge TTS 接口非官方，可能被微软封禁或变更 | 中 | 高 | 抽象 TTS provider 层，快速切换；保留 OpenAI TTS 作为备选 provider |
| 离线模式 Room DB 迁移导致用户数据丢失 | 低 | 高 | 使用 `addMigrations()` 替代 `fallbackToDestructiveMigration()` |
| 同步冲突导致数据不一致（离线操作 vs 云端数据） | 中 | 中 | 服务端始终为权威源，增量同步 + 冲突日志 |
| PDF 生成在高并发下后端内存/CPU 飙升 | 中 | 中 | PDF 生成设为异步任务（Celery/BackgroundTask），限制并发数 |
| Flashcard 左右划动手势与 RecyclerView 滚动冲突 | 低 | 中 | 使用 Compose 自定义手势处理，不与父容器滚动冲突 |
| 用户缓存数据占用过多存储（如 100MB+） | 低 | 低 | 设置缓存上限 + LRU 淘汰策略，在设置页展示清理入口 |

### 6.2 依赖关系

| 任务 | 依赖 | 说明 |
|:-----|:-----|:-----|
| P1-1 (离线模式-同步) | analytics/stats 路由就绪 (Sprint 1) | 同步依赖 stats 查询已有数据 |
| P1-1 (离线模式-缓存) | Room DB 迁移经验 | 需要实施 v1→v2 迁移而非破坏性重建 |
| P1-2 (报告导出) | `analytics/stats` 路由已实现 (Sprint 1) | 报告数据聚合复用 stats 逻辑 |
| P1-2 (报告导出) | `backend/requirements.txt` 添加 reportlab/matplotlib | 需确认 Python 依赖兼容性 |
| P2-1 (TTS 替换) | 无外部依赖 | 纯后端 Edge TTS 调研 + 实现 |
| P2-1 (TTS 替换) | Android 端现有 VoiceRepository 架构 | 扩展 provider 切换逻辑 |
| P2-2 (Flashcard) | `wrong_answers` 表已有 (Sprint 1 已实现) | 卡片数据源就绪 |
| P2-2 (Flashcard) | gamification 积分体系 (Sprint 1) | 复用积分加分逻辑 |
| P2-2 (Flashcard) | `POST /api/v1/game/sync/score` (Sprint 1) | 积分同步路由就绪 |

### 6.3 不纳入 Sprint 2 的内容

以下功能已评估但不在本 Sprint 范围内：

- 家长监控面板（P3）— 包含家长端 Web 查看学习报告
- 手写 OCR 识别（P4）— 需要额外的 ML 模型集成
- 离线 AI 出题（仅在思考阶段）— 需要设备端 LLM 部署，技术尚未成熟
- 语音对话打断（P3）— 需要底层 AudioFocus 改造
- 多语言国际化（P3）— 需要全量字符串资源翻译
- 学习计划自动生成（P3）— 需要 AI 排课算法

---

## 7. 附录：参考数据

### 7.1 Sprint 2 新增 API 路由一览

| # | 方法 | 路径 | 工作量 | 对应模块 |
|:-:|:----:|:-----|:------:|:---------|
| 1 | GET | `/api/v1/sync` | 1 人日 | P1-1 离线模式 |
| 2 | POST | `/api/v1/sync` | 1 人日 | P1-1 离线模式 |
| 3 | POST | `/api/v1/report/export` | 1.5 人日 | P1-2 报告导出 |
| 4 | GET | `/api/v1/tts` | 1 人日 | P2-1 TTS 替换 |
| 5 | GET | `/api/v1/flashcard/today` | 1 人日 | P2-2 Flashcard |
| 6 | POST | `/api/v1/flashcard/review` | 0.5 人日 | P2-2 Flashcard |
| 7 | POST | `/api/v1/flashcard/sync` | 0.5 人日 | P2-2 Flashcard |

### 7.2 Sprint 2 工作量汇总

| 模块 | Android (人日) | 后端 (人日) | 总计 (人日) |
|:-----|:--------------:|:-----------:|:-----------:|
| P1-1 离线模式 | 4-5 | 1-2 | 5-7 |
| P1-2 报告导出 | 1 | 1 | 2 |
| P2-1 TTS 替换 | 1 | 1 | 2 |
| P2-2 Flashcard | 2-3 | 1-2 | 3-5 |
| **总计** | **8-10** | **4-6** | **12-16** |

### 7.3 Android Room 数据库版本演进

| 版本 | 添加内容 | 对应 Sprint |
|:----:|:---------|:-----------:|
| v1 | ConversationEntity, MessageEntity | Sprint 0 (基础) |
| v2 | CachedQuestionEntity, CachedWrongAnswerEntity, CachedConversationEntity, FlashcardEntity | Sprint 2 |

### 7.4 TTS Provider 对比

| Provider | 成本 | 中文音质 | 集成难度 | 稳定性 | 运维成本 |
|:---------|:----:|:--------:|:--------:|:------:|:--------:|
| OpenAI TTS (当前) | $0.015/1K char | ★★★★☆ | 简单 | 高 | 低 |
| Edge TTS (推荐) | 免费 | ★★★★★ | 中等 | 中（非官方） | 低 |
| Fish Speech (自部署) | GPU 服务器成本 | ★★★★☆ | 复杂 | 依赖部署质量 | 高 |

### 7.5 参考文档

- [Sprint 1 PRD](PRD_v4.0_Sprint1.md) — Sprint 2 格式模板和架构依赖
- [主 PRD 文档](PRD.md) — 全量功能定义
- [架构文档](ARCH.md) — 整体技术架构和数据流
- [API 文档](API.md) — 现有 API 路由清单
- [需求拆分表](PRD_SPLIT.md) — 细化开发任务参考
- [QA 审计报告](QA_AUDIT_REPORT.md) — Sprint 1 测试结果和回归建议

---

> **文档结束** — 本 PRD 由 PM 基于 Sprint 1 完成状态、现有代码库分析和用户反馈编写。
> 评审后由 Coder 拆分为具体开发任务，建议在 Sprint 回顾会后进行任务分配。
