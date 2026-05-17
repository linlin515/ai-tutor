"""
AI 学伴后端 - 订阅模型
"""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class Subscription(Base):
    """订阅表 — 记录用户的订阅计划"""
    __tablename__ = "subscriptions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), unique=True, nullable=False, comment="用户 ID"
    )
    plan_type: Mapped[str] = mapped_column(
        String(20), default="free", comment="订阅类型: free / premium"
    )
    start_date: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), comment="订阅开始时间")
    end_date: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), comment="订阅结束时间")
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, comment="是否有效")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    user = relationship("User", back_populates="subscription")
