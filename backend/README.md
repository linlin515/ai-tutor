# 🤖 AI 学伴后端

> **面向 8-14 岁青少年的 AI 学习助手后端服务**
> 基于苏格拉底问答法，引导学生独立思考，而不是直接给答案。

## 📋 项目简介

AI 学伴（AI Tutor）是一个专为青少年设计的智能学习辅助系统后端。它通过苏格拉底问答法（启发式提问）引导学生独立思考，支持多学科问题解答、拍照解题、语音交互等功能。

### 核心功能

- 💬 **智能对话** — 基于苏格拉底问答法的引导式学习对话
- 📷 **拍照解题** — 上传题目图片，OCR 识别后自动解题
- 🎤 **语音交互** — 语音识别（ASR）和文本转语音（TTS）
- 🔐 **用户认证** — 手机号验证码注册/登录，JWT 身份认证
- 📊 **订阅管理** — Google Play 订阅验证与状态管理
- 📈 **学习记录** — 每日配额、问题记录追踪
- 🔄 **流式对话** — SSE 实时流式对话（兼容 OpenAI API 格式）

---

## 🛠️ 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **框架** | FastAPI 0.115+ | 高性能异步 Web 框架 |
| **运行时** | Python 3.12+ | — |
| **数据库** | SQLite / PostgreSQL 16 | 开发用 SQLite，生产用 PostgreSQL |
| **ORM** | SQLAlchemy 2.0 (async) | 异步数据库 ORM |
| **缓存** | Redis 7 | 缓存与队列（预留） |
| **认证** | JWT (python-jose) | Bearer Token 认证 |
| **AI 网关** | new-api | 兼容 OpenAI API 的模型代理 |
| **部署** | Docker Compose | 容器化一键部署 |

---

## 🚀 快速开始

### 环境要求

- Python 3.12+
- pip / uv / poetry（任选）
- Docker & Docker Compose（可选，容器部署时需要）

### 1. 本地开发启动

```bash
# 克隆项目
cd ai-tutor-backend

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate  # Linux/Mac
# 或 venv\Scripts\activate   # Windows

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入实际配置

# 启动开发服务器（热重载）
uvicorn app.main:app --reload --host 0.0.0.0 --port 8100
```

### 2. Docker Compose 部署

```bash
# 1. 准备 Docker 环境变量
cp .env.docker .env
# 编辑 .env，修改 NEW_API_KEY 等配置

# 2. 一键启动所有服务
docker compose up -d

# 3. 查看日志
docker compose logs -f app

# 4. 停止服务
docker compose down

# 5. 停止并删除数据卷（会清空数据库）
docker compose down -v
```
### 启动后访问

- **API 服务**: http://localhost:8100（本地开发） / https://aitutor.googlecloud.ccwu.cc（生产）
- **API 文档**: http://localhost:8100/docs
- **健康检查**: http://localhost:8100/api/v1/health

---

## 🐳 Docker 部署架构

```
┌─────────────────────────────────────────────────────┐
│                      Docker                          │
│  ┌──────────────┐   ┌──────────┐   ┌──────────┐    │
│  │   ai-tutor    │   │ postgres │   │  redis   │    │
│  │     app       │◄──│   16     │   │    7     │    │
│  │   :8100       │   │  :5432   │   │  :6379   │    │
│  └──────┬───────┘   └──────────┘   └──────────┘    │
│         │                                            │
│         │  host.docker.internal:3000                 │
│         └──────────────────────────────────┐         │
│                                            │         │
└────────────────────────────────────────────┼─────────┘
                                             │
                                   ┌─────────▼──────┐
                                   │    new-api      │
                                   │  (宿主机/其他)  │
                                   └────────────────┘
```

### 容器说明

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| **app** | 自定义构建 | 8100 | FastAPI 后端应用 |
| **postgres** | postgres:16-alpine | 5432 | 主数据库 |
| **redis** | redis:7-alpine | 6379 | 缓存（预留） |

### Docker 数据卷

- `ai-tutor-pgdata` — PostgreSQL 数据持久化
- `ai-tutor-redisdata` — Redis 数据持久化

---

## 🔗 API 文档

服务启动后，交互式 API 文档可通过以下地址访问：

- **Swagger UI**: http://localhost:8100/docs
- **ReDoc**: http://localhost:8100/redoc

### 主要接口一览

#### 系统

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 服务信息 |
| GET | `/api/v1/health` | 健康检查 |

#### 认证 (prefix: `/api/v1/auth`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/register` | 用户注册（手机号 + 密码） |
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/refresh` | Token 刷新 |

#### 模型

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/models` | 获取可用 AI 模型列表 |

#### 用户 (prefix: `/api/v1/user`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/user/profile` | 获取用户资料 |
| PUT | `/api/v1/user/profile` | 更新用户资料 |

#### 对话 (prefix: `/api/v1/chat`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat` | 提交问题（AI 解题） |
| GET | `/api/v1/chat/history` | 获取对话历史 |
| POST | `/api/v1/chat/solve-photo` | 拍照解题（上传图片） |
| POST | `/api/v1/chat/completions` | OpenAI 兼容流式对话（SSE） |

