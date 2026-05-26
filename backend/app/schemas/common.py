"""
AI 学伴后端 - 统一 API 响应格式
"""

from __future__ import annotations

from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")
ItemT = TypeVar("ItemT")


class ApiResponse(BaseModel, Generic[T]):
    """统一 API 响应格式"""
    code: int = 0
    message: str = "success"
    data: T | None = None


class PaginatedData(BaseModel, Generic[ItemT]):
    """通用分页数据结构"""
    items: list[ItemT] = Field(default_factory=list, description="当前页数据列表")
    total: int = Field(default=0, description="总记录数")
    page: int = Field(default=1, description="当前页码")
    page_size: int = Field(default=20, description="每页记录数")
    total_pages: int = Field(default=0, description="总页数")


def success(data: Any = None, message: str = "success") -> ApiResponse:
    """成功响应"""
    return ApiResponse(code=0, message=message, data=data)


def error(code: int = -1, message: str = "error", data: Any = None) -> ApiResponse:
    """错误响应"""
    return ApiResponse(code=code, message=message, data=data)
