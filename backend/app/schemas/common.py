"""
AI 学伴后端 - 统一 API 响应格式
"""

from __future__ import annotations

from typing import Any, Generic, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """统一 API 响应格式"""
    code: int = 0
    message: str = "success"
    data: T | None = None


def success(data: Any = None, message: str = "success") -> ApiResponse:
    """成功响应"""
    return ApiResponse(code=0, message=message, data=data)


def error(code: int = -1, message: str = "error", data: Any = None) -> ApiResponse:
    """错误响应"""
    return ApiResponse(code=code, message=message, data=data)
