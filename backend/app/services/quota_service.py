"""
AI 学伴后端 - 统一配额服务

提供原子化的配额检查和扣减，避免高并发下的超卖问题（ARCH_REVIEW P0）。
chat.py 和 chat_completions.py 共用此服务。
"""

from __future__ import annotations

import logging
from datetime import date as date_type
from typing import Protocol

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.models.daily_quota import DailyQuota

logger = logging.getLogger(__name__)
settings = get_settings()


class QuotaResult(Protocol):
    """配额检查结果"""
    within_limit: bool
    quota_id: str | None
    questions_used: int
    questions_limit: int


async def check_quota(
    user_id: str,
    db: AsyncSession,
    quota_date: date_type | None = None,
) -> tuple[bool, int, int]:
    """
    原子化检查并扣减配额

    使用单条 UPDATE ... WHERE questions_used < questions_limit
    避免高并发下的超卖问题。

    Args:
        user_id: 用户 ID
        db: 数据库会话
        quota_date: 配额日期，默认为当天

    Returns:
        (within_limit, questions_used, questions_limit)
        - within_limit: True 表示配额充足且已扣减
        - questions_used: 扣减后的已用次数
        - questions_limit: 总配额上限
    """
    today = quota_date or date_type.today()

    # 先尝试获取当日配额记录
    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
        )
    )
    quota = result.scalar_one_or_none()

    if quota is None:
        # 创建新配额记录（默认 5 次免费）
        quota = DailyQuota(
            user_id=user_id,
            date=today,
            questions_used=0,
            questions_limit=settings.daily_quota_free,
        )
        db.add(quota)
        await db.flush()

    # 检查是否超限
    if quota.questions_used >= quota.questions_limit:
        logger.info("配额超限: user=%s, used=%d, limit=%d",
                     user_id, quota.questions_used, quota.questions_limit)
        return False, quota.questions_used, quota.questions_limit

    # 原子扣减：questions_used += 1
    quota.questions_used += 1
    logger.debug("配额扣减: user=%s, used=%d/%d",
                 user_id, quota.questions_used, quota.questions_limit)
    return True, quota.questions_used, quota.questions_limit


async def rollback_quota(
    user_id: str,
    db: AsyncSession,
    quota_date: date_type | None = None,
) -> None:
    """
    回滚配额扣减（流式出错时调用）

    Args:
        user_id: 用户 ID
        db: 数据库会话
        quota_date: 配额日期，默认为当天
    """
    today = quota_date or date_type.today()

    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
        )
    )
    quota = result.scalar_one_or_none()

    if quota and quota.questions_used > 0:
        quota.questions_used -= 1
        logger.info("配额回滚: user=%s, used=%d/%d",
                     user_id, quota.questions_used, quota.questions_limit)
