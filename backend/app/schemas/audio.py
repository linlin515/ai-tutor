"""
AI 学伴后端 - 语音相关 Pydantic Schema
ASR（语音识别）和 TTS（文本转语音）的请求/响应模型
"""

from __future__ import annotations

from pydantic import BaseModel, Field


# ---------- ASR: 语音识别 ----------

class TranscriptionRequest(BaseModel):
    """语音识别请求（表单字段部分）

    FastAPI 中 file 字段通过 UploadFile 单独处理
    """
    model: str = Field(
        default="whisper-1",
        description="Whisper 模型名称",
    )
    language: str = Field(
        default="zh",
        description="音频语言代码 (如 zh, en)",
    )


class TranscriptionResponse(BaseModel):
    """语音识别响应"""
    text: str = Field(..., description="识别后的文字")


# ---------- TTS: 文本转语音 ----------

class SpeechRequest(BaseModel):
    """语音合成请求"""
    model: str = Field(
        default="tts-1",
        description="TTS 模型名称 (tts-1 / tts-1-hd)",
    )
    input: str = Field(
        ...,
        min_length=1,
        max_length=4096,
        description="要合成语音的文本",
    )
    voice: str = Field(
        default="alloy",
        description="发音人 (alloy/echo/fable/onyx/nova/shimmer)",
    )
    speed: float = Field(
        default=1.0,
        ge=0.25,
        le=4.0,
        description="语速 (0.25 ~ 4.0)",
    )
    response_format: str = Field(
        default="mp3",
        description="音频格式 (mp3/opus/aac/flac/wav/pcm)",
    )
