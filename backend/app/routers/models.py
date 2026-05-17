"""
AI 学伴后端 - 可用模型列表路由
提供 AI 学伴支持的模型元数据
"""

from __future__ import annotations

from fastapi import APIRouter

from app.schemas.common import success

router = APIRouter(prefix="/api/v1", tags=["模型"])

# 可用模型列表（静态配置，后续可从数据库或配置中心动态加载）
AVAILABLE_MODELS = [
    {"id": "gpt-4o", "name": "GPT-4o", "provider": "OpenAI"},
    {"id": "gpt-4o-mini", "name": "GPT-4o Mini", "provider": "OpenAI"},
    {"id": "claude-3.5-sonnet", "name": "Claude 3.5 Sonnet", "provider": "Anthropic"},
    {"id": "deepseek-chat", "name": "DeepSeek Chat", "provider": "DeepSeek"},
    {"id": "gemini-2.0-flash", "name": "Gemini 2.0 Flash", "provider": "Google"},
    {"id": "qwen2.5-72b", "name": "Qwen 2.5 (72B)", "provider": "Alibaba Cloud"},
    {"id": "glm-4", "name": "GLM-4", "provider": "Zhipu AI"},
]


@router.get("/models")
async def list_models():
    """
    获取可用模型列表
    返回平台支持的所有 AI 模型，包含 id、名称和提供商信息
    """
    return success(data={"models": AVAILABLE_MODELS})
