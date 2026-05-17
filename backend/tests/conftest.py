"""
AI 学伴后端 - 测试配置与通用 fixtures

提供测试所需的数据库引擎、HTTP 客户端、认证 header 等 fixtures。
"""

from __future__ import annotations

from typing import AsyncGenerator

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.database import Base, get_db
from app.main import app

# ============================================================
# 测试数据库 — 使用共享缓存的 in-memory SQLite
# ============================================================
TEST_DATABASE_URL = "sqlite+aiosqlite://"


@pytest_asyncio.fixture
async def db_engine():
    """创建测试用内存数据库引擎，并初始化所有表"""
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    await engine.dispose()


@pytest_asyncio.fixture
async def db_session(db_engine) -> AsyncGenerator[AsyncSession, None]:
    """独立的数据库会话，用于测试中直接操作数据库"""
    session = async_sessionmaker(
        db_engine, expire_on_commit=False
    )()
    try:
        yield session
    finally:
        await session.close()


@pytest_asyncio.fixture
async def async_client(db_engine) -> AsyncGenerator[AsyncClient, None]:
    """
    FastAPI 异步测试客户端
    """
    test_session_factory = async_sessionmaker(
        db_engine, expire_on_commit=False
    )

    async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
        """覆盖 app 中的 get_db 依赖，使用测试用数据库会话"""
        async with test_session_factory() as session:
            try:
                yield session
                await session.commit()
            except Exception:
                await session.rollback()
                raise
            finally:
                await session.close()

    app.dependency_overrides[get_db] = override_get_db

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as client:
        yield client

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def auth_headers(async_client) -> dict[str, str]:
    """
    注册一个测试用户并返回 Authorization header
    """
    response = await async_client.post(
        "/api/v1/auth/register",
        json={"phone": "13800138000", "password": "test123", "email": "test@test.com"},
    )
    assert response.status_code == 200, f"注册失败: {response.text}"
    data = response.json()
    token = data["data"]["access_token"]
    return {"Authorization": f"Bearer {token}"}


# ============================================================
# 自动 Mock 外部 AI 服务
# ============================================================
@pytest.fixture(autouse=True)
def mock_external_services(monkeypatch):
    """
    自动 Mock 所有外部 AI 服务调用
    """

    async def _mock_chat(*args, **kwargs):
        """模拟 AI 聊天返回固定回答"""
        return {
            "content": (
                "让我们一起来思考这个问题。首先，我们需要理解题目在问什么。🤔\n\n"
                "我们可以把这个问题分解成几个小步骤来解决。"
            ),
            "model": "gpt-4o-mini",
            "usage": {"total_tokens": 50},
            "total_tokens": 50,
        }

    async def _mock_content_filter(*args, **kwargs):
        """模拟内容安全过滤，默认返回安全"""
        return {"safe": True, "reason": ""}

    # Mock router 中的引用
    monkeypatch.setattr("app.routers.chat.ai_chat", _mock_chat)
    monkeypatch.setattr("app.routers.chat.content_filter", _mock_content_filter)
    monkeypatch.setattr("app.routers.chat_completions.content_filter", _mock_content_filter)
    monkeypatch.setattr("app.routers.steps.ai_chat", _mock_chat)

    # Mock middleware 中的引用
    monkeypatch.setattr(
        "app.middleware.content_safety.ai_content_filter", _mock_content_filter
    )
    monkeypatch.setattr(
        "app.middleware.content_safety._keyword_filter",
        lambda text: {"safe": True, "reason": ""},
    )
