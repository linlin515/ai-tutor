# AI 学伴 Android App — 需求拆分表 (PRD SPLIT)

> **文档版本：** v1.0
> **日期：** 2026-05-15
> **负责人：** PM
> **基准 PRD：** PRD.md v1.2（终稿）
> **目标受众：** Coder（可直接开工）

---

## 拆分原则

1. **按模块分组**：每个任务对应一个独立可测的功能模块
2. **依赖先行**：标注前置依赖（列表为空则无依赖）
3. **工作量分级**：S（≤2人日）、M（3-5人日）、L（6-8人日）
4. **输出明确**：标注需要创建/修改的源文件路径
5. **验收对齐**：每个任务引用 PRD 中的 AC 编号

---

## Phase 1S — 基础设施 (P0)

> 第 1 周 | 预计总工时：6-8 人日 | 需 2 Coder 并行

### T1 — 项目脚手架与工程配置

| 属性 | 内容 |
|------|------|
| **ID** | T1 |
| **优先级** | P0 — 前置条件 |
| **功能** | 项目初始化、Gradle 配置、架构骨架搭建 |
| **负责** | Coder-A (Android 架构) |
| **依赖** | 无 |
| **工作量** | S (2 人日) |
| **PRD 章节** | §1.3 约束条件 |
| **AC 关联** | COMP01-COMP04 |
| **创建/修改文件** | |
| | `build.gradle.kts` (project + app) — 新增依赖管理 |
| | `app/src/main/java/com/aitutor/app/...` — 包结构搭建 |
| | `di/AppModule.kt` — Hilt 根模块 |
| | `data/local/AppDatabase.kt` — Room 数据库声明 |
| | `data/remote/NetworkModule.kt` — Retrofit + OkHttp |
| | `data/remote/ApiService.kt` — API 接口定义（骨架） |
| | `ui/theme/Theme.kt` — Material3 主题（浅色/深色） |
| | `ui/navigation/NavGraph.kt` — 导航图骨架 |
| **描述** | 搭建基于 MVVM + Clean Architecture 的 Android 项目骨架。配置 Gradle 依赖（Hilt, Room, Retrofit, OkHttp, Compose, Navigation, CameraX, ML Kit）。创建三层包结构（data/domain/ui）。完成基础主题和导航框架。 |

### T2 — 认证模块 (Auth)

| 属性 | 内容 |
|------|------|
| **ID** | T2 |
| **优先级** | P0 — 前置条件 |
| **功能** | 手机号注册/登录 + Token 管理 |
| **负责** | Coder-A (Android 架构) |
| **依赖** | T1 ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F01-F03, §4.1-SplashScreen, §4.2-LoginScreen, §5.5-Token续期 |
| **AC 关联** | AC01-AC04 |
| **创建/修改文件** | |
| | `data/remote/api/AuthApi.kt` — 登录/注册/刷新 API |
| | `data/remote/dto/AuthDto.kt` — 请求/响应 DTO |
| | `data/local/TokenManager.kt` — DataStore Token 持久化 |
| | `data/repository/AuthRepository.kt` — 认证仓库 |
| | `domain/usecase/LoginUseCase.kt` |
| | `domain/usecase/RegisterUseCase.kt` |
| | `ui/screen/splash/SplashScreen.kt` |
| | `ui/screen/splash/SplashViewModel.kt` |
| | `ui/screen/login/LoginScreen.kt` |
| | `ui/screen/login/LoginViewModel.kt` |
| | `ui/navigation/NavGraph.kt` (修改 — 添加 auth 路由) |
| **描述** | 实现手机号注册/登录全流程：Splash 页面 Token 检查 → 有效则跳首页，无效则跳登录页。登录成功后 Token 持久化到 DataStore。OkHttp Interceptor 实现 401 自动刷新 Token，刷新失败跳回登录页。输入校验：手机号格式（11位）、密码长度（6-20位）。 |

---

## Phase 1A — 核心对话 (P0)

> 第 1-2 周 | 预计总工时：12-16 人日 | 需 2 Coder 并行

### T3 — 会话管理 (Conversation)

| 属性 | 内容 |
|------|------|
| **ID** | T3 |
| **优先级** | P0 |
| **功能** | 会话 CRUD、自动命名、排序、搜索历史 |
| **负责** | Coder-A (Data Layer) |
| **依赖** | T1 ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F04, F13, F28, F33, §5.4-会话管理流程 |
| **AC 关联** | AC10, AC11, AC12, AC13, AC28 |
| **创建/修改文件** | |
| | `data/local/dao/ConversationDao.kt` — Room DAO |
| | `data/local/entity/ConversationEntity.kt` — Room Entity |
| | `data/repository/ConversationRepository.kt` |
| | `domain/model/Conversation.kt` — Domain 模型 |
| | `ui/screen/chat/ConversationListSheet.kt` — 会话列表 BottomSheet |
| | `ui/screen/chat/ConversationViewModel.kt` (修改 — 添加会话管理状态) |
| **描述** | Room 层实现会话的增删改查，包括级联删除消息、按更新时间排序、搜索功能。首条消息前 20 字自动命名的后处理逻辑。UI 层实现会话列表 BottomSheet（搜索框+列表+长按删除+左滑删除+新建按钮）。 |

### T4 — 聊天界面与消息发送 (Chat UI + Messaging)

| 属性 | 内容 |
|------|------|
| **ID** | T4 |
| **优先级** | P0 |
| **功能** | 消息气泡、发送/接收、发送状态、空状态 |
| **负责** | Coder-B (UI Layer) |
| **依赖** | T3 ✅ |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F05, F06, F12, §4.4-ChatScreen, §4.5-ConversationList |
| **AC 关联** | AC05, AC08, AC09, AC14 |
| **创建/修改文件** | |
| | `ui/screen/chat/ChatScreen.kt` — 主聊天页面 |
| | `ui/screen/chat/ChatViewModel.kt` — 聊天 ViewModel |
| | `ui/screen/chat/components/MessageBubble.kt` — 消息气泡组件 |
| | `ui/screen/chat/components/ChatInputBar.kt` — 底部输入栏 |
| | `ui/screen/chat/components/EmptyStateView.kt` — 空状态引导 |
| | `ui/screen/chat/components/ErrorView.kt` — 错误提示/重试 |
| | `data/repository/ChatRepository.kt` — 聊天数据仓库 |
| **描述** | 实现气泡式消息布局（用户右对齐蓝色/AI 左对齐灰色）、多轮对话上下文保持、发送按钮禁用/启用逻辑、消息发送状态指示（发送中/成功/失败）、AI 思考中动画、空状态欢迎语+示例问题卡片。 |

