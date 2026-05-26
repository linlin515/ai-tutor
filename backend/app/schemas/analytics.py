"""
AI 学伴后端 - 学习统计相关 Pydantic Schema
"""
from __future__ import annotations

from pydantic import BaseModel, Field


class DailyStat(BaseModel):
    """每日统计"""
    date: str
    study_minutes: int
    questions_solved: int
    accuracy: float


class SubjectBreakdown(BaseModel):
    """学科细分"""
    subject: str
    questions_solved: int
    accuracy: float
    study_minutes: int


class WeakArea(BaseModel):
    """薄弱环节"""
    topic: str
    mastery: float
    subject: str


class Overview(BaseModel):
    """概览数据"""
    study_minutes_today: int
    questions_solved_today: int
    accuracy_today: float
    streak_days: int
    total_knowledge_points: int
    mastered_points: int
    mastery_rate: float


class AnalyticsStatsData(BaseModel):
    """学习统计数据"""
    overview: Overview
    daily_stats: list[DailyStat]
    subject_breakdown: list[SubjectBreakdown]
    weak_areas: list[WeakArea]


class SyncScoreData(BaseModel):
    """同步积分请求中的积分数据"""
    total_points: int
    daily_points: int
    streak_days: int = 0
    achievements_unlocked: list[str] = Field(default_factory=list)


class SyncScoreRequest(BaseModel):
    """同步积分请求"""
    score_data: SyncScoreData
    sync_timestamp: str  # ISO 8601


class SyncScoreResponse(BaseModel):
    """同步积分响应"""
    synced: bool
    server_total_points: int
    rank: int
    total_users: int
    new_achievements: list[str] = Field(default_factory=list)
    conflict: bool = False
