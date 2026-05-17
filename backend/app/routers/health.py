"""
AI 学伴后端 - 健康检查路由
"""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.schemas.common import success

router = APIRouter(tags=["健康检查"])


@router.get("/health")
async def health_check(db: AsyncSession = Depends(get_db)):
    """健康检查接口 — 包含数据库连通性验证"""
    db_status = "ok"
    try:
        await db.execute(text("SELECT 1"))
    except Exception:
        db_status = "error"

    return success(
        data={
            "status": "ok",
            "version": "1.0.0",
            "service": "AI学伴后端",
            "database": db_status,
        }
    )
