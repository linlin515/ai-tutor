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
---

## 7. 语音云服务降级与打断协调模块 (F16 + F18 + F20)

> PRD 基准：§2.2 F16/F18/F20, §4.6, §5.2
> 优先级：P1（语音体验完善）

### 7.1 技术选型

| 层级 | 技术 | 用途 |
|------|------|------|
| 本地 ASR | Android SpeechRecognizer | 短音频实时识别（降级前优先） |
| 云端 ASR | CloudAsrEngine (Retrofit + Audio Upload) | 长音频/低置信度降级识别 |
| 本地 TTS | Android TextToSpeech | 短文本朗读 (<500字) |
| 云端 TTS | CloudTtsEngine (SSE Audio Stream) | 长文本流式朗读 (≥500字) |
| 音频播放 | ExoPlayer / AudioTrack | 云端 TTS PCM 音频块播放 |
| 状态管理 | VoiceUiState (sealed class) | 语音状态机仲裁 |
| 性能指标 | Acoust NoiseSuppression (可选) | 环境自适应降噪 |

### 7.2 语音状态机架构

```kotlin
// 统一状态管理：语音交互的完整生命周期
sealed class VoiceState {
    object Idle : VoiceState()
    data class Listening(
        val amplitude: Float,        // 音量振幅 (UI 动画用)
        val durationMs: Long,        // 已录音时长
        val source: AudioSource      // LOCAL / CLOUD
    ) : VoiceState()
    data class Processing(
        val partialText: String?,    // 部分识别结果
        val source: AudioSource
    ) : VoiceState()
    data class Speaking(
        val messageId: String,        // 正在播放的消息 ID
        val progress: Float,          // 播放进度 0.0~1.0
        val source: AudioSource       // LOCAL / CLOUD
    ) : VoiceState()
    data class Error(val code: VoiceErrorCode, val message: String) : VoiceState()
}

enum class VoiceErrorCode {
    ASR_NO_SPEECH,      // 未检测到语音
    ASR_RECOG_FAIL,     // 识别失败
    TTS_ENGINE_MISSING, // TTS 引擎未安装
    CLOUD_TIMEOUT,      // 云端超时
    NETWORK_UNAVAILABLE // 无网络
}
```

**状态机流转规则**：

```
                   ┌──────────┐
                   │   IDLE   │◄─────────────────────────┐
                   └────┬─────┘                          │
                        │ 长按语音按钮                      │
                        ▼                                │
                   ┌──────────┐        松开 + 识别成功      │
         ┌────────→│ LISTENING│────────────────────────┐  │
         │         └────┬─────┘                        │  │
         │              │ 松开 / 自动结束               │  │
         │              ▼                              │  │
         │         ┌──────────┐     conf<0.6 / >30s    │  │
         │         │PROCESSING│──── ─ ─ ─ ─ ─ ─ ─ ─→  │  │
         │         └────┬─────┘    CloudAsrEngine      │  │
         │              │ 识别完成                      │  │
         │              ▼                              │  │
         │         ┌──────────┐                        │  │
         │         │   DONE   │──→ 填充文本到输入框       │  │
         │         └──────────┘                        │  │
         │                                            │  │
         │  TTS 播放触发:                               │  │
         │    ┌──────────┐    打断: 点击其他消息         │  │
         │    │ SPEAKING │──── ─ ─ ─ ─ ─ ─ ─ ─ ─ ─────┘  │
         │    └────┬─────┘    或: 长按语音按钮触发        │  │
         │         │ 播放完成                            │  │
         └─────────┴─────────────────────────────────────┘

触发打断时：
  SPEAKING → 立即停止 TTS → 清空 AudioTrack Buffer → LISTENING
  打断延迟目标: < 300ms
```

### 7.3 F16 云端 ASR 降级策略

#### 降级判定链

```
用户语音输入完成
    │
    ├─ 音频时长 > 30s ?
    │   ├─ 是 → 跳过本地ASR → 直接上传云端
    │   └─ 否 → 走本地 SpeechRecognizer
    │
    ├─ 本地识别结果 conf < 0.6 ?
    │   ├─ 是 → 触发云端 ASR 备选
    │   └─ 否 → 使用本地结果
    │
    └─ 云端 ASR 返回后：
        ├─ 本地结果未展示 → 替换为云端结果
        └─ 本地结果已展示 → 在输入框追加/替换（用户可手动编辑）
```

#### 接口定义

```
POST /api/v1/voice/asr
Content-Type: multipart/form-data
Authorization: Bearer ***

Fields:
  audio: File (WAV/PCM/MP3, ≤10MB, 单声道 16kHz 采样率)
  source: String ("voice_input" / "long_audio")
  noise_level: String? ("quiet" / "moderate" / "noisy")

Response:
{
  "text": "识别后的文本",
  "confidence": 0.92,
  "language": "zh-CN",
  "segments": [
    {"start_ms": 0, "end_ms": 1200, "text": "第一句"},
    {"start_ms": 1200, "end_ms": 3000, "text": "第二句"}
  ],
  "duration_ms": 3500
}

错误响应:
{
  "error": "AUDIO_TOO_SHORT",
  "message": "音频过短，请重新录制"
}
```

#### 降级配置参数 (DataStore)

```kotlin
data class AsrFallbackConfig(
    val durationThresholdMs: Long = 30_000L,   // 长音频阈值 30s
    val confidenceThreshold: Float = 0.6f,      // 本地识别置信度阈值
    val cloudTimeoutMs: Long = 15_000L,         // 云端超时 15s
    val noiseThresholdDb: Float = 60f           // 噪声阈值 60dB
)
```

#### 关键类

```
data/media/
├── CloudAsrEngine.kt          // 云端 ASR 调用封装
│   ├── fun recognize(audioFile: File, config: AsrFallbackConfig): Flow<AsrResult>
│   ├── suspend fun cancel()
│   └── fun isCloudAvailable(): Boolean
│
├── AsrFallbackStrategy.kt     // 降级判定逻辑 (纯 Kotlin, 无 Android 依赖)
│   └── fun shouldFallback(localConfidence: Float, audioDurationMs: Long, noiseDb: Float): FallbackDecision
│       // return SKIP / FALLBACK / USE_LOCAL
│
└── VoiceRepository.kt (修改)
    └── fun recognize(...): Flow<VoiceResult>
        // 内部协调本地 → 云端降级逻辑
```

### 7.4 F18 云端 TTS 流式播放

#### 本地/云端切换策略

```
用户点击 TTS 按钮
    │
    ├─ 文本长度 < 500 字 AND 本地引擎可用 ?
    │   ├─ 是 → Android TextToSpeech 本地播放
    │   └─ 否 → 请求云端 TTS
    │
    └─ 云端 TTS:
        ├─ POST /api/v1/voice/tts (SSE)
        ├─ SSE 逐块接收 audio/base64 chunk
        ├─ ExoPlayer 逐块追加播放
        └─ 流结束 → 标记播放完成
```

#### 接口定义

```
POST /api/v1/voice/tts (SSE)
Authorization: Bearer ***

{
  "text": "需要朗读的文本内容...",
  "voice": "zh-CN-XiaoxiaoNeural",  // 音色
  "speed": 1.0,                      // 语速 0.5~2.0
  "pitch": 1.0,                      // 音调 0.5~1.5
  "format": "pcm_16000hz_mono"       // 音频格式
}

SSE events:
  event: audio_chunk
    data: {"sequence": 0, "audio_base64": "...", "duration_ms": 800}
  
  event: audio_chunk
    data: {"sequence": 1, "audio_base64": "...", "duration_ms": 900}
  
  event: complete
    data: {"total_chunks": 12, "total_duration_ms": 10500}
  
  event: error
    data: {"code": "TTS_ENGINE_ERROR", "message": "语音合成失败"}
```

#### 客户端播放架构

```
CloudTtsEngine
├── fun streamTts(request: TtsRequest): Flow<TtsChunk>
│   // SSE 流式接收 → emit TtsChunk(audioBytes, sequence, durationMs)
│
├── TtsAudioPlayer
│   ├── AudioTrack (PCM 直接播放)
│   │   ├── fun enqueueChunk(chunk: ByteArray)  // 追加到播放缓冲区
│   │   ├── fun play()          // 开始播放
│   │   ├── fun pause()         
│   │   ├── fun stop()          // 立即停止 + 清空缓冲区
│   │   └── fun flush()         // 清空未播放缓冲区
│   │
│   └── PlaybackState: IDLE / BUFFERING / PLAYING / PAUSED / STOPPED
│
├── 缓存策略:
│   ├── LruCache<String, ByteArray>  // text_hash → 完整音频
│   ├── maxEntries: 50
│   └── 相同文本 5 分钟内直接命中缓存
```

### 7.5 F20 语音打断协调

#### 打断流程（精确到毫秒）

```
TTS 播放中 (SPEAKING)
    │
    ├─ 事件: 用户点击其他消息 TTS 按钮
    │   ├─ 1. TtsAudioPlayer.stop()        // 0ms: 立即停止播放
    │   ├─ 2. AudioTrack.pause() + flush() // <10ms: 清空缓冲区
    │   ├─ 3. 切换 VoiceState 为 PROCESSING // <20ms
    │   ├─ 4. 新 TTS 请求发起               // <50ms
    │   └─ 总打断延迟: <300ms ✅
    │
    ├─ 事件: 用户长按语音按钮触发 ASR
    │   ├─ 1. TtsAudioPlayer.stop()         // 0ms
    │   ├─ 2. AudioTrack.flush()            // <10ms
    │   ├─ 3. VoiceState → LISTENING        // <20ms
    │   └─ 4. SpeechRecognizer.start()      // <100ms
    │
    └─ 事件: 用户关闭 App / 切到后台
        ├─ TtsAudioPlayer.pause()
        └─ VoiceState → IDLE (onSaveInstanceState 持久化)
```

#### 防冲突机制

| 并发场景 | 处理策略 |
|---------|---------|
| TTS 播放中触发同一消息 | 忽略重复请求，继续保持播放 |
| TTS 播放中多次打断 | 以最后一次操作为准，旧的打断操作被取消 |
| ASR 识别中触发 TTS | ASR 优先 → 等待 ASR 完成 → 若需要再 TTS |
| 同时收到多个 TTS chunk | ExoPlayer 队列 FIFO 播放 |
| 打断后瞬间再次打断 | 500ms 消抖窗口，窗口内合并为一次打断 |

#### 关键类文件

```
data/media/
├── CloudTtsEngine.kt           // (新建) 云端 TTS + 流式播放
├── AsrFallbackStrategy.kt      // (新建) ASR 降级判定
├── TtsAudioPlayer.kt           // (新建) AudioTrack 播放封装
├── VoiceRepository.kt          // (修改) 打断协调 + 降级逻辑
└── NoiseSuppression.kt         // (可选, F47) 降噪处理

ui/screen/chat/components/
└── VoiceInputBar.kt            // (修改) 打断交互流畅化

domain/model/
└── VoiceState.kt               // (新建) 统一语音状态机
```

### 7.6 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 云端 ASR 超时 (网络差) | 识别延迟 >15s | 设置 15s 超时 + 超时后提示"网络不稳定，请稍后重试" |
| 云端 TTS 流式中断 | 播放卡顿/中断 | SSE 自动重连 + 已缓冲音频继续播放至耗尽 |
| 打断时产生爆音 | 用户体验差 | AudioTrack 停止前快速 fade-out (10ms), 新播放 fade-in |
| 本地 TTS 引擎未安装 | TTS 功能不可用 | 检测引擎 → 引导安装 → 降级为云端 TTS |
| 多段音频拼接处有杂音 | 听感不连贯 | AudioTrack 缓冲区无缝拼接 + 交叉淡化 (crossfade) 算法 |

---

## 8. ML Kit OCR 集成方案 (F23)

> PRD 基准：§2.2 F23, §4.7, §7.4
> 优先级：P1（已归入 T8 基础模块，此处为独立架构描述）
> 说明：F23 是 F40 拍照解题的底层基础设施，同时也为未来其他 OCR 场景（取景框文字检测、实时翻译等）提供基础能力。

### 8.1 技术选型

| 组件 | 版本 | 用途 |
|------|------|------|
| ML Kit Text Recognition v2 | com.google.mlkit:text-recognition:16.0.1 | 本地设备端文字识别 |
| ML Kit Text Recognition Chinese | com.google.mlkit:text-recognition-chinese:16.0.1 | 中文字符识别增强包 |
| CameraX Analyzer | androidx.camera:camera-core | 实时取景框帧分析 |
| GraphicOverlay | 自定义 Canvas | OCR 检测框叠加层 |

