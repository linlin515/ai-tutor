"""
AI 学伴后端 - 用户信息接口单元测试

覆盖获取用户资料、更新用户资料、未认证访问等场景。
"""

from __future__ import annotations

import pytest


class TestUserProfile:
    """用户资料接口测试"""

    @pytest.mark.asyncio
    async def test_get_profile_requires_auth(self, async_client):
        """测试获取用户资料需要认证"""
        response = await async_client.get("/api/v1/user/profile")
        assert response.status_code == 401
        data = response.json()
        assert "请先登录" in data["detail"]

    @pytest.mark.asyncio
    async def test_get_profile_success(self, async_client, auth_headers):
        """测试获取用户资料成功"""
        response = await async_client.get(
            "/api/v1/user/profile", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["phone"] == "13800138000"
        assert data["data"]["nickname"] == "学伴_8000"
        assert data["data"]["id"] is not None

    @pytest.mark.asyncio
    async def test_get_profile_fields(self, async_client, auth_headers):
        """测试返回的用户资料包含所有期望字段"""
        response = await async_client.get(
            "/api/v1/user/profile", headers=auth_headers
        )
        data = response.json()["data"]
        expected_fields = {
            "id", "phone", "nickname",
            "avatar_url", "grade", "created_at",
        }
        assert set(data.keys()) == expected_fields


class TestUserProfileUpdate:
    """用户资料更新接口测试"""

    @pytest.mark.asyncio
    async def test_update_nickname(self, async_client, auth_headers):
        """测试更新用户昵称"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={"nickname": "小明"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["nickname"] == "小明"
        assert data["message"] == "更新成功"

    @pytest.mark.asyncio
    async def test_update_grade(self, async_client, auth_headers):
        """测试更新用户年级"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={"grade": "初一"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["data"]["grade"] == "初一"

    @pytest.mark.asyncio
    async def test_update_avatar_url(self, async_client, auth_headers):
        """测试更新用户头像 URL"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={"avatar_url": "https://example.com/avatar.png"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["data"]["avatar_url"] == "https://example.com/avatar.png"

    @pytest.mark.asyncio
    async def test_update_multiple_fields(self, async_client, auth_headers):
        """测试同时更新多个字段"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={
                "nickname": "小红",
                "grade": "三年级",
                "avatar_url": "https://example.com/new_avatar.png",
            },
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["nickname"] == "小红"
        assert data["grade"] == "三年级"
        assert data["avatar_url"] == "https://example.com/new_avatar.png"

    @pytest.mark.asyncio
    async def test_update_requires_auth(self, async_client):
        """测试更新用户资料需要认证"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={"nickname": "小明"},
        )
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_update_empty_body_returns_400(
        self, async_client, auth_headers
    ):
        """测试空更新请求返回 400"""
        response = await async_client.patch(
            "/api/v1/user/profile",
            json={},
            headers=auth_headers,
        )
        assert response.status_code == 400
        data = response.json()
        assert "没有需要更新的字段" in data["detail"]