### T5 — SSE 流式输出 + 打字机效果 (Streaming)

| 属性 | 内容 |
|------|------|
| **ID** | T5 |
| **优先级** | P0 |
| **功能** | SSE 逐块接收 + 打字机逐字动画 |
| **负责** | Coder-A (Network Layer) |
| **依赖** | T4 ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F07, F08 |
| **AC 关联** | AC06 |
| **创建/修改文件** | |
| | `data/remote/api/ChatStreamApi.kt` — SSE 流式 API |
| | `data/remote/stream/SseEvent.kt` — SSE 事件模型 |
| | `data/remote/stream/StreamingParser.kt` — SSE 流解析器 |
| | `ui/screen/chat/components/StreamingTextView.kt` — 打字机效果组件 |
| | `ui/screen/chat/ChatViewModel.kt` (修改 — 添加流式状态管理) |
| | `data/repository/ChatRepository.kt` (修改 — 添加 SSE 逻辑) |
| **描述** | 基于 OkHttp EventSource 实现 SSE 连接管理。逐块接收 AI 响应 data chunk，解析后通过 Flow/Channel 下发到 ViewModel。打字机效果组件逐字渲染流式文本，光标闪烁指示。异常处理：自动重连（指数退避 1s/2s/4s，最多 3 次）、连接中断提示。 |

### T6 — Markdown/代码/公式渲染

| 属性 | 内容 |
|------|------|
| **ID** | T6 |
| **优先级** | P0 |
| **功能** | Markdown 渲染、代码块复制、数学公式 |
| **负责** | Coder-B (UI Layer) |
| **依赖** | T4 ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F09, §4.4-ChatScreen |
| **AC 关联** | AC07, AC27 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/MarkdownRender.kt` — Markdown 渲染器 |
| | `ui/screen/chat/components/CodeBlock.kt` — 代码块组件（带复制按钮） |
| | `ui/screen/chat/components/FormulaRender.kt` — 数学公式渲染 |
| | `ui/screen/chat/components/MessageBubble.kt` (修改 — 集成渲染) |
| **描述** | 实现 Compose 端 Markdown 渲染器：粗体/斜体/列表/表格/代码块/数学公式（LaTeX）。代码块右上角增加复制按钮，点击复制到剪贴板并 Toast 提示。流式输出时渲染逐步追加。 |

### T7 — 本地消息持久化 (Room)

| 属性 | 内容 |
|------|------|
| **ID** | T7 |
| **优先级** | P0 |
| **功能** | 消息写入 Room、离线查看、清空会话 |
| **负责** | Coder-A (Data Layer) |
| **依赖** | T3 ✅, T4 ✅ |
| **工作量** | S (2 人日) |
| **PRD 章节** | §2.2 F10, F11, F14 |
| **AC 关联** | AC09, AC15 |
| **创建/修改文件** | |
| | `data/local/dao/MessageDao.kt` — Room DAO |
| | `data/local/entity/MessageEntity.kt` — Room Entity |
| | `data/local/entity/Converters.kt` — Room 类型转换器 |
| | `data/repository/ChatRepository.kt` (修改 — 添加消息持久化) |
| | `ui/screen/chat/ChatViewModel.kt` (修改 — 离线加载) |
| **描述** | Room Entity 设计消息表：id、会话id、角色(用户/AI)、消息类型(文本/图片/语音)、内容、时间戳、状态、流式完成标记。ViewModel 启动时从 Room 加载历史消息，发送时先写入 Room（状态：发送中），流完成后更新状态。清空会话时级联删除消息。支持分页加载（每次 50 条）。 |

---

## Phase 1B — 拍照解题 (P0)

> 第 2-3 周 | 预计总工时：7-10 人日 | 需 Coder-A + Coder-B 协作

### T8 — 拍照解题基础版 (Camera + Photo)

| 属性 | 内容 |
|------|------|
| **ID** | T8 |
| **优先级** | P0 |
| **功能** | CameraX 拍照上传 + 相册选择 + ML Kit OCR + 图片消息 |
| **负责** | Coder-B (UI + Camera) |
| **依赖** | T1 ✅, T4 ✅ |
| **工作量** | M (4 人日) |
| **PRD 章节** | §2.2 F21, F22, F23, F24, §4.7-CameraScreen |
| **AC 关联** | AC21, AC22, AC23 |
| **创建/修改文件** | |
| | `ui/screen/camera/CameraScreen.kt` — 拍照页面 |
| | `ui/screen/camera/CameraViewModel.kt` — 相机 ViewModel |
| | `ui/screen/camera/PhotoPreviewSheet.kt` — 拍照预览确认 |
| | `data/remote/api/SolveApi.kt` — 解题 API |
| | `data/remote/dto/SolveDto.kt` — 解题请求/响应 DTO |
| | `ui/screen/chat/components/ImageMessage.kt` — 图片消息组件 |
| | `ui/navigation/NavGraph.kt` (修改 — 添加 camera 路由) |
| | `ui/screen/chat/components/ChatInputBar.kt` (修改 — 添加拍照入口) |
| **描述** | 实现 CameraX 全屏相机预览、拍照→确认→Multipart 上传→POST /api/v1/solve/photo → AI 分析返回→跳转聊天页面显示解答。支持前后摄像头切换。相册选择基于 ActivityResultContracts.PickVisualMedia。ML Kit OCR 实时检测取景框文字（透明边框覆盖层）。图片消息气泡显示缩略图，点击全屏预览。异常处理：权限拒绝引导、图片过大自动压缩。 |

### T9 — 拍照解题增强版 (全学科 + SSE 流式)

| 属性 | 内容 |
|------|------|
| **ID** | T9 |
| **优先级** | P0 |
| **功能** | 全学科 OCR + 学科选择器 + 流式分步解题 |
| **负责** | Coder-A (Integration) + Coder-B (UI) |
| **依赖** | T5 ✅, T8 ✅ |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F40, §4.7-CameraScreen, §9.1-F40 |
| **AC 关联** | AC34, AC35, AC36, PERF07 |
| **创建/修改文件** | |
| | `ui/screen/camera/CameraScreen.kt` (修改 — 添加学科选择器) |
| | `ui/screen/camera/SubjectSelector.kt` — 学科选择组件 |
| | `data/remote/api/SolveApi.kt` (修改 — 添加学科参数/SSE) |
| | `data/remote/stream/SolveStreamParser.kt` — 解题流解析 |
| | `data/repository/SolveRepository.kt` — 解题仓库 |
| | `ui/screen/chat/components/StepByStepCard.kt` — 分步卡片组件 |
| | (后端配合: 多学科 AI 分析引擎 + SSE 流式解题) |
| **描述** | 在基础拍照解题上增加：(1) 学科选择器（数学/物理/化学/生物/语文/英语/自动），拍照前可选，默认识别；(2) 全学科 OCR 识别，支持手写体(≥85%)和印刷体；(3) 解题结果通过 SSE 流式逐步骤返回，客户端渲染为 Step 1/N 分步卡片；(4) 端到端延迟≤5s。 |

---

## Phase 1C — AI 教学增强 (P0)

> 第 2-3 周 | 预计总工时：7-9 人日 | 需 Coder-A + Coder-B 并行

### T10 — 自适应分步讲解

| 属性 | 内容 |
|------|------|
| **ID** | T10 |
| **优先级** | P0 |
| **功能** | 年级自适应讲解、步骤折叠、难度切换 |
| **负责** | Coder-B (UI) + 后端配合 (Prompt Engineering) |
| **依赖** | T4 ✅, T5 ✅ |
| **工作量** | M (3-4 人日) |
| **PRD 章节** | §2.2 F41, §5.6-自适应分步讲解, §9.1-F41 |
| **AC 关联** | AC37, AC38, AC39 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/CollapsibleStepCard.kt` — 可折叠步骤卡片 |
| | `ui/screen/chat/components/DifficultySwitcher.kt` — 难度切换按钮 |
| | `ui/screen/chat/ChatScreen.kt` (修改 — 集成步骤卡片) |
| | `data/repository/UserProfileRepository.kt` — 用户年级信息读取 |
| | (后端: ChatApi prompt 模板加入年级上下文) |
| **描述** | AI 回复以结构化步骤块组织（Step 1/N, Step 2/N…），客户端渲染为可展开/折叠的步骤卡片。用户年级信息（小学/初中/高中/大学）作为 AI prompt 上下文，自动调整讲解详细程度。支持用户手动切换难度级别，AI 重新生成。步骤卡片底部"没看懂"按钮 → 触发 AI 补充说明。 |

