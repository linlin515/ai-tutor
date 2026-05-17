"""
AI 学伴后端 - SSE 流式对话端点路由

提供 OpenAI 兼容的 POST /v1/chat/completions 端点，
通过 Server-Sent Events (SSE) 实现流式对话，转发到 new-api 代理。

关键特性:
- 完全兼容 OpenAI Chat Completions 请求/响应格式
- SSE 流式输出 (text/event-stream)
- 非流式输出支持 (stream=false)
- JWT Bearer Token 认证
- 每日配额检查与原子扣减
- 输入内容安全过滤
- 输出内容安全扫描
- SSE 异常时自动回滚配额
- 异步 httpx 流式转发
"""

from __future__ import annotations

import json
import logging
import time
import uuid
from datetime import date as date_type
from typing import AsyncGenerator

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.responses import StreamingResponse

from app.config import get_settings
from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.daily_quota import DailyQuota
from app.models.question_record import QuestionRecord
from app.models.user import User
from app.schemas.chat_completions import (
    ChatCompletionChunk,
    ChatCompletionMessage,
    ChatCompletionRequest,
    ChoiceDelta,
    DeltaContent,
)
from app.services.ai_service import content_filter, _keyword_filter
from app.services.quota_service import check_quota, rollback_quota

logger = logging.getLogger(__name__)
settings = get_settings()

router = APIRouter(prefix="/v1", tags=["Chat Completions (OpenAI 兼容)"])


# ============================================================
# 工具函数
# ============================================================

def _make_chunk(
    content: str = "",
    role: str | None = None,
    finish_reason: str | None = None,
    usage: dict | None = None,
    chunk_id: str = "",
    model: str = "",
    created: int = 0,
) -> str:
    """
    构造 OpenAI 兼容的 SSE data chunk
    """
    delta = {}
    if role:
        delta["role"] = role
    if content:
        delta["content"] = content

    chunk = ChatCompletionChunk(
        id=chunk_id or f"chatcmpl-{uuid.uuid4().hex[:12]}",
        created=created or int(time.time()),
        model=model or settings.default_ai_model,
        choices=[
            ChoiceDelta(
                index=0,
                delta=DeltaContent(**delta) if delta else DeltaContent(),
                finish_reason=finish_reason,
            )
        ],
        usage=usage,
    )
    return f"data: {chunk.model_dump_json(exclude_none=True)}\n\n"


# ============================================================
# SSE 流式生成器
# ============================================================

async def _stream_chat_completions(
    request_body: dict,
    chunk_id: str,
    created: int,
) -> AsyncGenerator[str, None]:
    """
    底层 SSE 生成器 — 从 new-api 流式获取数据并转化为 SSE 事件
    """
    headers = {
        "Authorization": f"Bearer {settings.new_api_key}",
        "Content-Type": "application/json",
    }

    async with httpx.AsyncClient(timeout=120.0) as client:
        async with client.stream(
            "POST",
            f"{settings.new_api_base_url}/chat/completions",
            headers=headers,
            json=request_body,
        ) as response:
            if response.status_code != 200:
                error_body = await response.aread()
                error_detail = error_body.decode("utf-8", errors="replace")
                logger.error(
                    "new-api 返回错误: status=%d, body=%s",
                    response.status_code,
                    error_detail,
                )
                yield _make_chunk(
                    content=f"## 服务异常\n\nnew-api 返回 {response.status_code}，请稍后再试。",
                    finish_reason="stop",
                    chunk_id=chunk_id,
                    created=created,
                )
                yield "data: [DONE]\n\n"
                return

            async for line in response.aiter_lines():
                if not line.strip():
                    continue

                if line.startswith("data: "):
                    data_str = line[6:]

                    if data_str.strip() == "[DONE]":
                        yield "data: [DONE]\n\n"
                        return

                    try:
                        original_chunk = json.loads(data_str)
                        original_chunk["id"] = chunk_id
                        original_chunk["created"] = created

                        # 输出内容安全扫描（ARCH_REVIEW P1）
                        content_delta = ""
                        choices = original_chunk.get("choices", [])
                        if choices:
                            delta = choices[0].get("delta", {})
                            content_delta = delta.get("content", "")
                            if content_delta:
                                kw_result = _keyword_filter(content_delta)
                                if not kw_result["safe"]:
                                    logger.warning(
                                        "输出内容安全拦截: reason=%s", kw_result["reason"]
                                    )
                                    yield _make_chunk(
                                        content="[内容已过滤]",
                                        finish_reason="stop",
                                        chunk_id=chunk_id,
                                        created=created,
                                    )
                                    yield "data: [DONE]\n\n"
                                    return

                        yield f"data: {json.dumps(original_chunk, ensure_ascii=False)}\n\n"
                    except json.JSONDecodeError:
                        yield f"data: {data_str}\n\n"


# ============================================================
# 主端点
# ============================================================

