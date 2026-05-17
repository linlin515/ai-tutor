# AI Tutor - Monorepo

智能 AI 辅导应用

## 项目结构

```
.
├── android/   # Android 客户端源码（Kotlin + Jetpack Compose）
├── backend/   # FastAPI 后端服务（Python）
└── docs/      # 项目文档（PRD、架构、测试报告等）
```

### android/
Android 原生应用，使用 Kotlin 语言 + Jetpack Compose 框架开发。  
包含 app/ 模块、Gradle 构建配置等。

### backend/
Python FastAPI 后端服务，提供 API 接口、数据库模型、业务逻辑。  
包含 app/ 模块、alembic 数据库迁移、测试等。

### docs/
项目文档集合，包括：
- PRD、架构设计、API 文档
- 测试报告、审计报告
- 发布说明、路线图
- Web 页面资源
