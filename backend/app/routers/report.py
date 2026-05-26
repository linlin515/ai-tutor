"""
AI 学伴后端 - 学习报告导出路由
POST /api/v1/report/export  — 导出 PDF 或 CSV 学习报告

配额控制:
- 免费用户: 3次/日
- Premium 用户: 20次/日
- 通过 Redis 计数器跟踪
"""
from __future__ import annotations

import asyncio
import logging
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.subscription import Subscription
from app.models.user import User
from app.schemas.report import ExportRequest
from app.services.redis_service import cache_get, cache_set

from app.services.report_service import generate_report

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/report", tags=["report"])

# 导出超时时间（秒）
REPORT_TIMEOUT_SECONDS = 30
# Redis 配额 key 前缀
QUOTA_KEY_PREFIX = "report:quota:"
# 配额 TTL（秒）= 1天
QUOTA_TTL = 86400
# 配额上限
FREE_DAILY_LIMIT = 3
PREMIUM_DAILY_LIMIT = 20


async def _check_report_quota(user_id: str) -> int:
    """
    检查并扣除用户当日导出配额

    Returns:
        剩余配额次数

    Raises:
        HTTPException 429: 配额不足
    """
    today_str = date.today().isoformat()
    quota_key = f"{QUOTA_KEY_PREFIX}{user_id}:{today_str}"

    # 检查当前使用次数
    used_str = await cache_get(quota_key)
    used = int(used_str) if used_str else 0

    # 判断用户是否为 premium（简化：查询 subscription 表在 router 中处理）
    # 这里返回当前使用次数和是否超限，由 caller 判断
    return used


async def _increment_quota(user_id: str):
    """增加当日配额计数"""
    today_str = date.today().isoformat()
    quota_key = f"{QUOTA_KEY_PREFIX}{user_id}:{today_str}"

    used_str = await cache_get(quota_key)
    used = int(used_str) if used_str else 0
    new_used = used + 1

    # 用 set 实现（带 TTL）
    await cache_set(quota_key, str(new_used), ttl=QUOTA_TTL)


async def _check_quota_with_db(
    user_id: str,
    db: AsyncSession,
) -> tuple[int, int]:
    """检查配额，返回 (used, limit)"""
    from sqlalchemy import select

    today_str = date.today().isoformat()
    quota_key = f"{QUOTA_KEY_PREFIX}{user_id}:{today_str}"

    used_str = await cache_get(quota_key)
    used = int(used_str) if used_str else 0

    # 检查 subscription 判断是否为 premium
    result = await db.execute(
        select(Subscription).where(Subscription.user_id == user_id)
    )
    sub = result.scalar_one_or_none()

    is_premium = sub is not None and sub.is_active and sub.plan_type in ("premium", "pro")
    limit = PREMIUM_DAILY_LIMIT if is_premium else FREE_DAILY_LIMIT

    return used, limit


@router.post("/export")
async def export_report(
    req: ExportRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    导出学习报告（PDF 或 CSV）

    参数：
    - format: pdf / csv（默认 pdf）
    - period: today / week / month / all（默认 week）
    - subject: 学科过滤（可选，默认 all）

    配额：免费用户 3 次/日，Premium 用户 20 次/日
    超时：30 秒自动返回 504
    """
    # --- 配额检查 ---
    used, limit = await _check_quota_with_db(current_user.id, db)
    if used >= limit:
        raise HTTPException(
            status_code=429,
            detail=f"今日导出次数已达上限 ({limit}次/日)，请明日再试或升级为 Premium",
        )

    # --- 超时控制 ---
    try:
        data, content_type, filename = await asyncio.wait_for(
            generate_report(
                user_id=current_user.id,
                format=req.format.value,
                period=req.period.value,
                subject=req.subject or "all",
                db=db,
            ),
            timeout=REPORT_TIMEOUT_SECONDS,
        )
    except asyncio.TimeoutError:
        logger.warning("报告生成超时: user=%s, format=%s", current_user.id, req.format)
        raise HTTPException(
            status_code=504,
            detail="报告生成超时，请缩小统计周期后重试",
        )

    # --- 更新配额 ---
    await _increment_quota(current_user.id)

    # --- 返回文件 ---
    return Response(
        content=data.getvalue(),
        media_type=content_type,
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"',
            "X-Report-Quota-Remaining": str(limit - used - 1),
        },
    )
