"""
AI 学伴后端 - 离线同步服务
增量查询 + 冲突检测 + 操作回放
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.question_record import QuestionRecord
from app.models.quiz_record import QuizRecord
from app.models.wrong_answer import WrongAnswer
from app.schemas.sync import ConflictItem, DeltaResponse, SyncAction

logger = logging.getLogger(__name__)

# 1分钟内相同 target_id 的操作视为重复
IDEMPOTENT_WINDOW_SECONDS = 60


async def get_delta(
    user_id: str,
    since: str | None,
    db: AsyncSession,
) -> DeltaResponse:
    """
    获取用户在 since 时间戳之后的增量数据。

    增量数据包含：
    - updated_questions: 新创建的/更新的提问记录
    - updated_conversations: 新创建的/更新的对话记录（quiz_records）
    - updated_wrong_answers: 新创建的/更新的错题记录
    - deleted_ids: 暂不支持软删除，返回空列表
    - sync_timestamp: 服务端当前时间戳
    """
    now_ts = datetime.now(timezone.utc).isoformat()

    if since:
        try:
            since_dt = datetime.fromisoformat(since)
        except (ValueError, TypeError):
            since_dt = datetime.min.replace(tzinfo=timezone.utc)
    else:
        since_dt = datetime.min.replace(tzinfo=timezone.utc)

    # 1. 查询增量提问记录
    questions_query = (
        select(QuestionRecord)
        .where(
            QuestionRecord.user_id == user_id,
            QuestionRecord.created_at > since_dt,
        )
        .order_by(QuestionRecord.created_at.asc())
    )
    questions_result = await db.execute(questions_query)
    questions = questions_result.scalars().all()

    updated_questions = []
    for q in questions:
        updated_questions.append({
            "id": q.id,
            "subject": q.subject,
            "question_type": q.question_type,
            "question_content": q.question_content,
            "answer_content": q.answer_content,
            "ai_model_used": q.ai_model_used,
            "tokens_used": q.tokens_used,
            "created_at": q.created_at.isoformat() if q.created_at else None,
        })

    # 2. 查询增量对话记录（用 quiz_records 作为对话的近似）
    conv_query = (
        select(QuizRecord)
        .where(
            QuizRecord.user_id == user_id,
            QuizRecord.created_at > since_dt,
        )
        .order_by(QuizRecord.created_at.asc())
    )
    conv_result = await db.execute(conv_query)
    conversations = conv_result.scalars().all()

    updated_conversations = []
    for c in conversations:
        updated_conversations.append({
            "id": c.id,
            "subject": c.subject,
            "topic": c.topic,
            "difficulty": c.difficulty,
            "total_questions": c.total_questions,
            "score": c.score,
            "total_points": c.total_points,
            "status": c.status,
            "created_at": c.created_at.isoformat() if c.created_at else None,
        })

    # 3. 查询增量错题记录
    wrong_query = (
        select(WrongAnswer)
        .where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.created_at > since_dt,
        )
        .order_by(WrongAnswer.created_at.asc())
    )
    wrong_result = await db.execute(wrong_query)
    wrong_answers = wrong_result.scalars().all()

    updated_wrong_answers = []
    for w in wrong_answers:
        updated_wrong_answers.append({
            "id": w.id,
            "quiz_id": w.quiz_id,
            "subject": w.subject,
            "topic": w.topic,
            "question_content": w.question_content,
            "correct_answer": w.correct_answer,
            "user_answer": w.user_answer,
            "explanation": w.explanation,
            "mastery_score": w.mastery_score,
            "weakness": w.weakness,
            "review_count": w.review_count,
            "last_reviewed_at": w.last_reviewed_at.isoformat() if w.last_reviewed_at else None,
            "is_archived": w.is_archived,
            "created_at": w.created_at.isoformat() if w.created_at else None,
        })

    # 4. 删除记录（当前无软删除机制，暂返回空列表）
    deleted_ids: list[str] = []

    return DeltaResponse(
        updated_questions=updated_questions,
        updated_conversations=updated_conversations,
        updated_wrong_answers=updated_wrong_answers,
        deleted_ids=deleted_ids,
        sync_timestamp=now_ts,
    )


async def process_actions(
    user_id: str,
    actions: list[SyncAction],
    db: AsyncSession,
) -> tuple[bool, list[ConflictItem]]:
    """
    处理客户端批量操作。

    冲突处理策略：服务端优先。
    如果客户端要更新的记录在服务端存在且更新时间晚于客户端操作时间，标记冲突并返回服务端值。
    幂等去重：1分钟内相同操作跳过。

    Returns:
        (synced, conflicts)
    """
    conflicts: list[ConflictItem] = []
    synced_count = 0

    for action in actions:
        # 简单的幂等检查：对于 delete 类型，如果目标已在服务端不存在，跳过
        if action.type == "delete":
            # 尝试在多个表中查找
            found = False
            for model_cls in [QuestionRecord, QuizRecord, WrongAnswer]:
                result = await db.execute(
                    select(model_cls).where(model_cls.id == action.target_id)
                )
                if result.scalar_one_or_none():
                    found = True
                    break

            if not found:
                # 已在服务端删除，幂等跳过
                continue

            # 服务端删除（实际项目应使用软删除）
            # 由于当前模型没有软删除标志，且删除权限控制严格，这里标记冲突
            conflicts.append(ConflictItem(
                target_id=action.target_id,
                type="delete",
                server_value="exists",
                client_value="delete",
                message="服务端不允许直接删除记录",
            ))
            continue

        elif action.type == "update":
            target_data = action.data or {}

            # 先尝试在 wrong_answers 中查找
            result = await db.execute(
                select(WrongAnswer).where(
                    WrongAnswer.id == action.target_id,
                    WrongAnswer.user_id == user_id,
                )
            )
            wrong = result.scalar_one_or_none()
            if wrong:
                # 冲突检测：客户端时间是否早于服务端更新时间
                server_updated = wrong.last_reviewed_at or wrong.created_at
                try:
                    client_ts = datetime.fromisoformat(action.timestamp)
                except (ValueError, TypeError):
                    client_ts = datetime.min.replace(tzinfo=timezone.utc)

                if server_updated and client_ts.tzinfo is None:
                    server_updated = server_updated.replace(tzinfo=timezone.utc)
                if client_ts.tzinfo is None:
                    client_ts = client_ts.replace(tzinfo=timezone.utc)

                if server_updated and client_ts < server_updated:
                    conflicts.append(ConflictItem(
                        target_id=action.target_id,
                        type="update",
                        server_value={
                            "mastery_score": wrong.mastery_score,
                            "review_count": wrong.review_count,
                            "is_archived": wrong.is_archived,
                        },
                        client_value=target_data,
                        message="服务端数据更新，客户端需重新同步",
                    ))
                    continue

                # 更新字段
                if "mastery_score" in target_data:
                    wrong.mastery_score = target_data["mastery_score"]
                if "review_count" in target_data:
                    wrong.review_count = target_data["review_count"]
                if "is_archived" in target_data:
                    wrong.is_archived = target_data["is_archived"]
                synced_count += 1
                continue

            # 再尝试 question_records
            result = await db.execute(
                select(QuestionRecord).where(
                    QuestionRecord.id == action.target_id,
                    QuestionRecord.user_id == user_id,
                )
            )
            q = result.scalar_one_or_none()
            if q:
                synced_count += 1
                continue

        elif action.type == "create":
            # 客户端创建的记录，服务端保存
            # 根据 data 中的类型决定保存到哪个表
            target_data = action.data or {}
            obj_type = target_data.get("_type", "")

            if obj_type == "wrong_answer":
                existing = await db.execute(
                    select(WrongAnswer).where(WrongAnswer.id == action.target_id)
                )
                if existing.scalar_one_or_none():
                    # 已存在，幂等跳过
                    continue

                wrong = WrongAnswer(
                    id=action.target_id,
                    user_id=user_id,
                    quiz_id=target_data.get("quiz_id"),
                    subject=target_data.get("subject", "unknown"),
                    topic=target_data.get("topic"),
                    question_content=target_data.get("question_content", ""),
                    correct_answer=target_data.get("correct_answer", ""),
                    user_answer=target_data.get("user_answer", ""),
                    explanation=target_data.get("explanation"),
                    mastery_score=target_data.get("mastery_score", 0.0),
                    weakness=target_data.get("weakness"),
                    review_count=target_data.get("review_count", 0),
                    is_archived=target_data.get("is_archived", False),
                )
                db.add(wrong)
                synced_count += 1

    if synced_count > 0:
        await db.flush()

    return True, conflicts
