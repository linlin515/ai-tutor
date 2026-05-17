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
    redis_url: str = ""  # Docker 部署时为 redis://redis:6379/0

    # new-api 代理配置（兼容 OpenAI API 格式）
    new_api_base_url: str = "http://localhost:3000/v1"
    new_api_key: str = "sk-you...here"

    # OCR
    ocr_enabled: bool = True

    # JWT
    secret_key: str = ""  # 若为空，启动时会自动生成随机密钥
    jwt_algorithm: str = "HS256"
    jwt_expiration_hours: int = 72

    # AI 模型
    default_ai_model: str = "gpt-4o-mini"

    # CORS
    cors_origins: list[str] = ["*"]
    cors_allow_credentials: bool = True
    cors_allow_methods: list[str] = ["*"]
    cors_allow_headers: list[str] = ["*"]

    # 配额
    daily_quota_free: int = 5
    daily_quota_premium: int = 50

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
