"""
AI 学伴后端 - 测验路由
POST /api/v1/quiz/generate  — 生成练习题
POST /api/v1/quiz/submit    — 提交答案并批改
"""
from __future__ import annotations

import json
import logging
import random
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.daily_quota import DailyQuota
from app.models.quiz_record import QuizRecord
from app.models.quiz_question import QuizQuestion
from app.models.question_record import QuestionRecord
from app.models.user import User
from app.models.wrong_answer import WrongAnswer
from app.schemas.common import success, error as api_error
from app.schemas.quiz import (
    AnswerItem,
    QuizGenerateRequest,
    QuizGenerateData,
    QuizQuestionOut,
    QuizSubmitRequest,
    QuizSubmitData,
    QuestionResult,
    QuizSummary,
)
from app.services.redis_service import cache_get, cache_set

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/quiz", tags=["测验"])

# 有效学科列表
VALID_SUBJECTS = {"math", "physics", "chemistry", "biology", "chinese", "english"}
# 有效难度
VALID_DIFFICULTIES = {"easy", "medium", "hard"}
# 有效题型
VALID_QUESTION_TYPES = {"multiple_choice", "fill_blank", "true_false", "essay"}

# ---------------------------------------------------------------------------
# 内置题库（Sprint 1 兜底题库 — 各学科预置题目）
# 当 AI 不可用时从此库随机抽取
# ---------------------------------------------------------------------------
BUILTIN_QUESTIONS: dict[str, list[dict]] = {
    "math": [
        {
            "type": "multiple_choice",
            "content": "一次函数 y = 2x + 3 的图像与 y 轴的交点坐标是？",
            "options": [
                {"key": "A", "value": "(0, 3)"},
                {"key": "B", "value": "(3, 0)"},
                {"key": "C", "value": "(0, -3)"},
                {"key": "D", "value": "(-3, 0)"},
            ],
            "correct_answer": "A",
            "explanation": "当 x = 0 时，y = 3，因此与 y 轴的交点为 (0, 3)。",
            "difficulty": "easy",
            "points": 10,
        },
        {
            "type": "multiple_choice",
            "content": "二次函数 y = x² - 4x + 3 的顶点坐标是？",
            "options": [
                {"key": "A", "value": "(2, -1)"},
                {"key": "B", "value": "(-2, -1)"},
                {"key": "C", "value": "(2, 1)"},
                {"key": "D", "value": "(-2, 1)"},
            ],
            "correct_answer": "A",
            "explanation": "顶点横坐标 x = -b/(2a) = 4/2 = 2，代入得 y = 4 - 8 + 3 = -1。",
            "difficulty": "medium",
            "points": 10,
        },
        {
            "type": "multiple_choice",
            "content": "下列哪个是勾股数？",
            "options": [
                {"key": "A", "value": "3, 4, 5"},
                {"key": "B", "value": "2, 3, 4"},
                {"key": "C", "value": "5, 6, 7"},
                {"key": "D", "value": "1, 2, 3"},
            ],
            "correct_answer": "A",
            "explanation": "3² + 4² = 9 + 16 = 25 = 5²，所以 3, 4, 5 是勾股数。",
            "difficulty": "easy",
            "points": 10,
        },
        {
            "type": "fill_blank",
            "content": "圆的面积公式是 S = ______（用 π 和 r 表示）。",
            "options": [],
            "correct_answer": "πr²",
            "explanation": "圆的面积等于圆周率乘以半径的平方。",
            "difficulty": "easy",
            "points": 10,
        },
        {
            "type": "true_false",
            "content": "两条平行线可以相交。",
            "options": [
                {"key": "A", "value": "正确"},
                {"key": "B", "value": "错误"},
            ],
            "correct_answer": "B",
            "explanation": "平行线永不相交，这是欧几里得几何的基本公理。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
    "physics": [
        {
            "type": "multiple_choice",
            "content": "牛顿第一定律也称为？",
            "options": [
                {"key": "A", "value": "惯性定律"},
                {"key": "B", "value": "万有引力定律"},
                {"key": "C", "value": "作用力与反作用力定律"},
                {"key": "D", "value": "动能定律"},
            ],
            "correct_answer": "A",
            "explanation": "牛顿第一定律又称惯性定律：一切物体在没有受到力的作用时，总保持静止状态或匀速直线运动状态。",
            "difficulty": "easy",
            "points": 10,
        },
        {
            "type": "multiple_choice",
            "content": "光在真空中的传播速度约为？",
            "options": [
                {"key": "A", "value": "3×10⁸ m/s"},
                {"key": "B", "value": "3×10⁶ m/s"},
                {"key": "C", "value": "3×10⁴ m/s"},
                {"key": "D", "value": "3×10² m/s"},
            ],
            "correct_answer": "A",
            "explanation": "光在真空中的传播速度约为 3×10⁸ m/s，是宇宙中最快的速度。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
    "chemistry": [
        {
            "type": "multiple_choice",
            "content": "水的化学式是？",
            "options": [
                {"key": "A", "value": "H₂O"},
                {"key": "B", "value": "CO₂"},
                {"key": "C", "value": "NaCl"},
                {"key": "D", "value": "O₂"},
            ],
            "correct_answer": "A",
            "explanation": "水由两个氢原子和一个氧原子组成，化学式为 H₂O。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
    "biology": [
        {
            "type": "multiple_choice",
            "content": "人体最大的器官是？",
            "options": [
                {"key": "A", "value": "皮肤"},
                {"key": "B", "value": "心脏"},
                {"key": "C", "value": "肝脏"},
                {"key": "D", "value": "肺"},
            ],
            "correct_answer": "A",
            "explanation": "皮肤是人体最大的器官，成年人的皮肤面积约为 1.5-2 平方米。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
    "chinese": [
        {
            "type": "multiple_choice",
            "content": "下列哪个成语形容学习勤奋？",
            "options": [
                {"key": "A", "value": "悬梁刺股"},
                {"key": "B", "value": "守株待兔"},
                {"key": "C", "value": "画蛇添足"},
                {"key": "D", "value": "刻舟求剑"},
            ],
            "correct_answer": "A",
            "explanation": "悬梁刺股形容刻苦学习，孙敬悬梁、苏秦刺股。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
    "english": [
        {
            "type": "multiple_choice",
            "content": "What is the past tense of 'go'?",
            "options": [
                {"key": "A", "value": "went"},
                {"key": "B", "value": "gone"},
                {"key": "C", "value": "going"},
                {"key": "D", "value": "goed"},
            ],
            "correct_answer": "A",
            "explanation": "'go' 的过去式是 'went'，属于不规则动词变化。",
            "difficulty": "easy",
            "points": 10,
        },
    ],
}


def _pick_builtin_questions(
    subject: str,
    count: int,
    difficulty: str = "medium",
) -> list[dict]:
    """从内置题库中随机选题（Sprint 1 兜底方案）"""
    pool = BUILTIN_QUESTIONS.get(subject, BUILTIN_QUESTIONS["math"])
    # 按难度过滤
    filtered = [q for q in pool if q["difficulty"] == difficulty]
    if not filtered:
        filtered = pool
    # 随机选取
    selected = random.sample(filtered, min(count, len(filtered)))
    # 如果不够数，从全部题目中补足
    while len(selected) < count:
        extra = random.choice(pool)
        if extra not in selected:
            selected.append(extra)
    return selected


async def _check_quiz_rate_limit(user_id: str) -> bool:
    """检查 30 秒内是否重复出题"""
    key = f"quiz:rate_limit:{user_id}"
    exists = await cache_get(key)
    if exists:
        return False  # 频率超限
    await cache_set(key, "1", ttl=30)
    return True


async def _check_quiz_quota(
    user_id: str,
    db: AsyncSession,
) -> tuple[bool, int]:
    """
    检查免费用户每日出题配额（每日最多 3 次测验生成）
    返回 (within_limit, remaining)
    """
    from datetime import date

    today = date.today()
    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
            DailyQuota.feature == "quiz_generate",
        )
    )
    quota = result.scalar_one_or_none()

    if quota is None:
        # 首次使用，创建记录
        quota = DailyQuota(
            user_id=user_id,
            date=today,
            feature="quiz_generate",
            questions_used=0,
            questions_limit=3,
        )
        db.add(quota)
        await db.flush()

    if quota.questions_used >= quota.questions_limit:
        return False, 0

    return True, quota.questions_limit - quota.questions_used


# ============================================================
# POST /api/v1/quiz/generate
# ============================================================


@router.post("/generate")
async def generate_quiz(
    req: QuizGenerateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    生成练习题
    - 请求指定学科、知识点、难度和数量
    - 从内置题库随机出题（Sprint 1 兜底方案）
    - 免费用户每日限制 3 次
    """
    # 1. 参数校验
    if req.subject not in VALID_SUBJECTS:
        return api_error(code=400, message=f"不支持的学科: {req.subject}，可选: {', '.join(sorted(VALID_SUBJECTS))}")

    if req.difficulty not in VALID_DIFFICULTIES:
        return api_error(code=400, message=f"不支持的难度: {req.difficulty}，可选: easy/medium/hard")

    if req.count < 1 or req.count > 10:
        return api_error(code=400, message="题目数量范围为 1-10")

    # 2. 配额检查（免费用户每日 3 次）
    if current_user.grade is None or current_user.grade != "premium":
        within_limit, remaining = await _check_quiz_quota(current_user.id, db)
        if not within_limit:
            return api_error(
                code=403,
                message="今日出题配额已用完（免费用户每日 3 次）",
                data={"remaining": 0},
            )

    # 3. 频率限制（30 秒）
    if not await _check_quiz_rate_limit(current_user.id):
        return api_error(code=429, message="请求过于频繁，请 30 秒后再试")

    # 4. 出题
    quiz_id = str(uuid.uuid4())
    topic = req.topic or "通用"
    now_str = datetime.now(timezone.utc).isoformat()

    # 从内置题库选题
    questions_data = _pick_builtin_questions(
        subject=req.subject,
        count=req.count,
        difficulty=req.difficulty,
    )

    # 5. 持久化到数据库
    quiz_record = QuizRecord(
        id=quiz_id,
        user_id=current_user.id,
        subject=req.subject,
        topic=topic,
        difficulty=req.difficulty,
        total_questions=len(questions_data),
        status="pending",
    )
    db.add(quiz_record)

    quiz_questions_out = []
    for idx, q_data in enumerate(questions_data, start=1):
        options_json = json.dumps(q_data.get("options", []), ensure_ascii=False)
        q = QuizQuestion(
            id=str(uuid.uuid4()),
            quiz_id=quiz_id,
            question_index=idx,
            question_type=q_data["type"],
            content=q_data["content"],
            options=options_json,
            correct_answer=q_data["correct_answer"],
            explanation=q_data.get("explanation", ""),
            difficulty=q_data.get("difficulty", req.difficulty),
            points=q_data.get("points", 10),
        )
        db.add(q)
        quiz_questions_out.append(
            QuizQuestionOut(
                id=idx,
                type=q_data["type"],
                content=q_data["content"],
                options=q_data.get("options", []),
                correct_answer=q_data["correct_answer"],
                explanation=q_data.get("explanation", ""),
                difficulty=q_data.get("difficulty", req.difficulty),
                points=q_data.get("points", 10),
            )
        )

    await db.flush()

    # 6. 扣减配额
    if current_user.grade is None or current_user.grade != "premium":
        await _deduct_quiz_quota(current_user.id, db)

    return success(
        data=QuizGenerateData(
            quiz_id=quiz_id,
            questions=quiz_questions_out,
            total_questions=len(questions_data),
            subject=req.subject,
            topic=topic,
            generated_at=now_str,
        ).model_dump(),
        message="出题成功",
    )


async def _deduct_quiz_quota(user_id: str, db: AsyncSession):
    """扣减 quiz_generate 配额"""
    from datetime import date

    today = date.today()
    result = await db.execute(
        select(DailyQuota).where(
            DailyQuota.user_id == user_id,
            DailyQuota.date == today,
            DailyQuota.feature == "quiz_generate",
        )
    )
    quota = result.scalar_one_or_none()
    if quota:
        quota.questions_used += 1
        await db.flush()


# ============================================================
# POST /api/v1/quiz/submit
# ============================================================


@router.post("/submit")
async def submit_quiz(
    req: QuizSubmitRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    提交作答并批改
    - 判题评分
    - 记录答错题目到 wrong_answers 表
    - 计算 mastery_score
    """
    # 1. 校验 quiz_id
    result = await db.execute(
        select(QuizRecord).where(
            QuizRecord.id == req.quiz_id,
            QuizRecord.user_id == current_user.id,
        )
    )
    quiz_record = result.scalar_one_or_none()
    if quiz_record is None:
        return api_error(code=404, message="测验不存在或不属于当前用户")

    if quiz_record.status == "completed":
        return api_error(code=400, message="该测验已提交过，不可重复提交")

    # 2. 获取题目
    questions_result = await db.execute(
        select(QuizQuestion)
        .where(QuizQuestion.quiz_id == req.quiz_id)
        .order_by(QuizQuestion.question_index)
    )
    db_questions = questions_result.scalars().all()

    if len(db_questions) != len(req.answers):
        return api_error(
            code=400,
            message=f"作答数量不匹配：题目 {len(db_questions)} 道，提交 {len(req.answers)} 道",
        )

    # 3. 逐题判分
    answer_map: dict[int, AnswerItem] = {a.question_id: a for a in req.answers}
    results: list[QuestionResult] = []
    correct_count = 0
    total_points = 0
    earned_points = 0
    wrong_answer_ids: list[str] = []

    for q in db_questions:
        total_points += q.points
        ans = answer_map.get(q.question_index)
        if ans is None:
            continue

        user_answer = ans.selected_answer.strip()
        correct_answer = q.correct_answer.strip()

        # 判题逻辑
        if q.question_type == "fill_blank":
            # 填空题：忽略大小写和空格
            is_correct = user_answer.lower().replace(" ", "") == correct_answer.lower().replace(" ", "")
        else:
            is_correct = user_answer == correct_answer

        if is_correct:
            correct_count += 1
            earned_points += q.points
        else:
            earned_points += 0

        # 计算 mastery_score
        if is_correct:
            # 用时越短掌握度越高：0.8 ~ 1.0
            time_factor = max(0, 1.0 - ans.time_spent_seconds / 120.0)
            mastery_score = round(0.8 + 0.2 * time_factor, 2)
        else:
            # 错误：0.0 ~ 0.5
            mastery_score = round(random.uniform(0.0, 0.5), 2)

        # 解析 options（JSON 字符串 → dict）
        options_list = []
        if q.options:
            try:
                options_list = json.loads(q.options)
            except (json.JSONDecodeError, TypeError):
                options_list = []

        weakness = None
        if not is_correct and mastery_score < 0.6:
            weakness = f"{q.question_type}理解不足"

        result_item = QuestionResult(
            question_id=q.question_index,
            is_correct=is_correct,
            correct_answer=q.correct_answer,
            user_answer=ans.selected_answer,
            explanation=q.explanation or "暂无解析",
            mastery_score=mastery_score,
            weakness=weakness,
        )
        results.append(result_item)

        # 4. 记录错题
        if not is_correct:
            wrong_id = str(uuid.uuid4())
            wrong_answer_ids.append(wrong_id)
            wrong = WrongAnswer(
                id=wrong_id,
                user_id=current_user.id,
                quiz_id=req.quiz_id,
                subject=quiz_record.subject,
                topic=quiz_record.topic,
                question_content=q.content,
                correct_answer=q.correct_answer,
                user_answer=ans.selected_answer,
                explanation=q.explanation,
                mastery_score=mastery_score,
                weakness=weakness,
            )
            db.add(wrong)

    # 5. 计算得分
    score = round((earned_points / total_points) * 100) if total_points > 0 else 0

    # 6. 更新测验记录
    quiz_record.score = score
    quiz_record.total_points = total_points
    quiz_record.status = "completed"
    await db.flush()

    # 7. 生成汇总
    accuracy = correct_count / len(db_questions) if db_questions else 0
    if accuracy >= 0.9:
        mastery_level = "优秀"
        suggestions_tpl = ["继续保持！"]
    elif accuracy >= 0.7:
        mastery_level = "良好"
        suggestions_tpl = ["建议复习错题，巩固薄弱知识点"]
    elif accuracy >= 0.5:
        mastery_level = "一般"
        suggestions_tpl = ["需要加强练习，重点关注错题"]
    else:
        mastery_level = "待加强"
        suggestions_tpl = ["建议重新学习相关知识点，多做练习"]

    summary = QuizSummary(
        total=len(db_questions),
        correct=correct_count,
        accuracy=round(accuracy, 2),
        mastery_level=mastery_level,
        suggestions=suggestions_tpl,
    )

    return success(
        data=QuizSubmitData(
            quiz_id=req.quiz_id,
            score=score,
            total_points=total_points,
            earned_points=earned_points,
            results=results,
            summary=summary,
            wrong_answer_ids=wrong_answer_ids,
        ).model_dump(),
        message="批改完成",
    )
