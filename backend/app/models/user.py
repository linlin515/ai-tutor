"""
AI 学伴后端 - 用户模型
"""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, Text, func
from sqlalchemy.dialects.sqlite import TEXT as SQLITE_TEXT
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def gen_uuid() -> str:
    """生成 UUID 字符串作为主键"""
    return str(uuid.uuid4())


class User(Base):
    """用户表"""
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    phone: Mapped[str] = mapped_column(String(20), unique=True, nullable=False, comment="手机号")
    email: Mapped[str | None] = mapped_column(String(255), comment="邮箱")
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False, default="", comment="密码哈希（bcrypt）")
    nickname: Mapped[str | None] = mapped_column(String(50), comment="昵称")
    avatar_url: Mapped[str | None] = mapped_column(String(500), comment="头像 URL")
    grade: Mapped[str | None] = mapped_column(String(20), comment="年级，如 '三年级'、'初一'")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), comment="更新时间"
    )

    # 关联
    auth_methods = relationship("UserAuth", back_populates="user", lazy="selectin")
    subscription = relationship("Subscription", back_populates="user", uselist=False, lazy="selectin")
    question_records = relationship("QuestionRecord", back_populates="user", lazy="selectin")


class UserAuth(Base):
    """用户认证表 — 支持手机号/微信等多种登录方式"""
    __tablename__ = "user_auth"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=gen_uuid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False, comment="用户 ID")
    auth_type: Mapped[str] = mapped_column(
        String(20), nullable=False, comment="认证类型: phone / wechat"
    )
    credential: Mapped[str] = mapped_column(String(500), nullable=False, comment="凭证（哈希存储）")
    expires_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), comment="凭证过期时间（验证码有效期为 5 分钟）"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), comment="创建时间"
    )

    user = relationship("User", back_populates="auth_methods")
