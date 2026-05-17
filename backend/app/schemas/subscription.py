"""
AI 学伴后端 - 订阅相关 Pydantic Schema
"""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class SubscriptionStatus(BaseModel):
    """订阅状态"""
    plan_type: str
    is_active: bool
    start_date: datetime | None = None
    end_date: datetime | None = None
    days_remaining: int | None = None
    daily_quota: int = 5
    features: list[str] = Field(default_factory=list, description="功能列表")

    model_config = {"from_attributes": True}


class VerifySubscriptionRequest(BaseModel):
    """验证 Google Play 订阅请求"""
    purchase_token: str = Field(..., description="Google Play 购买凭证 token")
    product_id: str = Field(..., description="商品 ID")
