"""
AI 学伴后端 - Flashcard 路由
错题复习卡片 API
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.common import ApiResponse, success, error as api_error
from app.schemas.flashcard import (
    FlashcardOut,
    ReviewRequest,
    ReviewResponse,
    SyncFlashcardRequest,
    SyncFlashcardResponse,
    TodayCardsResponse,
)
from app.services.flashcard_service import (
    archive_old_cards,
    get_today_cards,
    review_card,
    sync_flashcard_cards,
)
from app.services.redis_service import cache_exists, cache_set

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/flashcard", tags=["flashcard"])


@router.get("/today", response_model=ApiResponse[TodayCardsResponse])
async def get_today_flashcards(
    limit: int = Query(default=10, ge=1, le=50, description="返回卡片数量上限"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    获取今日待复习的错题卡片。

    筛选逻辑：
    - 掌握度 < 0.8
    - 未归档
    - 未复习的优先
    """
    cards = await get_today_cards(
        user_id=current_user.id,
        limit=limit,
        db=db,
    )

    total_result = await db.execute(
        __import__("sqlalchemy").select(__import__("sqlalchemy").func.count())
        .select_from(__import__("app.models.wrong_answer", fromlist=["WrongAnswer"]).WrongAnswer)
        .where(
            __import__("app.models.wrong_answer", fromlist=["WrongAnswer"]).WrongAnswer.user_id == current_user.id,
            __import__("app.models.wrong_answer", fromlist=["WrongAnswer"]).WrongAnswer.mastery_score < 0.8,
            __import__("app.models.wrong_answer", fromlist=["WrongAnswer"]).WrongAnswer.is_archived == False,
        )
    )
    total = total_result.scalar() or 0

    card_list = []
    for c in cards:
        card_list.append(FlashcardOut(
            card_id=c.id,
            subject=c.subject,
            topic=c.topic,
            question_content=c.question_content,
            correct_answer=c.correct_answer,
            user_answer=c.user_answer,
            explanation=c.explanation,
            mastery_score=c.mastery_score,
            review_count=c.review_count,
            last_reviewed_at=c.last_reviewed_at.isoformat() if c.last_reviewed_at else None,
            is_archived=c.is_archived,
        ))

    return success(
        data=TodayCardsResponse(
            cards=card_list,
            total=total,
        ).model_dump(),
        message="获取今日卡片成功",
    )


@router.post("/review", response_model=ApiResponse[ReviewResponse])
async def review_flashcard(
    req: ReviewRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    复习一张错题卡片并更新掌握度。

    掌握度规则：
    - mastered：new = min(1.0, old + (1.0 - old) * 0.3)
    - unfamiliar：new = max(0.0, old - 0.2)

    掌握度 >= 0.8 自动归档。
    每次复习发放 +2 积分。
    """
    try:
        result = await review_card(
            user_id=current_user.id,
            card_id=req.card_id,
            judgment=req.judgment,
            time_spent_seconds=req.time_spent_seconds,
            db=db,
        )
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

    # 下次复习建议
    if result["new_mastery"] >= 0.8:
        next_suggestion = "已掌握，无需再次复习"
    elif result["new_mastery"] < 0.3:
        next_suggestion = "建议明天再次复习"
    elif result["new_mastery"] < 0.6:
        next_suggestion = "建议 2-3 天后再次复习"
    else:
        next_suggestion = "建议 3-5 天后再次复习"

    return success(
        data=ReviewResponse(
            card_id=result["card_id"],
            new_mastery=result["new_mastery"],
            is_archived=result["is_archived"],
            points_earned=result["points_earned"],
            next_review_suggestion=next_suggestion,
        ).model_dump(),
        message="复习成功",
    )


@router.post("/sync", response_model=ApiResponse[SyncFlashcardResponse])
async def sync_flashcard(
    req: SyncFlashcardRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    批量同步错题卡片数据（离线操作同步）。

    幂等处理：1分钟内相同请求自动跳过。
    冲突策略：服务端掌握度高于客户端时标记冲突。
    """
    # 幂等检查
    import json
    idempotency_key = f"flashcard:sync:{current_user.id}:{hash(str(req.model_dump()))}"
    if await cache_exists(idempotency_key):
        return success(
            data=SyncFlashcardResponse(
                synced=True,
                synced_count=0,
                conflicts=[],
            ).model_dump(),
            message="重复请求，已跳过",
        )

    # 设置幂等 key（1分钟过期）
    await cache_set(idempotency_key, json.dumps({"processed": True}), ttl=60)

    cards_data = [c.model_dump() for c in req.cards]
    synced_count, conflicts = await sync_flashcard_cards(
        user_id=current_user.id,
        cards_data=cards_data,
        db=db,
    )

    return success(
        data=SyncFlashcardResponse(
            synced=True,
            synced_count=synced_count,
            conflicts=conflicts,
        ).model_dump(),
        message=f"同步完成，成功 {synced_count} 条" + (f"，冲突 {len(conflicts)} 条" if conflicts else ""),
    )