@router.post("/chat/completions")
async def chat_completions(
    request: Request,
    req: ChatCompletionRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    OpenAI 兼容的对话端点（支持流式和非流式）

    流式 (stream=true): 返回 SSE text/event-stream
    非流式 (stream=false): 返回完整 JSON 响应
    """
    # ----------------------------------------------------------
    # Step 1: 检查每日配额（原子扣减）
    # ----------------------------------------------------------
    within_limit, used, limit_val = await check_quota(current_user.id, db)
    if not within_limit:
        raise HTTPException(
            status_code=429,
            detail=f"今日提问次数已达上限（{limit_val}次），升级为高级版可解锁更多次数",
        )

    # ----------------------------------------------------------
    # Step 2: 内容安全过滤
    # ----------------------------------------------------------
    user_messages = [
        msg.content
        for msg in req.messages
        if msg.role == "user"
    ]
    for user_text in user_messages:
        safety = await content_filter(user_text)
        if not safety["safe"]:
            # 配额已扣减，需要回滚
            await rollback_quota(current_user.id, db)
            logger.warning(
                "内容安全拦截: user=%s, reason=%s",
                current_user.id,
                safety.get("reason", ""),
            )
            raise HTTPException(
                status_code=400,
                detail="输入内容不符合安全规范，请重新输入",
            )

    # ----------------------------------------------------------
    # Step 3: 记录提问
    # ----------------------------------------------------------
    last_user_msg = user_messages[-1] if user_messages else ""

    record = QuestionRecord(
        user_id=current_user.id,
        subject="general",
        question_type="chat_completion",
        question_content=last_user_msg,
        ai_model_used=req.model,
        tokens_used=None,
    )
    db.add(record)
    await db.flush()

    # ----------------------------------------------------------
    # Step 4: 构建请求体
    # ----------------------------------------------------------
    is_stream = req.stream if req.stream is not None else True

    payload = {
        "model": req.model,
        "messages": [m.model_dump() for m in req.messages],
        "stream": is_stream,
        "temperature": req.temperature or 0.7,
        "max_tokens": req.max_tokens or 2000,
    }
    if req.top_p is not None:
        payload["top_p"] = req.top_p
    # v2.0 Agent: 透传 tools 和 tool_choice
    if req.tools:
        payload["tools"] = [t.model_dump(exclude_none=True) for t in req.tools]
    if req.tool_choice is not None:
        payload["tool_choice"] = req.tool_choice

    chunk_id = f"chatcmpl-{uuid.uuid4().hex[:12]}"
    created = int(time.time())

    logger.info(
        "ChatCompletion 请求: user=%s, model=%s, messages=%d, stream=%s",
        current_user.id,
        req.model,
        len(req.messages),
        is_stream,
    )

    # ----------------------------------------------------------
    # Step 5a: 非流式处理 (stream=false)
    # ----------------------------------------------------------
    if not is_stream:
        headers = {
            "Authorization": f"Bearer {settings.new_api_key}",
            "Content-Type": "application/json",
        }
        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(
                    f"{settings.new_api_base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                result = response.json()

            answer = result["choices"][0]["message"]["content"]
            usage = result.get("usage", {})
            total_tokens = usage.get("total_tokens", 0)

            # 更新记录
            record.answer_content = answer
            record.tokens_used = total_tokens

            return {
                "id": chunk_id,
                "object": "chat.completion",
                "created": created,
                "model": req.model,
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": answer,
                        },
                        "finish_reason": "stop",
                    }
                ],
                "usage": usage,
            }

        except httpx.TimeoutException:
            await rollback_quota(current_user.id, db)
            raise HTTPException(status_code=504, detail="AI 服务超时，请稍后再试")
        except httpx.HTTPStatusError as e:
            await rollback_quota(current_user.id, db)
            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"AI 服务返回错误: {e.response.status_code}",
            )
        except Exception as e:
            await rollback_quota(current_user.id, db)
            logger.error("非流式请求异常: %s", e)
            raise HTTPException(status_code=500, detail="AI 服务调用失败")

    # ----------------------------------------------------------
    # Step 5b: 流式处理 (SSE)
    # ----------------------------------------------------------

    async def event_generator() -> AsyncGenerator[str, None]:
        """SSE 事件生成器"""
        had_error = False
        try:
            async for sse_event in _stream_chat_completions(payload, chunk_id, created):
                yield sse_event
        except Exception as e:
            had_error = True
            logger.error("SSE 流式传输异常: %s", e, exc_info=True)
            yield _make_chunk(
                content="## 连接异常\n\n抱歉，AI 回复过程出现异常，请稍后重试。",
                finish_reason="stop",
                chunk_id=chunk_id,
                created=created,
            )
            yield "data: [DONE]\n\n"
        finally:
            # 如果流式异常，回滚配额
            if had_error:
                await rollback_quota(current_user.id, db)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