### 8.2 分层架构

```
┌──────────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                           │
│                                                               │
│  CameraScreen                                                 │
│  ├── PreviewView (CameraX)                                    │
│  ├── GraphicOverlay (Canvas 叠加层)                            │
│  │   └── TextGraphic: 文字边框 + 识别文本显示                  │
│  └── SubjectSelector (学科选择器) ← 依赖 OCR 预识别            │
│                                                               │
│  CameraViewModel                                              │
│  └── ocrText: StateFlow<OcrResult?>                           │
│                                                               │
├──────────────────────────────────────────────────────────────┤
│  Data Layer                                                    │
│                                                               │
│  OcrEngine (data/vision/)                                      │
│  ├── ML Kit 初始化 + 配置                                      │
│  ├── fun analyze(imageProxy: ImageProxy): OcrResult            │
│  └── fun analyzeBitmap(bitmap: Bitmap): OcrResult              │
│                                                               │
│  OcrResult model:                                              │
│  ├── rawText: String          // 原始识别文本全文              │
│  ├── blocks: List<TextBlock>  // 文本块列表                    │
│  │   ├── text: String                                         │
│  │   ├── boundingBox: Rect                                    │
│  │   ├── confidence: Float                                    │
│  │   └── lines: List<TextLine>                                │
│  ├── detectedLanguage: String // 检测语言/学科                  │
│  └── confidence: Float        // 整体置信度                    │
└──────────────────────────────────────────────────────────────┘
```

### 8.3 实时帧分析策略

```
CameraX ImageAnalysis
    │
    ├─ 分析频率: 每 500ms 分析一帧 (跳过中间帧)
    │   └─ 原因: ML Kit OCR 非实时消耗大, 500ms 间隔对取景框足够
    │
    ├─ 帧尺寸裁剪: 分析前缩放到 480p (宽边 ≤ 640px)
    │   └─ 平衡识别精度与性能
    │
    ├─ 分析结果:
    │   ├─ 检测到文字 → 更新 GraphicOverlay 边框
    │   │   └─ 边框颜色: 绿 (高置信度) / 黄 (中置信度)
    │   ├─ 未检测到文字 → 清空 GraphicOverlay
    │   └─ ML Kit 识别失败 → 保留上次结果 (不闪烁)
    │
    └─ 性能指标:
        ├─ 单帧分析延迟: < 200ms
        ├─ 内存增长: < 50MB (OCR 模型常驻)
        └─ 电池影响: CameraX 生命周期感知, 仅在预览时工作
```

### 8.4 离线/在线混合策略

```
OcrEngine.analyze()
    │
    ├─ 始终优先使用 ML Kit 本地识别 (零延迟, 离线可用)
    │
    ├─ 本地识别结果可选补充:
    │   ├─ 用户手动点击"增强识别"
    │   │   └─ 上传图片至 POST /api/v1/solve/photo (server-side OCR)
    │   └─ 本地置信度 < 0.5 时自动提示"使用云端识别提升精度"
    │
    └─ 学科预检测:
        ├─ ML Kit 检测文字 → 关键词匹配 → 推测学科
        └─ 自动设置 SubjectSelector 默认值
```

### 8.5 关键类

```
data/vision/
├── OcrEngine.kt                   // OCR 引擎封装
│   ├── class OcrEngine @Inject constructor()
│   ├── suspend fun analyzeImageProxy(imageProxy: ImageProxy): OcrResult
│   ├── suspend fun analyzeBitmap(bitmap: Bitmap): OcrResult
│   └── fun release()              // 释放 ML Kit 资源
│
├── OcrGraphicOverlay.kt           // 取景框叠加层
│   └── class OcrGraphicOverlay(context) : View
│       └── fun update(results: OcrResult)  // 更新文字边框
│
└── SubjectDetector.kt             // 学科预检测器
    └── fun detectSubject(ocrText: String): Subject
        // 基于关键词匹配: "方程"→数学, "F=ma"→物理
```

### 8.6 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| ML Kit 中文识别准确率不足 | 中文字符识别错误 | 加载中文识别增强包；降低期望，定位为辅；拍照上传后服务端 OCR 为主 |
| 手写体识别率低 | 手写题目无法识别 | ML Kit 不支持手写 → 提示用户使用印刷体或上传以触发服务端 OCR |
| 持续 OCR 导致发热/耗电 | 电池消耗快 | 500ms 帧间隔 + 拍照后停止分析；CameraX 生命周期绑定 |
| OCR 模型 APK 体积大 (~8MB) | APK 增长超限 | 使用 ML Kit 按需下载 (Downloadable) 而非捆绑；首次使用提示下载 |

---

## 9. 图片消息渲染组件 (F24)

> PRD 基准：§2.2 F24, §4.4
> 优先级：P1（聊天体验完善）
> 依赖：T4 (聊天界面) ✅, T8 (拍照基础) ✅

### 9.1 组件架构

```
MessageBubble (修改)
│
├── isImageMessage(message) ?  // 消息类型判断
│   ├─ 是 → ImageMessage 组件       ← 新分支
│   └─ 否 → 原有文本渲染
│
└── ImageMessage 组件 (新建)
    ├── 缩略图模式 (聊天列表内)
    │   ├── 最大尺寸: 240dp x 240dp (宽高比保持)
    │   ├── Coil ImageLoader 异步加载 (内存缓存 + 磁盘缓存)
    │   ├── 加载中: placeholder shimmer 动画
    │   ├── 加载成功: Crossfade 淡入
    │   └── 加载失败: 错误占位图 + "重新加载"按钮
    │
    └── 全屏预览模式 (点击后)
        ├── PhotoView (Gesture 库) 
        │   ├── 手势双指缩放 (1x ~ 5x)
        │   ├── 单指拖动平移
        │   └── 双击缩放/还原
        ├── 顶部工具栏: 返回 + 发送时间
        └── 底部: "保存到相册"按钮
```

### 9.2 数据流

```
消息接收/发送
    │
    ├─ 用户发送图片 → 压缩后上传 → 成功后消息写入 Room
    │   └─ message.imageUrl = backend_url
    │
    ├─ AI 回复含图片 → SSE 返回 image_url 字段
    │   └─ message.imageUrl = ai_generated_url
    │
    ├─ ImageMessage 渲染:
    │   ├─ Coil: ImageRequest(url)
    │   │   ├─ memoryCachePolicy = ENABLED
    │   │   ├─ diskCachePolicy = ENABLED
    │   │   ├─ size = 240dp (缩略图) / original (全屏)
    │   │   └─ crossfade(300ms)
    │   └─ 加载成功 → 显示图片
    │
    └─ 异常处理:
        ├─ 网络错误 → 显示错误状态 + 重试按钮
        ├─ URL 过期 → 显示"图片已过期"
        └─ 格式不支持 → 显示"暂不支持此图片格式"
```

### 9.3 缓存策略

```kotlin
// Coil ImageLoader 配置
imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(0.25)      // 占用 25% 应用堆内存
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(50 * 1024 * 1024)  // 50MB 磁盘缓存
            .build()
    }
    .build()

// 缓存清理入口 (与 F31 CacheManager 联动)
class ImageCacheCleaner @Inject constructor(
    private val imageLoader: ImageLoader
) {
    suspend fun clearMemoryCache() = withContext(Dispatchers.Main) {
        imageLoader.memoryCache?.clear()
    }
    suspend fun clearDiskCache() {
        imageLoader.diskCache?.clear()
    }
}
```

### 9.4 关键类

```
ui/screen/chat/components/
├── ImageMessage.kt          // (新建) 图片消息组件
│   └── @Composable fun ImageMessage(
│           url: String,
│           isMine: Boolean,
│           onRetry: () -> Unit,
│           onClick: () -> Unit  // 触发全屏预览
│       )
│
├── PhotoPreviewDialog.kt    // (新建) 全屏预览弹窗
│   └── @Composable fun PhotoPreviewDialog(
│           url: String,
│           onDismiss: () -> Unit
│       )
│
└── MessageBubble.kt         // (修改) 添加图片类型分支
```

### 9.5 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 大图 OOM | App 崩溃 | Coil 缩略图 size 限制 + Glide 自动 downsampling |
| 图片 URL 失效 | 图片不显示 | 展示"图片已过期"占位图；本地缓存图片文件作为 fallback |
| 全屏预览时手势冲突 | 缩放/滑动异常 | PhotoView 库处理手势冲突 + Compose 嵌套滚动协调 |
| 图片缓存占用过多磁盘 | 存储空间不足 | 50MB 上限 + LRU 淘汰 + 与 F31 联动清理 |

---

## 10. 订阅管理数据绑定模块 (F30)

> PRD 基准：§2.2 F30, §4.13
> 优先级：P2（体验完善）
> 依赖：T20 (个人中心) ✅, 后端 Subscription API ✅

### 10.1 数据流

```
SubscriptionScreen
    │
    ├─ 页面加载:
    │   ├─ SubscriptionVM.init()
    │   │   ├─ 读本地缓存 (SubscriptionEntity, DataStore)
    │   │   └─ 并行请求 GET /api/v1/subscription/status
    │   │       ├─ 成功 → 更新本地缓存 + StateFlow
    │   │       └─ 失败 → 使用缓存数据 + 显示静默刷新状态
    │   │
    │   └─ 渲染订阅状态:
    │       ├─ 套餐卡片 (免费/订阅)
    │       ├─ 配额进度条 (daily_used / daily_quota)
    │       ├─ 功能对比表 (动态: 基于后端返回的功能列表)
    │       └─ 订阅按钮/管理按钮
    │
    ├─ 用户操作:
    │   ├─ 点击"订阅" → 跳转外部支付链接
    │   ├─ 点击"管理订阅" → 跳转外部管理页面
    │   └─ 配额用尽 → 弹窗引导订阅
    │
    └─ 配额状态全局共享:
        └─ SubscriptionRepository.subscriptionState: StateFlow<SubscriptionState>
            ├─ ChatViewModel 读取 → 超配额时禁用发送
            ├─ CameraViewModel 读取 → 超配额时禁用拍照解题
            └─ VoiceRepository 读取 → 超配额时禁用云端语音
```

### 10.2 接口定义

```
GET /api/v1/subscription/status
Authorization: Bearer ***

Response:
{
  "plan_type": "free",          // free / premium / trial
  "status": "active",           // active / expired / cancelled / grace_period
  "features": {
    "chat": {"enabled": true, "limit": 100, "used": 23},
    "solve_photo": {"enabled": true, "limit": 5, "used": 3},
    "voice_asr_cloud": {"enabled": false, "limit": 0, "used": 0},
    "voice_tts_cloud": {"enabled": false, "limit": 0, "used": 0},
    "dashboard": {"enabled": true, "limit": null, "used": null},
    "quiz": {"enabled": true, "limit": null, "used": null},
    "review": {"enabled": true, "limit": null, "used": null},
    "gamification": {"enabled": true, "limit": null, "used": null}
  },
  "daily_quota": {
    "total_queries": 100,
    "used_queries": 23,
    "reset_at": "2026-05-16T00:00:00+08:00"
  },
  "valid_until": null,          // premium 套餐有截止日期
  "trial_available": true       // 是否可试用
}
```

### 10.3 配额拦截架构

```
┌────────────────────────────────────────────────────────────────┐
│                  全局配额守卫 (QuotaGuard)                       │
│                                                                │
│  SubscriptionRepository                                        │
│  ├── subscriptionState: StateFlow<SubscriptionState>           │
│  ├── fun checkQuota(feature: FeatureType): QuotaResult         │
│  │   ├── QuotaResult.Allowed                                   │
│  │   ├── QuotaResult.Exceeded(resetAt)                         │
│  │   └── QuotaResult.Disabled(feature)                         │
│  └── fun consumeQuota(feature: FeatureType)                    │
│       // 调用后端 POST /api/v1/subscription/consume            │
│                                                                │
│  消费拦截点:                                                    │
│  ├── ChatViewModel.sendMessage() → checkQuota("chat")          │
│  ├── SolveRepository.solvePhoto() → checkQuota("solve_photo")  │
│  ├── CloudAsrEngine → checkQuota("voice_asr_cloud")            │
│  └── CloudTtsEngine → checkQuota("voice_tts_cloud")            │
└────────────────────────────────────────────────────────────────┘
```

