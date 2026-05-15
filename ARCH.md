# AI 学伴 Android App — 架构文档

> 文档版本：v1.2（终稿）
> 日期：2026-05-15
> 架构师：CFO
> 基准 PRD：PRD.md v1.2（F40-F47 新增功能）

---

## 1. 当前架构 (2026-05-15)

### 1.1 整体架构：Clean Architecture + MVVM

```
UI Layer (Compose) → ViewModel → UseCase → Repository (接口) → RepositoryImpl → Data Sources
                          ↘                                               ↙
                        domain/                                        data/
                     (纯Kotlin,无Android依赖)
```

### 1.2 认证流程（当前实现）

```
┌─────────────────────┐         ┌──────────────────┐        ┌─────────────────────┐
│   LoginScreen       │         │  AuthRepository   │        │   Backend API       │
│   (Jetpack Compose) │         │  (domain/)        │        │   (FastAPI)         │
│                     │         │                   │        │                     │
│  ┌─────────────┐    │ 调用    │  ┌──────────────┐ │ HTTP   │  POST /auth/register│
│  │ LoginVM     │────┼────────┼─→│ register()    │─┼────────┼→  POST /auth/login  │
│  │ (HiltVM)    │    │         │  │ login()       │ │        │                     │
│  └─────────────┘    │         │  │ refreshToken()│ │        │  响应格式:          │
│                     │         │  └──────────────┘ │        │  {access_token      │
│                     │         │                   │        │   user_id            │
│                     │         │  ┌──────────────┐ │        │   nickname           │
│                     │         │  │ AuthRepository│ │        │   daily_quota        │
│                     │         │  │ Impl         │ │        │   daily_used}        │
│                     │         │  │ (data/)      │ │        │                     │
│                     │         │  │ → Gson解析    │ │        │  认证: Bearer Token  │
│                     │         │  │ → Token保存   │ │        └─────────────────────┘
│                     │         │  └──────────────┘ │
└─────────────────────┘         └──────────────────┘

认证方式：手机号 + 密码（pbkdf2_sha256 加密）
JWT Token：HS256 签名，72h 过期
```

### 1.3 核心数据流
```
User Action → Compose UI → ViewModel StateFlow → UseCase → Repository → 
  API/DB → <反序列化> → Domain Model → StateFlow → Compose UI 更新
```

### 1.4 技术选型
- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.02.00) + Material3
- **DI**: Hilt 2.50
- **Network**: Retrofit + OkHttp + Gson
- **Local DB**: Room 2.6.1 (KSP)
- **Auth**: JWT (python-jose on backend, Bearer on Android)
- **Streaming**: OkHttp SSE (EventSource)
- **Settings**: DataStore Preferences
- **Camera**: CameraX
- **OCR**: ML Kit Text Recognition
- **Navigation**: Jetpack Navigation Compose

---

## 2. 拍照解题增强模块 (F40)

> PRD 基准：§2.2 F40, §4.7, §5.3, §9.1 F40
> 优先级：P0（核心差异化）

### 2.1 技术选型

| 层级 | 技术 | 用途 |
|------|------|------|
| 摄像头 | CameraX (Lifecycle-aware) | 拍照预览 + 拍照捕获 |
| 本地 OCR | ML Kit Text Recognition v2 | 实时取景框文字检测（覆盖层显示） |
| 图片压缩 | BitmapFactory + Compressor (自定义) | 上传前自动压缩至 ≤2MB, ≤2048px 边长 |
| 上传协议 | OkHttp MultipartBody | POST /api/v1/solve/photo 图片上传 |
| 流式返回 | OkHttp EventSource (SSE) | 逐步骤接收解题结果 |
| 学科预测 | 后端 AI 引擎 | 自动识别学科，客户端传递 subject=auto |
| 权限管理 | Accompanist Permissions | 相机/存储运行时权限请求 |

### 2.2 数据流

```
┌─────────────────────────────────────────────────────────────────────┐
│  CameraScreen                                                        │
│                                                                     │
│  ① 拍照/选图                                                        │
│   ┌──────────┐    ┌──────────────┐    ┌──────────────────────┐     │
│   │ CameraX  │───→│ PhotoPreview │───→│ Compressor (压缩)     │     │
│   │ 拍照     │    │  Sheet(确认) │    │ ≤2MB / ≤2048px      │     │
│   └──────────┘    └──────────────┘    └──────────┬───────────┘     │
│                                                   │                 │
│   ┌──────────┐                                    │                 │
│   │ ML Kit   │←─── 实时OCR覆盖层（取景框文字检测） │                 │
│   │ OCR      │    (不阻断拍照流程)                 │                 │
│   └──────────┘                                    │                 │
│                                                   ▼                 │
│                                          ┌──────────────────┐       │
│                                          │ SubjectSelector  │       │
│                                          │ (数学/物理/化学/  │       │
│                                          │  生物/语文/英语/  │       │
│                                          │  自动识别)        │       │
│                                          └────────┬─────────┘       │
│                                                   │                 │
│                                                   ▼                 │
│                                          ┌──────────────────┐       │
│                                          │ SolveRepository  │       │
│                                          │ .solvePhoto()    │       │
│                                          └────────┬─────────┘       │
└──────────────────────────────────────────────────┼──────────────────┘
                                                    │
                     ════════════════ HTTP ════════════
                                                    ▼
┌────────────────────────────────────────────────────────────────┐
│  Backend                                                        │
│  ② POST /api/v1/solve/photo (Multipart: image + subject)       │
│  ③ 后端 OCR (PaddleOCR / Tesseract) → AI Analysis              │
│  ④ SSE 流式返回解题步骤                                          │
└────────────────────────────────────────────────────────────────┘
                                                    │
                     ════════════ SSE (EventSource) ═════════════
                                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│  ChatScreen (跳转聊天页面)                                       │
│                                                                 │
│  ⑤ SolveStreamParser 逐块解析 SSE events:                       │
│     event: step     → {"step": 1, "total": 5, "content": "..."} │
│     event: step     → {"step": 2, "total": 5, "content": "..."} │
│     event: answer   → {"answer": "最终答案: x = 5"}             │
│     event: complete → {"status": "success"}                     │
│                                                                 │
│  ⑥ StepByStepCard 渲染为分步卡片（可展开/折叠）                  │
│  ⑦ 最终答案高亮显示，底部"继续追问"按钮                          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 接口定义

**拍照解题上传**
```
POST /api/v1/solve/photo
Content-Type: multipart/form-data
Authorization: Bearer <token>

