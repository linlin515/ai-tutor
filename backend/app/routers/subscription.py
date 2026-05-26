"""
AI 学伴后端 - 订阅路由
查询订阅状态、验证 Google Play 订阅、消耗配额
"""
from __future__ import annotations

import logging
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.subscription import Subscription
from app.models.daily_quota import DailyQuota
from app.models.user import User
from app.schemas.common import success, error as api_error
from app.schemas.subscription import (
    SubscriptionStatus,
    VerifySubscriptionRequest,
    ConsumeQuotaRequest,
    ConsumeQuotaResponse,
)

logger = logging.getLogger(__name__)
settings = get_settings()

router = APIRouter(prefix="/api/v1/subscription", tags=["订阅"])

# 各套餐的功能列表
PLAN_FEATURES = {
    "free": ["基础对话", "拍照解题", "每日 5 次提问"],
    "premium": ["基础对话", "拍照解题", "语音交互", "无限提问", "优先模型"],
}


@router.get("/status")
async def get_subscription_status(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """查询当前用户的订阅状态"""
    result = await db.execute(
        select(Subscription).where(Subscription.user_id == current_user.id)
    )
    sub = result.scalar_one_or_none()

    if sub is None:
        # 无订阅记录，返回免费版
        return success(
            data=SubscriptionStatus(
                plan_type="free",
                is_active=True,
                daily_quota=settings.daily_quota_free,
                features=PLAN_FEATURES["free"],
            )
        )

    # 计算剩余天数
    days_remaining = None
    if sub.end_date:
        end = sub.end_date
        if isinstance(end, datetime):
            end = end.date()
        days_remaining = (end - date.today()).days if end > date.today() else 0

    # 根据套餐类型确定每日配额
    plan_quota = settings.daily_quota_premium if sub.plan_type == "premium" else settings.daily_quota_free

    return success(
        data=SubscriptionStatus(
            plan_type=sub.plan_type,
            is_active=sub.is_active,
            start_date=sub.start_date,
            end_date=sub.end_date,
            days_remaining=max(0, days_remaining) if days_remaining is not None else None,
            daily_quota=plan_quota,
            features=PLAN_FEATURES.get(sub.plan_type, PLAN_FEATURES["free"]),
        )
    )


@router.post("/verify")
async def verify_subscription(
    req: VerifySubscriptionRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    验证 Google Play 订阅
    在实际生产环境中，应向 Google Play Developer API 验证 purchase_token
    """
    # 查找现有订阅
    result = await db.execute(
        select(Subscription).where(Subscription.user_id == current_user.id)
    )
    sub = result.scalar_one_or_none()

    now = datetime.now(timezone.utc)

    if sub:
        # 更新现有订阅
        sub.plan_type = "premium"
        sub.start_date = now
        sub.end_date = now + timedelta(days=30)
        sub.is_active = True
    else:
        # 创建新订阅
        sub = Subscription(
            user_id=current_user.id,
            plan_type="premium",
            start_date=now,
            end_date=now + timedelta(days=30),
            is_active=True,
        )
        db.add(sub)

    await db.flush()

    return success(
        data=SubscriptionStatus(
            plan_type=sub.plan_type,
            is_active=sub.is_active,
            start_date=sub.start_date,
            end_date=sub.end_date,
            days_remaining=30,
            daily_quota=settings.daily_quota_premium,
            features=PLAN_FEATURES["premium"],
        ),
        message="订阅验证成功",
    )


# ============================================================
# POST /api/v1/subscription/consume
# ============================================================


@router.post("/consume")
async def consume_quota(
    req: ConsumeQuotaRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    消耗配额

    验证配额 → 扣减 → 返回
    - premium 用户：不消耗配额，返回 quota_remaining=-1
    - free 用户：原子扣减，防止超卖
    """
    user_id = current_user.id
    today = date.today()

    # 1. 获取用户订阅状态
    result = await db.execute(
        select(Subscription).where(Subscription.user_id == user_id)
    )
    sub = result.scalar_one_or_none()

    is_premium = sub is not None and sub.plan_type == "premium" and sub.is_active

    # 2. premium 用户：不消耗配额
    if is_premium:
        return success(
            data=ConsumeQuotaResponse(
                consumed=True,
                quota_remaining=-1,
                plan_type="premium",
                is_premium=True,
            ).model_dump(),
            message="配额消耗成功（premium 无限）",
        )

    # 3. free 用户：原子配额扣减
    feature = req.feature

    # 先尝试获取当日配额记录
    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
            DailyQuota.feature == feature,
        )
    )
    quota = result.scalar_one_or_none()

    if quota is None:
        # 创建新配额记录
        quota = DailyQuota(
            user_id=user_id,
            date=today,
            feature=feature,
            questions_used=0,
            questions_limit=settings.daily_quota_free,
        )
        db.add(quota)
        await db.flush()

    # 4. 检查配额是否充足
    if quota.questions_used >= quota.questions_limit:
        return api_error(
            code=403,
            message="今日配额已用完",
            data=ConsumeQuotaResponse(
                consumed=False,
                quota_remaining=0,
                plan_type="free",
                is_premium=False,
            ).model_dump(),
        )

    # 5. 原子扣减
    quota.questions_used += 1
    await db.flush()

    remaining = quota.questions_limit - quota.questions_used

    return success(
        data=ConsumeQuotaResponse(
            consumed=True,
            quota_remaining=remaining,
            plan_type="free",
            is_premium=False,
        ).model_dump(),
        message="配额消耗成功",
    )
