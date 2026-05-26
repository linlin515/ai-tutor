"""
AI 学伴后端 - OpenAI TTS Provider
封装现有 OpenAI TTS 功能（通过 new-api 代理）
"""
from __future__ import annotations

import logging

import httpx

from app.config import get_settings
from app.services.tts_base import TTSProvider

logger = logging.getLogger(__name__)

# OpenAI 标准发音人
OPENAI_VOICES = [
    {"voice_id": "alloy", "name": "Alloy", "gender": "neutral"},
    {"voice_id": "echo", "name": "Echo", "gender": "male"},
    {"voice_id": "fable", "name": "Fable", "gender": "neutral"},
    {"voice_id": "onyx", "name": "Onyx", "gender": "male"},
    {"voice_id": "nova", "name": "Nova", "gender": "female"},
    {"voice_id": "shimmer", "name": "Shimmer", "gender": "female"},
]

OPENAI_MODEL = "tts-1"
OPENAI_MAX_TEXT_LENGTH = 4096


class OpenAITTSProvider(TTSProvider):
    """
    OpenAI TTS Provider
    通过 new-api 代理调用 OpenAI TTS API
    """

    def __init__(self):
        self.settings = get_settings()

    def name(self) -> str:
        return "openai"

    async def synthesize(
        self,
        text: str,
        voice: str | None = None,
        speed: float = 1.0,
    ) -> bytes:
        """
        调用 OpenAI TTS 合成语音

        Args:
            text: 要合成的文本
            voice: 发音人 (alloy/echo/fable/onyx/nova/shimmer)
            speed: 语速 (0.25-4.0)

        Returns:
            MP3 音频二进制数据
        """
        voice = voice or "alloy"
        speed = max(0.25, min(4.0, speed))

        headers = {
            "Authorization": f"Bearer {self.settings.new_api_key}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": OPENAI_MODEL,
            "input": text,
            "voice": voice,
            "speed": speed,
            "response_format": "mp3",
        }

        async with httpx.AsyncClient(timeout=60.0) as client:
            try:
                response = await client.post(
                    f"{self.settings.new_api_base_url}/audio/speech",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                return response.content

            except httpx.TimeoutException:
                logger.error("OpenAI TTS 请求超时")
                raise RuntimeError("TTS 请求超时")
            except httpx.HTTPStatusError as e:
                logger.error("OpenAI TTS HTTP 错误: %s", e.response.status_code)
                raise RuntimeError(f"TTS API 返回错误: {e.response.status_code}")
            except Exception as e:
                logger.error("OpenAI TTS 异常: %s", str(e))
                raise

    async def get_supported_voices(self) -> list[dict]:
        """返回支持的发音人列表"""
        return OPENAI_VOICES