Fields:
  image: File (JPEG/PNG/WebP, ≤10MB 原始, 自动压缩至 ≤2MB)
  subject: String (可选: math/physics/chemistry/biology/chinese/english/auto, 默认 auto)
  grade: String (可选: 小学/初中/高中/大学, 从 user profile 自动读取)

Response (SSE stream):
  event: ocr_result
    data: {"text": "...", "subject": "math", "confidence": 0.95}
  
  event: step
    data: {"step": 1, "total": 5, "title": "分析已知条件", "content": "..."}
  
  event: step
    data: {"step": 2, "total": 5, "title": "建立方程", "content": "..."}
  
  event: answer
    data: {"answer": "x = 5", "explanation": "..."}
  
  event: complete
    data: {"status": "success", "solve_id": "uuid"}
  
  event: error
    data: {"code": "UNRECOGNIZABLE", "message": "未检测到题目"}
```

**客户端重新生成（某步骤没看懂）**
```
POST /api/v1/solve/step/retry
{
  "solve_id": "uuid",
  "step_index": 2,
  "question": "为什么这里要用勾股定理？"
}

Response (SSE): 重新生成该步骤及后续步骤
```

### 2.4 关键类图

```
┌─────────────────────────┐
│ CameraScreen            │──→ CameraViewModel
│  CameraX PreviewView    │       │
│  ML Kit OCR Overlay     │       ├── SubjectSelector (学科选择)
│  SubjectSelector        │       ├── PhotoPreviewSheet (拍照确认)
│  PhotoPreviewSheet      │       └── SolveRepository
│                         │               │
└─────────────────────────┘       ┌───────┴──────────┐
                                  │                  │
                          ┌───────┴───────┐  ┌───────┴────────┐
                          │ SolveApi      │  │ SolveStreamParser
                          │ (Retrofit)    │  │ (SSE EventSource)
                          └───────────────┘  └────────────────


data/repository/SolveRepository.kt
├── fun solvePhoto(image: File, subject: String, grade: String): Flow<SolveEvent>
│       ← 返回 Flow，emit 类型: OcrResult / StepProgress / AnswerResult / ErrorResult
├── fun retryStep(solveId: String, stepIndex: Int, question: String): Flow<SolveEvent>
└── fun cancelSolve(solveId: String)

domain/model/SolveEvent.kt (sealed class)
├── data class OcrResult(text: String, subject: String, confidence: Float)
├── data class StepProgress(step: Int, total: Int, title: String, content: String)
├── data class AnswerResult(answer: String, explanation: String)
├── data class Complete(status: String, solveId: String)
└── data class SolveError(code: String, message: String)
```

### 2.5 错误处理与离线 Fallback

| 场景 | 处理方式 |
|------|---------|
| 拍照权限拒绝 | 弹窗引导 → 跳转系统设置；拒绝后禁用拍照按钮，仅保留相册 |
| 图片过大 (>10MB 原始) | Auto-compress → 压缩至 ≤2MB，显示"正在压缩..." |
| 无网络上传 | 检测网络 → 提示"当前无网络，已保存图片到本地"→ 将图片暂存到 Room PendingUpload 表，网络恢复后自动上传 |
| OCR 未检测到文字 | 不上传 → 提示"未在图片中检测到文字，请对准题目区域" |
| SSE 连接中断 | 自动重连（指数退避 1s/2s/4s, 最多 3 次）→ 重连失败显示"连接中断" |
| AI 无法识别 | event: error → "无法解析图像，请重新拍照或手动输入题目" |
| 上传超时 (30s) | 取消请求 → 提示"上传超时，请检查网络后重试" |
| 拍照 → 解题 → 跳转聊天，但是用户中途退出 CameraScreen | 后台继续 SSE 连接，完成后在聊天页面追加结果 |
| 离线 Fallback | ML Kit 本地 OCR 提取文字 → 文字显示给用户 → 提示"网络恢复后可上传分析" |

---

## 3. 自适应分步讲解模块 (F41)

> PRD 基准：§2.2 F41, §5.6, §9.1 F41
> 优先级：P0（核心价值主张）

### 3.1 技术选型

| 层级 | 技术 | 用途 |
|------|------|------|
| 年级信息源 | DataStore (UserProfile) / Room (UserEntity) | 存储用户年级字段（小学/初中/高中/大学） |
| 参数传递 | ChatApi request 中扩展 grade 字段 | 每次 AI 请求附带年级上下文 |
| Prompt 模板 | 后端 Prompt Engineering | 结构化步骤标记 + 年级自适应规则 |
| 步骤渲染 | Compose CollapsibleCard (自定义) | 可展开/折叠的 Step card |
| 难度切换 | 重新请求 API (携带新的 grade/hint 参数) | 用户切换难度 → 重新生成 |
| "没看懂"按钮 | POST retry API | 针对某步骤请求更详细解释 |

### 3.2 年级参数传递方式

```
┌──────────────┐    ┌───────────────────┐    ┌──────────────────┐
│ UserProfile  │    │ ChatRepository    │    │ ChatApi          │
│ (DataStore)  │───→│ .sendMessage()    │───→│ POST /v1/chat/   │
│              │    │ 合并年级参数       │    │ completions (SSE)│
│ grade: "初三" │    │ 到请求体           │    │                  │
└──────────────┘    │ {                 │    │ {               │
                    │   "messages":..., │    │   grade: "初三", │
                    │   "grade": "初三", │    │   ...           │
                    │   "step_mode": true│    │ }               │
                    │ }                 │    └──────────────────┘
                    └───────────────────┘
```

**年级字段映射**：
- `grade=小学` → Prompt: "请用最简单的语言，每步骤一句话，多举生活例子"
- `grade=初中` → Prompt: "适中详细程度，补充公式推导，使用初中教材术语"
- `grade=高中` → Prompt: "完整步骤，含理论推导和公式证明，使用标准学术术语"
- `grade=大学` → Prompt: "深入原理，可引用文献/定理，提供扩展阅读方向"

### 3.3 Prompt 模板设计 (后端)

后端 Chat API 的 system prompt 中注入以下结构：

```
## 自适应讲解模式
- 每次回复必须以结构化步骤块组织，使用标记 <step>...</step>
- 根据用户年级({{grade}})调整讲解详细程度
- 步骤格式：<step index="1" total="5" title="步骤标题">内容</step>
- 最后一步必须包含最终答案，标记 <answer>...</answer>
- 每步骤后留"没看懂"入口
- 如果用户切换难度，重新生成全部步骤
```

**客户端解析规则**（ChatRepository / StreamChatUseCase）：
- SSE data chunk 中检测 `<step>` 标记
- 解析为 `StepProgress(step, total, title, content)` 
- ViewModel 维护 `steps: List<StepState>` 状态
- Compose 渲染为 CollapsibleStepCard 列表

### 3.4 前后端交互协议

```
Request:
POST /api/v1/chat/completions (SSE)
{
  "conversation_id": "uuid",
  "messages": [
    {"role": "user", "content": "请解释一下勾股定理"}
  ],
  "grade": "初三",          // ← 新增: 从用户profile读取
  "step_mode": true,        // ← 新增: 启用分步讲解模式
  "model": "gpt-4o",        // 用户选择的模型
  "temperature": 0.7,
  "stream": true
}

Response (SSE):
event: step
data: {"step": 1, "total": 4, "title": "勾股定理的定义", "content": "..."}

event: step
data: {"step": 2, "total": 4, "title": "公式推导", "content": "..."}

event: done
data: {"conversation_id": "uuid", "message_id": "uuid"}
```

**难度切换请求**:
```
POST /api/v1/chat/completions
{
  "conversation_id": "uuid",
  "regenerate_steps": true,    // ← 标记重新生成
  "grade": "高中",             // ← 新的年级级别
  "step_mode": true,
  "stream": true
}
```

### 3.5 关键类图

```
domain/
├── model/
│   ├── StepState.kt           // step, total, title, content, isExpanded, isRetrying
│   └── GradeLevel.kt          // enum: PRIMARY, JUNIOR, SENIOR, UNIVERSITY
├── repository/
│   └── ChatRepository.kt      // 扩展: sendMessage() 增加 grade 参数
└── usecase/chat/
    └── StreamChatUseCase.kt   // 扩展: 解析 step 标记

data/
├── local/
│   └── UserProfileDataStore.kt  // grade 字段持久化
└── remote/
    └── dto/ChatDtos.kt        // 扩展: StepRequest, StepResponse

ui/screen/chat/components/
├── CollapsibleStepCard.kt     // 可折叠步骤卡片组件
│   └── 每个卡片: 标题栏(可点击) + 内容区(展开/折叠动画)
│   └── 底部: "没看懂" 按钮 + "切换难度" 按钮
├── DifficultySwitcher.kt      // 难度切换按钮 (小学/初中/高中/大学)
└── StepProgressIndicator.kt   // Step 1/4 进度指示器
```

### 3.6 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Prompt 生成的步骤标记不完整/格式错乱 | 客户端解析失败 | 后端强校验：非合法格式的响应整体降级为普通文本；客户端增加容错解析 |
| 年级信息缺失(用户未填写) | 无法自适应 | 默认使用"初中"，在聊天页面顶部提示"填年级获得个性化讲解" |
| 切换难度后重新生成长时间等待 | 用户体验差 | 显示"正在重新生成..."；复用已有步骤直到新结果到达 |
| SSE 流中步骤边界检测延迟 | 首字显示慢 | 不等待完整 step 块，逐 token 渲染 + 步骤边界检测到达后插入分割线 |

---

## 4. AI 对话式教学增强模块 (F42)

> PRD 基准：§2.2 F42, §5.7, §9.1 F42
> 优先级：P0（差异化 + 真实学习价值）

### 4.1 ChatMode 三种模式架构

```
enum class ChatMode {
    ASSISTANT,    // 普通问答模式（直接回答）
    TUTOR,        // 苏格拉底式教学（引导提问 → 用户回答 → 逐步深入）
    QUIZ          // 测验模式（出题 → 批改）
}
```

**模式切换机制**：

```
┌──────────────────────────────────────────────────────────────────┐
│  ChatViewModel                                                     │
│                                                                    │
│  chatMode: MutableStateFlow<ChatMode>                              │
│    ┌──── ASSISTANT ────┐                                           │
│    │  普通问答模式     │◄──── 自动/Toggle 切换                     │
│    └───────────────────┘                                           │
│            │                                                        │
│            │  AI 检测到"学习型问题"                                  │
│            ▼                                                        │
│    ┌─────────────────┐                                              │
│    │  TUTOR          │──── 教学模式 Banner 展示                     │
│    │  苏格拉底式教学  │──── 用户可输入"直接给我答案"退出             │
│    └────────┬────────┘                                              │
│             │                                                        │
│             │  教学结束后                                            │
│             ▼                                                        │
│    ┌─────────────────┐                                              │
│    │  ASSISTANT      │                                              │
│    │  (回到普通模式)  │                                              │
│    └─────────────────┘                                              │
│                                                                    │
│  QUIZ 模式: 从聊天页面/仪表盘/图谱节点触发出题                      │
│  进入 QuizScreen (独立页面, 非聊天内模式)                           │
└──────────────────────────────────────────────────────────────────┘
```

**关键状态管理**（ChatUiState）：

```kotlin
data class ChatUiState(
    val messages: List<Message>,
    val chatMode: ChatMode = ChatMode.ASSISTANT,
    val teachingState: TeachingState? = null,   // TUTOR 模式专用
    val streamState: StreamState = StreamState.IDLE,
    val isThinking: Boolean = false,
    val error: String? = null
)

data class TeachingState(
    val currentPhase: TeachingPhase,   // CONCEPT_BREAKDOWN → GUIDING_QUESTION → USER_RESPONSE → DEEP_DIVE → SUMMARY
    val currentQuestion: String?,      // 当前引导问题
    val history: List<QaPair>,         // 问答记录 [Q: "你觉得...", A: "我认为是..."]
    val understanding: Understanding?  // 教学结束后的理解度评估
)

