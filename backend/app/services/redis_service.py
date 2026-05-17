"""
AI 学伴后端 - Redis 缓存服务

提供统一的 Redis 缓存操作接口。
ARCH_REVIEW P1: 用于模型列表缓存、订阅状态缓存、验证码 TTL 防重发。
"""

from __future__ import annotations

import json
import logging
from typing import Any

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# 全局 Redis 连接（惰性初始化）
_redis_client = None


def _get_redis() -> Any | None:
    """惰性获取 Redis 连接"""
    global _redis_client

    if _redis_client is not None:
        return _redis_client

    if not settings.redis_url:
        logger.info("Redis 未配置（redis_url 为空），缓存功能不可用")
        return None

    try:
        import redis.asyncio as aioredis

        _redis_client = aioredis.from_url(
            settings.redis_url,
            decode_responses=True,
            socket_connect_timeout=2,
            socket_timeout=3,
        )
        logger.info("Redis 连接已建立: %s", settings.redis_url)
    except ImportError:
        logger.warning("redis-py 未安装，缓存功能不可用")
    except Exception as e:
        logger.warning("Redis 连接失败: %s", e)

    return _redis_client


async def cache_get(key: str) -> str | None:
    """获取缓存"""
    client = _get_redis()
    if client is None:
        return None
    try:
        return await client.get(key)
    except Exception as e:
        logger.warning("Redis get 失败 (key=%s): %s", key, e)
        return None


async def cache_set(key: str, value: str, ttl: int = 300) -> bool:
    """设置缓存（带 TTL）"""
    client = _get_redis()
    if client is None:
        return False
    try:
        await client.setex(key, ttl, value)
        return True
    except Exception as e:
        logger.warning("Redis setex 失败 (key=%s): %s", key, e)
        return False


async def cache_get_json(key: str) -> Any | None:
    """获取 JSON 缓存"""
    val = await cache_get(key)
    if val is None:
        return None
    try:
        return json.loads(val)
    except json.JSONDecodeError:
        return None


async def cache_set_json(key: str, value: Any, ttl: int = 300) -> bool:
    """设置 JSON 缓存"""
    return await cache_set(key, json.dumps(value, ensure_ascii=False), ttl)


async def cache_delete(key: str) -> bool:
    """删除缓存"""
    client = _get_redis()
    if client is None:
        return False
    try:
        await client.delete(key)
        return True
    except Exception as e:
        logger.warning("Redis delete 失败 (key=%s): %s", key, e)
        return False


async def cache_exists(key: str) -> bool:
    """检查 key 是否存在"""
    client = _get_redis()
    if client is None:
        return False
    try:
        return await client.exists(key) > 0
    except Exception as e:
        logger.warning("Redis exists 失败 (key=%s): %s", key, e)
        return False


async def close_redis():
    """关闭 Redis 连接"""
    global _redis_client
    if _redis_client is not None:
        try:
            await _redis_client.close()
        except Exception:
            pass
        _redis_client = None
