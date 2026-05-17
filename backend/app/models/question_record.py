"""
AI 学伴后端 - 提问记录模型
"""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class QuestionRecord(Base):
    """提问记录表 — 记录每次学生提问及 AI 回答"""
    __tablename__ = "question_records"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID"
    )
    subject: Mapped[str] = mapped_column(
        String(20), nullable=False, comment="学科: math / chinese / english / science"
    )
    question_type: Mapped[str] = mapped_column(
        String(20), nullable=False, comment="提问类型: text / photo / chat_completion"
    )
    question_content: Mapped[str] = mapped_column(Text, nullable=False, comment="问题内容（文字）")
    answer_content: Mapped[str | None] = mapped_column(Text, comment="AI 回答内容")
    ai_model_used: Mapped[str | None] = mapped_column(String(50), comment="使用的 AI 模型")
    tokens_used: Mapped[int | None] = mapped_column(Integer, comment="消耗的 token 数")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="提问时间"
    )

    user = relationship("User", back_populates="question_records")