enum class TeachingPhase {
    CONCEPT_BREAKDOWN,     // 概念拆解
    GUIDING_QUESTION,      // 引导提问 (等待用户回答)
    USER_RESPONSE,         // 用户已回答 (正在分析正确性)
    DEEP_DIVE,             // 逐步深入
    SUMMARY                // 总结归纳
}
```

### 4.2 苏格拉底式教学数据流

```
用户提问: "为什么天空是蓝色的？"
    │
    ├─ AI SSE 流式返回 (自动检测为"学习型问题")
    │   ├─ event: mode_switch → {"mode": "tutor", "reason": "detected_learning_question"}
    │   ├─ event: text → "好的，让我们一起来探索这个问题！我先问你..."
    │   └─ event: socratic_question → {"question": "你觉得光是什么颜色的？"}
    │
    ├─ 客户端: ChatMode 切换为 TUTOR
    │   ├─ 顶部 Banner: "苏格拉底式教学" (带模式图标)
    │   ├─ 特殊气泡样式: 引导问题用紫色/引用色渲染
    │   └─ 底部: "退出教学" 按钮
    │
    ├─ 用户回答: "白色的"
    │   ├─ POST /v1/chat/completions (携带 teaching_session_id)
    │   ├─ AI 判断正确性 → SSE 流式返回
    │   │   ├─ event: correctness → {"correct": false, "hint": "那彩虹为什么有七种颜色？"}
    │   │   └─ event: socratic_question → {"question": "..."}
    │   └─ 循环: 引导提问 → 用户回答 → 正确性判断 → 深入
    │
    ├─ 用户输入"直接给我答案"
    │   ├─ POST 携带 exit_teaching=true
    │   ├─ AI 切回 ASSISTANT 模式 → 直接给出完整解答
    │   └─ 客户端: ChatMode → ASSISTANT, TeachingBanner 消失
    │
    └─ 教学结束 (AI 判断已掌握)
        ├─ event: teaching_complete → {"understanding": "mastered", "summary": "..."}
        ├─ 客户端: 理解度评估写入 LearningProgressRepository
        └─ 自动回到 ASSISTANT 模式
```

### 4.3 接口定义

```
POST /api/v1/chat/completions (SSE)
{
  "conversation_id": "uuid",
  "messages": [...],
  "teaching_mode": "auto",      // auto / tutor / assistant / quiz
  "teaching_session_id": "uuid" // 可选: 教学会话ID, 用于多轮教学追踪
}

新增 SSE events:
  event: mode_switch
    data: {"mode": "tutor", "reason": "detected_learning_question"}
  
  event: socratic_question
    data: {"question": "你觉得这个公式中哪个变量最重要？", 
           "phase": "guiding",
           "hints": ["回忆一下...", "如果...会怎样？"]}
  
  event: correctness
    data: {"correct": false, 
           "hint": "思路接近了，再想想摩擦力的方向？",
           "confidence": 0.6}
  
  event: teaching_complete
    data: {"understanding": "mastered",   // mastered / need_reinforce
           "knowledge_points": ["光的散射", "波长"],
           "summary": "总结内容...",
           "suggested_quiz": true}
  
  event: teaching_exit
    data: {"mode": "assistant", "reason": "user_requested"}
```

### 4.4 关键类图

```
ui/screen/chat/
├── ChatViewModel.kt
│   ├── chatMode: MutableStateFlow<ChatMode>
│   ├── teachingState: MutableStateFlow<TeachingState?>
│   ├── fun switchToTutorMode()
│   ├── fun switchToAssistantMode()
│   ├── fun exitTeaching()
│   └── fun submitTeachingResponse(answer: String)
│
├── components/
│   ├── SocraticBanner.kt          // 教学模式顶部横幅
│   ├── TeachingModeToggle.kt       // 模式切换开关 (聊天输入栏上方)
│   ├── SocraticQuestionBubble.kt   // 引导问题特殊气泡
│   └── UnderstandingBadge.kt       // 理解度评估徽章

data/repository/
├── ChatRepository.kt (扩展)
│   └── fun sendTeachingMessage(...): Flow<ChatEvent>
│       // 处理 teaching_mode / teaching_session_id / exit_teaching 参数
└── LearningProgressRepository.kt
    └── fun recordUnderstanding(knowledgePoint: String, understanding: Understanding)

domain/
├── model/TeachingMode.kt
│   └── enum ChatMode { ASSISTANT, TUTOR, QUIZ }
├── model/TeachingState.kt
│   └── data class TeachingState(...)
└── usecase/chat/
    └── ProcessTeachingResponseUseCase.kt  // 处理教学对话的业务逻辑
```

### 4.5 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| AI 误判非学习问题为学习问题 | 用户困惑 | 进入 TUTOR 模式前弹窗询问"是否进入引导式教学？" |
| 用户连续答错陷入死循环 | 挫败感 | 累计 3 次答错后 AI 主动提供提示，5 次答错自动切换为 ASSISTANT |
| 教学 Session 长时间不活跃 | 状态丢失 | 30 分钟无交互自动退出 TUTOR 模式 |
| 用户在网络不稳定时退出教学 | 后端状态不一致 | 客户端发送 exit_teaching 失败时标记本地状态为 ASSISTANT，下次请求时同步 |

---

## 5. 学习进度仪表盘模块 (F43)

> PRD 基准：§2.2 F43, §4.8, §9.1 F43
> 优先级：P1（留存关键）

### 5.1 技术选型

| 层级 | 技术 | 用途 |
|------|------|------|
| 图表绘制 | Compose Canvas (自定义绘制) | 掌握度环图、折线趋势图 |
| 知识图谱 | Compose Canvas + Lottie (可选) | 树状/网状知识图谱节点 |
| 数据存储 | Room (新增 AnalyticsDao, LearningRecord Entity) | 本地统计数据缓存 |
| 云端同步 | AnalyticsApi (Retrofit) | 跨设备同步统计数据 |
| 下拉刷新 | Compose Material3 PullToRefresh | 手动刷新仪表盘 |
| 缓存策略 | MemoryCache + Room + Network | 首次≤1s, 缓存≤200ms |

### 5.2 Room 查询设计

**新增 Entity**:

```kotlin
@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,                    // "2026-05-15" (按天聚合)
    val learnDurationMin: Int,           // 今日学习分钟数
    val solveCount: Int,                 // 解题数
    val correctCount: Int,               // 正确数
    val wrongCount: Int,                 // 错误数
    val streakDays: Int,                 // 连续学习天数
    val totalKnowledgePoints: Int,       // 总知识点数
    val masteredPoints: Int,             // 已掌握知识点数
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "knowledge_points")
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val name: String,                    // "勾股定理"
    val subject: String,                 // "math"
    val parentId: String?,               // 父知识点 ID（树形结构）
    val status: String,                  // "mastered" / "learning" / "weak"
    val confidence: Float,               // 掌握度 0.0~1.0
    val lastReviewedAt: Long?,
    val wrongCount: Int
)
```

**查询设计**:

```kotlin
@Dao
interface AnalyticsDao {
    // 今日统计
    @Query("""
        SELECT COALESCE(SUM(solve_count), 0) as solveCount,
               COALESCE(SUM(correct_count), 0) as correctCount,
               COALESCE(SUM(learn_duration_min), 0) as duration
        FROM learning_records WHERE date = :today
    """)
    fun getTodayStats(today: String): Flow<TodayStats>

