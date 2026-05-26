"""
AI 学伴后端 - 游戏相关路由（排行榜等）
"""

from __future__ import annotations

import math
from typing import Any

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.user import User
from app.models.user_score import UserScore
from app.schemas.common import (
    ApiResponse,
    PaginatedData,
    success,
    error,
)

router = APIRouter(prefix="/api/v1/game", tags=["game"])


class LeaderboardItemOut:
    """排行榜单项输出"""
    rank: int
    user_id: str
    nickname: str | None
    avatar_url: str | None
    score: int


def leaderboard_item_to_dict(item: Any, rank: int) -> dict[str, Any]:
    """将 (User, UserScore) 元组转换为排行榜项字典"""
    user, user_score = item
    return {
        "rank": rank,
        "user_id": user.id,
        "nickname": user.nickname,
        "avatar_url": user.avatar_url,
        "score": user_score.score,
    }


@router.get("/leaderboard", response_model=ApiResponse[PaginatedData[dict]])
async def get_leaderboard(
    page: int = Query(default=1, ge=1, description="当前页码"),
    page_size: int = Query(default=20, ge=1, le=100, description="每页记录数"),
    type: str = Query(default="GLOBAL", description="排行榜类型: GLOBAL / FRIENDS"),
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[PaginatedData[dict]]:
    """
    获取排行榜（分页）

    - `page`: 当前页码，从 1 开始
    - `page_size`: 每页记录数，默认 20，最大 100
    - `type`: 排行榜类型，当前仅支持 GLOBAL（全局）
    """
    # 当前仅支持全局排行榜
    if type.upper() != "GLOBAL":
        return error(code=-1, message=f"不支持的排行榜类型: {type}")

    # 查询总记录数（有 score 记录的用户数）
    count_query = select(func.count()).select_from(UserScore)
    total_result = await db.execute(count_query)
    total = total_result.scalar() or 0

    # 计算总页数
    total_pages = max(1, math.ceil(total / page_size))

    # 如果请求页超出总页数，返回空列表
    if page > total_pages:
        return success(
            data=PaginatedData(
                items=[],
                total=total,
                page=page,
                page_size=page_size,
                total_pages=total_pages,
            ),
            message="获取排行榜成功",
        )

    # 查询当前页数据：按 score 降序排列，关联用户表获取昵称和头像
    offset_val = (page - 1) * page_size
    query = (
        select(User, UserScore)
        .join(UserScore, User.id == UserScore.user_id)
        .order_by(UserScore.score.desc(), User.id.asc())
        .offset(offset_val)
        .limit(page_size)
    )
    result = await db.execute(query)
    rows = result.all()

    # 计算起始排名
    start_rank = offset_val + 1
    items = [
        leaderboard_item_to_dict(row, rank)
        for rank, row in enumerate(rows, start=start_rank)
    ]

    return success(
        data=PaginatedData(
            items=items,
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages,
        ),
        message="获取排行榜成功",
    )
