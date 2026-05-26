"""
AI 学伴后端 - 离线同步路由
GET  /api/v1/sync?since={timestamp}  — 获取增量数据
POST /api/v1/sync                    — 批量同步操作
"""
from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.common import ApiResponse, success, error as api_error
from app.schemas.sync import DeltaResponse, SyncRequest, SyncResponse
from app.services.redis_service import cache_exists, cache_set
from app.services.sync_service import get_delta, process_actions

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/sync", tags=["sync"])


@router.get("", response_model=ApiResponse[DeltaResponse])
async def sync_get_delta(
    since: str = Query(default="", description="增量时间戳 (ISO 8601)，空则返回全部"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    获取增量数据：返回用户自 since 时间戳以来变更的数据。

    响应包含：
    - updated_questions: 更新的提问记录
    - updated_conversations: 更新的对话记录
    - updated_wrong_answers: 更新的错题记录
    - deleted_ids: 已删除记录 ID 列表
    - sync_timestamp: 服务端时间戳
    """
    since_param = since if since else None
    delta = await get_delta(
        user_id=current_user.id,
        since=since_param,
        db=db,
    )
    return success(
        data=delta.model_dump(),
        message="获取增量数据成功",
    )


@router.post("", response_model=ApiResponse[SyncResponse])
async def sync_post_actions(
    req: SyncRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    批量同步操作（幂等）。

    支持操作类型：
    - create: 创建记录（data 中需包含 _type 字段标识记录类型）
    - update: 更新记录
    - delete: 删除记录（当前返回冲突，服务端不允许直接删除）

    冲突策略：服务端优先。
    幂等去重：1分钟内相同 target_id 的操作自动跳过。
    """
    # 幂等检查：1分钟内相同用户不重复处理完全相同的请求
    idempotency_key = f"sync:actions:{current_user.id}:{hash(str(req.model_dump()))}"
    if await cache_exists(idempotency_key):
        return success(
            data=SyncResponse(
                synced=True,
                conflicts=[],
                sync_timestamp=__import__("datetime").datetime.now(
                    __import__("datetime").timezone.utc
                ).isoformat(),
            ).model_dump(),
            message="重复请求，已跳过",
        )

    # 设置幂等 key（1分钟过期）
    import json
    await cache_set(idempotency_key, json.dumps({"processed": True}), ttl=60)

    synced, conflicts = await process_actions(
        user_id=current_user.id,
        actions=req.actions,
        db=db,
    )

    import datetime
    now_ts = datetime.datetime.now(datetime.timezone.utc).isoformat()

    return success(
        data=SyncResponse(
            synced=synced,
            conflicts=conflicts,
            sync_timestamp=now_ts,
        ).model_dump(),
        message="同步成功" if synced else "同步部分完成",
    )