### T11 — AI 对话式教学增强 (苏格拉底式)

| 属性 | 内容 |
|------|------|
| **ID** | T11 |
| **优先级** | P0 |
| **功能** | 苏格拉底式引导提问 + 理解度评估 |
| **负责** | Coder-B (UI) + 后端 (Prompt Engineering) |
| **依赖** | T4 ✅, T5 ✅, T14 (F43 数据源 — 理解度记录) |
| **工作量** | M (4-5 人日) |
| **PRD 章节** | §2.2 F42, §5.7-Socratic流程, §9.1-F42 |
| **AC 关联** | AC40, AC41, AC42 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/SocraticBanner.kt` — 教学模式指示器 |
| | `ui/screen/chat/components/TeachingModeToggle.kt` — 教学模式切换 |
| | `ui/screen/chat/ChatViewModel.kt` (修改 — 教学模式状态) |
| | `ui/screen/chat/ChatScreen.kt` (修改 — 教学模式 UI) |
| | `data/repository/LearningProgressRepository.kt` (修改 — 理解度记录) |
| | (后端: Socratic Teaching Prompt 模板) |
| **描述** | 聊天中检测"学习型问题"→ 自动进入苏格拉底式教学模式：概念拆解→引导提问→用户回答→正确性判断→逐步深入→总结归纳。UI 上显示教学 Banner 指示当前模式，引导问题以特殊样式展示。用户可随时退出模式（输入"直接给我答案"）。教学结束后 AI 评估理解度（已掌握/需加强）并记录到学习进度系统。 |

---

## Phase 2A — 语音交互 (P1)

> 第 3-4 周 | 预计总工时：8-10 人日 | 需 Coder-A + Coder-B

### T12 — 语音输入 (ASR)

| 属性 | 内容 |
|------|------|
| **ID** | T12 |
| **优先级** | P1 |
| **功能** | 长按录音、ASR 转文字、云端备选、语音状态 |
| **负责** | Coder-A (Media Layer) |
| **依赖** | T4 ✅ |
| **工作量** | M (4 人日) |
| **PRD 章节** | §2.2 F15, F16, F19, §4.6-VoiceInputBar, §5.2-语音流程 |
| **AC 关联** | AC17, AC18 |
| **创建/修改文件** | |
| | `data/media/VoiceRepository.kt` — 语音仓库 |
| | `data/media/AsrEngine.kt` — Android SpeechRecognizer 封装 |
| | `data/media/CloudAsrEngine.kt` — 云端 ASR 备选 |
| | `ui/screen/chat/components/VoiceInputBar.kt` — 语音输入面板 |
| | `ui/screen/chat/components/VoiceUiState.kt` — 语音状态机 |
| | `ui/screen/chat/ChatInputBar.kt` (修改 — 集成语音按钮) |
| | `ui/screen/chat/ChatViewModel.kt` (修改 — 语音状态管理) |
| **描述** | 实现长按录音→ASR 转文字→自动发送流程。语音面板状态机：IDLE→LISTENING→PROCESSING→SPEAKING。长音频/复杂场景降级到云端 ASR API。上滑取消手势。ASR 超时处理（10s 无声音自动结束）。识别失败提示重试。 |

### T13 — TTS 朗读 + 语音打断

| 属性 | 内容 |
|------|------|
| **ID** | T13 |
| **优先级** | P1 |
| **功能** | 消息 TTS 朗读、云端 TTS 备选、打断支持 |
| **负责** | Coder-A (Media Layer) + Coder-B (UI) |
| **依赖** | T4 ✅, T12 ✅ |
| **工作量** | M (4 人日) |
| **PRD 章节** | §2.2 F17, F18, F20, §4.6-VoiceInputBar |
| **AC 关联** | AC19, AC20 |
| **创建/修改文件** | |
| | `data/media/TtsEngine.kt` — 本地 TextToSpeech 封装 |
| | `data/media/CloudTtsEngine.kt` — 云端 TTS 流式播放 |
| | `ui/screen/chat/components/MessageBubble.kt` (修改 — 添加 TTS 按钮) |
| | `ui/screen/chat/components/VoiceInputBar.kt` (修改 — 打断逻辑) |
| | `data/media/VoiceRepository.kt` (修改 — 打断协调) |
| **描述** | 点击消息气泡的扬声器图标→TTS 朗读。短消息使用本地 TTS 引擎，长消息（>500 字）降级到云端 TTS 流式播放。TTS 播放中可触发 ASR 打断：点击另一消息的 TTS 按钮→打断当前→播放新的。TTS 引擎未安装时弹窗引导。 |

---

## Phase 2B — 学习分析 (P1)

> 第 3-4 周 | 预计总工时：5-8 人日

### T14 — 学习进度仪表盘

| 属性 | 内容 |
|------|------|
| **ID** | T14 |
| **优先级** | P1 |
| **功能** | 概览卡片、知识图谱、趋势分析 |
| **负责** | Coder-B (UI + Canvas) + Coder-A (Data) |
| **依赖** | T1 ✅, T7 ✅ |
| **工作量** | L (6-8 人日) |
| **PRD 章节** | §2.2 F43, §4.8-DashboardScreen |
| **AC 关联** | AC43, AC44, AC45, PERF09 |
| **创建/修改文件** | |
| | `data/local/dao/AnalyticsDao.kt` — 统计查询 DAO |
| | `data/local/entity/LearningRecord.kt` — 学习记录 Entity |
| | `data/repository/AnalyticsRepository.kt` — 统计仓库 |
| | `data/remote/api/AnalyticsApi.kt` — 云端统计 API |
| | `domain/usecase/GetLearningStatsUseCase.kt` |
| | `ui/screen/dashboard/DashboardScreen.kt` — 仪表盘页面 |
| | `ui/screen/dashboard/DashboardViewModel.kt` |
| | `ui/screen/dashboard/components/StatsOverviewCard.kt` — 概览卡片 |
| | `ui/screen/dashboard/components/KnowledgeGraph.kt` — 知识图谱 (Compose Canvas) |
| | `ui/screen/dashboard/components/TrendChart.kt` — 趋势折线图 (Compose Canvas) |
| | `ui/navigation/NavGraph.kt` (修改 — 添加仪表盘 Tab) |
| | `ui/screen/main/MainScreen.kt` (修改 — 底部Tab新增"学习") |
| **描述** | 在 MainScreen 底部导航新增"学习"Tab。仪表盘三个模块：(1) 顶部概览卡片 — 今日学习时长、解题数/正确率、连续天数、总掌握度环图；(2) 知识图谱 — 树/网图，绿(已掌握)/黄(学习中)/红(待加强)；(3) 学习趋势 — 近7/30天折线图（时长/题量/正确率）。下拉刷新+缓存策略（首次≤1s，缓存≤200ms）。空状态引导。 |

---

## Phase 2C — 测评与复习 (P1)

> 第 4-5 周 | 预计总工时：9-13 人日 | 需 Coder-A + Coder-B

### T15 — 交互式测验

| 属性 | 内容 |
|------|------|
| **ID** | T15 |
| **优先级** | P1 |
| **功能** | AI 出题、在线作答、AI 批改 |
| **负责** | Coder-B (UI) + Coder-A (API) |
| **依赖** | T6 ✅ (公式渲染), T14 ✅ |
| **工作量** | L (6-8 人日) |
| **PRD 章节** | §2.2 F44, §4.9-QuizScreen, §5.8-测验流程 |
| **AC 关联** | AC46, AC47, AC48, PERF08 |
| **创建/修改文件** | |
| | `data/remote/api/QuizApi.kt` — 测验 API |
| | `data/remote/dto/QuizDto.kt` — 测验 DTO |
| | `data/repository/QuizRepository.kt` — 测验仓库 |
| | `ui/screen/quiz/QuizScreen.kt` — 测验页面 |
| | `ui/screen/quiz/QuizViewModel.kt` — 测验 ViewModel |
| | `ui/screen/quiz/components/QuestionCard.kt` — 题目卡片 |
| | `ui/screen/quiz/components/AnswerOption.kt` — 选项组件 |
| | `ui/screen/quiz/components/QuizResultCard.kt` — 批改结果 |
| | `ui/navigation/NavGraph.kt` (修改 — 添加测验路由) |
| **描述** | 用户选择学科/知识点/难度/题数→POST /api/v1/quiz/generate→AI 出题→逐题展示（单选/多选/填空，支持 MD 公式）→提交 POST /api/v1/quiz/submit→AI 批改（✅/❌、正解、错因解析、掌握度评分、学习建议）。答错题目自动加入错题本。生成超时(>15s)引导重试，提交失败本地缓存后自动重试。 |

### T16 — 间隔重复复习 (错题本 + 遗忘曲线)

| 属性 | 内容 |
|------|------|
| **ID** | T16 |
| **优先级** | P1 |
| **功能** | 错题自动收录、遗忘曲线复习、本地通知 |
| **负责** | Coder-A (Engine) + Coder-B (UI) |
| **依赖** | T15 ✅ |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F45, §4.10-ReviewScreen, §5.9-复习流程 |
| **AC 关联** | AC49, AC50, AC51, AC52 |
| **创建/修改文件** | |
| | `data/local/dao/WrongAnswerDao.kt` — 错题 DAO |
| | `data/local/entity/WrongAnswerEntity.kt` — 错题 Entity |
| | `data/repository/WrongAnswerRepository.kt` — 错题仓库 |
| | `domain/engine/SpacedRepetitionEngine.kt` — 遗忘曲线算法 (EBM 变体) |
| | `data/local/NotificationHelper.kt` — 复习通知 |
| | `ui/screen/review/ReviewScreen.kt` — 复习页面 |
| | `ui/screen/review/ReviewViewModel.kt` |
| | `ui/screen/review/components/ReviewCard.kt` — 复习卡片 |
| | `ui/screen/review/components/ReviewCalendar.kt` — 复习日历 |
| | `ui/navigation/NavGraph.kt` (修改 — 添加复习路由) |
| **描述** | 实现全来源错题自动收录（对话/测验/解题答错）。EBM 变体遗忘曲线算法：首次 1 天→递增 3/7/14 天，正确间隔加倍、错误重置为 1 天。复习时间到达→本地通知。复习模式展示题目（隐藏答案）→用户作答→比对→更新间隔→连续 3 次正确标记"已掌握"。分页加载、按学科过滤。 |

---

## Phase 2D — 设置 & 配置 (P1)

> 第 4 周 | 预计总工时：3-4 人日

### T17 — 设置页面

| 属性 | 内容 |
|------|------|
| **ID** | T17 |
| **优先级** | P1 |
| **功能** | 模型选择、参数调整、主题切换、搜索历史 |
| **负责** | Coder-B (UI) |
| **依赖** | T1 ✅, T7 ✅ |
| **工作量** | M (3-4 人日) |
| **PRD 章节** | §2.2 F25, F26, F27, F28, §4.8-SettingsScreen |
| **AC 关联** | AC24, AC25, AC26 |
| **创建/修改文件** | |
| | `ui/screen/settings/SettingsScreen.kt` — 设置页面 |
| | `ui/screen/settings/SettingsViewModel.kt` — 设置 ViewModel |
| | `data/local/AppPreferences.kt` — DataStore 偏好设置 |
| | `data/remote/api/ModelApi.kt` — 模型列表 API |
| | `data/repository/SettingsRepository.kt` — 设置仓库 |
| | `ui/screen/chat/components/SearchBar.kt` — 搜索组件 |
| | `ui/screen/chat/ConversationListSheet.kt` (修改 — 集成搜索) |
| **描述** | 实现分组设置页：模型选择（下拉列表 GET /api/v1/models）、Temperature(0-2)/Top-P(0-1)/Max Tokens(256-4096) 滑块调节、浅色/深色/跟随系统主题切换、TTS 语速音色、清空对话/清除缓存、版本号/关于。搜索历史对话（按关键词匹配会话标题和消息内容）。所有设置通过 DataStore 持久化。 |

---

## Phase 3A — 游戏化 (P2)

> 第 5 周 | 预计总工时：4-6 人日

### T18 — 游戏化系统

| 属性 | 内容 |
|------|------|
| **ID** | T18 |
| **优先级** | P2 |
| **功能** | 成就系统、连胜机制、排行榜 |
| **负责** | Coder-A (Engine) + Coder-B (UI) |
| **依赖** | T14 ✅ (学习数据) |
| **工作量** | M (4-6 人日) |
| **PRD 章节** | §2.2 F46, §9.1-F46 |
| **AC 关联** | AC53, AC54, AC55 |
| **创建/修改文件** | |
| | `domain/engine/GamificationEngine.kt` — 成就检测引擎 |
| | `data/local/dao/AchievementDao.kt` — 成就 DAO |
| | `data/local/entity/AchievementEntity.kt` — 成就 Entity |
| | `data/repository/GamificationRepository.kt` |
| | `ui/screen/dashboard/components/AchievementBadge.kt` — 成就徽章组件 |
| | `ui/screen/dashboard/components/StreakIndicator.kt` — 连胜指示器 |
| | `ui/screen/dashboard/components/LeaderboardView.kt` — 排行榜 |
| | `data/remote/api/LeaderboardApi.kt` — 排行榜 API |
| **描述** | 成就引擎：预设成就徽章（首次解题、连续7天学习、答对100题、学霸等）→ 学习行为触发 → 条件满足自动解锁 → 弹窗动画"成就解锁！"。连胜：连续学习天数自动累计（中断重置），展示于仪表盘。排行榜：学习积分（综合学习时长/解题数/正确率）排名，支持全局/好友排行，每日更新。本地优先+云端同步。 |

---

## Phase 3B — 语音增强 & 个人中心 (P2)

> 第 5-6 周 | 预计总工时：5-7 人日

### T19 — 语音交互增强

| 属性 | 内容 |
|------|------|
| **ID** | T19 |
| **优先级** | P2 |
| **功能** | ASR 降噪优化、打断流畅度 |
| **负责** | Coder-A (Media) |
| **依赖** | T12 ✅, T13 ✅ |
| **工作量** | M (3-4 人日) |
| **PRD 章节** | §2.2 F47, §9.1-F47 |
| **AC 关联** | AC56, AC57 |
| **创建/修改文件** | |
| | `data/media/NoiseSuppression.kt` — 环境自适应降噪 |
| | `data/media/VoiceRepository.kt` (修改 — 降噪集成) |
| | `data/media/CloudAsrEngine.kt` (修改 — 噪声环境下云降级策略) |
| | `data/media/VoiceRepository.kt` (修改 — 打断优化) |
| **描述** | ASR 识别率优化：噪声环境下自动降级到云端 ASR + 多麦克风融合 + 环境自适应降噪算法。语音打断优化：TTS 播放中用户触发 ASR → 旧 TTS 立即停止 → ASR 开始监听 → 识别后处理新请求，打断延迟 < 300ms，无杂音/爆音。 |

### T20 — 用户中心 + 订阅

| 属性 | 内容 |
|------|------|
| **ID** | T20 |
| **优先级** | P2 |
| **功能** | 个人资料编辑、订阅管理、退出登录 |
| **负责** | Coder-B (UI) |
| **依赖** | T2 ✅ |
| **工作量** | S (2 人日) |
| **PRD 章节** | §2.2 F29, F30, §4.9-ProfileScreen, §4.10-SubscriptionScreen |
| **AC 关联** | AC29, AC30, AC31 |
| **创建/修改文件** | |
| | `ui/screen/profile/ProfileScreen.kt` — 个人中心 |
| | `ui/screen/profile/ProfileViewModel.kt` |
| | `ui/screen/subscription/SubscriptionScreen.kt` — 订阅页面 |
| | `ui/screen/subscription/SubscriptionViewModel.kt` |
| | `ui/screen/settings/SettingsScreen.kt` (修改 — 添加个人中心入口) |
| | `ui/navigation/NavGraph.kt` (修改 — 添加 profile/subscription 路由) |
| **描述** | 个人中心：头像（圆形可点击更换）+ 昵称 + 年级 + 手机号（只读）+ 编辑功能 + 退出登录（清除 Token + 跳转登录）。订阅页面：当前计划卡片 + 配额进度（免费版 N/5）+ 功能对比表 + 订阅按钮（跳转外部支付）。 |

---

## Phase 3C — 体验完善 (P2)

> 第 6 周 | 预计总工时：2-3 人日

### T21 — UX 完善项

| 属性 | 内容 |
|------|------|
| **ID** | T21 |
| **优先级** | P2 |
| **功能** | 清除缓存、通知、空状态、错误重试 …… 散项 |
| **负责** | Coder-B (UI) |
| **依赖** | T4 ✅, T7 ✅ |
| **工作量** | S (2-3 人日) |
| **PRD 章节** | §2.2 F31, F32, F34, F35 |
| **AC 关联** | AC32, AC33 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/EmptyStateView.kt` (已有 — 确认/增强) |
| | `ui/screen/chat/components/ErrorView.kt` (已有 — 确认/增强) |
| | `data/local/CacheManager.kt` — 缓存清理 |
| | `data/local/NotificationHelper.kt` (已有 — 确认/增强) |
| | `ui/screen/settings/SettingsScreen.kt` (修改 — 清空/缓存入口) |
| **描述** | 清理图片缓存和非核心数据（带确认弹窗）。本地通知：后台新消息时通知（需后端推送配合）。EmptyState：无会话/无消息时的欢迎引导提示。错误重试：网络失败时消息气泡显示重试按钮，点击重新发送。 |

