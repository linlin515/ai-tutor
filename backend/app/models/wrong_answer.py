"""
AI 学伴后端 - 错题记录模型
"""
from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, Float, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class WrongAnswer(Base):
    """错题记录表 — 记录用户答错的题目，供错题本和间隔复习使用"""
    __tablename__ = "wrong_answers"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID"
    )
    quiz_id: Mapped[str | None] = mapped_column(String(36), comment="来源测验 ID")
    subject: Mapped[str] = mapped_column(String(20), nullable=False, comment="学科")
    topic: Mapped[str | None] = mapped_column(String(100), comment="知识点")
    question_content: Mapped[str] = mapped_column(Text, nullable=False, comment="题目原文")
    correct_answer: Mapped[str] = mapped_column(String(500), nullable=False, comment="正确答案")
    user_answer: Mapped[str] = mapped_column(String(500), nullable=False, comment="用户填写的答案")
    explanation: Mapped[str | None] = mapped_column(Text, comment="解析/错因")
    mastery_score: Mapped[float] = mapped_column(Float, default=0.0, comment="掌握度 0-1")
    weakness: Mapped[str | None] = mapped_column(String(200), comment="薄弱环节标签")
    review_count: Mapped[int] = mapped_column(Integer, default=0, comment="复习次数")
    last_reviewed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), comment="上次复习时间"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )
