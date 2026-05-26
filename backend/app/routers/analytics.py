"""
AI 学伴后端 - 学习统计路由
GET /api/v1/analytics/stats  — 学习统计数据
"""
from __future__ import annotations

import logging
from datetime import date, timedelta

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func, cast, Date
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.question_record import QuestionRecord
from app.models.quiz_record import QuizRecord
from app.models.quiz_question import QuizQuestion
from app.models.wrong_answer import WrongAnswer
from app.models.user import User
from app.schemas.common import success
from app.schemas.analytics import (
    AnalyticsStatsData,
    DailyStat,
    Overview,
    SubjectBreakdown,
    WeakArea,
)
from app.services.redis_service import cache_get, cache_set_json

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/analytics", tags=["学习统计"])


# ============================================================
# GET /api/v1/analytics/stats
# ============================================================


@router.get("/stats")
async def get_analytics_stats(
    period: str = Query(default="weekly", description="统计周期: daily/weekly/monthly/all"),
    subject: str = Query(default="all", description="学科过滤（可选）"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    获取学习统计数据
    聚合现有数据：question_records、quiz_records、wrong_answers
    """
    user_id = current_user.id
    today = date.today()

    # 尝试从缓存读取
    cache_key = f"analytics:stats:{user_id}:{period}:{subject}"
    cached = await cache_get(cache_key)
    if cached:
        import json
        return success(data=json.loads(cached), message="获取学习统计成功（缓存）")

    # --- 1. 计算学习天数范围 ---
    if period == "daily":
        days_back = 1
    elif period == "weekly":
        days_back = 7
    elif period == "monthly":
        days_back = 30
    else:  # all
        days_back = 90  # 限制最近 90 天

    start_date = today - timedelta(days=days_back - 1)

    # --- 2. 今日统计 ---
    today_str = today.isoformat()

    # 今日 question_records 统计
    today_q_count_query = select(func.count()).select_from(QuestionRecord).where(
        QuestionRecord.user_id == user_id,
        cast(QuestionRecord.created_at, Date) == today,
    )
    if subject != "all":
        today_q_count_query = today_q_count_query.where(QuestionRecord.subject == subject)
    today_q_count = (await db.execute(today_q_count_query)).scalar() or 0

    # 今日正确率（从 question_records 无法直接得正确率，用 quiz_records score 估算）
    # 使用 quiz_records 的分数作为辅助
    today_quiz_count_query = select(func.count()).select_from(QuizRecord).where(
        QuizRecord.user_id == user_id,
        cast(QuizRecord.created_at, Date) == today,
        QuizRecord.status == "completed",
    )
    today_quiz_count = (await db.execute(today_quiz_count_query)).scalar() or 0

    # 从 wrong_answers 估算今日正确率
    today_wrong_query = select(func.count()).select_from(WrongAnswer).where(
        WrongAnswer.user_id == user_id,
        cast(WrongAnswer.created_at, Date) == today,
    )
    today_wrong_count = (await db.execute(today_wrong_query)).scalar() or 0

    total_today = today_q_count + today_quiz_count
    today_accuracy = 0.0
    if total_today > 0:
        # 估算：总记录数 - 错题数 / 总记录数
        correct_estimate = max(0, total_today - today_wrong_count)
        today_accuracy = round(correct_estimate / total_today, 2)

    # 学习分钟估算（每道题≈3分钟）
    study_minutes_today = total_today * 3

    # --- 3. 连续学习天数 (streak_days) ---
    streak_days = await _calc_streak_days(user_id, db, today)

    # --- 4. 知识点掌握统计 ---
    # 总知识点数：从 quiz_questions 获取 distinct topic
    topic_count_query = select(func.count(func.distinct(QuizQuestion.content))).select_from(
        QuizQuestion
    ).join(QuizRecord, QuizRecord.id == QuizQuestion.quiz_id).where(
        QuizRecord.user_id == user_id
    )
    total_knowledge_points = (await db.execute(topic_count_query)).scalar() or 0

    # 已掌握知识点数：wrong_answers 中 mastery_score >= 0.6 的 topic 数
    mastered_query = select(func.count(func.distinct(WrongAnswer.topic))).where(
        WrongAnswer.user_id == user_id,
        WrongAnswer.mastery_score >= 0.6,
    )
    mastered_points = (await db.execute(mastered_query)).scalar() or 0

    mastery_rate = round(mastered_points / total_knowledge_points, 2) if total_knowledge_points > 0 else 0.0

    # --- 5. 每日统计 (daily_stats) ---
    daily_stats = await _calc_daily_stats(user_id, db, start_date, today, subject)

    # --- 6. 学科细分 (subject_breakdown) ---
    subject_breakdown = await _calc_subject_breakdown(user_id, db, start_date, today)

    # --- 7. 薄弱环节 (weak_areas) ---
    weak_areas = await _calc_weak_areas(user_id, db)

    overview = Overview(
        study_minutes_today=study_minutes_today,
        questions_solved_today=total_today,
        accuracy_today=today_accuracy,
        streak_days=streak_days,
        total_knowledge_points=total_knowledge_points,
        mastered_points=mastered_points,
        mastery_rate=mastery_rate,
    )

    data = AnalyticsStatsData(
        overview=overview,
        daily_stats=daily_stats,
        subject_breakdown=subject_breakdown,
        weak_areas=weak_areas,
    ).model_dump()

    # 写入缓存（5 分钟）
    import json
    await cache_set_json(cache_key, data, ttl=300)

    return success(data=data, message="获取学习统计成功")


async def _calc_streak_days(
    user_id: str,
    db: AsyncSession,
    today: date,
) -> int:
    """计算连续学习天数：从今天往前，每天至少有一次有效学习行为"""
    streak = 0
    check_date = today

    for _ in range(365):  # 最多检查一年
        q_count = (
            await db.execute(
                select(func.count()).select_from(QuestionRecord).where(
                    QuestionRecord.user_id == user_id,
                    cast(QuestionRecord.created_at, Date) == check_date,
                )
            )
        ).scalar() or 0
        quiz_count = (
            await db.execute(
                select(func.count()).select_from(QuizRecord).where(
                    QuizRecord.user_id == user_id,
                    cast(QuizRecord.created_at, Date) == check_date,
                )
            )
        ).scalar() or 0

        if q_count > 0 or quiz_count > 0:
            streak += 1
            check_date -= timedelta(days=1)
        else:
            break

    return streak


async def _calc_daily_stats(
    user_id: str,
    db: AsyncSession,
    start_date: date,
    end_date: date,
    subject_filter: str,
) -> list[dict]:
    """计算每日统计"""
    daily_stats = []
    current = start_date

    while current <= end_date:
        q_count_query = select(func.count()).select_from(QuestionRecord).where(
            QuestionRecord.user_id == user_id,
            cast(QuestionRecord.created_at, Date) == current,
        )
        if subject_filter != "all":
            q_count_query = q_count_query.where(QuestionRecord.subject == subject_filter)
        q_count = (await db.execute(q_count_query)).scalar() or 0

        wrong_query = select(func.count()).select_from(WrongAnswer).where(
            WrongAnswer.user_id == user_id,
            cast(WrongAnswer.created_at, Date) == current,
        )
        wrong_count = (await db.execute(wrong_query)).scalar() or 0

        accuracy = 0.0
        if q_count > 0:
            correct_est = max(0, q_count - wrong_count)
            accuracy = round(correct_est / q_count, 2)

        daily_stats.append({
            "date": current.isoformat(),
            "study_minutes": q_count * 3,
            "questions_solved": q_count,
            "accuracy": accuracy,
        })

        current += timedelta(days=1)

    return daily_stats


async def _calc_subject_breakdown(
    user_id: str,
    db: AsyncSession,
    start_date: date,
    end_date: date,
) -> list[dict]:
    """计算各学科统计数据"""
    subjects = ["math", "physics", "chemistry", "biology", "chinese", "english"]
    breakdown = []

    for subj in subjects:
        q_count_query = select(func.count()).select_from(QuestionRecord).where(
            QuestionRecord.user_id == user_id,
            QuestionRecord.subject == subj,
            cast(QuestionRecord.created_at, Date) >= start_date,
            cast(QuestionRecord.created_at, Date) <= end_date,
        )
        q_count = (await db.execute(q_count_query)).scalar() or 0

        wrong_query = select(func.count()).select_from(WrongAnswer).where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.subject == subj,
            cast(WrongAnswer.created_at, Date) >= start_date,
            cast(WrongAnswer.created_at, Date) <= end_date,
        )
        wrong_count = (await db.execute(wrong_query)).scalar() or 0

        accuracy = 0.0
        if q_count > 0:
            correct_est = max(0, q_count - wrong_count)
            accuracy = round(correct_est / q_count, 2)

        if q_count > 0:
            breakdown.append({
                "subject": subj,
                "questions_solved": q_count,
                "accuracy": accuracy,
                "study_minutes": q_count * 3,
            })

    return breakdown


async def _calc_weak_areas(
    user_id: str,
    db: AsyncSession,
) -> list[dict]:
    """计算薄弱环节"""
    result = await db.execute(
        select(
            WrongAnswer.topic,
            WrongAnswer.subject,
            func.avg(WrongAnswer.mastery_score).label("avg_mastery"),
        )
        .where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.topic.isnot(None),
        )
        .group_by(WrongAnswer.topic, WrongAnswer.subject)
        .having(func.avg(WrongAnswer.mastery_score) < 0.6)
    )
    rows = result.all()

    return [
        {
            "topic": row.topic,
            "mastery": round(float(row.avg_mastery), 2),
            "subject": row.subject,
        }
        for row in rows
        if row.topic
    ]
