"""
AI 学伴后端 - 认证接口单元测试

覆盖注册、登录、JWT Token、重复注册等场景。
每个测试使用独立的内存数据库。
"""

from __future__ import annotations

from jose import jwt
import pytest

from app.config import get_settings

settings = get_settings()


class TestAuthRegister:
    """注册接口测试"""

    @pytest.mark.asyncio
    async def test_register_success(self, async_client):
        """测试注册成功"""
        response = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13900139000", "password": "test123", "email": "test@test.com"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["message"] == "注册成功"
        assert data["data"]["access_token"] is not None
        assert data["data"]["user_id"] is not None
        assert data["data"]["nickname"] == "学伴_9000"

    @pytest.mark.asyncio
    async def test_register_duplicate_returns_409(self, async_client):
        """测试重复注册返回 409"""
        # 第一次注册
        await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13900139000", "password": "test123", "email": "test@test.com"},
        )
        # 第二次用同一手机号注册
        response = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13900139000", "password": "654321", "email": "test2@test.com"},
        )
        assert response.status_code == 409
        data = response.json()
        assert "已注册" in data["detail"]

    @pytest.mark.asyncio
    async def test_register_different_phone_allowed(self, async_client):
        """测试不同手机号可以分别注册"""
        resp1 = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138001", "password": "111111", "email": "a@a.com"},
        )
        resp2 = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138002", "password": "222222", "email": "b@b.com"},
        )
        assert resp1.status_code == 200
        assert resp2.status_code == 200
        data1 = resp1.json()
        data2 = resp2.json()
        assert data1["data"]["user_id"] != data2["data"]["user_id"]


class TestAuthLogin:
    """登录接口测试"""

    @pytest.mark.asyncio
    async def test_login_success(self, async_client):
        """测试登录成功"""
        # 先注册
        await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        # 再登录
        response = await async_client.post(
            "/api/v1/auth/login",
            json={"phone": "13800138000", "password": "test123"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["message"] == "登录成功"
        assert data["data"]["access_token"] is not None

    @pytest.mark.asyncio
    async def test_login_fail_phone_not_registered(self, async_client):
        """测试手机号未注册时登录返回 404"""
        response = await async_client.post(
            "/api/v1/auth/login",
            json={"phone": "13900000000", "password": "test123"},
        )
        assert response.status_code == 404
        data = response.json()
        assert "未注册" in data["detail"]

    @pytest.mark.asyncio
    async def test_login_wrong_password(self, async_client):
        """测试错误密码登录返回 401"""
        # 先注册
        await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        # 用错误密码登录
        response = await async_client.post(
            "/api/v1/auth/login",
            json={"phone": "13800138000", "password": "wrong_password"},
        )
        assert response.status_code == 401
        data = response.json()
        assert "密码错误" in data["detail"]


class TestJWTToken:
    """JWT Token 验证测试"""

    @pytest.mark.asyncio
    async def test_register_returns_valid_jwt(self, async_client):
        """测试注册后返回的 JWT Token 可解码且包含正确信息"""
        response = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        assert response.status_code == 200
        data = response.json()
        token = data["data"]["access_token"]
        user_id = data["data"]["user_id"]

        # 解码验证
        payload = jwt.decode(
            token,
            settings.secret_key,
            algorithms=[settings.jwt_algorithm],
        )
        assert payload["sub"] == user_id
        assert "exp" in payload
        assert "iat" in payload

    @pytest.mark.asyncio
    async def test_login_returns_valid_jwt(self, async_client):
        """测试登录后返回的 JWT Token 可解码且包含正确信息"""
        # 注册
        await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        # 登录
        response = await async_client.post(
            "/api/v1/auth/login",
            json={"phone": "13800138000", "password": "test123"},
        )
        assert response.status_code == 200
        data = response.json()
        token = data["data"]["access_token"]
        user_id = data["data"]["user_id"]

        payload = jwt.decode(
            token,
            settings.secret_key,
            algorithms=[settings.jwt_algorithm],
        )
        assert payload["sub"] == user_id

    @pytest.mark.asyncio
    async def test_jwt_registration_and_login_same_user(self, async_client):
        """测试注册和登录返回的 JWT 指向同一用户"""
        # 注册
        reg_resp = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        reg_data = reg_resp.json()
        reg_user_id = reg_data["data"]["user_id"]

        # 登录
        login_resp = await async_client.post(
            "/api/v1/auth/login",
            json={"phone": "13800138000", "password": "test123"},
        )
        login_data = login_resp.json()
        login_user_id = login_data["data"]["user_id"]

        assert reg_user_id == login_user_id

    @pytest.mark.asyncio
    async def test_refresh_token(self, async_client):
        """测试 Token 刷新"""
        # 注册获取 token
        resp = await async_client.post(
            "/api/v1/auth/register",
            json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
        )
        token = resp.json()["data"]["access_token"]

        # 刷新 token
        refresh_resp = await async_client.post(
            "/api/v1/auth/refresh",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert refresh_resp.status_code == 200
        refresh_data = refresh_resp.json()
        assert refresh_data["code"] == 0
        assert refresh_data["data"]["token"] is not None

    @pytest.mark.asyncio
    async def test_refresh_token_invalid(self, async_client):
        """测试无效 Token 刷新返回 401"""
        response = await async_client.post(
            "/api/v1/auth/refresh",
            headers={"Authorization": "Bearer invalid_token_here"},
        )
        assert response.status_code == 401
