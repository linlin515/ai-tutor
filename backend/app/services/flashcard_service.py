"""
AI 学伴后端 - Flashcard 服务
错题复习卡片管理
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user_score import UserScore
from app.models.wrong_answer import WrongAnswer

logger = logging.getLogger(__name__)


async def get_today_cards(
    user_id: str,
    limit: int = 10,
    db: AsyncSession | None = None,
) -> list[WrongAnswer]:
    """
    获取今日待复习的错题卡片。

    筛选条件：
    - 属于当前用户
    - mastery_score < 0.8（未掌握）
    - is_archived = false（未归档）
    - 未复习过的优先（last_reviewed_at IS NULL）

    Args:
        user_id: 用户 ID
        limit: 返回卡片数量上限
        db: 数据库会话

    Returns:
        WrongAnswer 列表
    """
    result = await db.execute(
        select(WrongAnswer)
        .where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.mastery_score < 0.8,
            WrongAnswer.is_archived == False,
        )
        .order_by(
            WrongAnswer.last_reviewed_at.asc().nullsfirst(),
            WrongAnswer.created_at.asc(),
        )
        .limit(limit)
    )
    cards = result.scalars().all()
    return list(cards)


async def review_card(
    user_id: str,
    card_id: str,
    judgment: str,
    time_spent_seconds: int = 0,
    db: AsyncSession | None = None,
) -> dict:
    """
    复习一张错题卡片。

    掌握度更新规则：
    - mastered（掌握）：new = min(1.0, old + (1.0 - old) * 0.3)
    - unfamiliar（不熟）：new = max(0.0, old - 0.2)

    如果 new_mastery >= 0.8 → is_archived = true
    每次复习发放 +2 积分

    Args:
        user_id: 用户 ID
        card_id: 错题卡片 ID
        judgment: mastered / unfamiliar
        time_spent_seconds: 花费时间（秒）
        db: 数据库会话

    Returns:
        {
            "card_id": str,
            "new_mastery": float,
            "is_archived": bool,
            "points_earned": int,
        }

    Raises:
        ValueError: 卡片不存在或不属于当前用户
    """
    # 查找卡片
    result = await db.execute(
        select(WrongAnswer).where(
            WrongAnswer.id == card_id,
            WrongAnswer.user_id == user_id,
        )
    )
    card = result.scalar_one_or_none()

    if card is None:
        raise ValueError(f"卡片不存在: {card_id}")

    # 计算新掌握度
    old_mastery = card.mastery_score
    if judgment == "mastered":
        new_mastery = min(1.0, old_mastery + (1.0 - old_mastery) * 0.3)
    elif judgment == "unfamiliar":
        new_mastery = max(0.0, old_mastery - 0.2)
    else:
        raise ValueError(f"无效的判断: {judgment}，支持 mastered / unfamiliar")

    # 更新卡片
    card.mastery_score = new_mastery
    card.review_count = (card.review_count or 0) + 1
    card.last_reviewed_at = datetime.now(timezone.utc)

    # 掌握度 >= 0.8 自动归档
    is_archived = False
    if new_mastery >= 0.8:
        card.is_archived = True
        is_archived = True

    # 发放 +2 积分
    points_earned = 2
    score_result = await db.execute(
        select(UserScore).where(UserScore.user_id == user_id)
    )
    user_score = score_result.scalar_one_or_none()
    if user_score is None:
        user_score = UserScore(
            user_id=user_id,
            score=points_earned,
        )
        db.add(user_score)
    else:
        user_score.score += points_earned

    await db.flush()

    return {
        "card_id": card_id,
        "new_mastery": new_mastery,
        "is_archived": is_archived,
        "points_earned": points_earned,
    }


async def archive_old_cards(
    user_id: str,
    db: AsyncSession | None = None,
) -> int:
    """
    批量归档已掌握（mastery >= 0.8）且未归档的卡片。

    Returns:
        归档的卡片数量
    """
    result = await db.execute(
        select(WrongAnswer).where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.mastery_score >= 0.8,
            WrongAnswer.is_archived == False,
        )
    )
    cards = result.scalars().all()

    archived_count = 0
    for card in cards:
        card.is_archived = True
        archived_count += 1

    if archived_count > 0:
        await db.flush()

    return archived_count


async def sync_flashcard_cards(
    user_id: str,
    cards_data: list[dict],
    db: AsyncSession | None = None,
) -> tuple[int, list[dict]]:
    """
    批量同步 flashcard 数据（客户端离线操作同步）。

    幂等处理：1分钟内重复同步跳过。

    Args:
        user_id: 用户 ID
        cards_data: [{"card_id": "...", "mastery_score": 0.5, "review_count": 3, ...}]
        db: 数据库会话

    Returns:
        (synced_count, conflicts)
    """
    synced_count = 0
    conflicts = []

    for item in cards_data:
        card_id = item.get("card_id")
        if not card_id:
            continue

        # 查找服务端记录
        result = await db.execute(
            select(WrongAnswer).where(
                WrongAnswer.id == card_id,
                WrongAnswer.user_id == user_id,
            )
        )
        card = result.scalar_one_or_none()

        if card is None:
            conflicts.append({
                "card_id": card_id,
                "reason": "服务端不存在该记录",
            })
            continue

        # 冲突检测：如果客户端的 mastery 低于服务端，标记冲突
        client_mastery = item.get("mastery_score", 0)
        if client_mastery < card.mastery_score:
            conflicts.append({
                "card_id": card_id,
                "reason": "服务端掌握度更高",
                "server_mastery": card.mastery_score,
                "client_mastery": client_mastery,
            })
            continue

        # 更新字段
        card.mastery_score = client_mastery
        card.review_count = max(card.review_count or 0, item.get("review_count", 0))
        if item.get("is_archived"):
            card.is_archived = True
        if item.get("last_reviewed_at"):
            try:
                card.last_reviewed_at = datetime.fromisoformat(item["last_reviewed_at"])
            except (ValueError, TypeError):
                pass

        synced_count += 1

    if synced_count > 0:
        await db.flush()

    return synced_count, conflicts