---

> 以上 T1-T21 为 Phase 1~3 原始拆分，以下 T22-T31 为补齐冲刺新增。

---

## Phase 4A — P0 核心补齐（补全冲刺）

> Sprint 1 | 第 1-2 周 | 预计总工时：12-16 人日 | 2 Coder 并行（无互依赖）

### T22 — F40 拍照解题增强：全学科 + SSE 流式

| 属性 | 内容 |
|------|------|
| **ID** | T22 |
| **优先级** | P0 |
| **功能** | 全学科 OCR + 学科选择器 + 流式分步解题完成 |
| **负责** | Coder-A (Integration) + Coder-B (UI) |
| **依赖** | T5 (SSE) ✅, T8 (拍照基础) ✅ |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F40, §4.7-CameraScreen, §9.1-F40 |
| **AC 关联** | AC34, AC35, AC36, PERF07 |
| **创建/修改文件** | |
| | `ui/screen/camera/components/SubjectSelector.kt` — 新增学科选择器 |
| | `data/remote/stream/SolveStreamParser.kt` — 新增解题 SSE 流解析器 |
| | `data/repository/SolveRepository.kt` — 新建/修改 |
| | `ui/screen/chat/components/StepByStepCard.kt` — 新增分步卡片组件 |
| | `ui/screen/camera/CameraScreen.kt` — 修改：集成 SubjectSelector |
| | `data/remote/api/SolveApi.kt` — 修改：学科参数 + SSE 端点 |
| **描述** | 在已有基础拍照解题上完成增强：(1) 学科选择器（数学/物理/化学/生物/语文/英语/自动），拍照前可选；(2) 解题结果通过 SSE 流式逐步骤返回，渲染为 Step 1/N 分步卡片；(3) 端到端延迟 ≤5s。保留并增强已有拍照上传功能。 |