    // 近 N 天趋势
    @Query("""
        SELECT * FROM learning_records 
        WHERE date >= :since ORDER BY date ASC
    """)
    fun getTrend(since: String): Flow<List<LearningRecordEntity>>

    // 知识点掌握度汇总
    @Query("""
        SELECT status, COUNT(*) as count 
        FROM knowledge_points GROUP BY status
    """)
    fun getKnowledgeSummary(): Flow<List<KnowledgeSummary>>

    // 知识点图谱 (树形)
    @Query("""
        SELECT * FROM knowledge_points 
        WHERE subject = :subject ORDER BY name
    """)
    fun getKnowledgeGraph(subject: String): Flow<List<KnowledgePointEntity>>

    // 连续学习天数
    @Query("""
        SELECT COUNT(DISTINCT date) FROM learning_records
        WHERE date >= :since AND solve_count > 0
    """)
    fun getActiveDays(since: String): Flow<Int>
}
```

### 5.3 UI 组件树

```
DashboardScreen
├── DashboardViewModel
│   ├── stats: StateFlow<DashboardStats>
│   ├── trends: StateFlow<List<TrendPoint>>
│   ├── knowledgeGraph: StateFlow<List<KnowledgeNode>>
│   ├── isLoading: StateFlow<Boolean>
│   └── isRefreshing: StateFlow<Boolean>
│
├── PullToRefreshBox
│   │
│   ├── StatsOverviewCard (顶部概览)
│   │   ├── StatItem ("今日学习", duration, icon)
│   │   ├── StatItem ("今日解题", count, icon)
│   │   ├── StatItem ("连续学习", streakDays, icon)
│   │   └── KnowledgeRing (掌握度环图, Canvas 绘制)
│   │       └── 根据 mastered / total 计算角度
│   │
│   ├── KnowledgeGraph (知识图谱区块)
│   │   ├── SubjectFilterChip (学科过滤器: 全部/数学/物理/...)
│   │   ├── Canvas绘制区域 (节点+连线)
│   │   │   ├── Node: 圆角矩形 + 知识点名 + 状态颜色(绿/黄/红)
│   │   │   ├── Edge: 连接线 + 方向指示
│   │   │   └── 交互: 点击节点 → 弹窗显示详情 + "去练习"按钮
│   │   └── Legend (图例: 已掌握/学习中/待加强)
│   │
│   └── TrendChart (趋势分析区块)
│       ├── TimeGranularityToggle (7天/30天 切换)
│       ├── MetricSelector (时长/题量/正确率)
│       └── LineChart (Compose Canvas 折线图)
│           ├── X轴: 日期标签
│           ├── Y轴: 值域
│           ├── 折线: 数据点连线 + 渐变填充
│           └── 交互: 点击数据点显示具体数值
│
└── EmptyStateView (首次使用)
    └── "开始学习吧！" 引导卡片 → 指向聊天/解题页面
```

### 5.4 数据聚合方案

```
数据来源:
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  ChatRoom    │   │  SolveRecords │   │ QuizRecords  │
│  (对话统计)   │   │  (解题记录)   │   │  (测验记录)   │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌──────────────────────────────────────────────────┐
│              AnalyticsEngine                      │
│  (在 App 启动 / 每个学习行为后触发)                │
│                                                    │
│  事件驱动更新:                                      │
│  ├─ MessageSent → 记录学习时长                      │
│  ├─ SolveCompleted → solveCount++, correct++/wrong++│
│  ├─ QuizCompleted → 更新知识点掌握度                 │
│  └─ TeachingComplete → 更新 understanding            │
│                                                    │
│  聚合策略:                                          │
│  ├─ 实时: 每次事件触发 → 更新 in-memory cache       │
│  ├─ 定时落盘: 30s 无事件 → 批量写入 Room            │
│  └─ 云端同步: 每次拉取时合并本地+云端                │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│              API 数据源                           │
│  GET /api/v1/analytics/stats                     │
│  Response: {                                      │
│    today: {solve_count, correct_rate, duration},  │
│    trend: [{date, duration, solve_count}, ...],    │
│    knowledge: [{id, name, status, confidence}, ...]│
│  }                                                │
└──────────────────────────────────────────────────┘
```

**缓存策略**: 首次加载 ≤1s, 缓存加载 ≤200ms
```
DashboardViewModel.init() → 
  ① 读本地 Room (LearningRecord, 上次缓存)
  ② 并行请求 GET /api/v1/analytics/stats (云端)
  ③ 合并: 本地优先展示 → 云端返回后更新
  ④ 写入 Room 缓存 (30 分钟过期)
