# 🧠 AI Tutor — 智能 AI 学伴

> **面向 8-14 岁青少年的 AI 学习助手** · 基于苏格拉底问答法，引导学生独立思考

AI Tutor 是一款专为青少年设计的智能辅导应用。它不只给答案——它通过 **苏格拉底问答法（Socratic Method）** 一步步启发式提问，引导孩子自己找到解题思路，真正培养独立思考和解决问题的能力。

---

## ✨ 功能特性 Features

| 功能 | 说明 |
|------|------|
| 📷 **拍照解题** | 拍下题目（数学/物理/化学等），OCR 识别后自动解析并引导解题 |
| 💬 **AI 对话教学** | 基于大语言模型的智能对话，支持多轮追问与学科知识解答 |
| 🎤 **语音交互** | ASR 语音识别输入 + TTS 文字转语音朗读，解放双手 |
| 🧩 **自适应讲解** | 根据学生水平和答题情况动态调整讲解深度与节奏 |
| 📊 **学习进度** | 每日学习配额追踪、历史问题记录、知识点掌握度分析 |
| 🏆 **游戏化激励** | 积分、成就徽章、学习排行，让学习像闯关一样有趣 |

---

## 🛠️ 技术栈 Tech Stack

### 前端 Android

| 技术 | 用途 |
|------|------|
| **Kotlin** | 核心开发语言 |
| **Jetpack Compose** | 声明式 UI 框架 + Material 3 设计 |
| **Hilt** | 依赖注入（DI） |
| **Room** | 本地数据持久化 |
| **Retrofit + OkHttp** | 网络请求与 SSE 流式通信 |
| **CameraX** | 相机拍照 |
| **ML Kit** | OCR 文字识别 |
| **Coil** | 图片加载 |
| **DataStore** | 键值对本地存储 |
| **Navigation Compose** | 页面导航 |

### 后端 Backend

| 技术 | 用途 |
|------|------|
| **FastAPI** | 高性能异步 Web 框架 |
| **Python 3.12+** | 运行时 |
| **SQLAlchemy 2.0 (async)** | 异步 ORM |
| **SQLite / PostgreSQL 16** | 数据库（开发/生产） |
| **Redis 7** | 缓存（预留） |
| **JWT** | 用户认证 |
| **Docker Compose** | 容器化部署 |
| **Alembic** | 数据库迁移 |

---

## 🚀 快速开始 Quick Start

### 环境要求

| 工具 | 版本 |
|------|------|
| **Android Studio** | Hedgehog (2023.1.1+) |
| **JDK** | 17+ |
| **Android SDK** | API 34 |
| **Gradle** | 8.x（项目内置 wrapper） |
| **Python** | 3.12+（后端开发） |

### Android 客户端

```bash
# 1. 克隆项目
git clone https://github.com/linlin515/ai-tutor.git
cd ai-tutor

# 2. 用 Android Studio 打开项目根目录
#    等待 Gradle 同步完成

# 3. 运行
#    - 选择 android/app 模块
#    - 点击 Run ▶ 或执行：
./android/gradlew :app:installDebug
```

### 后端服务

```bash
# 进入后端目录
cd backend

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 填入实际配置

# 启动开发服务器（热重载）
uvicorn app.main:app --reload --host 0.0.0.0 --port 8100
```

启动后访问：

- **API 服务**: http://localhost:8100
- **交互式文档**: http://localhost:8100/docs
- **健康检查**: http://localhost:8100/api/v1/health

---

## 📦 构建发布 Build & Release

### Release APK

```bash
# 需要设置签名环境变量
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password

# 构建 Release APK
./android/gradlew :app:assembleRelease
```

构建产物位于：`android/app/build/outputs/apk/release/app-release.apk`

> ⚠️ 签名配置详见 `android/app/build.gradle.kts` 中的 `signingConfigs.release`

### 下载

| 资源 | 地址 |
|------|------|
| 📱 **APK 下载** | [https://aitutor.googlecloud.ccwu.cc/download.apk](https://aitutor.googlecloud.ccwu.cc/download.apk) |
| 📖 **API 文档** | [https://aitutor.googlecloud.ccwu.cc/docs](https://aitutor.googlecloud.ccwu.cc/docs) |
| 🔷 **Swagger UI** | [https://aitutor.googlecloud.ccwu.cc/docs](https://aitutor.googlecloud.ccwu.cc/docs) |

---

## 📁 项目结构 Project Structure

```
ai-tutor/
├── android/              # Android 客户端（Kotlin + Jetpack Compose）
│   ├── app/
│   │   ├── src/main/     # 主源码（UI / ViewModel / Repository / DI）
│   │   └── build.gradle.kts
│   ├── gradle/
│   └── settings.gradle.kts
├── backend/              # FastAPI 后端服务（Python）
│   ├── app/
│   │   ├── main.py       # 应用入口
│   │   ├── routers/      # API 路由（auth / chat / solve / audio ...）
│   │   ├── models/       # 数据模型（User / Subscription / QuestionRecord ...）
│   │   ├── schemas/      # Pydantic 验证模型
│   │   ├── services/     # 业务逻辑（AI / OCR）
│   │   └── middleware/    # 中间件（JWT / 内容安全）
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── requirements.txt
├── docs/                 # 项目文档
│   ├── PRD.md            # 产品需求文档
│   ├── ARCH.md           # 架构设计
│   ├── API.md            # API 文档
│   ├── TEST_REPORT.md    # 测试报告
│   └── ...               # 更多文档
├── version.properties    # 语义版本号
└── README.md             # 本文件
```

---

## 🔗 相关链接 Related Links

| 链接 | 地址 |
|------|------|
| 🌐 **GitHub 仓库** | [https://github.com/linlin515/ai-tutor](https://github.com/linlin515/ai-tutor) |
| 📖 **API 文档（Swagger）** | [https://aitutor.googlecloud.ccwu.cc/docs](https://aitutor.googlecloud.ccwu.cc/docs) |
| 📱 **APK 下载** | [https://aitutor.googlecloud.ccwu.cc/download.apk](https://aitutor.googlecloud.ccwu.cc/download.apk) |
| 📄 **隐私政策** | [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) |
| 🗺️ **路线图** | [docs/ROADMAP.md](docs/ROADMAP.md) |

---

## 🤝 贡献指南 Contributing

欢迎贡献！请先阅读 [docs/PRD.md](docs/PRD.md) 了解产品定位，然后：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

---

## 📄 许可证 License

[MIT](LICENSE) © AI Tutor Team

---

<p align="center">
  <sub>用 AI 启发思考，而不是替代思考。</sub>
</p>
