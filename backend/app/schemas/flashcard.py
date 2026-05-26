"""
AI 学伴后端 - Flashcard Pydantic Schema
"""
from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class FlashcardOut(BaseModel):
    """错题卡片输出"""
    card_id: str = Field(..., description="错题记录 ID")
    subject: str = Field(..., description="学科")
    topic: str | None = Field(default=None, description="知识点")
    question_content: str = Field(..., description="题目原文")
    correct_answer: str = Field(..., description="正确答案")
    user_answer: str = Field(..., description="用户上次填写的答案")
    explanation: str | None = Field(default=None, description="解析")
    mastery_score: float = Field(..., description="当前掌握度 0-1")
    review_count: int = Field(default=0, description="复习次数")
    last_reviewed_at: str | None = Field(default=None, description="上次复习时间")
    is_archived: bool = Field(default=False, description="是否已归档")


class TodayCardsResponse(BaseModel):
    """今日待复习卡片响应"""
    cards: list[FlashcardOut] = Field(default_factory=list, description="待复习卡片列表")
    total: int = Field(default=0, description="总待复习数")


class ReviewRequest(BaseModel):
    """复习请求"""
    card_id: str = Field(..., description="错题卡片 ID")
    judgment: str = Field(..., description="判断: mastered / unfamiliar")
    time_spent_seconds: int = Field(default=0, ge=0, description="花费时间（秒）")


class ReviewResponse(BaseModel):
    """复习响应"""
    card_id: str = Field(..., description="错题卡片 ID")
    new_mastery: float = Field(..., description="新的掌握度")
    is_archived: bool = Field(default=False, description="是否已归档")
    points_earned: int = Field(default=0, description="获得积分")
    next_review_suggestion: str = Field(default="", description="下次复习建议")


class SyncFlashcardItem(BaseModel):
    """批量同步的单个卡片项"""
    card_id: str = Field(..., description="错题记录 ID")
    mastery_score: float = Field(..., description="客户端掌握度")
    review_count: int = Field(default=0, description="复习次数")
    is_archived: bool = Field(default=False, description="是否归档")
    timestamp: str = Field(..., description="操作时间 (ISO 8601)")


class SyncFlashcardRequest(BaseModel):
    """批量同步 flashcard 请求"""
    cards: list[SyncFlashcardItem] = Field(default_factory=list, description="待同步的卡片列表")


class SyncFlashcardResponse(BaseModel):
    """批量同步 flashcard 响应"""
    synced: bool = Field(default=True)
    synced_count: int = Field(default=0)
    conflicts: list[dict] = Field(default_factory=list)
