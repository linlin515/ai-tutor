"""
AI 学伴后端 - 语音路由
ASR（语音识别）: POST /v1/audio/transcriptions
TTS（文本转语音）: POST /v1/audio/speech

通过 new-api (http://localhost:3000/v1) 代理调用 OpenAI Whisper / TTS API
"""

from __future__ import annotations

import logging

import httpx
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from fastapi.responses import Response

from app.config import get_settings
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.audio import SpeechRequest, TranscriptionResponse

logger = logging.getLogger(__name__)
settings = get_settings()

router = APIRouter(prefix="/v1", tags=["语音"])

# TTS 响应格式到 Content-Type 的映射
TTS_CONTENT_TYPES = {
    "mp3": "audio/mpeg",
    "opus": "audio/opus",
    "aac": "audio/aac",
    "flac": "audio/flac",
    "wav": "audio/wav",
    "pcm": "audio/L16",  # PCM 原始音频
}


# ============================================================
# ASR — 语音识别（语音 → 文字）
# ============================================================


@router.post(
    "/audio/transcriptions",
    response_model=TranscriptionResponse,
    summary="语音识别",
    description="上传音频文件，返回识别后的文字（使用 OpenAI Whisper API）",
)
async def transcribe_audio(
    file: UploadFile = File(..., description="音频文件 (wav/mp3/m4a/ogg 等)"),
    model: str = Form(default="whisper-1", description="Whisper 模型名称"),
    language: str = Form(default="zh", description="音频语言代码，如 zh/en/ja"),
    current_user: User = Depends(get_current_user),
):
    """
    ASR 语音识别

    转发到 new-api 的 /v1/audio/transcriptions 接口（兼容 OpenAI Whisper API）

    Args:
        file: 上传的音频文件
        model: 模型名称，默认 whisper-1
        language: 音频语言，默认 zh
        current_user: 当前登录用户（通过 JWT 认证）

    Returns:
        TranscriptionResponse: {\"text\": \"识别后的文字\"}
    """
    # 读取上传的音频文件
    audio_bytes = await file.read()
    logger.info(
        "ASR 请求: user=%s, filename=%s, size=%d, model=%s, lang=%s",
        current_user.id,
        file.filename,
        len(audio_bytes),
        model,
        language,
    )

    # 构造转发到 new-api 的请求
    headers = {
        "Authorization": f"Bearer {settings.new_api_key}",
    }

    # multipart/form-data: 文件 + 表单字段
    data = {"model": model, "language": language}
    files = {
        "file": (file.filename or "audio.wav", audio_bytes, file.content_type or "audio/wav"),
    }

    async with httpx.AsyncClient(timeout=120.0) as client:
        try:
            response = await client.post(
                f"{settings.new_api_base_url}/audio/transcriptions",
                headers=headers,
                data=data,
                files=files,
            )
            response.raise_for_status()
            result = response.json()

            transcribed_text = result.get("text", "")
            logger.info(
                "ASR 成功: user=%s, text_len=%d",
                current_user.id,
                len(transcribed_text),
            )

            return TranscriptionResponse(text=transcribed_text)

        except httpx.TimeoutException:
            logger.error("ASR 请求超时: user=%s", current_user.id)
            raise HTTPException(status_code=504, detail="语音识别服务超时，请稍后再试")
        except httpx.HTTPStatusError as e:
            logger.error("ASR HTTP 错误: user=%s, status=%s", current_user.id, e.response.status_code)
            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"语音识别服务返回错误: {e.response.status_code}",
            )
        except Exception as e:
            logger.error("ASR 异常: user=%s, err=%s", current_user.id, str(e))
            raise HTTPException(status_code=500, detail="语音识别服务调用失败")


# ============================================================
# TTS — 文本转语音（文字 → 语音）
# ============================================================


@router.post(
    "/audio/speech",
    summary="语音合成",
    description="将文字转换为语音音频（使用 OpenAI TTS API）",
    response_class=Response,
)
async def create_speech(
    req: SpeechRequest,
    current_user: User = Depends(get_current_user),
):
    """
    TTS 语音合成

    转发到 new-api 的 /v1/audio/speech 接口（兼容 OpenAI TTS API）
    返回二进制音频数据

    Args:
        req: TTS 请求参数 (model, input, voice, speed, response_format)
        current_user: 当前登录用户（通过 JWT 认证）

    Returns:
        二进制音频数据，Content-Type 由 response_format 决定
    """
    logger.info(
        "TTS 请求: user=%s, input_len=%d, voice=%s, format=%s",
        current_user.id,
        len(req.input),
        req.voice,
        req.response_format,
    )

    headers = {
        "Authorization": f"Bearer {settings.new_api_key}",
        "Content-Type": "application/json",
    }

    payload = req.model_dump()

    async with httpx.AsyncClient(timeout=120.0) as client:
        try:
            response = await client.post(
                f"{settings.new_api_base_url}/audio/speech",
                headers=headers,
                json=payload,
            )
            response.raise_for_status()

            # 获取二进制音频数据
            audio_bytes = response.content

            # 确定 Content-Type
            content_type = TTS_CONTENT_TYPES.get(
                req.response_format,
                "audio/mpeg",
            )

            logger.info(
                "TTS 成功: user=%s, size=%d, format=%s",
                current_user.id,
                len(audio_bytes),
                req.response_format,
            )

            return Response(
                content=audio_bytes,
                media_type=content_type,
                headers={
                    "Content-Disposition": f'attachment; filename="speech.{req.response_format}"',
                },
            )

        except httpx.TimeoutException:
            logger.error("TTS 请求超时: user=%s", current_user.id)
            raise HTTPException(status_code=504, detail="语音合成服务超时，请稍后再试")
        except httpx.HTTPStatusError as e:
            logger.error(
                "TTS HTTP 错误: user=%s, status=%s, body=%s",
                current_user.id,
                e.response.status_code,
                e.response.text[:500],
            )
            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"语音合成服务返回错误: {e.response.status_code}",
            )
        except Exception as e:
            logger.error("TTS 异常: user=%s, err=%s", current_user.id, str(e))
            raise HTTPException(status_code=500, detail="语音合成服务调用失败")
