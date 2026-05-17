"""
AI 学伴后端 - 用户信息路由
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.common import success
from app.schemas.user import UserProfile, UserProfileUpdate

router = APIRouter(prefix="/api/v1/user", tags=["用户信息"])


@router.get("/profile")
async def get_profile(
    current_user: User = Depends(get_current_user),
):
    """获取当前用户信息"""
    return success(
        data=UserProfile.model_validate(current_user),
    )


@router.patch("/profile")
async def update_profile(
    updates: UserProfileUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """更新用户信息"""
    update_data = updates.model_dump(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=400, detail="没有需要更新的字段")

    for field, value in update_data.items():
        setattr(current_user, field, value)

    await db.flush()
    await db.refresh(current_user)

    return success(
        data=UserProfile.model_validate(current_user),
        message="更新成功",
    )
