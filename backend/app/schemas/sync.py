"""
AI 学伴后端 - 离线同步 Pydantic Schema
"""
from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


class SyncAction(BaseModel):
    """同步操作"""
    type: str = Field(..., description="操作类型: create/update/delete")
    target_id: str = Field(..., description="目标记录 ID")
    data: dict[str, Any] = Field(default_factory=dict, description="操作数据")
    timestamp: str = Field(..., description="操作时间 (ISO 8601)")


class SyncRequest(BaseModel):
    """同步请求体"""
    actions: list[SyncAction] = Field(default_factory=list, description="待同步的操作列表")


class ConflictItem(BaseModel):
    """冲突项"""
    target_id: str = Field(..., description="冲突记录 ID")
    type: str = Field(..., description="冲突类型")
    server_value: Any = Field(default=None, description="服务端值")
    client_value: Any = Field(default=None, description="客户端值")
    message: str = Field(default="", description="冲突描述")


class SyncResponse(BaseModel):
    """同步响应"""
    synced: bool = Field(default=True, description="是否同步成功")
    conflicts: list[ConflictItem] = Field(default_factory=list, description="冲突列表")
    sync_timestamp: str = Field(..., description="服务端同步时间戳 (ISO 8601)")


class DeltaResponse(BaseModel):
    """增量数据响应"""
    updated_questions: list[dict] = Field(default_factory=list, description="更新的提问记录")
    updated_conversations: list[dict] = Field(default_factory=list, description="更新的对话记录")
    updated_wrong_answers: list[dict] = Field(default_factory=list, description="更新的错题记录")
    deleted_ids: list[str] = Field(default_factory=list, description="已删除的记录 ID 列表")
    sync_timestamp: str = Field(..., description="服务端时间戳 (ISO 8601)")