```

### 5.5 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Canvas 绘制大量知识图谱节点导致卡顿 | UI 帧率下降 | 限制同时显示 ≤50 节点；使用 Canvas Layer 缓存；超过阈值使用 LazyColumn 替代 |
| 跨设备数据冲突 | 统计数据不准确 | 云端为主 + 本地为辅；同步时以服务器时间戳最新的为准 |
| 首次打开仪表盘无数据 | 空白页面 | 空状态引导 + 骨架屏(Skeleton)动画 |
| 统计数据实时更新压力大 | 频繁 Room 写入 | 批量写入 + 30s 静默窗口 |

---

## 6. 交互式测验模块 (F44)

> PRD 基准：§2.2 F44, §4.9, §5.8, §9.1 F44
> 优先级：P1（对标 Quizlet 核心功能）

### 6.1 测验生命周期

```
┌───────────────────────────────────────────────────────────────┐
│  测验生命周期: 出题 → 作答 → 批改 → 反馈                      │
│                                                               │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐ │
│  │ ① 出题   │───→│ ② 作答   │───→│ ③ 批改   │───→│ ④ 反馈   │ │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘ │
│       │              │              │              │          │
│       ▼              ▼              ▼              ▼          │
│  QuizGenerate    QuizScreen    QuizSubmit     QuizResult       │
│  Request         UserAnswers   API Call      Card             │
│                                                               │
│  状态流转:                                                     │
│  IDLE → GENERATING → ANSWERING → SUBMITTING → GRADING → DONE │
│         ↓ timeout             ↓ error                         │
│       ERROR                RETRY (本地缓存答案, 网络恢复提交)  │
└───────────────────────────────────────────────────────────────┘
```

### 6.2 状态管理

```kotlin
data class QuizUiState(
    val phase: QuizPhase = QuizPhase.IDLE,
    val config: QuizConfig,                    // 学科/知识点/难度/题数
    val questions: List<Question>,             // 题目列表
    val answers: Map<String, String>,          // 用户答案 (questionId → answer)
    val result: QuizResult?,                   // 批改结果
    val error: String?,
    val retryPending: Boolean = false          // 是否有待重试的提交
)

enum class QuizPhase {
    IDLE,           // 未开始
    GENERATING,     // 正在生成题目 (Loading)
    ANSWERING,      // 用户作答中
    SUBMITTING,     // 提交批改中
    GRADING,        // AI 批改中 (Loading)
    DONE,           // 批改完成
    ERROR           // 错误状态
}

enum class QuestionType {
    SINGLE_CHOICE,    // 单选题
    MULTIPLE_CHOICE,  // 多选题
    FILL_BLANK        // 填空题
}

data class Question(
    val id: String,
    val type: QuestionType,
    val content: String,          // Markdown 格式题目文本
    val options: List<String>?,   // 选项 (选择题)
    val correctAnswer: String?,   // 正确答案 (批改后填充)
    val explanation: String?,     // 解析 (批改后填充)
    val isCorrect: Boolean?,      // 是否正确 (批改后填充)
    val knowledgePoint: String    // 关联知识点
)
```

### 6.3 接口定义

**生成测验**
```
POST /api/v1/quiz/generate
{
  "subject": "math",              // 学科
  "knowledge_points": ["二次函数", "勾股定理"],  // 可选: 指定知识点
  "difficulty": "medium",         // easy / medium / hard
  "question_count": 5,            // 3-10 题
  "question_types": ["single_choice", "multiple_choice", "fill_blank"],
  "grade": "初三"                 // 年级上下文
}

Response:
{
  "quiz_id": "uuid",
  "questions": [
    {
      "id": "q1",
      "type": "single_choice",
      "content": "二次函数 $y = ax^2 + bx + c$ 的图像是什么？",
      "options": ["直线", "抛物线", "双曲线", "椭圆"],
      "knowledge_point": "二次函数",
      "difficulty": "medium"
    },
    ...
  ],
  "total_questions": 5,
  "estimated_time_min": 10
}
```

**提交批改**
```
POST /api/v1/quiz/submit
{
  "quiz_id": "uuid",
  "answers": {
    "q1": "B",
    "q2": "抛物线",
    "q3": ["A", "C"]
  },
  "duration_seconds": 300      // 作答耗时
}

Response:
{
  "quiz_id": "uuid",
  "results": [
    {
      "question_id": "q1",
      "is_correct": true,
      "correct_answer": "B",
      "user_answer": "B",
      "explanation": "抛物线正确！二次函数的图像是开口向上或向下的抛物线。",
      "score": 1.0,
      "knowledge_point": "二次函数"
    },
    ...
  ],
  "overall_score": 0.8,          // 总体得分
  "mastery_update": {            // 掌握度更新
    "二次函数": 0.85,
    "勾股定理": 0.6
  },
  "suggestions": ["建议复习勾股定理的逆定理应用"],
  "wrong_questions_added": ["q2"]  // 已自动加入错题本
}
```

### 6.4 本地存储模型

```kotlin
@Entity(tableName = "quiz_records")
data class QuizRecordEntity(
    @PrimaryKey val quizId: String,
    val subject: String,
    val knowledgePoints: String,    // JSON array
    val difficulty: String,
    val questionCount: Int,
    val score: Float?,
    val durationSeconds: Int?,
    val createdAt: Long,
    val syncedToCloud: Boolean = false
)

@Entity(tableName = "pending_submissions")
data class PendingSubmissionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val answers: String,            // JSON Map
    val durationSeconds: Int,
    val createdAt: Long,
    val retryCount: Int = 0
)
```

**本地缓存 + 自动重试逻辑**:
```kotlin
class QuizRepositoryImpl @Inject constructor(
    private val quizApi: QuizApi,
    private val quizRecordDao: QuizRecordDao,
    private val pendingSubmissionDao: PendingSubmissionDao
) : QuizRepository {
    
    override fun submitAnswers(quizId: String, answers: Map<String, String>, duration: Int): Flow<QuizSubmitResult> = flow {
        try {
            val result = quizApi.submitQuiz(SubmitRequest(quizId, answers, duration))
            emit(QuizSubmitResult.Success(result))
        } catch (e: IOException) {
            // 网络失败 → 缓存到本地
            pendingSubmissionDao.insert(PendingSubmissionEntity(
                quizId = quizId,
                answers = Gson().toJson(answers),
                durationSeconds = duration
            ))
            emit(QuizSubmitResult.PendingLocal)
        }
    }
    
    // App 启动 / 网络恢复时调用
    override suspend fun syncPendingSubmissions() {
        val pendings = pendingSubmissionDao.getAll()
        for (pending in pendings) {
            try {
                val result = quizApi.submitQuiz(...)
                pendingSubmissionDao.delete(pending.id)
            } catch (e: IOException) {
                if (pending.retryCount >= 3) {
                    // 超过最大重试次数 → 丢弃
                    pendingSubmissionDao.delete(pending.id)
                } else {
                    pendingSubmissionDao.updateRetryCount(pending.id, pending.retryCount + 1)
                }
                break  // 失败一个就停，等下次机会
            }
        }
    }
}
```

### 6.5 关键类图

```
ui/screen/quiz/
├── QuizScreen.kt
│   ├── 顶部: 学科+知识点标签 + 题数 + 进度 N/M
│   ├── 题目区: LazyColumn 逐题展示
│   ├── 选项区: 单选/多选/填空组件
│   ├── 提交按钮 (全部作答完毕才启用)
│   └── 结果区 (批改后): ✅/❌ + 正解 + 解析 + 建议
│
├── QuizViewModel.kt
│   ├── state: MutableStateFlow<QuizUiState>
│   ├── fun generateQuiz(config: QuizConfig)
│   ├── fun submitAnswer(questionId: String, answer: String)
│   ├── fun submitAllAnswers()
│   └── fun retryGenerate()
│
└── components/
    ├── QuestionCard.kt          // 题目卡片 (支持 MD 公式渲染)
    ├── AnswerOption.kt          // 选项 (单选 RadioButton / 多选 Checkbox)
    ├── FillBlankInput.kt       // 填空输入框
    ├── QuizResultCard.kt       // 批改结果卡片
    └── QuizProgressBar.kt     // 顶部进度指示器

