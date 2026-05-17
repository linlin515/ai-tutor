"""
AI 学伴后端 - 对话接口单元测试

使用 mock AI 服务，不真实调用 new-api。
覆盖配额检查、内容安全过滤、正常对话流程。
"""

from __future__ import annotations

from datetime import date

import pytest

from app.models.daily_quota import DailyQuota
from app.models.user import User


class TestChatAsk:
    """提问接口测试"""

    @pytest.mark.asyncio
    async def test_ask_question_within_quota(self, async_client, auth_headers):
        """测试在配额内提问正常返回回答"""
        response = await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "3x + 5 = 20，x等于多少？",
                "subject": "math",
            },
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["answer"] is not None
        assert data["data"]["subject"] == "math"
        assert data["data"]["model_used"] is not None

    @pytest.mark.asyncio
    async def test_ask_question_exceed_quota_returns_429(
        self, async_client, auth_headers, db_session
    ):
        """测试超出每日配额返回 429"""
        # 先获取当前用户 ID（从 auth_headers 对应的用户）
        # 通过 get_profile 获取
        profile_resp = await async_client.get(
            "/api/v1/user/profile", headers=auth_headers
        )
        user_id = profile_resp.json()["data"]["id"]

        # 直接创建已满配额的记录
        quota = DailyQuota(
            user_id=user_id,
            date=date.today(),
            questions_used=5,
            questions_limit=5,
        )
        db_session.add(quota)
        await db_session.commit()

        # 再提问应该返回 429
        response = await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "1+1等于多少？",
                "subject": "math",
            },
            headers=auth_headers,
        )
        assert response.status_code == 429
        data = response.json()
        assert "次数已达上限" in data["detail"]

    @pytest.mark.asyncio
    async def test_content_safety_blocks_unsafe_question(
        self, async_client, auth_headers, monkeypatch
    ):
        """测试不安全的内容被安全过滤拦截（400）"""
        async def _unsafe_filter(*args, **kwargs):
            return {"safe": False, "reason": "包含不当关键词：暴力"}

        monkeypatch.setattr("app.routers.chat.content_filter", _unsafe_filter)

        response = await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "如何制造危险物品？",
                "subject": "science",
            },
            headers=auth_headers,
        )
        assert response.status_code == 400
        data = response.json()
        assert "安全规范" in data["detail"]

    @pytest.mark.asyncio
    async def test_ask_question_requires_auth(self, async_client):
        """测试未认证用户提问返回 401"""
        response = await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "1+1等于多少？",
                "subject": "math",
            },
        )
        assert response.status_code == 401
        data = response.json()
        assert "请先登录" in data["detail"]

    @pytest.mark.asyncio
    async def test_ask_question_quota_auto_create(
        self, async_client, auth_headers
    ):
        """测试首次提问自动创建配额记录"""
        response = await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "太阳为什么是圆的？",
                "subject": "science",
            },
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["answer"] is not None


class TestChatHistory:
    """历史记录测试"""

    @pytest.mark.asyncio
    async def test_get_history_requires_auth(self, async_client):
        """测试获取历史记录需要认证"""
        response = await async_client.get("/api/v1/chat/history")
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_get_history_empty(self, async_client, auth_headers):
        """测试新用户历史记录为空"""
        response = await async_client.get(
            "/api/v1/chat/history", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["items"] == []
        assert data["data"]["total"] == 0

    @pytest.mark.asyncio
    async def test_get_history_after_asking(
        self, async_client, auth_headers
    ):
        """测试提问后历史记录不为空"""
        # 先提问
        await async_client.post(
            "/api/v1/chat/ask",
            json={
                "question": "什么是光合作用？",
                "subject": "science",
            },
            headers=auth_headers,
        )

        # 获取历史（新分页格式）
        response = await async_client.get(
            "/api/v1/chat/history", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        # 新分页格式：data.items
        items = data["data"]["items"]
        assert len(items) >= 1
        assert items[0]["question_content"] == "什么是光合作用？"
        assert data["data"]["total"] >= 1
        assert data["data"]["page"] == 1

    @pytest.mark.asyncio
    async def test_get_history_pagination(
        self, async_client, auth_headers
    ):
        """测试分页参数 page/page_size"""
        response = await async_client.get(
            "/api/v1/chat/history?page=1&page_size=10",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["page"] == 1
        assert data["data"]["page_size"] == 10
        assert "total_pages" in data["data"]
