"""
AI 学伴后端 - 学习报告导出 Pydantic Schema
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field
from enum import Enum


class ReportFormat(str, Enum):
    PDF = "pdf"
    CSV = "csv"


class Period(str, Enum):
    TODAY = "today"
    WEEK = "week"
    MONTH = "month"
    ALL = "all"


class ExportRequest(BaseModel):
    """导出请求"""
    format: ReportFormat = Field(
        default=ReportFormat.PDF,
        description="导出格式: pdf / csv",
    )
    period: Period = Field(
        default=Period.WEEK,
        description="统计周期: today / week / month / all",
    )
    subject: Optional[str] = Field(
        default=None,
        description="学科过滤（可选，None 表示全部）",
    )