### T23 — F41 自适应分步讲解

| 属性 | 内容 |
|------|------|
| **ID** | T23 |
| **优先级** | P0 |
| **功能** | 年级自适应讲解、步骤折叠、难度切换 |
| **负责** | Coder-B (UI) + 后端 (Prompt Engineering) |
| **依赖** | T4 (聊天界面) ✅, T5 (SSE) ✅, UserProfile 年级信息 |
| **工作量** | M (4 人日) |
| **PRD 章节** | §2.2 F41, §5.6-自适应分步讲解, §9.1-F41 |
| **AC 关联** | AC37, AC38, AC39 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/CollapsibleStepCard.kt` — 新增可折叠步骤卡片 |
| | `ui/screen/chat/components/DifficultySwitcher.kt` — 新增难度切换按钮 |
| | `ui/screen/chat/components/StepProgressIndicator.kt` — 新增步骤进度指示器 |
| | `data/repository/UserProfileRepository.kt` — 新建/修改用户年级信息读取 |
| | `ui/screen/chat/ChatScreen.kt` — 修改：集成步骤卡片 |
| | `data/repository/ChatRepository.kt` — 修改：grade 参数传递 |
| **描述** | AI 回复以 Step 1/N 结构化块 SSE 流式渲染。步骤卡片可展开/折叠，底部「没看懂」按钮→补充说明。用户年级字段（小学/初中/高中/大学）作为 prompt 上下文自动调整详细程度。手动切换难度后重新生成。年级缺失默认「初中」。 |

### T24 — F42 AI 对话式教学增强（苏格拉底式）

| 属性 | 内容 |
|------|------|
| **ID** | T24 |
| **优先级** | P0 |
| **功能** | 苏格拉底式引导提问 + 理解度评估 |
| **负责** | Coder-B (UI) + 后端 (Prompt Engineering) |
| **依赖** | T4 ✅, T5 ✅, T14 (F43 理解度记录) |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F42, §5.7-Socratic流程, §9.1-F42 |
| **AC 关联** | AC40, AC41, AC42 |
| **创建/修改文件** | |
| | `domain/model/ChatMode.kt` — 新增 ChatMode 枚举 (ASSISTANT/TUTOR/QUIZ) |
| | `domain/model/TeachingState.kt` — 新增教学状态模型 |
| | `ui/screen/chat/components/SocraticBanner.kt` — 新增教学模式横幅 |
| | `ui/screen/chat/components/TeachingModeToggle.kt` — 新增教学模式切换 |
| | `ui/screen/chat/components/SocraticQuestionBubble.kt` — 新增引导问题气泡 |
| | `ui/screen/chat/components/UnderstandingBadge.kt` — 新增理解度徽章 |
| | `ui/screen/chat/ChatViewModel.kt` — 修改：ChatMode 状态管理 |
| | `ui/screen/chat/ChatScreen.kt` — 修改：教学 UI 集成 |
| | `data/repository/LearningProgressRepository.kt` — 修改：理解度记录 |
| **描述** | AI 检测学习型问题→自动进入苏格拉底式教学：概念拆解→引导提问→用户回答→正确性判断→逐步深入→总结归纳。引导问题以紫色气泡展示，SocraticBanner 指示当前模式。用户可输入"直接给我答案"退出。教学结束生成理解度评估（已掌握/需加强）。30 分钟无交互自动退出。 |

---

## Phase 4B — P1 语音完善 + 图片消息（补全冲刺）

> Sprint 2 | 第 3 周 | 预计总工时：7-9 人日 | 2 Coder 并行

### T25 — F16 云端 ASR 备选

| 属性 | 内容 |
|------|------|
| **ID** | T25 |
| **优先级** | P1 |
| **功能** | 长音频/复杂场景降级到云端 ASR API |
| **负责** | Coder-A (Media Layer) |
| **依赖** | T12 (本地 ASR) ✅ |
| **工作量** | S (2 人日) |
| **PRD 章节** | §2.2 F16, §5.2-语音流程 |
| **AC 关联** | AC17 |
| **创建/修改文件** | |
| | `data/media/CloudAsrEngine.kt` — 新增云端 ASR 备选 |
| | `data/media/VoiceRepository.kt` — 修改：降级逻辑 |
| **描述** | 长音频（>30s）或本地识别置信度过低（<0.6）时自动降级到云端 ASR API。实现两段式策略：先试本地（低延迟），失败/低分后触发云端备选，返回后替换。 |

### T26 — F18 云端 TTS + F20 语音打断协调完成

| 属性 | 内容 |
|------|------|
| **ID** | T26 |
| **优先级** | P1 |
| **功能** | 长文本云端 TTS 流式播放 + 打断协调完整实现 |
| **负责** | Coder-A (Media) + Coder-B (UI) |
| **依赖** | T13 (TTS 基础) ✅ |
| **工作量** | M (4 人日) |
| **PRD 章节** | §2.2 F18, F20, §4.6-VoiceInputBar |
| **AC 关联** | AC19, AC20 |
| **创建/修改文件** | |
| | `data/media/CloudTtsEngine.kt` — 新增云端 TTS 流式播放 |
| | `data/media/VoiceRepository.kt` — 修改：打断协调逻辑完善 |
| | `ui/screen/chat/components/VoiceInputBar.kt` — 修改：打断交互流畅化 |
| **描述** | 长文本（>500 字）自动降级云端 TTS 流式播放，短文本使用本地引擎。TTS 播放中触发 ASR → 旧播放立即停止 → ASR 监听 → 识别后处理新请求，打断延迟 < 300ms，无杂音/爆音。点击另一消息 TTS 按钮→打断当前→播放新的。 |

### T27 — F24 图片消息渲染

| 属性 | 内容 |
|------|------|
| **ID** | T27 |
| **优先级** | P1 |
| **功能** | 消息气泡中显示图片缩略图，点击全屏预览 |
| **负责** | Coder-B (UI) |
| **依赖** | T4 (聊天界面) ✅, T8 (拍照) ✅ |
| **工作量** | S (1 人日) |
| **PRD 章节** | §2.2 F24, §4.4-ChatScreen |
| **AC 关联** | AC23 |
| **创建/修改文件** | |
| | `ui/screen/chat/components/ImageMessage.kt` — 新增图片消息组件 |
| | `ui/screen/chat/components/MessageBubble.kt` — 修改：图片类型分支 |
| **描述** | 图片消息在气泡中显示缩略图（320dp 最大宽/高），点击→全屏预览（PhotoView 支持缩放）。加载中显示占位图+动画，失败显示「图片加载失败」+ 重试按钮。 |

---

## Phase 4C — P2 体验补齐（补全冲刺）

> Sprint 3 | 第 4 周 | 预计总工时：10-13 人日 | 2 Coder 并行

### T28 — F30 订阅管理后端绑定

| 属性 | 内容 |
|------|------|
| **ID** | T28 |
| **优先级** | P2 |
| **功能** | 订阅状态对接后端 API，真实配额绑定 |
| **负责** | Coder-B (UI) + Coder-A (Data) |
| **依赖** | T20 (个人中心) ✅ |
| **工作量** | S (2 人日) |
| **PRD 章节** | §2.2 F30, §4.13-SubscriptionScreen |
| **AC 关联** | AC30, AC31 |
| **创建/修改文件** | |
| | `ui/screen/subscription/SubscriptionScreen.kt` — 修改：绑定真实数据 |
| | `ui/screen/subscription/SubscriptionViewModel.kt` — 修改：API 对接 |
| | `data/repository/SubscriptionRepository.kt` — 新建/修改 |
| **描述** | 订阅页面对接后端 `GET /api/v1/subscription/status` 真实数据。配额进度条反映实际使用量（daily_used/daily_quota），功能对比表根据订阅类型动态展示。 |

### T29 — F31 清除缓存 + F32 新消息通知

| 属性 | 内容 |
|------|------|
| **ID** | T29 |
| **优先级** | P2 |
| **功能** | 缓存清理 + 本地通知 |
| **负责** | Coder-B (UI) |
| **依赖** | T7 (Room) ✅, T17 (设置) ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F31, F32 |
| **AC 关联** | AC32, AC33 |
| **创建/修改文件** | |
| | `data/local/CacheManager.kt` — 新增缓存清理管理器 |
| | `data/local/NotificationHelper.kt` — 新增/增强通知辅助类 |
| | `ui/screen/settings/SettingsScreen.kt` — 修改：清除缓存 + 通知设置入口 |
| | `AndroidManifest.xml` — 修改：通知权限声明 |
| **描述** | 设置页新增「清除缓存」按钮+确认弹窗，清理图片缓存/非核心数据/日志。新 AI 消息到达时发送本地通知（NotificationCompat + 通知渠道），点击跳转到对应会话。 |

### T30 — F46 游戏化系统

| 属性 | 内容 |
|------|------|
| **ID** | T30 |
| **优先级** | P2 |
| **功能** | 成就系统 + 连胜机制 + 排行榜 |
| **负责** | Coder-A (Engine) + Coder-B (UI) |
| **依赖** | T14 (仪表盘) ✅ |
| **工作量** | M (5 人日) |
| **PRD 章节** | §2.2 F46, §9.1-F46 |
| **AC 关联** | AC53, AC54, AC55 |
| **创建/修改文件** | |
| | `domain/engine/GamificationEngine.kt` — 新增成就检测引擎 |
| | `data/local/dao/AchievementDao.kt` — 新增成就 DAO |
| | `data/local/entity/AchievementEntity.kt` — 新增成就 Entity |
| | `data/repository/GamificationRepository.kt` — 新增游戏化仓库 |
| | `ui/screen/dashboard/components/AchievementBadge.kt` — 新增成就徽章组件 |
| | `ui/screen/dashboard/components/StreakIndicator.kt` — 新增连胜指示器 |
| | `ui/screen/dashboard/components/LeaderboardView.kt` — 新增排行榜组件 |
| **描述** | 成就引擎：预设成就徽章（首次解题/连续7天学习/答对100题/学霸等），学习行为触发自动检测→条件满足解锁→弹窗动画。连胜自动累计中断重置。排行榜：学习积分（时长/解题数/正确率综合），支持全局/好友，每日更新。本地优先+云端同步。 |

---

## Phase 4D — 语音增强（可选，P2）

> Sprint 4 | 第 5 周 | 预计总工时：3 人日

### T31 — F47 语音交互增强

| 属性 | 内容 |
|------|------|
| **ID** | T31 |
| **优先级** | P2 |
| **功能** | ASR 降噪优化 + 打断流畅度进一步提升 |
| **负责** | Coder-A (Media) |
| **依赖** | T12 ✅, T13 ✅ |
| **工作量** | M (3 人日) |
| **PRD 章节** | §2.2 F47, §9.1-F47 |
| **AC 关联** | AC56, AC57 |
| **创建/修改文件** | |
| | `data/media/NoiseSuppression.kt` — 新增环境自适应降噪 |
| | `data/media/VoiceRepository.kt` — 修改：降噪集成 |
| | `data/media/CloudAsrEngine.kt` — 修改：噪声环境降级策略 |
| **描述** | ASR 识别率优化：噪声环境自动降级到云端 ASR + 环境自适应降噪。打断延迟持续 < 300ms，识别率在 60dB 噪声环境下提升 ≥15%。 |

---

## 更新后任务依赖全景图

```
T1 脚手架 ──────┬── T2 认证 ─── T20 个人中心 ─── T28 订阅绑定
                ├── T3 会话管理 ─── T4 聊天界面 ─── T27 图片消息
                │                   ├── T5 SSE 流式 ─── T22 拍照增强SSE
                │                   │                └── T23 自适应讲解
                │                   │                └── T24 苏格拉底式
                │                   ├── T6 Markdown渲染
                │                   └── T7 持久化 ─── T29 缓存+通知
                ├── T8 拍照基础 ─── T9 拍照增强
                │                └── T22 全学科SSE
                ├── T10 自适应讲解 (→ T23 改为独立Sprint)
                ├── T11 苏格拉底式 (→ T24 改为独立Sprint)
                ├── T12 ASR ─── T13 TTS+打断 ─── T25 云端ASR
                │                              ├── T26 TTS+打断完善
                │                              └── T31 语音增强(可选)
                └── T14 仪表盘 ─── T15 测验 ─── T16 间隔重复
                                  └── T18 游戏化 (→ T30)