### 10.4 本地缓存模型

```kotlin
// Room Entity — 本地缓存订阅快照
@Entity(tableName = "subscription_cache")
data class SubscriptionCacheEntity(
    @PrimaryKey val id: String = "subscription",
    val planType: String,            // free / premium / trial
    val status: String,              // active / expired / ...
    val featuresJson: String,        // JSON: features map
    val dailyQuotaTotal: Int,        
    val dailyQuotaUsed: Int,
    val validUntil: Long?,           
    val updatedAt: Long
)

// DataStore 偏好 — 配额计数器 (离线可用)
// 键: "quota_chat_used", "quota_solve_used", ...
// 值: 当日已使用次数
// 每日 00:00 自动重置 (App 启动时检查日期)
```

### 10.5 关键类

```
ui/screen/subscription/
├── SubscriptionScreen.kt        (修改: 绑定真实数据)
├── SubscriptionViewModel.kt     (修改: API 对接)
└── components/
    ├── PlanCard.kt              (套餐卡片)
    ├── QuotaProgressBar.kt      (配额进度条)
    ├── FeatureComparisonTable.kt (功能对比表)
    └── QuotaExceededDialog.kt   (配额用尽弹窗)

data/
├── remote/
│   └── api/SubscriptionApi.kt   (新建: GET status + POST consume)
├── local/
│   ├── dao/SubscriptionCacheDao.kt  (新建)
│   └── entity/SubscriptionCacheEntity.kt (新建)
├── repository/
│   └── SubscriptionRepository.kt (新建: 接口+实现)
└── model/
    └── QuotaGuard.kt             (新建: 全局配额守卫)

domain/
└── model/
    ├── SubscriptionState.kt     (新建: 订阅领域模型)
    └── FeatureType.kt           (新建: 功能类型枚举)
```

### 10.6 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 后端订阅 API 不可用 | 页面显示空数据 | 本地缓存兜底 + 显示"数据加载中"状态 |
| 配额计数漂移 (本地≠后端) | 功能提前/延迟锁定 | 每次 API 请求同步最新配额；本地为"近似值" |
| 用户多设备同步延迟 | 配额不同步 | 配额在服务端严格计数；客户端仅展示参考值 |
| 免费版用户看到付费功能入口 | 困惑 | 后端 features 控制开关；客户端按 enabled 字段渲染 |

---

## 11. 缓存清理与本地通知模块 (F31 + F32)

> PRD 基准：§2.2 F31, F32
> 优先级：P2（体验完善）
> 依赖：T7 (Room) ✅, T17 (设置) ✅

### 11.1 F31 缓存清理逻辑

#### 缓存分类与清理策略

