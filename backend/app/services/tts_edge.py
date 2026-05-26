"""
AI 学伴后端 - Microsoft Edge TTS Provider
通过 WebSocket 调用微软 Edge 浏览器内置的 TTS 服务（免费）
"""
from __future__ import annotations

import json
import logging
import uuid
from typing import AsyncGenerator

import httpx

from app.services.tts_base import TTSProvider

logger = logging.getLogger(__name__)

# Edge TTS WebSocket 端点
EDGE_TTS_WSS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClient=Edge"
EDGE_TTS_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"

# 默认中文语音
EDGE_DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"

# 可用语音列表（常用）
EDGE_SUPPORTED_VOICES = [
    {"voice_id": "zh-CN-XiaoxiaoNeural", "name": "晓晓 (女)", "gender": "female", "locale": "zh-CN"},
    {"voice_id": "zh-CN-XiaoyiNeural", "name": "晓伊 (女)", "gender": "female", "locale": "zh-CN"},
    {"voice_id": "zh-CN-YunjianNeural", "name": "云健 (男)", "gender": "male", "locale": "zh-CN"},
    {"voice_id": "zh-CN-YunxiNeural", "name": "云希 (男)", "gender": "male", "locale": "zh-CN"},
    {"voice_id": "zh-CN-YunxiaNeural", "name": "云夏 (男)", "gender": "male", "locale": "zh-CN"},
    {"voice_id": "zh-CN-YunyangNeural", "name": "云扬 (男)", "gender": "male", "locale": "zh-CN"},
    {"voice_id": "zh-CN-liaoning-XiaobeiNeural", "name": "晓北 (女, 辽宁)", "gender": "female", "locale": "zh-CN"},
    {"voice_id": "en-US-AriaNeural", "name": "Aria (女, 美式)", "gender": "female", "locale": "en-US"},
    {"voice_id": "en-US-GuyNeural", "name": "Guy (男, 美式)", "gender": "male", "locale": "en-US"},
    {"voice_id": "en-GB-SoniaNeural", "name": "Sonia (女, 英式)", "gender": "female", "locale": "en-GB"},
    {"voice_id": "ja-JP-NanamiNeural", "name": "Nanami (女, 日语)", "gender": "female", "locale": "ja-JP"},
]


def _build_ssml(text: str, voice: str, rate: str = "0%") -> str:
    """构建 SSML 格式请求"""
    return (
        f"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis'"
        f" xmlns:mstts='http://www.w3.org/2001/mstts' xml:lang='{voice[:5]}'>"
        f"<voice name='{voice}'>"
        f"<prosody rate='{rate}'>{_escape_xml(text)}</prosody>"
        f"</voice></speak>"
    )


def _escape_xml(text: str) -> str:
    """XML 转义"""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") \
               .replace('"', "&quot;").replace("'", "&apos;")


def _parse_speed(speed: float) -> str:
    """将速度值转换为 Edge TTS 的 rate 字符串"""
    if speed <= 0.5:
        return "-50%"
    elif speed <= 0.7:
        return "-30%"
    elif speed <= 0.9:
        return "-10%"
    elif speed <= 1.1:
        return "0%"
    elif speed <= 1.3:
        return "+10%"
    elif speed <= 1.5:
        return "+30%"
    elif speed <= 1.7:
        return "+50%"
    else:
        return "+70%"


