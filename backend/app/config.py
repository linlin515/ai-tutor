"""
AI 学伴后端 - 配置管理
从环境变量读取配置，提供统一的配置接口
"""

from __future__ import annotations

import secrets
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置类，从环境变量读取"""

    # 数据库
    database_url: str = "sqlite+aiosqlite:///./ai_tutor.db"

    # Redis 缓存
    redis_url: str = ""  # Docker 部署时为 redis://redis:***@lru_cache()

    # AI 服务
    new_api_base_url: str = "http://localhost:3000/v1"  # new-api 服务地址
    new_api_key: str = ""

    # JWT
    secret_key: str = secrets.token_urlsafe(32)
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60 * 24 * 7  # 7 days

    # CORS
    cors_origins: list[str] = ["*"]
    cors_allow_credentials: bool = True
    cors_allow_methods: list[str] = ["*"]
    cors_allow_headers: list[str] = ["*"]

    # TTS 配置
    tts_default_provider: str = "edge"  # edge / openai
    tts_edge_enabled: bool = True
    tts_openai_enabled: bool = True
    tts_max_text_length: int = 1024  # 单次合成最大字符数

    # 日志
    log_level: str = "INFO"
    log_format: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

    def model_post_init(self, __context):
        """初始化后处理：若 secret_key 为空则自动生成随机密钥"""
        if not self.secret_key:
            self.secret_key = secrets.token_hex(32)


@lru_cache()
def get_settings() -> Settings:
    """获取单例配置对象"""
    return Settings()