#### 订阅 (prefix: `/api/v1/subscription`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/subscription/status` | 查询订阅状态 |
| POST | `/api/v1/subscription/verify` | 验证 Google Play 订阅 |

#### 语音 (prefix: `/v1/audio`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/audio/transcriptions` | ASR 语音识别 |
| POST | `/v1/audio/speech` | TTS 文本转语音 |

---

## 🔧 环境变量说明

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `DATABASE_URL` | 是 | `sqlite+aiosqlite:///./ai_tutor.db` | 数据库连接字符串（Docker 使用 PostgreSQL） |
| `REDIS_URL` | 否 | — | Redis 连接字符串（Docker 中使用） |
| `NEW_API_BASE_URL` | 是 | `http://localhost:3000/v1` | new-api 代理地址（AI 模型网关） |
| `NEW_API_KEY` | 是 | `sk-you...here` | new-api 的 API Key |
| `OCR_ENABLED` | 否 | `true` | 是否启用 OCR 功能 |
| `SECRET_KEY` | 是 | — | JWT 签名密钥（生产环境请替换为随机字符串） |
| `JWT_ALGORITHM` | 否 | `HS256` | JWT 签名算法 |
| `JWT_EXPIRATION_HOURS` | 否 | `72` | JWT Token 过期时间（小时） |
| `DEFAULT_AI_MODEL` | 否 | `gpt-4o-mini` | 默认使用的 AI 模型 |
| `LOG_LEVEL` | 否 | `INFO` | 日志级别（DEBUG/INFO/WARNING/ERROR） |

### Docker 部署的环境变量说明

Docker 部署使用 `.env.docker` 文件，核心差异：

- `DATABASE_URL` 使用 `postgresql+asyncpg://aiuser:ai_password@postgres:5432/ai_tutor`（容器内连接）
- `NEW_API_BASE_URL` 使用 `http://host.docker.internal:3000/v1`（连接宿主机服务）
- 新增 `REDIS_URL=redis://redis:6379/0`（连接容器内 Redis）

---

## 📁 项目结构

```
ai-tutor-backend/
├── app/                          # 应用主代码
│   ├── main.py                   # FastAPI 应用入口
│   ├── config.py                 # 配置管理（pydantic-settings）
│   ├── database.py               # 数据库连接（异步 SQLAlchemy）
│   ├── middleware/                # 中间件
│   │   ├── auth.py               # JWT 认证中间件
│   │   └── content_safety.py     # 内容安全过滤中间件
│   ├── models/                   # SQLAlchemy 数据模型
│   │   ├── user.py               # 用户模型
│   │   ├── subscription.py       # 订阅模型
│   │   ├── question_record.py    # 问题记录模型
│   │   └── daily_quota.py        # 每日配额模型
│   ├── routers/                  # API 路由
│   │   ├── health.py             # 健康检查
│   │   ├── auth.py               # 认证注册登录
│   │   ├── models.py             # 模型列表
│   │   ├── user.py               # 用户信息
│   │   ├── chat.py               # 对话/解题
│   │   ├── chat_completions.py   # OpenAI 兼容流式对话
│   │   ├── solve.py              # 拍照解题
│   │   ├── subscription.py       # 订阅管理
│   │   └── audio.py              # 语音识别/合成
│   ├── schemas/                  # Pydantic 数据验证模型
│   │   ├── common.py             # 通用响应格式
│   │   ├── user.py               # 用户相关
│   │   ├── chat.py               # 对话相关
│   │   ├── chat_completions.py   # 流式对话相关
│   │   ├── subscription.py       # 订阅相关
│   │   └── audio.py              # 语音相关
│   └── services/                 # 业务逻辑层
│       ├── ai_service.py         # AI 对话服务（苏格拉底问答法）
│       └── ocr_service.py        # OCR 图片识别服务
├── Dockerfile                    # Docker 构建文件
├── docker-compose.yml            # Docker Compose 编排
├── .env.example                  # 环境变量示例
├── .env.docker                   # Docker 部署环境变量
├── requirements.txt              # Python 依赖
├── check_import.py               # 快速导入检查脚本
└── README.md                     # 本文件
```

---

## 📝 开发说明

### 添加新路由

1. 在 `app/routers/` 下创建路由文件
2. 在 `app/main.py` 中注册路由: `app.include_router(your_router)`
3. 在 `app/schemas/` 中添加请求/响应模型
4. 如果涉及数据操作，在 `app/models/` 中添加 ORM 模型

### 数据库迁移

项目使用 SQLAlchemy 2.0 的 `create_all` 自动建表（开发阶段）。
生产环境建议使用 Alembic 管理迁移：

```bash
alembic init alembic
alembic revision --autogenerate -m "描述"
alembic upgrade head
```

### 测试

```bash
# 运行所有测试
pytest

# 带详细输出
pytest -v

# 指定测试文件
pytest tests/test_auth.py
```

---

## 🔒 安全注意事项

1. **生产环境务必替换 `SECRET_KEY`** 为随机字符串（推荐 `openssl rand -hex 32`）
2. **生产环境不要使用 `--reload`** 启动
3. **生产环境关闭 `/docs` / `/redoc`** 或添加认证
4. **Docker 部署时修改数据库默认密码**
5. **配置 HTTPS** 以保护用户数据传输

---

## 📄 许可证

MIT License