class EdgeTTSProvider(TTSProvider):
    """
    Microsoft Edge TTS Provider
    通过 WebSocket 连接 Edge TTS 服务合成语音（免费）
    """

    def __init__(self):
        self._session_id = str(uuid.uuid4())
        self._request_id = 0

    def name(self) -> str:
        return "edge"

    async def synthesize(
        self,
        text: str,
        voice: str | None = None,
        speed: float = 1.0,
    ) -> bytes:
        """
        使用 Edge TTS 合成语音

        通过 HTTP 请求 Connect 和 SSML 来调用 Edge TTS 服务。
        使用微软的语音合成 API 边界协议。
        """
        voice = voice or EDGE_DEFAULT_VOICE
        rate = _parse_speed(speed)
        ssml = _build_ssml(text, voice, rate)

        # 使用 HTTP 方式调用 Edge TTS（简化实现，无需维护 WebSocket 长连接）
        return await self._synthesize_via_http(ssml, voice)

    async def _synthesize_via_http(self, ssml: str, voice: str) -> bytes:
        """
        通过 HTTP 请求 Edge TTS 服务

        使用 Microsoft Edge 的 Speech SDK HTTP API
        """
        # 构造请求
        url = (
            f"https://{voice[:5].lower()}.tts.speech.microsoft.com/cognitiveservices/v1"
        )

        headers = {
            "Authorization": "Bearer anonymous",
            "Content-Type": "application/ssml+xml",
            "X-Microsoft-OutputFormat": "audio-24khz-96kbitrate-mono-mp3",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        }

        async with httpx.AsyncClient(timeout=30.0) as client:
            try:
                response = await client.post(
                    url,
                    headers=headers,
                    content=ssml.encode("utf-8"),
                )
                response.raise_for_status()
                return response.content
            except httpx.HTTPStatusError as e:
                logger.warning(
                    "Edge TTS HTTP 请求失败: status=%s, body=%s",
                    e.response.status_code,
                    e.response.text[:200],
                )
                # 降级：尝试使用 Edge 的 WebSocket 方式
                return await self._synthesize_via_ws(ssml)
            except Exception as e:
                logger.warning("Edge TTS 请求异常: %s", str(e))
                raise

    async def _synthesize_via_ws(self, ssml: str) -> bytes:
        """
        通过 WebSocket 调用 Edge TTS（备用方案）
        实际使用 websockets 库
        """
        try:
            import asyncio
            import websockets

            audio_chunks: list[bytes] = []
            ws_url = EDGE_TTS_WSS_URL

            async with websockets.connect(
                ws_url,
                origin=EDGE_TTS_ORIGIN,
                extra_headers={
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                },
            ) as ws:
                # 发送连接消息
                connect_msg = (
                    f"Content-Type:application/json; charset=utf-8\r\n"
                    f"Path:speech.config\r\n\r\n"
                    f'{{"context":{{"synthesis":{{"audio":{{"metadataoptions":{{'
                    f'"sentenceBoundaryEnabled":false,"wordBoundaryEnabled":false}},'
                    f'"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}}}}}'
                )
                await ws.send(connect_msg)

                # 发送 SSML
                ssml_msg = (
                    f"Content-Type:application/ssml+xml\r\n"
                    f"Path:speech\r\n\r\n"
                    f"{ssml}"
                )
                await ws.send(ssml_msg)

                # 接收音频数据
                turn_end = False
                while not turn_end:
                    try:
                        msg = await asyncio.wait_for(ws.recv(), timeout=10.0)
                        if isinstance(msg, bytes):
                            # 解析 Edge TTS 二进制响应
                            # 格式: [Path 头部]\r\n\r\n[音频数据]
                            header_end = msg.find(b"\r\n\r\n")
                            if header_end >= 0:
                                path_line = msg[:header_end].decode("utf-8", errors="ignore")
                                audio_data = msg[header_end + 4:]
                                if "Path:audio" in path_line:
                                    audio_chunks.append(audio_data)
                                elif "Path:turn.end" in path_line:
                                    turn_end = True
                            else:
                                audio_chunks.append(msg)
                        elif isinstance(msg, str):
                            if "Path:turn.end" in msg:
                                turn_end = True
                    except asyncio.TimeoutError:
                        break

            return b"".join(audio_chunks)

        except Exception as e:
            logger.error("Edge TTS WebSocket 合成失败: %s", str(e))
            raise RuntimeError(f"Edge TTS 合成失败: {e}") from e

    async def get_supported_voices(self) -> list[dict]:
        """返回支持的语音列表"""
        return EDGE_SUPPORTED_VOICES
