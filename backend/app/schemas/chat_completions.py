"""
AI 学伴后端 - OpenAI 兼容的 Chat Completions Schema

定义与 OpenAI Chat Completions API 兼容的请求/响应模型，
供 SSE 流式对话端点 POST /v1/chat/completions 使用。
"""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


# ============================================================
# 工具调用模型（v2.0 Agent 支持）
# ============================================================

class ToolFunction(BaseModel):
    """OpenAI 工具函数的 JSON Schema 定义"""
    name: str = Field(..., description="工具名称")
    description: str | None = Field(default=None, description="工具描述")
    parameters: dict[str, Any] | None = Field(default=None, description="工具参数的 JSON Schema")


class ToolDefinition(BaseModel):
    """OpenAI 工具定义"""
    type: str = Field(default="function", description="工具类型，目前仅支持 function")
    function: ToolFunction = Field(..., description="函数定义")


class ToolCallFunction(BaseModel):
    """工具调用中的函数调用信息"""
    name: str | None = Field(default=None, description="函数名称")
    arguments: str | None = Field(default=None, description="函数参数（JSON 字符串）")


class ToolCallDelta(BaseModel):
    """SSE delta 中的工具调用增量"""
    index: int = Field(default=0, description="工具调用索引")
    id: str | None = Field(default=None, description="工具调用 ID")
    type: str | None = Field(default=None, description="类型，固定为 function")
    function: ToolCallFunction | None = Field(default=None, description="函数调用信息")


# ============================================================
# 请求体模型（兼容 OpenAI Chat Completions 格式）
# ============================================================

class ChatCompletionMessage(BaseModel):
    """对话消息，兼容 OpenAI 的 messages 格式"""
    role: str = Field(
        ...,
        description="角色: system / user / assistant",
    )
    content: str = Field(
        ...,
        description="消息内容",
    )


class ChatCompletionRequest(BaseModel):
    """
    OpenAI 兼容的 Chat Completions 请求体

    参考: https://platform.openai.com/docs/api-reference/chat/create
    """
    model: str = Field(
        default="gpt-4o-mini",
        description="模型名称",
    )
    messages: list[ChatCompletionMessage] = Field(
        ...,
        min_length=1,
        description="对话消息列表",
    )
    stream: bool = Field(
        default=True,
        description="是否使用流式输出（默认开启）",
    )
    temperature: float | None = Field(
        default=0.7,
        ge=0.0,
        le=2.0,
        description="采样温度，控制输出的随机性",
    )
    top_p: float | None = Field(
        default=None,
        ge=0.0,
        le=1.0,
        description="核采样参数",
    )
    max_tokens: int | None = Field(
        default=2000,
        ge=1,
        le=8192,
        description="最大生成 token 数",
    )
    # v2.0 Agent 支持：工具调用
    tools: list[ToolDefinition] | None = Field(
        default=None,
        description="工具定义列表（Agent 模式下使用）",
    )
    tool_choice: str | dict[str, Any] | None = Field(
        default=None,
        description='工具选择策略: "auto" | "none" | {"type":"function","function":{"name":"..."}}',
    )


# ============================================================
# SSE 流式响应 Chunk 模型（OpenAI 兼容）
# ============================================================

class DeltaContent(BaseModel):
    """流式 delta 内容"""
    content: str | None = None
    role: str | None = None
    # v2.0 Agent 支持：工具调用增量
    tool_calls: list[ToolCallDelta] | None = None


class ChoiceDelta(BaseModel):
    """流式 choice 中的 delta 字段"""
    index: int = 0
    delta: DeltaContent = Field(default_factory=DeltaContent)
    finish_reason: str | None = None


class ChatCompletionChunk(BaseModel):
    """
    SSE 流式响应的每个 chunk

    格式:
    {
        "id": "chatcmpl-xxx",
        "object": "chat.completion.chunk",
        "created": 1234567890,
        "model": "gpt-4o-mini",
        "choices": [
            {
                "index": 0,
                "delta": {"content": "..."},
                "finish_reason": null
            }
        ]
    }
    """
    id: str = "chatcmpl-unknown"
    object: str = "chat.completion.chunk"
    created: int = 0
    model: str = ""
    choices: list[ChoiceDelta] = Field(default_factory=list)
    usage: dict[str, Any] | None = None