新任务标记: T22-T31
```

**任务启动条件：** 所有依赖（"依赖"列）为 ✅ 即可启动。
**并行策略：** T22/T23/T24 无互依赖可 3 路并行；T25/T26/T27 可并行；T28/T29/T30 可并行。

---

## 更新后排期总览

| 阶段 | 时间 | 任务 | 预估人日 |
|------|------|------|:--------:|
| **Phase 1S** | 第 1 周 | T1 + T2 | 6-8 |
| **Phase 1A** | 第 1-2 周 | T3 + T4 + T5 + T6 + T7 | 12-16 |
| **Phase 1B** | 第 2-3 周 | T8 + T9 | 7-10 |
| **Phase 1C** | 第 2-3 周 | T10 + T11 | 7-9 |
| **Phase 2A** | 第 3-4 周 | T12 + T13 | 8-10 |
| **Phase 2B** | 第 3-4 周 | T14 | 6-8 |
| **Phase 2C** | 第 4-5 周 | T15 + T16 | 9-13 |
| **Phase 2D** | 第 4 周 | T17 | 3-4 |
| **Phase 3A** | 第 5 周 | T18 | 4-6 |
| **Phase 3B** | 第 5-6 周 | T19 + T20 | 5-7 |
| **Phase 3C** | 第 6 周 | T21 | 2-3 |
| **— 原始总计 —** | **— 6 周 —** | **— T1-T21 —** | **— 69-94 人日 —** |
| **Phase 4A** (补全) | 第 1-2 周 | T22 + T23 + T24 | 12-16 |
| **Phase 4B** (补全) | 第 3 周 | T25 + T26 + T27 | 7-9 |
| **Phase 4C** (补全) | 第 4 周 | T28 + T29 + T30 | 10-13 |
| **Phase 4D** (可选) | 第 5 周 | T31 | 3 |
| **总计** | **约 4-5 周（追加）** | **31 个子任务（T1-T31）** | **101-135 人日** |

> **注：** Phase 4A/B/C/D 为补全冲刺阶段，可与原始 Phase 2-3 并行推进。
> 建议配置 2 名 Coder（Coder-A: Data/Network/Media；Coder-B: UI/Compose）。
> 测试、Code Review、Bug Fix 工时未计入。完成 T22-T30 后总完成率可达 95.5%。
