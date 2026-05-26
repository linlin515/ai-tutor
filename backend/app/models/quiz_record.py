"""
AI 学伴后端 - 测验记录模型
"""
from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class QuizRecord(Base):
    """测验记录表 — 记录每次生成的测验"""
    __tablename__ = "quiz_records"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID"
    )
    subject: Mapped[str] = mapped_column(
        String(20), nullable=False, comment="学科: math/physics/chemistry/biology/chinese/english"
    )
    topic: Mapped[str | None] = mapped_column(String(100), comment="知识点")
    difficulty: Mapped[str] = mapped_column(String(10), default="medium", comment="难度: easy/medium/hard")
    total_questions: Mapped[int] = mapped_column(Integer, default=5, comment="题目数量")
    score: Mapped[int | None] = mapped_column(Integer, comment="测验得分(若已批改)")
    total_points: Mapped[int | None] = mapped_column(Integer, comment="总分")
    status: Mapped[str] = mapped_column(String(10), default="pending", comment="状态: pending/completed")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    user = relationship("User")
    questions = relationship("QuizQuestion", back_populates="quiz", lazy="selectin")