data/
├── remote/
│   ├── api/QuizApi.kt
│   └── dto/QuizDto.kt
├── local/
│   ├── dao/QuizRecordDao.kt
│   └── entity/QuizRecordEntity.kt
└── repository/QuizRepositoryImpl.kt

domain/
├── model/QuizModels.kt
│   └── data class Question, QuizConfig, QuizResult, ...
├── repository/QuizRepository.kt (interface)
└── usecase/quiz/
    ├── GenerateQuizUseCase.kt
    └── SubmitQuizUseCase.kt
```

### 6.6 交互式测验 + 间隔重复复习 (F45) 联动架构

> F45 (间隔重复复习) 并非独立模块，而是 F44 的下游数据消费者和错题本的管理者。

```
┌───────────────────┐       测验/对话/解题答错
│  各种学习场景     │─────────────────────────┐
│  对话 → 答错      │                          │
│  测验 → 答错      │                          ▼
│  解题 → 答错      │            ┌────────────────────────┐
└───────────────────┘            │ WrongAnswerEntity      │
                                 │ (错题本, Room)         │
                                 │ ├─ 题目原文+答案        │
                                 │ ├─ 错误答案+时间        │
                                 │ ├─ 学科/知识点          │
                                 │ ├─ 来源 (chat/quiz/solve)│
                                 │ ├─ 复习间隔 (天)        │
                                 │ ├─ 下次复习时间          │
                                 │ └─ 连续正确次数         │
                                 └───────────┬────────────┘
                                             │
                                             ▼
                                 ┌────────────────────────┐
                                 │ SpacedRepetitionEngine │
                                 │ (domain/engine/)        │
                                 │                         │
                                 │ SM-2 变体:              │
                                 │ ├─ 首次: 1天           │
                                 │ ├─ 二次: 3天           │
                                 │ ├─ 三次: 7天           │
                                 │ ├─ 四次: 14天          │
                                 │ └─ 正确→间隔×2         │
                                 │    错误→重置为1天      │
                                 │    连续3次正确→已掌握   │
                                 └───────────┬────────────┘
                                             │
                                             ▼
                                 ┌────────────────────────┐
                                 │ ReviewScreen           │
                                 │ (复习页面)              │
                                 │ ├─ 今日待复习 N 题     │
                                 │ ├─ 错题本列表           │
                                 │ ├─ 复习日历             │
                                 │ └─ 作答→更新间隔        │
                                 └────────────────────────┘