| 缓存类型 | 存储位置 | 清理动作 | 清理后影响 |
|---------|---------|---------|-----------|
| 图片缓存 (Coil) | DiskCache ~/image_cache/ | clear() | 下次加载重新下载 |
| 图片内存缓存 | Coil MemoryCache | clear() | 下次加载重新解码 |
| OCR 模型文件 | ML Kit Downloadable | release() | 下次 OCR 触发重新下载 |
| Room 数据库 WAL | database/*-wal, *-shm | checkpoint+wal关闭 | 无数据丢失 |
| 日志文件 | logs/*.txt | deleteRecursively() | 调试日志丢失 |
| Temp 文件 | cacheDir/temp/* | deleteRecursively() | 无影响 |
| 用户偏好 (DataStore) | DataStore 文件 | ❌ 不清理 | 不清除用户设置 |
| 消息历史 (Room) | database/*.db | ❌ 不清理 | 需用户主动"清空对话" |

```kotlin
class CacheManager @Inject constructor(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val ocrEngine: OcrEngine,
    private val database: AppDatabase       // Room
) {
    data class CacheSize(
        val imageDisk: Long,                // 字节
        val imageMemory: Long,
        val logs: Long,
        val temp: Long,
        val total: Long
    )
    
    suspend fun calculateSize(): CacheSize {
        // 遍历各目录计算 → CacheSize
    }
    
    suspend fun clearAll(onProgress: (Float) -> Unit = {}) {
        onProgress(0.1f)
        withContext(Dispatchers.IO) {
            // 1. 清图片磁盘缓存
            imageLoader.diskCache?.clear()
            onProgress(0.3f)
            
            // 2. 清图片内存缓存 (必须在 MainThread)
            withContext(Dispatchers.Main) {
                imageLoader.memoryCache?.clear()
            }
            onProgress(0.5f)
            
            // 3. 释放 OCR 模型 (下次用时自动下载)
            ocrEngine.release()
            onProgress(0.7f)
            
            // 4. 清理日志
            context.cacheDir.resolve("logs").deleteRecursively()
            onProgress(0.85f)
            
            // 5. 清理临时文件
            context.cacheDir.resolve("temp").deleteRecursively()
            onProgress(0.95f)
            
            // 6. Room WAL checkpoint
            database.query("PRAGMA wal_checkpoint(FULL)", null)
            onProgress(1.0f)
        }
    }
}
```

#### UI 交互

```
SettingsScreen
└── "清除缓存" 行
    ├── 显示: "缓存占用: X.X MB" (首次进入设置时计算)
    ├── 点击: 弹出确认弹窗
    │   ├── "清除缓存后将重新下载图片和OCR模型"
    │   └── [取消] [确认清除]
    ├── 确认: 
    │   ├─ 显示进度: "正在清除... 50%"
    │   ├─ 完成后显示 Toast "已清除 X.X MB 缓存"
    │   └─ 更新缓存占用显示为 "0 B"
    └── 只在首次显示或手动刷新时计算 (避免每次打开设置都计算)
```

### 11.2 F32 本地通知方案

#### 通知渠道定义

```kotlin
object NotificationChannels {
    const val CHANNEL_MESSAGE = "new_message"       // 新消息通知
    const val CHANNEL_REVIEW = "review_reminder"    // 复习提醒
    const val CHANNEL_SYSTEM = "system"             // 系统/配额通知

    fun create(context: Context) {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_MESSAGE,
                "新消息", 
                NotificationManager.IMPORTANCE_HIGH   // 弹出 + 声音
            ).apply {
                description = "AI 回复新消息到达提醒"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_REVIEW,
                "复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT  // 无声音
            ).apply {
                description = "间隔重复复习时间到达提醒"
            },
            NotificationChannel(
                CHANNEL_SYSTEM,
                "系统通知",
                NotificationManager.IMPORTANCE_LOW      // 静默
            ).apply {
                description = "配额用尽、系统维护等"
            }
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(channels)
    }
}
```

#### 新消息通知流程

```
SSE 流式输出完成 (完整消息写入 Room)
    │
    ├─ 前提条件:
    │   ├─ App 在后台 (非前台可见)
    │   └─ 应用进程存活 (非 killed)
    │
    ├─ NotificationHelper.sendMessageNotification()
    │   ├─ 通知文案:
    │   │   ├─ 标题: "AI 学伴"
    │   │   ├─ 内容: 消息前 60 字 + "..."
    │   │   ├─ 图标: app icon
    │   │   └─ 频道: CHANNEL_MESSAGE
    │   ├─ Intent PendingIntent:
    │   │   ├─ 点击 → 打开 MainActivity
    │   │   ├─ data: ?conversation_id={id}
    │   │   └─ flags: FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP
    │   └─ 通知 ID: conversation_id.hashCode() (避免重复创建)
    │
    └─ 通知点击后:
        ├─ 打开 App → 跳转到对应会话
        └─ 清除该会话的通知 (NotificationManager.cancel(id))
```

#### 复习提醒通知流程

```
SpacedRepetitionEngine 每天启动时扫描
    │
    ├─ 查询: SELECT * FROM wrong_answers WHERE next_review_at <= now()
    │
    ├─ 有待复习题目?
    │   ├─ 是 → NotificationHelper.sendReviewReminder()
    │   │   ├─ 标题: "复习提醒"
    │   │   ├─ 内容: "你有 {N} 道题待复习"
    │   │   ├─ 频道: CHANNEL_REVIEW
    │   │   └─ 点击 → 打开 ReviewScreen
    │   └─ 否 → 跳过
    │
    └─ 每天仅发送一次 (避免重复打扰)
```

#### 关键类

```
data/local/
├── CacheManager.kt              (新建: 缓存清理管理器)
├── NotificationHelper.kt        (新建/增强: 通知辅助类)
└── NotificationChannels.kt      (新建: 通知渠道定义)

ui/screen/settings/
└── SettingsScreen.kt            (修改: 添加清除缓存 + 通知设置入口)

AndroidManifest.xml              (修改: POST_NOTIFICATIONS 权限声明)
```

### 11.3 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Android 13+ POST_NOTIFICATIONS 运行时权限 | 通知无法弹出 | 适配动态权限请求；拒绝后降级为无通知体验 |
| 用户清理缓存时 App 正在加载图片 | 图片加载失败 | 缓存清理时机: 无活跃下载时；清理后 ImageLoading 自动回源 |
| 通知重复 (同会话多条消息) | 通知栏被刷屏 | 按 conversation_id 聚合通知 (update 替换)；仅保留最新一条 |
| App 被系统杀掉后无法收到通知 | 复习/消息提醒失效 | WorkManager 定时检查 + 启动时恢复错过的提醒 |

---

## 12. 游戏化系统 (F46)

> PRD 基准：§2.2 F46, §9.1 F46
> 优先级：P2（体验增强）
> 依赖：T14 (仪表盘数据) ✅, Room (学习统计)

### 12.1 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│  事件收集层                                                          │
│                                                                     │
│  每次学习行为 → EventBus / AnalyticsEngine → GamificationEngine     │
│  ├─ MessageSent         (发送消息/提问)                              │
│  ├─ SolveCompleted      (完成解题)                                   │
│  ├─ QuizCompleted       (完成测验)                                   │
│  ├─ ReviewCompleted     (完成复习)                                   │
│  ├─ AppOpened           (每日打开 App)                               │
│  └─ StreakMaintained    (连胜维持, 每日自动)                          │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  引擎层 (domain/engine/)                                             │
│                                                                     │
│  GamificationEngine                                                  │
│  ├── fun onEvent(event: LearningEvent)                              │
│  │   ├── 更新积分: calculateScore(event)                             │
│  │   ├── 检测成就: checkAchievements(userStats)                      │
│  │   └── 更新连胜: updateStreak()                                    │
│  │                                                                   │
│  ├── fun getLeaderboard(type: LeaderboardType): Flow<List<RankEntry>>│
│  │   └── 本地排名 + 云端同步 (按学习积分)                            │
│  │                                                                   │
│  └── ScoreCalculator (纯函数)                                        │
│      ├── 解题: +10 分                                                │
│      ├── 对话: +2 分/条                                              │
│      ├── 测验: +20 分                                                │
│      ├── 复习: +15 分                                                │
│      └── 连胜加成: +5 分 × streak_days                              │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  持久化层 (data/local/)                                              │
│                                                                     │
│  Room 实体:                                                          │
│  ├── AchievementEntity     (成就列表, 已解锁/未解锁)                  │
│  ├── UserScoreEntity       (用户积分 + 连胜数据)                      │
│  └── ScoreLogEntity        (积分变更日志, 用于回滚/审计)               │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  UI 层 (ui/screen/dashboard/components/)                             │
│                                                                     │
│  ├── AchievementBadge.kt    (成就徽章网格)                            │
│  ├── StreakIndicator.kt     (连胜火焰指示器)                          │
│  └── LeaderboardView.kt     (排行榜页面)                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 12.2 成就系统设计

#### 预置成就列表 (MVP)

```kotlin
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,          // Emoji 或 Drawable 资源名
    val condition: AchievementCondition
) {
    FIRST_SOLVE(
        id = "first_solve",
        title = "初次解题",
        description = "完成第一次拍照解题",
        icon = "📷",
        condition = AchievementCondition.SolveCount(1)
    ),
    SOLVE_MASTER(
        id = "solve_master",
        title = "解题达人",
        description = "累计完成 100 次拍照解题",
        icon = "🏆",
        condition = AchievementCondition.SolveCount(100)
    ),
    STREAK_7(
        id = "streak_7",
        title = "持之以恒",
        description = "连续学习 7 天",
        icon = "🔥",
        condition = AchievementCondition.StreakDays(7)
    ),
    STREAK_30(
        id = "streak_30",
        title = "学霸养成",
        description = "连续学习 30 天",
        icon = "💎",
        condition = AchievementCondition.StreakDays(30)
    ),
    QUIZ_100(
        id = "quiz_100",
        title = "答题王者",
        description = "累计答对 100 道测验题",
        icon = "🎯",
        condition = AchievementCondition.QuizCorrectCount(100)
    ),
    QUIZ_PERFECT(
        id = "quiz_perfect",
        title = "满分选手",
        description = "完成一次全对测验",
        icon = "💯",
        condition = AchievementCondition.QuizPerfect()
    ),
    REVIEW_MASTER(
        id = "review_master",
        title = "温故知新",
        description = "完成 50 次复习",
        icon = "📖",
        condition = AchievementCondition.ReviewCount(50)
    ),
    SCHOLAR(
        id = "scholar",
        title = "学霸称号",
        description = "累计积分达到 5000 分",
        icon = "👑",
        condition = AchievementCondition.TotalScore(5000)
    );
    
    // 解锁后弹窗: achievement_dialog 显示标题+描述+icon+动画
}
```

#### 成就检测机制

```
GamificationEngine.onEvent(event)
    │
    ├─ 加载所有成就列表 (AchievementDao.getAll())
    │
    ├─ 过滤未解锁成就 (status = LOCKED)
    │
    ├─ 对每个未解锁成就:
    │   ├─ 读取当前用户统计数据 (UserStats)
    │   └─ condition.isMet(userStats) ?
    │       ├─ 是 → 解锁成就
    │       │   ├─ AchievementDao.updateStatus(id, UNLOCKED)
    │       │   ├─ 触发成就解锁弹窗 (SharedFlow → UI)
    │       │   └─ 记录解锁时间
    │       └─ 否 → 跳过
    │
    └─ 成就弹窗展示逻辑:
        ├─ 同时解锁多个 → 队列展示 (间隔 1.5s)
        ├─ 弹窗动画: 缩放 + 光效 (Compose Animation)
        ├─ 点击弹窗 → 跳转到成就页
        └─ 3s 自动消失
```

### 12.3 连胜机制

```kotlin
// 连胜计算逻辑 (纯 Kotlin, domain/engine 层)
class StreakCalculator {
    fun calculateStreak(learningDates: List<LocalDate>): StreakResult {
        // 1. 按日期降序排列 (最近在前的去重)
        // 2. 从今天往前数连续天数
        // 3. 中断条件: 某一天无学习记录
        // 4. 今日未学习 → streak 不变 (不算中断, 不加天数)
        
        val sorted = learningDates.distinct().sortedDescending()
        var streak = 0
        val today = LocalDate.now()
        
        if (sorted.isNotEmpty()) {
            // 从今天开始检查连续性
            var current = today
            for (date in sorted) {
                if (date == current) {
                    streak++
                    current = current.minusDays(1)
                } else if (date < current) {
                    break // 中断
                }
            }
        }
        
        return StreakResult(
            currentStreak = streak,
            longestStreak = loadLongestStreak(sorted),
            hasLearnedToday = today in sorted
        )
    }
}

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val hasLearnedToday: Boolean
)
```

#### 连胜 UI

```
StreakIndicator.kt
├── 显示位置:
│   ├─ DashboardScreen 概览卡片
│   └─ 主要: 连胜天数 + 火焰图标
│
├── 状态渲染:
│   ├─ streak ≥ 30: 🔥🔥🔥 + "N 天" (金色)
│   ├─ streak ≥ 7:  🔥🔥 + "N 天" (红色)
│   ├─ streak ≥ 3:  🔥 + "N 天" (橙色)
│   ├─ streak = 0:  "今天开始学习吧！"
│   └─ 连胜归零时: 显示倒计时 "已中断"
│
└── 每日首次打开 App:
    ├─ 连胜检测
    ├─ streak++ 时显示 "🎉 已连续学习 N 天"
    └─ 连胜中断时显示 "连胜中断，重新开始！"
```

### 12.4 排行榜设计

```
LeaderboardView.kt
│
├─ 排行榜类型:
│   ├─ 全局排行榜 (所有用户在云端排名, 每日更新)
│   └─ 好友排行榜 (预留, 需要好友系统)
│
├─ 数据来源:
│   ├─ 本地: LeaderboardEntity (Room 缓存)
│   └─ 云端: GET /api/v1/game/leaderboard
│
├─ 排名条目:
│   ├─ rank: Int          // 排名
│   ├─ nickname: String   // 昵称
│   ├─ score: Int         // 总积分
│   ├─ level: String?     // 等级 (预留)
│   └─ isMe: Boolean      // 是否当前用户
│
├─ 交互:
│   ├─ 自己排名始终在列表顶部高亮
│   ├─ 下拉刷新
│   └─ 点击用户 → 查看其公开成就 (预留)
│
└─ 数据同步:
    ├─ 本地积分变化 → 批量同步到云端
    ├─ 排行榜每日 00:00 刷新
    └─ 全局仅显示 Top 100
```

### 12.5 接口定义

```
GET /api/v1/game/leaderboard?type=global&limit=50
Authorization: Bearer ***

Response:
{
  "my_rank": 42,
  "total_users": 1250,
  "leaderboard": [
    {"rank": 1, "nickname": "学霸小明", "score": 8250, "avatar_url": "..."},
    {"rank": 2, "nickname": "物理达人", "score": 7900, "avatar_url": "..."},
    {"rank": 42, "nickname": "我", "score": 2340, "avatar_url": "..."}
  ],
  "updated_at": "2026-05-15T00:00:00Z"
}

POST /api/v1/game/sync/score
{
  "total_score": 2340,
  "current_streak": 7,
  "longest_streak": 14,
  "achievements_unlocked": ["first_solve", "streak_7"],
  "date": "2026-05-15"
}

Response:
{
  "synced": true,
  "global_rank": 42,
  "new_achievements": []     // 云端检测到的成就（预留）
}
```

### 12.6 Room 实体模型

```kotlin
// 成就
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,           // "first_solve"
    val title: String,
    val description: String,
    val iconName: String,
    val status: String,                    // LOCKED / UNLOCKED
    val unlockedAt: Long?,
    val progress: Float = 0f              // 解锁进度 0.0~1.0
)

// 用户积分
@Entity(tableName = "user_scores")
data class UserScoreEntity(
    @PrimaryKey val id: String = "user_score",
    val totalScore: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastLearningDate: String?,         // "2026-05-15"
    val updatedAt: Long
)

// 积分变更日志 (用于审计/回滚)
@Entity(tableName = "score_logs")
data class ScoreLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: String,                 // SOLVE / CHAT / QUIZ / REVIEW / STREAK
    val scoreChange: Int,                  // +10 / -0
    val balanceAfter: Int,                 // 变更后余额
    val createdAt: Long
)

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE status = 'LOCKED'")
    fun getLockedAchievements(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements ORDER BY status ASC, id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>
    
    @Query("UPDATE achievements SET status = 'UNLOCKED', unlockedAt = :now WHERE id = :id")
    suspend fun unlock(id: String, now: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(achievement: AchievementEntity)
    
    @Query("SELECT COUNT(*) FROM achievements WHERE status = 'UNLOCKED'")
    fun getUnlockedCount(): Flow<Int>
}
```

### 12.7 关键类

```
domain/engine/
├── GamificationEngine.kt       (新建: 游戏化引擎)
└── StreakCalculator.kt         (新建: 连胜计算器)

data/
├── local/
│   ├── dao/AchievementDao.kt   (新建)
│   ├── dao/UserScoreDao.kt     (新建)
│   ├── dao/ScoreLogDao.kt      (新建)
│   ├── entity/AchievementEntity.kt (新建)
│   ├── entity/UserScoreEntity.kt   (新建)
│   └── entity/ScoreLogEntity.kt    (新建)
├── remote/
│   └── api/GamificationApi.kt  (新建)
└── repository/
    └── GamificationRepository.kt (新建: 接口+实现)

ui/screen/dashboard/components/
├── AchievementBadge.kt         (新建: 成就徽章网格)
├── StreakIndicator.kt          (新建: 连胜火焰指示器)
└── LeaderboardView.kt          (新建: 排行榜)

domain/model/
└── Achievement.kt              (新建: 成就领域枚举)
```

### 12.8 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 游戏化数据无法云端同步 | 本地重置后积分丢失 | 每次修改后也同步到云端；本地优先+云端备份 |
| 成就条件误判导致提前解锁 | 逻辑缺陷 | 条件检查在 domain 层纯函数实现，无副作用 |
| 排行榜数据滞后 | 排名不准确 | 每日批量更新；客户端进度条显示"今日更新" |
| 游戏化导致用户过度关注分数 | 偏离学习目标 | 成就侧重于学习行为本身 (连续学习) 而非竞争 |
| 积分计算与云端不一致 | 本地与云端分歧 | 云端为主导；每次 sync 时以云端数据覆盖本地 |

---

## 附录 A 补充: 新增文件清单 (F16/F18/F20/F23/F24/F30/F31/F32/F46)

以下文件为上述 §7-§12 各模块的新增文件，与现有附录 A (F40-F45) 合并构成完整清单：

| 文件路径 | 所属模块 | 说明 |
|---------|---------|------|
| `domain/model/VoiceState.kt` | F16/F18/F20 | 统一语音状态机定义 |
| `domain/model/SubscriptionState.kt` | F30 | 订阅领域模型 |
| `domain/model/FeatureType.kt` | F30 | 功能类型枚举 |
| `domain/model/Achievement.kt` | F46 | 成就领域枚举 |
| `domain/engine/GamificationEngine.kt` | F46 | 游戏化引擎 |
| `domain/engine/StreakCalculator.kt` | F46 | 连胜计算器 |
| `data/media/CloudAsrEngine.kt` | F16 | 云端 ASR 调用封装 |
| `data/media/CloudTtsEngine.kt` | F18 | 云端 TTS 流式封装 |
| `data/media/TtsAudioPlayer.kt` | F18 | AudioTrack 播放器 |
| `data/media/AsrFallbackStrategy.kt` | F16 | ASR 降级判定逻辑 |
| `data/media/NoiseSuppression.kt` | F47 (可选) | 环境自适应降噪 |
| `data/vision/OcrEngine.kt` | F23 | ML Kit OCR 引擎封装 |
| `data/vision/OcrGraphicOverlay.kt` | F23 | OCR 取景框叠加层 |
| `data/vision/SubjectDetector.kt` | F23 | 学科预检测器 |
| `data/local/CacheManager.kt` | F31 | 缓存清理管理器 |
| `data/local/NotificationHelper.kt` | F32 | 本地通知辅助类 |
| `data/local/NotificationChannels.kt` | F32 | 通知渠道定义 |
| `data/local/dao/SubscriptionCacheDao.kt` | F30 | 订阅缓存 DAO |
| `data/local/dao/AchievementDao.kt` | F46 | 成就 DAO |
| `data/local/dao/UserScoreDao.kt` | F46 | 用户积分 DAO |
| `data/local/dao/ScoreLogDao.kt` | F46 | 积分日志 DAO |
| `data/local/entity/SubscriptionCacheEntity.kt` | F30 | 订阅缓存 Entity |
| `data/local/entity/AchievementEntity.kt` | F46 | 成就 Entity |
| `data/local/entity/UserScoreEntity.kt` | F46 | 积分 Entity |
| `data/local/entity/ScoreLogEntity.kt` | F46 | 积分日志 Entity |
| `data/remote/api/SubscriptionApi.kt` | F30 | 订阅 API |
| `data/remote/api/GamificationApi.kt` | F46 | 游戏化 API (排行榜/同步) |
| `data/repository/SubscriptionRepository.kt` | F30 | 订阅仓库 (接口+实现) |
| `data/repository/GamificationRepository.kt` | F46 | 游戏化仓库 (接口+实现) |
| `data/model/QuotaGuard.kt` | F30 | 全局配额守卫 |
| `ui/screen/chat/components/ImageMessage.kt` | F24 | 图片消息组件 |
| `ui/screen/chat/components/PhotoPreviewDialog.kt` | F24 | 全屏图片预览 |
| `ui/screen/dashboard/components/AchievementBadge.kt` | F46 | 成就徽章组件 |
| `ui/screen/dashboard/components/StreakIndicator.kt` | F46 | 连胜指示器 |
| `ui/screen/dashboard/components/LeaderboardView.kt` | F46 | 排行榜组件 |

### 修改文件清单

| 文件路径 | 所属模块 | 修改内容 |
|---------|---------|---------|
| `data/media/VoiceRepository.kt` | F16/F18/F20 | 降级逻辑 + 打断协调 |
| `ui/screen/chat/components/VoiceInputBar.kt` | F20 | 打断交互流畅化 |
| `ui/screen/chat/components/MessageBubble.kt` | F24 | 添加图片类型分支 |
| `ui/screen/chat/ChatViewModel.kt` | F30 | 接入 QuotaGuard 配额检查 |
| `ui/screen/camera/CameraScreen.kt` | F23 | ML Kit OCR 叠加层集成 |
| `ui/screen/settings/SettingsScreen.kt` | F31/F32 | 添加清除缓存 + 通知入口 |
| `ui/screen/subscription/SubscriptionScreen.kt` | F30 | 绑定真实后端数据 |
| `ui/screen/subscription/SubscriptionViewModel.kt` | F30 | API 对接 |
| `AndroidManifest.xml` | F32 | POST_NOTIFICATIONS 权限声明 |
| `ui/screen/dashboard/DashboardScreen.kt` | F46 | 集成成就/连胜组件 |

---

## 附录 B 补充: 完整模块依赖关系总图 (F40-F46)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     F40-F46 完整模块依赖关系 (含 P1/P2)                      │
│                                                                             │
│  ┌──────────────────────┐                                                    │
│  │  现有架构 (P0基础)    │                                                    │
│  │  ├─ 认证/会话/消息   │                                                    │
│  │  └─ T4 T5 T7 T8 T12  │                                                    │
│  │    T13 T14 T17 T20   │                                                    │
│  └──────────┬───────────┘                                                    │
│             │                                                                │
│  ┌──────────┴─────────────────────────────────────────────────────────────┐ │
│  │  Sprint 1: P0 核心补齐 (可并行)                                        │ │
│  │                                                                        │ │
│  │  ┌──────────────────────────────────────────┐                           │ │
│  │  │ T22 — F40 拍照解题增强                    │                           │ │
│  │  │ 依赖: T5(SSE) + T8(拍照) + F23(OCR)       │                           │ │
│  │  │ 产出: SubjectSelector + SolveStreamParser │                           │ │
│  │  └──────────────────────────────────────────┘                           │ │
│  │                                                                        │ │
│  │  ┌──────────────────────────────────────────┐                           │ │
│  │  │ T23 — F41 自适应分步讲解                  │                           │ │
│  │  │ 依赖: T4(聊天) + T5(SSE) + UserProfile   │                           │ │
│  │  │ 产出: CollapsibleStepCard + Difficulty   │                           │ │
│  │  └──────────────────────────────────────────┘                           │ │
│  │                                                                        │ │
│  │  ┌──────────────────────────────────────────┐                           │ │
│  │  │ T24 — F42 苏格拉底式教学                  │                           │ │
│  │  │ 依赖: T4 + T5 + T14(仪表盘数据)          │                           │ │
│  │  │ 产出: SocraticBanner + TeachingState     │                           │ │
│  │  └──────────────────────────────────────────┘                           │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│             │                                                                │
│  ┌──────────┴─────────────────────────────────────────────────────────────┐ │
│  │  Sprint 2: P1 语音完善 + 图片消息                                      │ │
│  │                                                                        │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │ │
│  │  │ T25 — F16    │  │ T26 — F18+F20│  │ T27 — F24   │                  │ │
│  │  │ 云端ASR备选  │  │ 云端TTS+打断 │  │ 图片消息渲染 │                  │ │
│  │  │ 依赖: T12    │  │ 依赖: T13    │  │ 依赖: T4+T8 │                  │ │
│  │  │ CloudAsrEngine│ │ CloudTts    │  │ ImageMsg    │                  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│             │                                                                │
│  ┌──────────┴─────────────────────────────────────────────────────────────┐ │
│  │  Sprint 3: P2 体验补齐 + 游戏化                                        │ │
│  │                                                                        │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │ │
│  │  │ T28 — F30    │  │ T29 — F31+F32│  │ T30 — F46   │                  │ │
│  │  │ 订阅绑定     │  │ 缓存+通知    │  │ 游戏化系统  │                  │ │
│  │  │ 依赖: T20    │  │ 依赖: T7+T17 │  │ 依赖: T14   │                  │ │
│  │  │ SubApi+Guard │  │ Cache+Notify │  │ GamifEngine │                  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                  │ │
│  │                                                                        │ │
│  │  ┌──────────────┐                                                      │ │
│  │  │ T31 — F47    │  (可选 Sprint 4)                                     │ │
│  │  │ 语音增强     │                                                      │ │
│  │  │ 依赖: T12+T13│                                                      │ │
│  │  └──────────────┘                                                      │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  并行策略:                                                                   │
│  ├─ Sprint 1: T22/T23/T24 无互依赖 → 2 Coder 并行                           │
│  ├─ Sprint 2: T25/T26/T27 无互依赖 → 2 Coder 并行                           │
│  └─ Sprint 3: T28/T29/T30 无互依赖 → 2 Coder 并行                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 13. AI Agent 能力增强模块 (F48)

> 版本：v2.0 新增 | 优先级：P0 | 后端依赖：new-api 原生 tool calling 能力

### 13.1 概述

AI Agent 增强的核心是：**在现有 SSE 流式对话基础上，增加 Tool Calling 支持**，使 AI 能够调用联网搜索、计算器等工具获取实时信息，并展示思考过程。

关键设计原则：
- **兼容现有架构**：复用现有的 POST /v1/chat/completions SSE 流式通道
- **客户端执行工具**：工具执行在 Android 端进行（web_search 通过 HTTP，calculator 本地计算）
- **标准 OpenAI 协议**：利用 new-api 原生的 `tool_calls` SSE 字段，后端透传即可
- **渐进式切换**：Agent 模式可开关，默认关闭，保留原有非 Agent 模式

### 13.2 架构总览

```text
                    ┌──────────────────────────────────────┐
                    │  Agent 模式（v2.0 新增）              │
                    │  ┌────────┐ ┌────────┐ ┌───────────┐ │
                    │  │Agent   │ │Tool    │ │AgentThought│ │
                    │  │Switch  │ │Registry│ │Bubble     │ │
                    │  └────┬───┘ └───┬────┘ └─────┬─────┘ │
                    │       │         │             │        │
                    │  ┌────▼─────────▼─────────────▼─────┐ │
                    │  │     AgentStateMachine            │ │
                    │  │  IDLE→THINKING→SEARCHING→       │ │
                    │  │  REASONING→RESPONDING→IDLE      │ │
                    │  └────────────────┬─────────────────┘ │
                    │                   │                    │
                    │  ┌────────────────▼─────────────────┐ │
                    │  │     ToolExecEngine                │ │
                    │  │  ├─ WebSearchClient (HTTP)       │ │
                    │  │  └─ Calculator (本地)            │ │
                    │  └───────────────┬──────────────────┘ │
                    └──────────────────┼────────────────────┘
                                       │
  ═════════════════════════════════════╪═══════════════════ 边界
                                       │
  ┌────────────────────────────────────▼────────────────────┐
  │  Backend (透传，v2.0 小幅修改)                            │
  │  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
  │  │ POST /v1/    │→ │ httpx 流式  │→ │ new-api       │  │
  │  │ chat/        │  │ 转发        │  │ (原生支持     │  │
  │  │ completions  │  │ (工具参数   │  │  tool_calls)  │  │
  │  │ +tools参数  │  │ 透传)       │  │               │  │
  │  └──────────────┘  └─────────────┘  └───────────────┘  │
  └─────────────────────────────────────────────────────────┘
```

### 13.3 Agent 状态机设计

```text
       用户发送消息
           │
           ▼
   ┌──────────────┐
   │    IDLE      │ ←─────────── 流结束回到空闲
   └──────┬───────┘
          │ 开始请求LLM
          ▼
   ┌──────────────┐      SSE 收到 tool_calls 字段
   │  THINKING    │ ────────────────────────────────►
   │  (思考中)     │
   └──────────────┘
          │ SSE 收到 content delta（无 tool_calls）
          ▼                              ┌──────────────┐
   ┌──────────────┐                      │  SEARCHING   │
   │  RESPONDING  │                      │  (工具执行中) │
   │  (流式输出)   │                      └──────┬───────┘
   └──────┬───────┘                             │ 工具执行完成
          │ stream complete    ┌──────────────┐  │
          │ 或 [DONE]          │  REASONING   │◄─┘
          ▼                    │  (等待AI推理) │
   ┌──────────────┐            └──────┬───────┘
   │    IDLE      │                   │ 再次 SSE 接收回复
   └──────────────┘                   ▼
                              ┌──────────────┐
                              │  RESPONDING  │───► IDLE
                              └──────────────┘
```

**AgentState** 枚举值：

| 状态 | UI 展示 | 说明 |
|------|---------|------|
| IDLE | 正常对话界面 | 非 Agent 状态，等待用户输入 |
| THINKING | "AI 正在思考..." 动画 | 请求已发出，等待 SSE 第一帧 |
| SEARCHING | "正在搜索：关键词" + 搜索动画 | SSE 收到 tool_calls，客户端正在执行工具 |
| REASONING | "AI 正在推理..." | 工具结果已提交回 LLM，等待推理回复 |
| RESPONDING | 流式内容逐字展示 | SSE content delta 正在输出 |

### 13.4 Tool Calling 消息模型

**新增消息类型：** `MessageType.AGENT_STEP` 和 SSE 端扩展支持 tool_calls。

#### 13.4.1 扩展 ChatMessage 模型

```kotlin
// domain/model/ChatMessage.kt — 新增字段
data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val contentType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val metadata: String? = null,
    // v2.0 新增：
    val agentStepType: AgentStepType? = null,  // Agent 步骤类型
    val toolName: String? = null,              // 工具名称（如 web_search）
    val toolQuery: String? = null,             // 工具的输入参数
    val toolResult: String? = null             // 工具的返回结果
)

// 新增枚举
enum class AgentStepType {
    THOUGHT,       // AI 思考过程
    TOOL_CALL,     // 调用工具
    TOOL_RESULT,   // 工具返回结果
    OBSERVATION    // AI 对结果的观察
}

// domain/model/ToolDefinition.kt — 新增
data class ToolDefinition(
    val name: String,              // web_search, calculator
    val description: String,       // 功能描述
    val parameters: Map<String, Any>  // JSON Schema 参数定义
)
```

#### 13.4.2 SSE 流新增字段解析

现有 Android 端 `ChatStreamApi` 只提取 `delta.content`。v2.0 需要同时解析 `delta.tool_calls`：

```text
SSE chunk (新增字段示例):
data: {
  "id": "chatcmpl-xxx",
  "choices": [{
    "index": 0,
    "delta": {
      "content": null,
      "tool_calls": [{
        "index": 0,
        "id": "call_xxx",
        "type": "function",
        "function": {
          "name": "web_search",
          "arguments": "{\"query\":\"2026年高考数学大纲\"}"
        }
      }]
    }
  }]
}
```

**客户端解析逻辑（ChatCompletionChunk 扩展）：**

```text
SSE line → JSON parse → ChatCompletionChunkV2
  ├── choices[0].delta.content != null → 正常流式文本
  └── choices[0].delta.tool_calls != null → 工具调用指令
       ├── 发送 AgentStep(TOOL_CALL) 消息
       ├── ToolExecEngine 执行工具
       ├── 工具完成后：
       │   ├── 发送 AgentStep(TOOL_RESULT) 消息
       │   └── 构建 tool_result 消息体，通过新 SSE 连接发送回 LLM
       └── 等待新 SSE 流读取 RESPONDING 内容
```

### 13.5 工具注册与执行框架

**ToolRegistry** — 管理所有可用工具的定义和处理器。

| 工具名称 | 描述 | 执行方式 | 依赖 |
|---------|------|---------|------|
| `web_search` | 联网搜索实时信息 | HTTP GET → 搜索引擎API (Serper/SearXNG) | 需要 API Key |
| `calculator` | 数学计算 | 本地表达式求值 (exp4j / eval) | 无网络依赖 |
| `datetime` | 获取当前日期时间 | 本地 kotlinx.datetime | 无网络依赖 |

**工具注册流程：**

```text
应用启动
  │
  ▼
ToolRegistry.init()
  ├── register(WebSearchTool)   // 需要网络权限
  ├── register(CalculatorTool)  // 纯本地
  └── register(DateTimeTool)    // 纯本地

发送请求时（Agent模式开启）：
  ChatCompletionRequest.tools = ToolRegistry.getAllToolDefinitions()
  → 序列化为 OpenAI tools 参数格式，传给 new-api
```

### 13.6 前端组件树

```text
ChatScreen (改造)
  ├── AgentSwitch (新)              // Agent 模式开关
  ├── MessageList
  │   ├── MessageBubble (改造)
  │   │   └── AgentThoughtBubble (新)  // 思考过程可视化
  │   │       ├── ThoughtCard          // 「思考」步骤卡片，灰底
  │   │       ├── SearchCard           // 「搜索中...」旋转图标 + 查询词
  │   │       └── ObservationCard      // 「观察到...」推理过程
  │   ├── TextMessage                 // 原有文本消息
  │   └── ImageMessage                // 原有图片消息
  ├── InputBar
  │   └── AgentStatusIndicator (新)   // 当前 Agent 状态提示
  └── VoiceInputBar (原有)
```

### 13.7 数据流：带 Tool Calling 的完整对话

```text
┌─────────────────────────────────────────────────────────────────────────┐
│  Android Client                                                          │
│                                                                          │
│  ① 用户输入 "今天有什么AI新闻？"                                          │
│     (Agent开关 = ON)                                                     │
│       │                                                                  │
│       ▼                                                                  │
│  ② ChatViewModel.sendMessage()                                          │
│     ├─ 保存用户消息到 Room                                               │
│     ├─ 创建 AI 消息占位符 (contentType=AGENT_STEP)                        │
│     └─ 调用 streamChat() 带 tools 参数                                   │
│       │                                                                  │
│       ▼                                                                  │
│  ③ ChatRepositoryImpl.streamChat()                                      │
│     └─ ChatCompletionRequest {                                           │
│           model: "...",                                                  │
│           messages: [...],                                               │
│           tools: [                                                       │
│             {"type":"function", "function":{"name":"web_search",...}},   │
│             {"type":"function", "function":{"name":"calculator",...}}    │
│           ],                                                             │
│           stream: true                                                   │
│         }                                                               │
│       │                                                                  │
│       ▼                                                                  │
│  ④ Client SSE 解析 (改造 ChatStreamApi)                                  │
│                                                                          │
│     SSE Event 序列:                                                     │
│     ─────────────────────────────────────────────────────────────────    │
│     [1] data: {"choices":[{"delta":{"content":"","role":"assistant"}}]} │
│         → AgentStateMachine: IDLE → THINKING                            │
│     [2] data: {"choices":[{"delta":{"content":"让我帮你查一下..."}}]}    │
│         → 正常流式文本 (THINKING 阶段)                                    │
│     [3] data: {"choices":[{"delta":{                                     │
│           "tool_calls":[{"function":{"name":"web_search",                │
│           "arguments":"{\"query\":\"2026年AI新闻\"}"}}]                   │
│         }}]}                                                             │
│         → AgentStateMachine: THINKING → SEARCHING                        │
│         → 发送 AgentStep(TOOL_CALL) 到消息列表                            │
│     [4] [ToolExecEngine 执行 web_search]                                 │
│         ├─ HTTP GET → Serper/SearXNG API                                │
│         └─ 结果: [{"title":"...", "snippet":"..."}, ...]                │
│     [5] AgentStateMachine: SEARCHING → REASONING                         │
│         → 发送 AgentStep(TOOL_RESULT) 到消息列表                          │
│         → 新建 SSE 连接发送 tool_result 给 LLM                            │
│     [6] data: {"choices":[{"delta":{"content":"根据搜索结果..."}}]}      │
│         → AgentStateMachine: REASONING → RESPONDING                      │
│         → 流式输出最终回答                                                │
│     [7] data: [DONE]                                                     │
│         → AgentStateMachine: RESPONDING → IDLE                           │
│     ─────────────────────────────────────────────────────────────────    │
│                                                                          │
│  ⑤ ChatViewModel 更新 ChatUiState                                       │
│     ├─ uiState.agentState = IDLE/THINKING/SEARCHING/...                 │
│     ├─ uiState.streamingContent = 当前流式文本                           │
│     └─ uiState.messages += AgentStep 消息                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

### 13.8 关键类图

```text
┌──────────────────────────────────────────┐
│ ChatViewModel (改造)                      │
│  ├─ agentState: MutableState<AgentState>  │
│  ├─ agentEnabled: MutableState<Boolean>   │
│  ├─ toggleAgentMode()                     │
│  └─ handleToolCallChunk()                 │
├──────────────────────────────────────────┤
│ AgentStateMachine                         │
│  ├─ currentState: AgentState              │
│  ├─ transition(event: AgentEvent)         │
│  └─ onToolCall(): suspend → ToolResult    │
├──────────────────────────────────────────┤
│ ToolRegistry (单例)                        │
│  ├─ registeredTools: Map<String,Tool>     │
│  ├─ register(tool: Tool)                  │
│  ├─ getToolDefinitions(): List<...>       │
│  └─ execute(name, args): ToolResult       │
├──────────────────────────────────────────┤
│ Tool (interface)                          │
│  ├─ name: String                          │
│  ├─ description: String                   │
│  ├─ parameters: JsonSchema                │
│  └─ suspend fun execute(args): ToolResult │
├──────────────────────────────────────────┤
│ WebSearchTool : Tool                      │
│  ├─ client: OkHttpClient                  │
│  └─ execute() → 搜索结果 JSON              │
├──────────────────────────────────────────┤
│ CalculatorTool : Tool                     │
│  └─ execute() → 计算结果 Double            │
├──────────────────────────────────────────┤
│ ChatStreamApi (改造)                       │
│  └─ streamChat() → Flow<StreamEvent>      │
│     (新增: 解析 tool_calls 字段)           │
├──────────────────────────────────────────┤
│ StreamEvent (密封类, 新增)                  │
│  ├─ TextChunk(content) : StreamEvent      │
│  ├─ ToolCallChunk(calls) : StreamEvent    │
│  ├─ RoleChunk(role) : StreamEvent         │
│  └─ Done : StreamEvent                   │
└──────────────────────────────────────────┘
```

### 13.9 后端修改方案

**后端改造成本极低**，因为 new-api 原生支持 tool calling。现有 `chat_completions.py` 只需：

1. **请求转发增加 tools 参数**：如果请求体包含 `tools` 字段，透传给 new-api
2. **SSE 透传 tool_calls**：不对 tool_calls 相关字段做特殊处理，原样转发给客户端
3. **输出安全过滤忽略工具字段**：对 `tool_calls` 中的 `arguments` 暂不进行关键词过滤（JSON 参数内容可能被误杀）

```text
chat_completions.py payload 构建（修改点）:
  payload = {
      "model": req.model,
      "messages": [...],
      "stream": is_stream,
      "temperature": ...,
  }
  + if req.tools:  // 新增
  +     payload["tools"] = [t.model_dump() for t in req.tools]
  + if req.tool_choice:
  +     payload["tool_choice"] = req.tool_choice
```

**POST /v1/chat/completions 请求体扩展（可选）:**

```text
// 新增字段（现有的 ChatCompletionRequest schema）
tools?: ToolDefinition[]    // 工具定义列表
tool_choice?: "auto" | "none" | {"type": "function", "function": {"name": "..."}}
```

**第二次 SSE 连接（工具结果回传）流程：**

```text
客户端工具执行完成后：
  → 构建新 SSE 请求 (无需新建 API，复用同一 POST /v1/chat/completions)
  → messages 追加:
      原有的 user 消息
      + assistant 消息 (含 tool_calls)
      + tool 消息:
          role: "tool"
          content: "搜索结果 JSON"
          tool_call_id: "call_xxx"
  → stream = true
  → 新的 SSE 流：AI 基于工具结果生成最终回答
```

### 13.10 风险点与边界情况

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 工具执行超时 | Agent 卡在 SEARCHING | 设置 15s 超时，超时后降级为非 Agent 模式回复 |
| 网络搜索无结果 | Agent 无法获取信息 | 返回"未找到相关信息" + 降级到模型固有知识 |
| 多轮工具调用 | 状态机复杂度上升 | 限制最多 3 轮工具调用，超出后强制结束 |
| 后端透传兼容性 | 旧版 new-api 不支持 tool calling | Agent 模式需要后端版本 >= 某版本，否则降级 |
| 配额消耗翻倍 | 工具调用可能触发多次 LLM | 工具执行后回传不额外扣配额 |
| SSE 解析兼容 | 解析 tool_calls 字段可能抛异常 | StreamEvent 密封类，解析失败降级为 TextChunk |

### 13.11 新增文件清单

| 文件路径 | 说明 |
|---------|------|
| `domain/model/AgentState.kt` | Agent 状态枚举 + 状态机定义 |
| `domain/model/AgentStepType.kt` | Agent 步骤类型枚举 |
| `domain/model/ToolDefinition.kt` | 工具定义数据类 |
| `domain/model/StreamEvent.kt` | SSE 流事件密封类 |
| `domain/repository/AgentRepository.kt` | Agent 模块仓库接口 |
| `data/remote/api/ToolStreamApi.kt` | 带工具参数的流式 API（扩展 ChatStreamApi） |
| `data/remote/dto/ToolDto.kt` | 工具相关 DTO（ToolCallChunkDto 等） |
| `data/repository/AgentRepositoryImpl.kt` | Agent 仓库实现 |
| `data/tool/registry/ToolRegistry.kt` | 工具注册中心 |
| `data/tool/engine/ToolExecEngine.kt` | 工具执行引擎 |
| `data/tool/impl/WebSearchTool.kt` | 联网搜索工具 |
| `data/tool/impl/CalculatorTool.kt` | 计算器工具 |
| `data/tool/impl/DateTimeTool.kt` | 日期时间工具 |
| `ui/chat/components/AgentSwitch.kt` | Agent 模式切换开关 |
| `ui/chat/components/AgentThoughtBubble.kt` | 思考过程气泡组件 |
| `ui/chat/components/AgentStatusIndicator.kt` | Agent 状态指示器 |
| `ui/chat/AgentUiState.kt` | Agent UI 状态 |

### 13.12 修改文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `domain/model/ChatMessage.kt` | 新增 agentStepType/toolName/toolQuery/toolResult 字段 |
| `data/remote/api/ChatStreamApi.kt` | 解析 tool_calls 字段 + 新增 StreamEvent 返回 |
| `data/remote/dto/ChatDtos.kt` | 新增工具相关 DTO（如 ChatCompletionChunk 扩展 tool_calls） |
| `data/repository/ChatRepositoryImpl.kt` | streamChat 支持 tools 参数透传 |
| `domain/repository/ChatRepository.kt` | streamChat 接口增加 tools 参数 |
| `ui/chat/ChatViewModel.kt` | 集成 AgentStateMachine，处理工具调用周期 |
| `ui/chat/ChatUiState.kt` | 新增 agentState/agentEnabled 字段 |
| `ui/chat/ChatScreen.kt` | 集成 AgentSwitch，条件渲染 AgentThoughtBubble |
| `ui/chat/components/MessageBubble.kt` | 新增 AGENT_STEP 类型分支渲染 |
| `ui/settings/SettingsScreen.kt` | 新增 Agent 模式默认开关设置 |

---

## 14. 学习报告 PDF 导出模块 (F49)

> 版本：v2.0 新增 | 优先级：P0 | 依赖：Room 数据库统计数据

### 14.1 概述

从 Room 本地数据库聚合学习统计数据，使用 Android 原生 `android.graphics.pdf.PdfDocument` API 生成 PDF，并通过系统分享面板导出。

技术选型理由：
- **Android PdfDocument**：原生 API，无额外依赖，Canvas 驱动的渲染方式适合自定义排版
- 不需要 iText 等第三方库（增加 APK 大小，需处理开源协议兼容性）
- 分享使用 `Intent.ACTION_SEND` + `FileProvider`

### 14.2 三层架构

```text
┌───────────────────────────────────────────────┐
│  Share Layer                                   │
│  ShareStudyReportUseCase                       │
│  ├─ 生成 PDF 到 cache/study_report/            │
│  └─ 启动 Intent.ACTION_SEND (FileProvider URI) │
├───────────────────────────────────────────────┤
│  PDF Render Layer                              │
│  PdfReportRenderer                             │
│  ├─ Page 1: 封面 (头像 + 昵称 + 报告日期)      │
│  ├─ Page 2: 学习概览 (总学习天数/总对话/今日)   │
│  ├─ Page 3: 学科分布饼图 (SVG→Canvas 绘制)     │
│  ├─ Page 4: 学习趋势 (近 7 天对话数柱状图)      │
│  └─ Page 5: 成就展示 (连胜天数/徽章)            │
│  (Canvas API 绘制，文本+图形+颜色)              │
├───────────────────────────────────────────────┤
│  Data Aggregation Layer                        │
│  StudyReportRepository                        │
│  ├─ Room DAO 查询:                            │
│  │   ├─ 总对话数 / 总消息数                    │
│  │   ├─ 每日学习统计 (近 7/30 天)              │
│  │   ├─ 学科分布 (按 content/空值)             │
│  │   ├─ 活跃天数 / 连胜数                      │
│  │   └─ 首次使用日期                           │
│  └─ StudyReportData (聚合模型)                 │
└───────────────────────────────────────────────┘
```

### 14.3 Room 数据查询设计

所有统计从本地 Room 数据库的 `messages` 和 `conversations` 表聚合：

```text
// DAO 查询方法 (新增)
StudyReportDao:

📊 总览统计
  SELECT COUNT(DISTINCT date(timestamp/1000, 'unixepoch')) as totalActiveDays,
         COUNT(*) as totalMessages,
         (SELECT COUNT(*) FROM messages WHERE date(timestamp/1000, 'unixepoch') = date('now')) as todayMessages,
         MIN(date(timestamp/1000, 'unixepoch')) as firstUseDate
  FROM messages

📈 近 7 天每日学习趋势
  SELECT date(timestamp/1000, 'unixepoch') as day,
         COUNT(*) as msgCount,
         COUNT(DISTINCT conversationId) as convCount
  FROM messages
  WHERE date(timestamp/1000, 'unixepoch') >= date('now', '-7 days')
  GROUP BY day ORDER BY day ASC

🏆 连胜天数
  (通过应用层遍历活跃日期，计算最长连续活跃天数)

📚 对话主题分布
  SELECT title, messageCount FROM conversations
  WHERE title != '' ORDER BY updatedAt DESC LIMIT 20

⏱ 平均回复长度
  SELECT AVG(LENGTH(content)) as avgLength FROM messages WHERE isUser = 0
```

### 14.4 PDF 渲染设计

**PdfReportRenderer** 使用 `android.graphics.pdf.PdfDocument` 逐页 Canvas 绘制：

```text
PdfDocument (A4: 595×842 pt)
  ├─ Page 1 — 封面页
  │   ├─ 应用图标 (Bitmap)
  │   ├─ 标题: "AI 学伴 - 学习报告"
  │   ├─ 用户昵称
  │   └─ 报告生成日期
  │
  ├─ Page 2 — 学习概览
  │   ├─ 卡片: 总活跃天数 / 总对话数 / 今日学习
  │   ├─ 卡片: 最长连胜 / 累计学习时长(估计)
  │   └─ 装饰性分隔线
  │
  ├─ Page 3 — 学习趋势 (柱状图)
  │   ├─ 标题: "近 7 天学习趋势"
  │   ├─ X 轴: 日期标签
  │   ├─ Y 轴: 对话次数
  │   └─ Paint.rect 柱状图
  │
  └─ Page 4 — 成就 & 统计摘要
      ├─ 获得的徽章列表
      ├─ 学习之星评价 (根据数据生成评语)
      └─ 页脚: "由 AI 学伴 App 生成"
```

### 14.5 关键类图

```text
┌───────────────────────────────────────┐
│ StudyReportRepository                 │
│  ├─ fun getReportData(): ReportData   │
│  └─ 依赖: ConversationDao, MessageDao │
├───────────────────────────────────────┤
│ ReportData                            │
│  ├─ totalActiveDays: Int              │
│  ├─ totalMessages: Int                │
│  ├─ totalConversations: Int           │
│  ├─ todayMessages: Int                │
│  ├─ dailyStats: List<DayStat>         │
│  ├─ streakDays: Int                   │
│  ├─ avgResponseLength: Int            │
│  └─ firstUseDate: String              │
├───────────────────────────────────────┤
│ PdfReportRenderer                     │
│  ├─ render(reportData): PdfDocument   │
│  ├─ drawCoverPage(canvas, ...)        │
│  ├─ drawOverviewPage(canvas, ...)     │
│  ├─ drawTrendChart(canvas, ...)       │
│  └─ drawAchievementsPage(canvas, ...) │
├───────────────────────────────────────┤
│ ShareStudyReportUseCase               │
│  ├─ invoke(): Uri                     │
│  ├─ generatePdf(): File               │
│  └─ createShareIntent(): Intent       │
└───────────────────────────────────────┘
```

### 14.6 分享流程

```text
用户点击"导出学习报告"
       │
       ▼
ShareStudyReportUseCase.invoke()
       │
       ├─ 1. ReportRepository.getReportData()
       │      └─ Room DAO 并发聚合数据
       │
       ├─ 2. PdfReportRenderer.render(reportData)
       │      ├─ 创建 PdfDocument
       │      ├─ 逐页 Canvas.draw...
       │      └─ document.writeTo(outputStream)
       │
       ├─ 3. 保存到 app cache/study_report/report_20260516.pdf
       │
       ├─ 4. FileProvider.getUriForFile()
       │
       └─ 5. Intent.ACTION_SEND
              ├─ type: "application/pdf"
              ├─ EXTRA_STREAM: content URI
              ├─ 添加 FLAG_GRANT_READ_URI_PERMISSION
              └─ startActivity(Intent.createChooser(...))
```

### 14.7 新增文件清单

| 文件路径 | 说明 |
|---------|------|
| `domain/model/ReportData.kt` | 学习报告聚合数据模型 |
| `domain/usecase/ShareStudyReportUseCase.kt` | 导出并分享报告的业务用例 |
| `domain/repository/StudyReportRepository.kt` | 报告数据仓库接口 |
| `data/local/dao/StudyReportDao.kt` | 统计查询 DAO |
| `data/repository/StudyReportRepositoryImpl.kt` | 报告数据仓库实现 |
| `ui/report/PdfReportRenderer.kt` | PDF 渲染引擎（Canvas 绘制） |
| `ui/report/ReportExportScreen.kt` | 报告预览/导出界面 |
| `ui/report/ReportExportViewModel.kt` | 导出 ViewModel |
| `xml/file_paths.xml` | FileProvider 路径配置 |
| `AndroidManifest.xml` | 注册 FileProvider + 声明权限 |

### 14.8 修改文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `data/local/db/AiTutorDatabase.kt` | 注册 StudyReportDao |
| `di/DatabaseModule.kt` | 注入 StudyReportDao |
| `ui/settings/SettingsScreen.kt` | 添加"导出学习报告"入口 |
| `app/build.gradle.kts` | 无新增依赖（PdfDocument 为平台 API） |
| `AndroidManifest.xml` | 注册 FileProvider |

### 14.9 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| PDF 内容中文渲染 | 文字乱码或不显示 | 使用 Typeface.create("sans-serif", Normal) 确保支持中文字符 |
| 大数据量渲染 | OOM | 限制查询范围（最多 30 天），PDF 页数不超过 10 页 |
| FileProvider URI 权限 | 部分 App 无法接收 | 使用 FileProvider + 临时授权，兼容主流分享目标 |
| Room 统计查询性能 | 主线程卡顿 | 统计查询全部使用 suspend 协程，Room 自动在后台线程执行 |

---

## 15. 多语言支持 (F50)

> 版本：v2.0 新增 | 优先级：P1 | 初始目标：中文 + 英文

### 15.1 概述

为 App 添加英文界面支持，使用 Android 标准 `strings.xml` 资源文件方案 + Compose 响应式语言切换。

技术方案：
- **Android 原生国际化**（`values/` + `values-en/` 资源目录）
- **Compose 响应式切换**：通过 `CompositionLocal` 观察 `AppLanguage` 状态
- **DataStore 持久化**：保存用户的语言偏好
- **后端语言参数**：请求头 `Accept-Language` 或请求体 `language` 字段控制 AI 回复语言

### 15.2 资源文件组织

```text
res/
  ├── values/                          (默认，中文)
  │   ├── strings.xml
  │   └── strings_chat.xml            (会话相关文案，按功能拆分)
  │
  ├── values-en/                       (英文)
  │   ├── strings.xml
  │   └── strings_chat.xml
  │
  └── values-ja/                       (日文，可选)
      ├── strings.xml
      └── strings_chat.xml
```

**strings.xml** 迁移计划：将所有硬编码中文字符串迁移到 `strings.xml`，按功能分组：

```text
<!-- 通用 -->
<string name="app_name">AI 学伴</string>
<string name="app_name_en">AI Tutor</string>

<!-- 导航 -->
<string name="nav_chat">对话</string>
<string name="nav_chat_en">Chat</string>
<string name="nav_dashboard">学习</string>
<string name="nav_dashboard_en">Dashboard</string>
<string name="nav_profile">我的</string>
<string name="nav_profile_en">Profile</string>

<!-- 聊天 -->
<string name="input_hint">输入你的问题...</string>
<string name="input_hint_en">Ask me anything...</string>
<string name="send">发送</string>
<string name="send_en">Send</string>

<!-- Agent -->
<string name="agent_mode">AI Agent</string>
<string name="agent_mode_en">AI Agent</string>
<string name="agent_thinking">AI 正在思考...</string>
<string name="agent_thinking_en">AI is thinking...</string>
<string name="agent_searching">正在搜索：%s</string>
<string name="agent_searching_en">Searching: %s</string>

<!-- 设置 -->
<string name="settings_language">语言</string>
<string name="settings_language_en">Language</string>
<string name="language_zh">中文</string>
<string name="language_en">English</string>
```

### 15.3 语言切换架构

```text
┌──────────────────────────────────────┐
│ AppLanguageState (DataStore 持久化)   │
│  ├─ currentLang: MutableStateFlow    │
│  │   = Locale("zh") / Locale("en")  │
│  ├─ setLanguage(locale)              │
│  └─ flow: Flow<Locale>               │
├──────────────────────────────────────┤
│ 提供层                                │
│  AppLanguageProvider (CompositionLocal)│
│  ├─ 读取 DataStore 中的语言设置        │
│  └─ 通过 CompositionLocal 分发         │
├──────────────────────────────────────┤
│ 使用层                                │
│  Compose 组件通过 stringResource()    │
│  或 LocalAppLanguage.current 获取     │
└──────────────────────────────────────┘
```

**DataStore 存储结构：**

```text
// data/remote/datastore/LanguagePreferences.kt (新增)
language_preferences {
  "app_language" : "zh" | "en" | "ja"  // DataStore Preferences
}
```

**CompositionLocal 实现：**

```kotlin
// AppLanguageProvider.kt (新增)
val LocalAppLanguage = staticCompositionLocalOf { Locale("zh") }

@Composable
fun AppLanguageProvider(
    languagePreferences: LanguagePreferences,
    content: @Composable () -> Unit
) {
    val locale by languagePreferences.currentLocale.collectAsState()
    CompositionLocalProvider(LocalAppLanguage provides locale) {
        // 更新 Configuration.locale
        val config = LocalConfiguration.current
        config.setLocale(locale)
        // 需要 context.createConfigurationContext 刷新资源
        content()
    }
}
```

**语言切换流程：**

```text
用户选择 "English"
       │
       ▼
SettingsViewModel.setLanguage("en")
       │
       ├─ 1. LanguagePreferences.setLocale(Locale("en"))
       │      └─ DataStore 保存
       │
       ├─ 2. AppLanguageProvider 感知到 StateFlow 变化
       │      └─ CompositionLocal 更新
       │
       ├─ 3. MainActivity recomposition
       │      ├─ createConfigurationContext(Configuration(Locale("en")))
       │      └─ setContentView 重新 inflate
       │
       └─ 4. 所有 stringResource() 自动返回英文文案
```

### 15.4 后端语言参数传递

对于 AI 回复的语言控制，通过 `ChatCompletionRequest` 增加 `language` 字段：

```text
// Android → Backend
POST /v1/chat/completions
{
  "model": "...",
  "messages": [...],
  "stream": true,
  "language": "zh" | "en",   // 新增字段
  ...
}

// Backend → new-api (system prompt 尾部追加指令)
messages = [
  {"role": "system", "content": "... \n\n请用英文回答。"},  // language=en 时
  ...
]
```

或者更简单的方案：客户端在消息列表中自动插入一条语言指令：

```text
// Android 在 streamChat 时对 messages 做前置处理
if (language == "en" && hasSystemPrompt) {
    messages.add(
        ChatMessageDto(role="system", content="Please answer in English.")
    )
}
```

推荐方案：**后端处理**。在 `chat_completions.py` 中接收 `language` 参数，自动注入 system prompt：

```text
POST /v1/chat/completions (增设字段)
  language?: "zh" | "en"    // 默认 "zh"

后端处理逻辑：
  1. 从请求体读取 language 字段
  2. 如果 language == "en":
     在 system message 中追加 "Please always answer in English."
  3. 仅影响 AI 回复语言，不影响 UI

后端架构变更极小：
  → 新增 chat_completions.py 中的处理分支即可
  → 无需数据库变更
  → 无需新增路由
```

### 15.5 UI 组件适配方案

| 组件类型 | 适配方式 | 示例 |
|---------|---------|------|
| 静态文本 | `stringResource(R.string.xxx)` | Text(text = stringResource(R.string.send)) |
| 动态文本 | 参数化字符串 | stringResource(R.string.agent_searching, query) |
| 用户生成内容 | 不翻译（消息内容） | 直接显示 |
| 日期/时间 | `java.time.format` 本地化 | DateTimeFormatter.ofLocalizedDate() |
| AI 回复 | 后端 language 参数 | 后端自动切换语言 |
| Toast/Snackbar | 同字符串资源 | stringResource(R.string.error_network) |
| 图标 alt 文本 | contentDescription 绑定 | contentDescription = stringResource(...) |

### 15.6 关键类图

```text
┌──────────────────────────────────────┐
│ LanguagePreferences                   │
│  ├─ currentLocale: Flow<Locale>      │
│  ├─ suspend setLocale(locale)        │
│  └─ (DataStore Preferences 实现)      │
├──────────────────────────────────────┤
│ AppLanguageProvider (Composable)      │
│  ├─ 读取 LanguagePreferences         │
│  └─ CompositionLocal 分发 locale     │
├──────────────────────────────────────┤
│ LocalAppLanguage (CompositionLocal)   │
├──────────────────────────────────────┤
│ LanguageSettingUseCase               │
│  ├─ getCurrentLanguage(): Locale     │
│  └─ setLanguage(locale)              │
├──────────────────────────────────────┤
│ SettingsViewModel (改造)              │
│  └─ availableLanguages: List<Locale> │
└──────────────────────────────────────┘
```

### 15.7 新增文件清单

| 文件路径 | 说明 |
|---------|------|
| `data/remote/datastore/LanguagePreferences.kt` | 语言偏好 DataStore |
| `ui/theme/AppLanguageProvider.kt` | Language CompositionLocal Provider |
| `res/values/strings.xml` | 默认中文资源（重构现有硬编码字符串） |
| `res/values-en/strings.xml` | 英文资源翻译 |
| `res/values-ja/strings.xml` | 日文资源（可选） |

### 15.8 修改文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `MainActivity.kt` | 包裹 AppLanguageProvider，监听语言变化重建 Context |
| `ui/settings/SettingsScreen.kt` | 新增语言选择列表 |
| `ui/settings/SettingsViewModel.kt` | 集成 LanguagePreferences |
| `ui/chat/ChatViewModel.kt` | streamChat 增加 language 参数 |
| `domain/repository/ChatRepository.kt` | streamChat 增加 language 参数 |
| `data/repository/ChatRepositoryImpl.kt` | 请求体传递 language 字段 |
| `data/remote/dto/ChatDtos.kt` | ChatCompletionRequest 增加 language 字段 |
| `ui/**/*.kt` | 所有硬编码中文字符串替换为 stringResource() |
| `app/build.gradle.kts` | 无新增依赖（国际化无需额外库） |
| 后端 `chat_completions.py` | 新增 language 字段处理逻辑 |
| 后端 `schemas/chat_completions.py` | ChatCompletionRequest 增加 language 字段 |

### 15.9 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| AI 回复语言不跟随 UI | 用户期望英文 AI 输出中文 | 后端 system prompt 注入语言指令，同时增加 `always` 限定词 |
| 动态字符串参数顺序变化 | 不同语言的语序不同 | 使用 `%1$s`, `%2$d` 位置参数，避免硬编码顺序 |
| 字符串遗漏 | 部分文本仍为中文 | 编写 lint 规则检查未国际化的字符串，配合 PR review |
| Compose 预览不刷新 | 开发体验下降 | 在 Preview 中手动指定 locale |
| 配置变更时状态保持 | 语言设置丢失 | DataStore 持久化，AppLanguageProvider 在 onCreate 时恢复 |

---

## 附录 C: v2.0 新增文件清单 (F48-F50)

### AI Agent 模块 (F48)

| 文件路径 | 说明 |
|---------|------|
| `domain/model/AgentState.kt` | Agent 状态机定义 |
| `domain/model/AgentStepType.kt` | Agent 步骤类型枚举 |
| `domain/model/ToolDefinition.kt` | 工具定义模型 |
| `domain/model/StreamEvent.kt` | SSE 流事件密封类 |
| `domain/repository/AgentRepository.kt` | Agent 仓库接口 |
| `data/remote/dto/ToolDto.kt` | 工具相关 DTO |
| `data/repository/AgentRepositoryImpl.kt` | Agent 仓库实现 |
| `data/tool/registry/ToolRegistry.kt` | 工具注册中心 |
| `data/tool/engine/ToolExecEngine.kt` | 工具执行引擎 |
| `data/tool/impl/WebSearchTool.kt` | 联网搜索工具 |
| `data/tool/impl/CalculatorTool.kt` | 计算器工具 |
| `data/tool/impl/DateTimeTool.kt` | 日期时间工具 |
| `ui/chat/components/AgentSwitch.kt` | Agent 模式开关按钮 |
| `ui/chat/components/AgentThoughtBubble.kt` | 思考过程展示组件 |
| `ui/chat/components/AgentStatusIndicator.kt` | 状态指示器组件 |
| `ui/chat/AgentUiState.kt` | Agent UI 状态类 |

### 学习报告 PDF 模块 (F49)

| 文件路径 | 说明 |
|---------|------|
| `domain/model/ReportData.kt` | 报告聚合数据模型 |
| `domain/usecase/ShareStudyReportUseCase.kt` | 分享报告用例 |
| `domain/repository/StudyReportRepository.kt` | 报告仓库接口 |
| `data/local/dao/StudyReportDao.kt` | 统计查询 DAO |
| `data/repository/StudyReportRepositoryImpl.kt` | 报告仓库实现 |
| `ui/report/PdfReportRenderer.kt` | PDF 渲染引擎 |
| `ui/report/ReportExportScreen.kt` | 报告导出界面 |
| `ui/report/ReportExportViewModel.kt` | 导出 ViewModel |
| `xml/file_paths.xml` | FileProvider 路径配置 |

### 多语言模块 (F50)

| 文件路径 | 说明 |
|---------|------|
| `data/remote/datastore/LanguagePreferences.kt` | 语言偏好 DataStore |
| `ui/theme/AppLanguageProvider.kt` | 语言 CompositionLocal Provider |
| `res/values/strings.xml` | 中文资源（重构为完整国际化的 strings） |
| `res/values-en/strings.xml` | 英文资源 |

## 附录 D: v2.0 模块依赖关系总图

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│  v2.0 架构扩展：F48(Agent) + F49(报告) + F50(多语言)                        │
│                                                                             │
│  现有 v1.0 架构：Clean Architecture + MVVM + F40-F46                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Sprint 1 (P0): 独立可并行开发                                         │   │
│  │                                                                        │   │
│  │  ┌──────────────────────┐ ┌────────────────┐ ┌──────────────────┐     │   │
│  │  │ F48 — AI Agent 增强  │ │ F49 — 学习报告   │ │ F50 — 多语言     │     │   │
│  │  │ 依赖: F42(Chat SSE)  │ │ PDF 导出         │ │ 依赖: 现有 UI   │     │   │
│  │  │       + 后端 new-api │ │ 依赖: Room DB +  │ │       + Dto     │     │   │
│  │  │       + F24(消息)   │ │       DataStore  │ │                  │     │   │
│  │  │ 产出:               │ │ 产出:            │ │ 产出:            │     │   │
│  │  │  AgentStateMachine  │ │  ReportRepo      │ │  strings.xml    │     │   │
│  │  │  ToolRegistry       │ │  PdfRenderer     │ │  en/ja 翻译     │     │   │
│  │  │  AgentThoughtBubble │ │  ShareUseCase    │ │  LanguagePref   │     │   │
│  │  │  StreamEvent解析    │ │                  │ │  AppLangProvider│     │   │
│  │  └──────────────────────┘ └────────────────┘ └──────────────────┘     │   │
│  │                                                                        │   │
│  │  并行策略: F48/F49/F50 无互依赖 → 3 人并行开发                         │   │
│  │  F48 需后端配合（极小改动：透传 tools + language 参数）                 │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Sprint 2 (整合 & QA):                                                │   │
│  │                                                                        │   │
│  │  ├─ F48 + F50 后端联合调试（language 参数 + tools 参数透传）           │   │
│  │  ├─ F49 多语言报告文案适配                                               │   │
│  │  ├─ F48 Agent 模式/普通模式切换测试                                      │   │
│  │  └─ F50 全部 UI 组件语言切换回归测试                                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 与现有架构兼容性分析

| 维度 | v1.0 架构 | v2.0 变更 | 兼容性 |
|------|-----------|-----------|--------|
| Clean Architecture | 3 层（ui/data/domain） | 不变 | ✅ 完全兼容 |
| MVVM + Compose | ChatViewModel + ChatUiState | 新增 AgentUiState + AgentStateMachine | ✅ 增量扩展 |
| SSE 流式通道 | ChatStreamApi → Flow\<String\> | 升级为 Flow\<StreamEvent\>（密封类） | ⚠️ 需修改解析层 |
| ChatMessage 模型 | 5 字段 | 扩展 4 个 Agent 字段（均为 nullable） | ✅ 不影响现有序列化 |
| Room 数据库 | Conversation + Message | 新增 StudyReportDao（只读查询） | ✅ 完全兼容 |
| Hilt DI | NetworkModule + DatabaseModule | 新增 AgentModule + ReportModule | ✅ 增量注入 |
| OkHttp SSE | EventSource API | 兼容（仅扩展解析逻辑） | ✅ 向后兼容 |
| 后端 new-api | 透传 content | 透传 tools + language | ✅ 向后兼容（新增可选参数） |
| 资源配置 | values/strings.xml | 新增 values-en/values-ja | ✅ Android 标准机制 |
| 导航 | Navigation Compose | 新增 ReportExportScreen 路由 | ✅ 增量添加 |
| 设置页 | SettingsScreen | 新增 Agent 开关 + 语言选择 | ✅ 增量添加 |

### v2.0 技术选型总表

| 模块 | 技术 | 理由 |
|------|------|------|
| Agent 工具框架 | 客户端 ToolRegistry + ToolExecEngine | 工具执行在端侧，无需服务器资源 |
| Agent 状态管理 | 自定义状态机 + Compose State | 轻量级，无需第三方库 |
| SSE 增强解析 | 扩展 OkHttp EventSource 解析逻辑 | 复用现有网络栈 |
| Web Search | Serper.dev / SearXNG API (HTTP) | 标准化搜索 API，非 Google 依赖 |
| Calculator | exp4j / Kotlin 表达式求值 | 纯本地，无网络依赖 |
| PDF 生成 | android.graphics.pdf.PdfDocument | 零依赖，平台原生 API |
| PDF 渲染 | Canvas API | 灵活性高，支持中文 |
| 文件分享 | FileProvider + Intent.ACTION_SEND | Android 标准方式 |
| 国际化 | strings.xml + values-xx 目录 | Android 标准方式 |
| 语言切换 | DataStore + CompositionLocal | 响应式 + 持久化 |
| 后端交互 | 请求体 language 字段 | 简单直接，无需新增端点 |
