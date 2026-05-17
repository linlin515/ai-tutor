"""
AI 学伴后端 - 用户相关 Pydantic Schema
"""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


# ---------- 注册 / 登录 ----------

class RegisterRequest(BaseModel):
    """注册请求"""
    phone: str = Field(..., pattern=r"^1\d{10}$", description="手机号，11 位")
    password: str = Field(..., min_length=6, max_length=128, description="密码")
    email: str | None = Field(None, max_length=255, description="邮箱（可选）")


class LoginRequest(BaseModel):
    """登录请求"""
    phone: str = Field(..., pattern=r"^1\d{10}$", description="手机号")
    password: str = Field(..., min_length=1, max_length=128, description="密码")


class AuthResponse(BaseModel):
    """认证响应"""
    access_token: str
    token_type: str = "bearer"
    user_id: str
    nickname: str | None = None
    daily_quota: int = 5
    daily_used: int = 0


# ---------- 用户信息 ----------

class UserProfile(BaseModel):
    """用户信息"""
    id: str
    phone: str
    nickname: str | None = None
    avatar_url: str | None = None
    grade: str | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class UserProfileUpdate(BaseModel):
    """用户信息更新请求"""
    nickname: str | None = Field(None, max_length=50)
    avatar_url: str | None = Field(None, max_length=500)
    grade: str | None = Field(None, max_length=20)
