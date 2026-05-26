"""
AI 学伴后端 - 排行榜接口单元测试

覆盖分页查询、参数验证、边界条件等场景。
"""

from __future__ import annotations

import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.models.user_score import UserScore


class TestLeaderboard:
    """排行榜接口测试"""

    @pytest.mark.asyncio
    async def test_leaderboard_requires_no_auth(self, async_client):
        """测试排行榜接口无需认证"""
        response = await async_client.get("/api/v1/game/leaderboard")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0

    @pytest.mark.asyncio
    async def test_leaderboard_default_pagination(self, async_client):
        """测试默认分页参数"""
        response = await async_client.get("/api/v1/game/leaderboard")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        result = data["data"]
        assert "items" in result
        assert "total" in result
        assert "page" in result
        assert result["page"] == 1
        assert "page_size" in result
        assert result["page_size"] == 20
        assert "total_pages" in result

    @pytest.mark.asyncio
    async def test_leaderboard_custom_pagination(self, async_client):
        """测试自定义分页参数"""
        response = await async_client.get(
            "/api/v1/game/leaderboard?page=1&page_size=5"
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        assert data["data"]["page"] == 1
        assert data["data"]["page_size"] == 5

    @pytest.mark.asyncio
    async def test_leaderboard_with_data(
        self, async_client, db_session: AsyncSession
    ):
        """测试排行榜返回用户数据"""
        # 先创建几个测试用户和分数
        users = []
        for i in range(1, 4):
            user = User(
                phone=f"1380013800{i}",
                nickname=f"用户{i}",
                hashed_password="test",
            )
            db_session.add(user)
            await db_session.flush()
            users.append(user)

        scores = [
            UserScore(user_id=users[0].id, score=100),
            UserScore(user_id=users[1].id, score=200),
            UserScore(user_id=users[2].id, score=50),
        ]
        for s in scores:
            db_session.add(s)
        await db_session.commit()

        response = await async_client.get(
            "/api/v1/game/leaderboard?page=1&page_size=10"
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        result = data["data"]
        assert result["total"] == 3
        assert result["total_pages"] == 1
        assert len(result["items"]) == 3

        # 验证按分数降序排列
        assert result["items"][0]["score"] == 200  # 用户2 最高
        assert result["items"][1]["score"] == 100  # 用户1 次之
        assert result["items"][2]["score"] == 50  # 用户3 最低

        # 验证排名
        assert result["items"][0]["rank"] == 1
        assert result["items"][1]["rank"] == 2
        assert result["items"][2]["rank"] == 3

        # 验证字段完整性
        item = result["items"][0]
        assert "user_id" in item
        assert "nickname" in item
        assert "avatar_url" in item

    @pytest.mark.asyncio
    async def test_leaderboard_page_out_of_range(
        self, async_client, db_session: AsyncSession
    ):
        """测试请求超出总页数时返回空列表"""
        user = User(
            phone="13800138999",
            nickname="测试用户",
            hashed_password="test",
        )
        db_session.add(user)
        await db_session.flush()
        db_session.add(UserScore(user_id=user.id, score=10))
        await db_session.commit()

        response = await async_client.get(
            "/api/v1/game/leaderboard?page=99&page_size=10"
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        result = data["data"]
        assert result["items"] == []
        assert result["total"] == 1
        assert result["page"] == 99
        assert result["total_pages"] == 1

    @pytest.mark.asyncio
    async def test_leaderboard_invalid_type(self, async_client):
        """测试不支持的排行榜类型"""
        response = await async_client.get(
            "/api/v1/game/leaderboard?type=FRIENDS"
        )
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == -1
        assert "不支持的排行榜类型" in data["message"]

    @pytest.mark.asyncio
    async def test_leaderboard_negative_page(self, async_client):
        """测试 page 小于 1 时返回 422"""
        response = await async_client.get(
            "/api/v1/game/leaderboard?page=0"
        )
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_leaderboard_empty_db(self, async_client):
        """测试空数据库时返回空列表"""
        response = await async_client.get("/api/v1/game/leaderboard")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 0
        result = data["data"]
        assert result["items"] == []
        assert result["total"] == 0
        assert result["total_pages"] == 1  # 至少一页
