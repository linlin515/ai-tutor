"""
AI 学伴后端 - TTS Provider 抽象基类
定义语音合成服务的统一接口
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import AsyncGenerator


class TTSProvider(ABC):
    """TTS 提供商抽象基类"""

    @abstractmethod
    async def synthesize(
        self,
        text: str,
        voice: str | None = None,
        speed: float = 1.0,
    ) -> bytes:
        """
        文本转语音

        Args:
            text: 要合成的文本
            voice: 发音人
            speed: 语速 (0.5-2.0)

        Returns:
            音频二进制数据 (MP3 格式)
        """
        ...

    @abstractmethod
    async def get_supported_voices(self) -> list[dict]:
        """
        获取支持的发音人列表

        Returns:
            [{"voice_id": "...", "name": "...", "gender": "..."}, ...]
        """
        ...

    @abstractmethod
    def name(self) -> str:
        """返回提供商名称"""
        ...
