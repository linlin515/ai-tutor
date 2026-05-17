"""
AI 学伴后端 - 对话及解题相关 Pydantic Schema
"""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


# ---------- 对话相关 ----------

class ChatMessage(BaseModel):
    """对话消息"""
    role: str = Field(..., pattern=r"^(user|assistant|system)$")
    content: str


class ChatRequest(BaseModel):
    """提问请求"""
    question: str = Field(..., min_length=1, max_length=5000, description="问题内容")
    subject: str = Field(
        default="math",
        pattern=r"^(math|chinese|english|science)$",
        description="学科",
    )


class ChatResponse(BaseModel):
    """AI 回答"""
    answer: str
    subject: str
    model_used: str | None = None
    tokens_used: int | None = None


class QuestionHistoryItem(BaseModel):
    """历史提问记录"""
    id: str
    question_content: str
    answer_content: str | None
    subject: str
    question_type: str
    created_at: datetime

    model_config = {"from_attributes": True}


# ---------- 拍照解题 ----------

class SolvePhotoResponse(BaseModel):
    """拍照解题响应"""
    recognized_text: str
    answer: str
    subject: str
    model_used: str | None = None
    tokens_used: int | None = None


# ---------- 分页 ----------

class PaginatedHistory(BaseModel):
    """分页历史记录"""
    items: list[QuestionHistoryItem]
    total: int
    page: int
    page_size: int
    total_pages: int
