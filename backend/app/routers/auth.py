"""
AI 学伴后端 - 认证路由
手机号 + 密码注册/登录；使用 JWT 做身份认证
"""

from __future__ import annotations

import logging
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Header, HTTPException
from jose import JWTError, jwt
from passlib.hash import pbkdf2_sha256
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.database import get_db
from app.models.user import User
from app.schemas.common import success
from app.schemas.user import (
    AuthResponse,
    LoginRequest,
    RegisterRequest,
)
from app.services.quota_service import check_quota

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter(prefix="/api/v1/auth", tags=["认证"])


def _create_token(user_id: str) -> str:
    """生成 JWT Token"""
    expire = datetime.now(timezone.utc) + timedelta(hours=settings.jwt_expiration_hours)
    payload = {
        "sub": user_id,
        "exp": expire,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, settings.secret_key, algorithm=settings.jwt_algorithm)


async def _get_quota_info(user_id: str, db: AsyncSession) -> tuple[int, int]:
    """获取用户当日配额信息"""
    from datetime import date as date_type
    from app.models.daily_quota import DailyQuota

    today = date_type.today()
    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
        )
    )
    quota = result.scalar_one_or_none()
    if quota:
        return quota.questions_limit, quota.questions_used
    return settings.daily_quota_free, 0


@router.post("/register")
async def register(req: RegisterRequest, db: AsyncSession = Depends(get_db)):
    """
    用户注册
    手机号 + 密码注册，密码使用 bcrypt 加密
    """
    # 检查手机号是否已注册
    result = await db.execute(select(User).where(User.phone == req.phone))
    existing_user = result.scalar_one_or_none()

    if existing_user:
        raise HTTPException(status_code=409, detail="该手机号已注册")

    # pbkdf2_sha256 加密密码
    hashed_pw = pbkdf2_sha256.hash(req.password)

    # 创建用户
    user = User(
        phone=req.phone,
        hashed_password=hashed_pw,
        email=req.email,
        nickname=f"学伴_{req.phone[-4:]}",
    )
    db.add(user)
    await db.flush()

    # 生成 Token
    token = _create_token(user.id)

    # 获取配额信息
    daily_limit, daily_used = await _get_quota_info(user.id, db)

    return success(
        data=AuthResponse(
            access_token=token,
            user_id=user.id,
            nickname=user.nickname,
            daily_quota=daily_limit,
            daily_used=daily_used,
        ),
        message="注册成功",
    )


@router.post("/login")
async def login(req: LoginRequest, db: AsyncSession = Depends(get_db)):
    """
    用户登录
    手机号 + 密码登录
    """
    # 查找用户
    result = await db.execute(select(User).where(User.phone == req.phone))
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(status_code=404, detail="该手机号未注册")

    # pbkdf2_sha256 验证密码
    if not pbkdf2_sha256.verify(req.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="密码错误")

    # 生成 Token
    token = _create_token(user.id)

    # 获取配额信息
    daily_limit, daily_used = await _get_quota_info(user.id, db)

    return success(
        data=AuthResponse(
            access_token=token,
            user_id=user.id,
            nickname=user.nickname,
            daily_quota=daily_limit,
            daily_used=daily_used,
        ),
        message="登录成功",
    )


@router.post("/refresh")
async def refresh_token(authorization: str = Header(..., alias="Authorization")):
    """
    Token 刷新
    接收旧 token（未过期），签发新 token
    """
    # 提取 Bearer token
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="无效的认证头格式")

    old_token = authorization.removeprefix("Bearer ")

    # 验证旧 token
    try:
        payload = jwt.decode(
            old_token,
            settings.secret_key,
            algorithms=[settings.jwt_algorithm],
        )
    except JWTError:
        raise HTTPException(status_code=401, detail="Token 无效或已过期")

    user_id: str | None = payload.get("sub")
    if user_id is None:
        raise HTTPException(status_code=401, detail="Token 中缺少用户标识")

    # 签发新 token
    new_token = _create_token(user_id)

    return success(data={"token": new_token, "token_type": "bearer"})
