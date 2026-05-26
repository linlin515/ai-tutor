"""
AI 学伴后端 - 成就记录模型
"""
from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class AchievementRecord(Base):
    """成就记录表 — 记录用户解锁的成就"""
    __tablename__ = "achievement_records"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID"
    )
    achievement_id: Mapped[str] = mapped_column(String(50), nullable=False, comment="成就 ID")
    unlocked_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="解锁时间"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    __table_args__ = (
        UniqueConstraint("user_id", "achievement_id", name="uk_user_achievement"),
    )
