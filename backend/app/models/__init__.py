"""
AI 学伴后端 - 模型包
导出所有 SQLAlchemy 模型以便 Alembic 和数据库初始化
"""

from app.models.user import User, UserAuth
from app.models.subscription import Subscription
from app.models.question_record import QuestionRecord
from app.models.daily_quota import DailyQuota
from app.models.user_score import UserScore
from app.models.quiz_record import QuizRecord
from app.models.quiz_question import QuizQuestion
from app.models.wrong_answer import WrongAnswer
from app.models.achievement_record import AchievementRecord

__all__ = [
    "User",
    "UserAuth",
    "Subscription",
    "QuestionRecord",
    "DailyQuota",
    "UserScore",
    "QuizRecord",
    "QuizQuestion",
    "WrongAnswer",
    "AchievementRecord",
]
