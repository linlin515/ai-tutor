# AI 助手 Android App API 接口设计

> 后端基础地址: `https://aitutor.googlecloud.ccwu.cc`（生产环境）
> 协议: REST over HTTP / SSE for streaming
> 认证方式: Bearer Token (Header: `Authorization: Bearer <token>`)
> 响应格式: `{ "code": 0, "message": "success", "data": {...} }`

---

## 1. 认证接口

### 1.1 注册

```
POST /api/v1/auth/register
```

**Request:**
```json
{
  "phone": "13800138000",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "access_token": "eyJhbG...NiIs...",
    "token_type": "bearer",
    "user_id": "c2e8305b-9469-4c51-9eaf-d6ce902e28e5",
    "nickname": "学伴_8000",
    "daily_quota": 5,
    "daily_used": 0
  }
}
```

### 1.2 登录

```
POST /api/v1/auth/login
```

**Request:**
```json
{
  "phone": "13800138000",
  "password": "password123"
}
```

**Response:** 同注册响应。

### 1.3 Token 刷新

```
POST /api/v1/auth/refresh
```

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "code": 0,
  "data": {
    "token": "new_token..."
  }
}
```

---

## 2. 用户接口

### 2.1 获取用户资料

```
GET /api/v1/user/profile
```

### 2.2 更新用户资料

```
PATCH /api/v1/user/profile
```

**Request:**
```json
{
  "nickname": "小明",
  "grade": "初三",
  "avatar": "data:image/png;base64,..."
}
```

---

## 3. 聊天接口

### 3.1 发送消息 (流式) — 新增

```
POST /v1/chat/completions
```

**协议:** Server-Sent Events (SSE)  
**Content-Type:** `text/event-stream`

**Request:**
```json
{
  "model": "gpt-4o-mini",
  "messages": [
    {"role": "system", "content": "你是一个AI学习助手..."},
    {"role": "user", "content": "请解释勾股定理"},
    {"role": "assistant", "content": "勾股定理是指..."},
    {"role": "user", "content": "能举个例子吗？"}
  ],
  "stream": true,
  "temperature": 0.7,
  "top_p": 1.0,
  "max_tokens": 2048
}
```

**Response (SSE 流式):**
```
data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"勾"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"股"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"定理"},"finish_reason":null}]}

data: [DONE]
```

**Android 端实现要点:**
- 使用 `OkHttp EventSource.Factory` 创建 SSE 连接
- 通过 `callbackFlow<ChatChunk>` 转为 Kotlin Flow
- `readTimeout` 设为 0 (无限)
- 每收到一个 chunk，写入 Room 并更新 UI State

### 3.2 发送消息 (非流式) — 保留兼容

```
POST /api/v1/chat/ask
```

**Request:**
```json
{
  "content": "请解释勾股定理",
  "subject": "math",
  "conversation_id": 1
}
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "answer": "勾股定理是指直角三角形中，两条直角边的平方和等于斜边的平方...",
    "tokens_used": 156,
    "conversation_id": 1
  }
}
```

### 3.3 获取聊天历史

```
GET /api/v1/chat/history
```

**Query:**
```
?page=1&page_size=20&conversation_id=1
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": 1,
        "content": "请解释勾股定理",
        "answer": "勾股定理是指...",
        "subject": "math",
        "created_at": "2026-05-14T10:00:00Z"
      }
    ],
    "total": 42,
    "page": 1,
    "page_size": 20
  }
}
```

### 3.4 获取可用模型列表 — 新增

```
GET /api/v1/models
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "models": [
      {"id": "gpt-4o", "name": "GPT-4o", "provider": "OpenAI"},
      {"id": "gpt-4o-mini", "name": "GPT-4o Mini", "provider": "OpenAI"},
      {"id": "claude-3.5-sonnet", "name": "Claude 3.5 Sonnet", "provider": "Anthropic"}
    ]
  }
}
```

---

## 4. 语音接口

### 4.1 语音转文字 (ASR) — 新增

```
POST /v1/audio/transcriptions
```

**Content-Type:** `multipart/form-data`

**Request:**
```
file: <录音文件> (支持格式: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm)
model: whisper-1
language: zh
```

**Response:**
```json
{
  "text": "请解释勾股定理"
}
```

### 4.2 文字转语音 (TTS) — 新增

```
POST /v1/audio/speech
```

**Request:**
```json
{
  "model": "tts-1",
  "input": "勾股定理是指直角三角形中两条直角边的平方和等于斜边的平方",
  "voice": "alloy",
  "speed": 1.0,
  "response_format": "mp3"
}
```

**Response:** 二进制音频数据 (Content-Type: audio/mpeg)

---

## 5. 拍照解题接口

### 5.1 拍照上传

```
POST /api/v1/solve/photo
```

**Content-Type:** `multipart/form-data`

**Request:**
```
photo: <图片文件>
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "answer": "这道题的解题步骤是...",
    "subject": "math",
    "tokens_used": 256
  }
}
```

---

## 6. 订阅接口

### 6.1 获取订阅状态

```
GET /api/v1/subscription/status
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "is_subscribed": false,
    "plan": "免费版",
    "expire_date": null,
    "daily_quota": 5,
    "features": ["基础对话", "拍照解题"]
  }
}
```

---

## 7. 健康检查

```
GET /api/v1/health
```

**Response:**
```json
{
  "status": "ok"
}
```

---

## 8. 关键数据模型

### 8.1 通用响应包装

```json
{
  "code": 0,          // 0=成功, 非0=错误
  "message": "...",   // 错误描述
  "data": {}          // 响应数据 (可为 null)
}
```

### 8.2 错误码规范

| Code | 含义 | 客户端处理 |
|------|------|-----------|
| 0 | 成功 | - |
| 400 | 参数错误 | 提示用户检查输入 |
| 401 | 未授权 / Token 过期 | 清除 Token，跳转登录 |
| 403 | 无权限 (配额不足) | 提示升级或等待次日 |
| 404 | 资源不存在 | 提示重新操作 |
| 429 | 请求过于频繁 | 显示冷却倒计时 |
| 500 | 服务器内部错误 | 提示稍后重试 |
| -1 | 未知错误 | 兜底提示 |

### 8.3 分页请求格式

```json
{
  "page": 1,
  "page_size": 20
}
```

### 8.4 分页响应格式

```json
{
  "items": [],
  "total": 100,
  "page": 1,
  "page_size": 20,
  "total_pages": 5
}
```
