"""
AI 学伴后端 - 健康检查接口单元测试
"""

from __future__ import annotations

import pytest


class TestHealthCheck:
    """健康检查接口测试"""

    @pytest.mark.asyncio
    async def test_health_check_returns_200(self, async_client):
        """测试 /api/v1/health 返回 200"""
        response = await async_client.get("/api/v1/health")
        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_health_check_status_ok(self, async_client):
        """测试健康检查返回 status 为 ok"""
        response = await async_client.get("/api/v1/health")
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["status"] == "ok"
        assert data["data"]["service"] == "AI学伴后端"
        assert data["data"]["version"] == "1.0.0"
