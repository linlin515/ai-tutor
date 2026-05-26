"""
AI 学伴后端 - 游戏相关路由（排行榜、积分同步等）
"""
from __future__ import annotations

import logging
import math
from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.models.user_score import UserScore
from app.models.achievement_record import AchievementRecord
from app.schemas.common import (
    ApiResponse,
    PaginatedData,
    success,
    error as api_error,
)
from app.schemas.analytics import SyncScoreRequest, SyncScoreResponse, SyncScoreData
from app.services.redis_service import cache_get, cache_set

logger = logging.getLogger(__name__)

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
        return api_error(code=-1, message=f"不支持的排行榜类型: {type}")

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


# ============================================================
# POST /api/v1/game/sync/score
# ============================================================


async def _check_sync_rate_limit(user_id: str) -> bool:
    """检查每分钟最多同步 1 次"""
    key = f"game:sync_rate:{user_id}"
    exists = await cache_get(key)
    if exists:
        return False
    await cache_set(key, "1", ttl=60)
    return True


async def _calc_rank(user_id: str, db: AsyncSession) -> int:
    """计算用户排名"""
    result = await db.execute(
        select(UserScore).where(UserScore.user_id == user_id)
    )
    user_score = result.scalar_one_or_none()
    if user_score is None:
        return 0

    higher_count = (
        await db.execute(
            select(func.count()).select_from(UserScore).where(
                UserScore.score > user_score.score
            )
        )
    ).scalar() or 0
    return higher_count + 1


async def _calc_total_users(db: AsyncSession) -> int:
    """计算总参与人数"""
    result = await db.execute(select(func.count()).select_from(UserScore))
    return result.scalar() or 0


@router.post("/sync/score")
async def sync_score(
    req: SyncScoreRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    同步游戏积分到云端（服务端权威存储）
    - 积分冲突处理：服务端始终为权威源
    - 频率限制：每分钟最多 1 次
    - 新成就自动记录
    """
    # 1. 频率限制
    if not await _check_sync_rate_limit(current_user.id):
        return api_error(code=429, message="同步过于频繁，请稍后再试")

    client_total = req.score_data.total_points
    conflict = False

    # 2. 查询服务端已有积分
    result = await db.execute(
        select(UserScore).where(UserScore.user_id == current_user.id)
    )
    user_score = result.scalar_one_or_none()

    if user_score is None:
        # 首次同步，创建记录
        user_score = UserScore(
            user_id=current_user.id,
            score=client_total,
        )
        db.add(user_score)
        server_total = client_total
    else:
        server_total = user_score.score
        # 3. 冲突处理：客户端低于服务端 → 返回服务端值，标记冲突
        if client_total < server_total:
            conflict = True
        elif client_total > server_total:
            # 客户端更高 → 以客户端为准更新
            user_score.score = client_total
            server_total = client_total
        # else: 相等，不做变更

    await db.flush()

    # 4. 处理成就
    new_achievements: list[str] = []
    achievement_ids = req.score_data.achievements_unlocked
    if achievement_ids:
        for aid in achievement_ids:
            existing = await db.execute(
                select(AchievementRecord).where(
                    AchievementRecord.user_id == current_user.id,
                    AchievementRecord.achievement_id == aid,
                )
            )
            if existing.scalar_one_or_none() is None:
                new_ach = AchievementRecord(
                    user_id=current_user.id,
                    achievement_id=aid,
                )
                db.add(new_ach)
                new_achievements.append(aid)

    await db.flush()

    # 5. 计算排名
    rank = await _calc_rank(current_user.id, db)
    total_users = await _calc_total_users(db)

    return success(
        data=SyncScoreResponse(
            synced=True,
            server_total_points=server_total,
            rank=rank,
            total_users=total_users,
            new_achievements=new_achievements,
            conflict=conflict,
        ).model_dump(),
        message="积分同步成功",
    )