```

**关键新增文件**:
```
data/local/entity/WrongAnswerEntity.kt
data/local/dao/WrongAnswerDao.kt
data/repository/WrongAnswerRepository.kt
domain/engine/SpacedRepetitionEngine.kt    // 纯 Kotlin, 无 Android 依赖
ui/screen/review/ReviewScreen.kt
ui/screen/review/ReviewViewModel.kt
ui/screen/review/components/ReviewCard.kt
ui/screen/review/components/ReviewCalendar.kt
```

### 6.7 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| AI 出题质量不稳定 (题目错误/歧义) | 用户学习效果差 | 客户端缓存题目 + 设置"报告题目问题"按钮；出题超时 15s 引导重试 |
| 测验生成耗时 >8s | 用户等待焦虑 | 骨架屏 + 进度条 + 预计等待时间；提供"简化模式(3题)"快速出题选项 |
| 提交失败导致答案丢失 | 用户挫败感 | 本地缓存 pending_submissions → 网络恢复自动重试 (最多 3 次) |
| 错题本数据量达 1000+ 条 | 加载缓慢 | 分页加载 (50条/页) + 按学科索引 + 懒加载 |
| 间隔重复算法在本地与云端不一致 | 复习计划混乱 | 以本地算法为主，云端仅做同步备份；每次同步时比较 next_review_at，取最近值 |

---

## 附录 A: 新增文件清单 (F40-F45)

基于 PROJECT_STRUCTURE.md 的待新建文件清单，扩展新增模块所需文件：

| 文件路径 | 所属模块 | 说明 |
|---------|---------|------|
| `data/remote/api/SolveApi.kt` | F40 拍照解题 | Multipart 上传 + SSE 解题流 |
| `data/remote/stream/SolveStreamParser.kt` | F40 | 解题 SSE 事件解析 |
| `data/remote/dto/SolveDto.kt` | F40 | 解题请求/响应 DTO |
| `data/repository/SolveRepository.kt` (接口+impl) | F40 | 解题仓库 |
| `domain/model/SolveEvent.kt` | F40 | SSE 事件 sealed class |
| `ui/screen/camera/CameraViewModel.kt` | F40 | 相机 ViewModel (增强) |
| `ui/screen/camera/SubjectSelector.kt` | F40 | 学科选择器组件 |
| `ui/screen/camera/PhotoPreviewSheet.kt` | F40 | 拍照预览确认 Sheet |
| `ui/screen/chat/components/StepByStepCard.kt` | F40/F41 | 分步卡片组件 |
| `ui/screen/chat/components/CollapsibleStepCard.kt` | F41 | 可折叠步骤卡片 |
| `ui/screen/chat/components/DifficultySwitcher.kt` | F41 | 难度切换按钮 |
| `ui/screen/chat/components/StepProgressIndicator.kt` | F41 | 步骤进度指示器 |
| `domain/model/ChatMode.kt` | F42 | ChatMode 枚举 |
| `domain/model/TeachingState.kt` | F42 | 教学状态模型 |
| `ui/screen/chat/components/SocraticBanner.kt` | F42 | 教学模式横幅 |
| `ui/screen/chat/components/TeachingModeToggle.kt` | F42 | 教学模式切换 |
| `ui/screen/chat/components/SocraticQuestionBubble.kt` | F42 | 引导问题气泡 |
| `ui/screen/chat/components/UnderstandingBadge.kt` | F42 | 理解度徽章 |
| `domain/usecase/chat/ProcessTeachingResponseUseCase.kt` | F42 | 教学响应处理 |
| `data/local/dao/AnalyticsDao.kt` | F43 | 统计查询 DAO |
| `data/local/entity/LearningRecord.kt` | F43 | 学习记录 Entity |
| `data/local/entity/KnowledgePointEntity.kt` | F43 | 知识点 Entity |
| `data/repository/AnalyticsRepository.kt` (接口+impl) | F43 | 统计仓库 |
| `data/remote/api/AnalyticsApi.kt` | F43 | 云端统计 API |
| `domain/engine/AnalyticsEngine.kt` | F43 | 聚合引擎 |
| `domain/usecase/GetLearningStatsUseCase.kt` | F43 | 获取统计 UseCase |
| `ui/screen/dashboard/DashboardScreen.kt` | F43 | 仪表盘页面 |
| `ui/screen/dashboard/DashboardViewModel.kt` | F43 | 仪表盘 ViewModel |
| `ui/screen/dashboard/components/StatsOverviewCard.kt` | F43 | 概览卡片 |
| `ui/screen/dashboard/components/KnowledgeGraph.kt` | F43 | 知识图谱 Canvas |
| `ui/screen/dashboard/components/TrendChart.kt` | F43 | 趋势折线图 |
| `data/remote/api/QuizApi.kt` | F44 | 测验 API |
| `data/remote/dto/QuizDto.kt` | F44 | 测验 DTO |
| `data/repository/QuizRepository.kt` (接口+impl) | F44 | 测验仓库 |
| `data/local/dao/QuizRecordDao.kt` | F44 | 测验记录 DAO |
| `data/local/entity/QuizRecordEntity.kt` | F44 | 测验记录 Entity |
| `data/local/entity/PendingSubmissionEntity.kt` | F44 | 待提交缓存 Entity |
| `domain/model/QuizModels.kt` | F44 | 测验领域模型 |
| `domain/usecase/quiz/GenerateQuizUseCase.kt` | F44 | 生成测验 UseCase |
| `domain/usecase/quiz/SubmitQuizUseCase.kt` | F44 | 提交批改 UseCase |
| `ui/screen/quiz/QuizScreen.kt` | F44 | 测验页面 |
| `ui/screen/quiz/QuizViewModel.kt` | F44 | 测验 ViewModel |
| `ui/screen/quiz/components/QuestionCard.kt` | F44 | 题目卡片 |
| `ui/screen/quiz/components/AnswerOption.kt` | F44 | 选项组件 |
| `ui/screen/quiz/components/FillBlankInput.kt` | F44 | 填空输入 |
| `ui/screen/quiz/components/QuizResultCard.kt` | F44 | 批改结果卡片 |
| `data/local/entity/WrongAnswerEntity.kt` | F45 | 错题 Entity |
| `data/local/dao/WrongAnswerDao.kt` | F45 | 错题 DAO |
| `data/repository/WrongAnswerRepository.kt` | F45 | 错题仓库 |
| `domain/engine/SpacedRepetitionEngine.kt` | F45 | 遗忘曲线算法 |
| `ui/screen/review/ReviewScreen.kt` | F45 | 复习页面 |
| `ui/screen/review/ReviewViewModel.kt` | F45 | 复习 ViewModel |
| `ui/screen/review/components/ReviewCard.kt` | F45 | 复习卡片 |
| `ui/screen/review/components/ReviewCalendar.kt` | F45 | 复习日历 |
| `data/local/NotificationHelper.kt` | F45 | 复习通知 (增强) |

---

## 附录 B: 模块依赖关系总图

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          F40-F45 模块依赖关系                                  │
│                                                                              │
│  ┌──────────────────┐                                                       │
│  │ 现有架构 (P0基础)  │                                                       │
│  │  ├─ T1 脚手架     │                                                       │
│  │  ├─ T2 认证       │                                                       │
│  │  ├─ T3 会话管理   │                                                       │
│  │  ├─ T4 聊天界面   │──┬── T5 SSE流式 ──┬── T6 Markdown渲染 ── T7 持久化    │
│  │  └─ T8 拍照基础   │  └───────────────┴───────────────────────────┘        │
│  └──────────────────┘                                                       │
│           │                                                                  │
│           ▼                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐        │
│  │  Phase 1B: F40 拍照解题增强 (T9)                                │        │
│  │  依赖: T5(SSE) + T8(拍照基础) + SolveApi + SolveStreamParser    │        │
│  └──────────────────────────────────────────────────────────────────┘        │
│           │                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐        │
│  │  Phase 1C: F41 自适应讲解 + F42 苏格拉底式教学                    │        │
│  │  依赖: T4(聊天界面) + T5(SSE) + UserProfile(年级信息)            │        │
│  │  输出: F43 数据源 (理解度记录 → LearningProgressRepository)      │        │
│  └──────────────────────────────────────────────────────────────────┘        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐        │
│  │  Phase 2B: F43 学习进度仪表盘 (T14)                             │        │
│  │  依赖: Room + AnalyticsEngine + AnalyticsApi                    │        │
│  └──────────────────────────────────────────────────────────────────┘        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐        │
│  │  Phase 2C: F44 交互式测验 (T15) + F45 间隔重复复习 (T16)         │        │
│  │  依赖: F43(仪表盘) + QuizApi + SpacedRepetitionEngine           │        │
│  │  F44 → 答错 → F45(错题本自动收录)                                │        │
│  └──────────────────────────────────────────────────────────────────┘        │
│                                                                              │
│  开发并行策略:                                                                │
│  ├─ F40 / F41 / F42 可并行开发 (依赖 T4+T5 完成后)                           │
│  ├─ F43 需 F41/F42 的理解度数据源                                            │
│  └─ F44+F45 需 F43 的仪表盘数据                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```
