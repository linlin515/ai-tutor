"""
AI 学伴后端 - 模型列表接口单元测试
"""

from __future__ import annotations

import pytest


class TestModels:
    """模型列表接口测试"""

    @pytest.mark.asyncio
    async def test_list_models_returns_200(self, async_client):
        """测试获取模型列表返回 200"""
        response = await async_client.get("/api/v1/models")
        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_list_models_has_models_key(self, async_client):
        """测试返回数据包含 models 数组"""
        response = await async_client.get("/api/v1/models")
        data = response.json()
        assert data["code"] == 0
        assert "models" in data["data"]
        assert isinstance(data["data"]["models"], list)

    @pytest.mark.asyncio
    async def test_list_models_contains_expected_fields(self, async_client):
        """测试每个模型包含 id/name/provider 字段"""
        response = await async_client.get("/api/v1/models")
        data = response.json()
        models = data["data"]["models"]
        assert len(models) > 0
        for model in models:
            assert "id" in model
            assert "name" in model
            assert "provider" in model

    @pytest.mark.asyncio
    async def test_list_models_does_not_require_auth(self, async_client):
        """测试模型列表不需要认证"""
        response = await async_client.get("/api/v1/models")
        assert response.status_code == 200
