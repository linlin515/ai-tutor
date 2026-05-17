"""
AI 学伴后端 - 对话路由
提交问题、获取历史记录
"""

from __future__ import annotations

import logging
from datetime import date as date_type

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.daily_quota import DailyQuota
from app.models.question_record import QuestionRecord
from app.models.user import User
from app.schemas.chat import (
    ChatRequest,
    ChatResponse,
    PaginatedHistory,
    QuestionHistoryItem,
)
from app.schemas.common import success
from app.services.ai_service import chat as ai_chat
from app.services.ai_service import content_filter
from app.services.quota_service import check_quota

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/chat", tags=["对话"])


@router.post("/ask")
async def ask_question(
    req: ChatRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """提交问题给 AI 学伴"""
    # 1. 检查每日配额（原子扣减）
    within_limit, used, limit_val = await check_quota(current_user.id, db)
    if not within_limit:
        raise HTTPException(
            status_code=429,
            detail=f"今日提问次数已达上限（{limit_val}次），升级为高级版可解锁更多次数",
        )

    # 2. 检查问题内容安全
    safety = await content_filter(req.question)
    if not safety["safe"]:
        raise HTTPException(
            status_code=400,
            detail="问题内容不符合安全规范，请重新提问",
        )

    # 3. 构造对话消息
    messages = [
        {"role": "user", "content": f"学科：{req.subject}\n\n问题：{req.question}"},
    ]

    # 4. 调用 AI
    try:
        result = await ai_chat(messages)
        answer = result["content"]

        # 5. 记录提问
        record = QuestionRecord(
            user_id=current_user.id,
            subject=req.subject,
            question_type="text",
            question_content=req.question,
            answer_content=answer,
            ai_model_used=result.get("model"),
            tokens_used=result.get("total_tokens"),
        )
        db.add(record)

        return success(
            data=ChatResponse(
                answer=answer,
                subject=req.subject,
                model_used=result.get("model"),
                tokens_used=result.get("total_tokens"),
            )
        )

    except (TimeoutError, RuntimeError) as e:
        raise HTTPException(status_code=503, detail=str(e))


@router.get("/history")
async def get_history(
    page: int = Query(default=1, ge=1, description="页码"),
    page_size: int = Query(default=20, ge=1, le=100, description="每页记录数"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """获取用户的历史提问记录（支持 page/page_size 分页）"""
    # 计算 offset
    offset = (page - 1) * page_size

    # 查询总数
    count_result = await db.execute(
        select(func.count()).select_from(QuestionRecord).where(
            QuestionRecord.user_id == current_user.id
        )
    )
    total = count_result.scalar() or 0

    # 查询列表
    result = await db.execute(
        select(QuestionRecord)
        .where(QuestionRecord.user_id == current_user.id)
        .order_by(QuestionRecord.created_at.desc())
        .offset(offset)
        .limit(page_size)
    )
    records = result.scalars().all()

    total_pages = max(1, (total + page_size - 1) // page_size)

    return success(
        data=PaginatedHistory(
            items=[QuestionHistoryItem.model_validate(r) for r in records],
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages,
        ),
    )
