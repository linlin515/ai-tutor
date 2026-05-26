"""
AI 学伴后端 - 每日配额模型
"""
from __future__ import annotations

import uuid
from datetime import date as date_type, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class DailyQuota(Base):
    """每日配额表 — 记录用户每日各功能使用次数限制"""
    __tablename__ = "daily_quotas"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID"
    )
    date: Mapped[date_type] = mapped_column(Date, nullable=False, comment="日期")
    feature: Mapped[str] = mapped_column(
        String(20), default="general", comment="功能类型: general / text_chat / photo_solve / quiz_generate / voice_asr / tts"
    )
    questions_used: Mapped[int] = mapped_column(Integer, default=0, comment="已使用次数（指定 feature）")
    questions_limit: Mapped[int] = mapped_column(Integer, default=5, comment="每日上限（默认 5 次免费）")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    user = relationship("User")

    __table_args__ = (
        UniqueConstraint("user_id", "date", "feature", name="uq_daily_quota_user_date_feature"),
    )
