"""
AI 学伴后端 - 测验题目模型
"""
from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.sqlite import TEXT as SQLITE_TEXT

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class QuizQuestion(Base):
    """测验题目表 — 记录测验中的每道题"""
    __tablename__ = "quiz_questions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    quiz_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("quiz_records.id"), nullable=False, comment="关联测验 ID"
    )
    question_index: Mapped[int] = mapped_column(Integer, nullable=False, comment="题号(1-based)")
    question_type: Mapped[str] = mapped_column(
        String(20), nullable=False, comment="题型: multiple_choice/fill_blank/true_false/essay"
    )
    content: Mapped[str] = mapped_column(Text, nullable=False, comment="题目内容")
    options: Mapped[str | None] = mapped_column(
        Text, comment="选项(JSON 格式，选择题用)"
    )
    correct_answer: Mapped[str] = mapped_column(String(500), nullable=False, comment="正确答案")
    explanation: Mapped[str | None] = mapped_column(Text, comment="解析")
    difficulty: Mapped[str] = mapped_column(String(10), default="medium", comment="难度")
    points: Mapped[int] = mapped_column(Integer, default=10, comment="分值")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    quiz = relationship("QuizRecord", back_populates="questions")
