"""
AI 学伴后端 - 测验相关 Pydantic Schema
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class QuizGenerateRequest(BaseModel):
    """生成测验请求"""
    subject: str = Field(..., description="学科: math/physics/chemistry/biology/chinese/english")
    topic: Optional[str] = Field(default="通用", description="知识点")
    difficulty: str = Field(default="medium", description="难度: easy/medium/hard")
    count: int = Field(default=5, ge=1, le=10, description="题目数量 1-10")
    question_types: list[str] = Field(default=["multiple_choice"], description="题型列表")
    grade: Optional[str] = Field(default=None, description="年级, 默认从用户 profile 读取")

    model_config = {"json_schema_extra": {
        "example": {
            "subject": "math",
            "topic": "二次函数",
            "difficulty": "medium",
            "count": 5,
            "question_types": ["multiple_choice"],
            "grade": "初三",
        }
    }}


class QuizQuestionOut(BaseModel):
    """输出用题目模型"""
    id: int
    type: str
    content: str
    options: list[dict] = Field(default_factory=list)
    correct_answer: str
    explanation: str
    difficulty: str
    points: int


class QuizGenerateData(BaseModel):
    """生成测验响应数据"""
    quiz_id: str
    questions: list[QuizQuestionOut]
    total_questions: int
    subject: str
    topic: str
    generated_at: str


class AnswerItem(BaseModel):
    """单题作答"""
    question_id: int
    selected_answer: str
    time_spent_seconds: int = Field(default=0, ge=0)


class QuizSubmitRequest(BaseModel):
    """提交测验请求"""
    quiz_id: str
    answers: list[AnswerItem]

    model_config = {"json_schema_extra": {
        "example": {
            "quiz_id": "uuid-here",
            "answers": [
                {"question_id": 1, "selected_answer": "A", "time_spent_seconds": 30},
                {"question_id": 2, "selected_answer": "C", "time_spent_seconds": 45},
            ],
        }
    }}


class QuestionResult(BaseModel):
    """单题批改结果"""
    question_id: int
    is_correct: bool
    correct_answer: str
    user_answer: str
    explanation: str
    mastery_score: float
    weakness: str | None = None


class QuizSummary(BaseModel):
    """测验汇总"""
    total: int
    correct: int
    accuracy: float
    mastery_level: str
    suggestions: list[str]


class QuizSubmitData(BaseModel):
    """提交测验响应数据"""
    quiz_id: str
    score: int
    total_points: int
    earned_points: int
    results: list[QuestionResult]
    summary: QuizSummary
    wrong_answer_ids: list[str]
