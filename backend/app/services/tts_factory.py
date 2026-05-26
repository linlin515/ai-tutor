"""
AI 学伴后端 - TTS Provider 工厂
注册、获取和管理 TTS 提供商实例
"""
from __future__ import annotations

import logging

from app.config import get_settings
from app.services.tts_base import TTSProvider
from app.services.tts_edge import EdgeTTSProvider
from app.services.tts_openai import OpenAITTSProvider

logger = logging.getLogger(__name__)


class TTSProviderFactory:
    """
    TTS 提供商工厂

    管理所有注册的 TTS Provider，提供统一的获取接口。
    支持 lazy 初始化。
    """

    _providers: dict[str, TTSProvider] = {}
    _initialized = False

    @classmethod
    def _ensure_initialized(cls):
        """确保所有 provider 已注册并初始化"""
        if cls._initialized:
            return

        cls._providers.clear()

        # 注册 Edge TTS Provider
        try:
            edge_provider = EdgeTTSProvider()
            cls._providers[edge_provider.name()] = edge_provider
            logger.info("TTS Provider 已注册: edge")
        except Exception as e:
            logger.warning("Edge TTS Provider 注册失败: %s", e)

        # 注册 OpenAI TTS Provider
        try:
            openai_provider = OpenAITTSProvider()
            cls._providers[openai_provider.name()] = openai_provider
            logger.info("TTS Provider 已注册: openai")
        except Exception as e:
            logger.warning("OpenAI TTS Provider 注册失败: %s", e)

        cls._initialized = True

    @classmethod
    def get_provider(cls, name: str | None = None) -> TTSProvider:
        """
        获取 TTS Provider

        Args:
            name: provider 名称（edge/openai），None 则返回默认

        Returns:
            TTSProvider 实例

        Raises:
            ValueError: 不支持的 provider
        """
        cls._ensure_initialized()

        if name is None:
            # 返回默认 provider
            settings = get_settings()
            default = settings.tts_default_provider
            name = default if default in cls._providers else "edge"

        provider = cls._providers.get(name)
        if provider is None:
            raise ValueError(f"不支持的 TTS Provider: {name}，可用: {list(cls._providers.keys())}")

        return provider

    @classmethod
    def list_providers(cls) -> list[str]:
        """列出所有已注册的 provider 名称"""
        cls._ensure_initialized()
        return list(cls._providers.keys())
