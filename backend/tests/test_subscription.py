"""
AI 学伴后端 - 订阅接口单元测试
"""

from __future__ import annotations

import pytest


class TestSubscriptionStatus:
    """订阅状态查询测试"""

    @pytest.mark.asyncio
    async def test_get_status_requires_auth(self, async_client):
        """测试获取订阅状态需要认证"""
        response = await async_client.get("/api/v1/subscription/status")
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_get_status_free_default(self, async_client, auth_headers):
        """测试新用户默认为免费版"""
        response = await async_client.get(
            "/api/v1/subscription/status", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["plan_type"] == "free"
        assert data["data"]["is_active"] is True
        assert "features" in data["data"]
        assert "daily_quota" in data["data"]

    @pytest.mark.asyncio
    async def test_subscription_has_features(self, async_client, auth_headers):
        """测试订阅返回包含功能列表"""
        response = await async_client.get(
            "/api/v1/subscription/status", headers=auth_headers
        )
        data = response.json()
        features = data["data"]["features"]
        assert isinstance(features, list)
        assert len(features) > 0


class TestSubscriptionVerify:
    """订阅验证测试"""

    @pytest.mark.asyncio
    async def test_verify_requires_auth(self, async_client):
        """测试验证订阅需要认证"""
        response = await async_client.post(
            "/api/v1/subscription/verify",
            json={"purchase_token": "test_token", "product_id": "premium_monthly"},
        )
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_verify_success(self, async_client, auth_headers):
        """测试订阅验证成功"""
        response = await async_client.post(
            "/api/v1/subscription/verify",
            json={"purchase_token": "test_token", "product_id": "premium_monthly"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["plan_type"] == "premium"
        assert data["data"]["is_active"] is True
        assert data["data"]["days_remaining"] >= 0

    @pytest.mark.asyncio
    async def test_verify_updates_subscription(self, async_client, auth_headers):
        """测试验证后订阅状态更新"""
        # 验证
        await async_client.post(
            "/api/v1/subscription/verify",
            json={"purchase_token": "test_token", "product_id": "premium_monthly"},
            headers=auth_headers,
        )

        # 查询状态
        response = await async_client.get(
            "/api/v1/subscription/status", headers=auth_headers
        )
        data = response.json()
        assert data["data"]["plan_type"] == "premium"
        features = " ".join(data["data"]["features"])
        assert "无限提问" in features or "语音交互" in features
