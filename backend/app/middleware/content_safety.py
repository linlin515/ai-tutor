"""
AI 学伴后端 - 内容安全中间件
对 AI 输出做二次检查，基于关键词 + AI 双重过滤
"""

from __future__ import annotations

import logging
from typing import Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

from app.services.ai_service import content_filter as ai_content_filter
from app.services.ai_service import _keyword_filter

logger = logging.getLogger(__name__)

UNSAFE_RESPONSE_MESSAGE = "这个问题我暂时无法回答，请换个问题试试～"

# 需要检查安全性的路由前缀
_CHECK_PATHS = [
    "/api/v1/chat/ask",
    "/api/v1/solve/photo",
]


class ContentSafetyMiddleware(BaseHTTPMiddleware):
    """
    内容安全中间件
    对 AI 输出的内容做二次安全过滤
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """处理请求并检查响应内容"""
        response = await call_next(request)

        # 只检查特定路径的 POST 请求
        if request.method != "POST":
            return response

        path = request.url.path
        if not any(path.startswith(prefix) for prefix in _CHECK_PATHS):
            return response

        # 读取响应体
        body = b""
        async for chunk in response.body_iterator:
            body += chunk

        # 重新构建响应以便读取 body
        if not body:
            return Response(
                content=body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type,
            )

        # 尝试解析 JSON 响应
        try:
            import json

            resp_data = json.loads(body)
            # 提取回答内容进行检查
            answer_text = ""
            if isinstance(resp_data, dict):
                data = resp_data.get("data", {})
                if isinstance(data, dict):
                    answer_text = data.get("answer", "") or data.get("recognized_text", "")

            if answer_text:
                # 先做关键词快速过滤
                kw_result = _keyword_filter(answer_text)
                if not kw_result["safe"]:
                    logger.warning("内容安全拦截（关键词）: %s", kw_result["reason"])
                    return JSONResponse(
                        content={
                            "code": -1,
                            "message": UNSAFE_RESPONSE_MESSAGE,
                            "data": None,
                        },
                        status_code=200,
                    )

                # AI 二次过滤（异步，避免阻塞）
                # 注意：这里为了性能，只对较长回答做 AI 过滤
                if len(answer_text) > 50:
                    try:
                        ai_result = await ai_content_filter(answer_text)
                        if not ai_result["safe"]:
                            logger.warning(
                                "内容安全拦截（AI检测）: %s", ai_result["reason"]
                            )
                            return JSONResponse(
                                content={
                                    "code": -1,
                                    "message": UNSAFE_RESPONSE_MESSAGE,
                                    "data": None,
                                },
                                status_code=200,
                            )
                    except Exception as e:
                        logger.error("内容安全 AI 过滤异常: %s", e)

            # 返回原始响应
            return Response(
                content=body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type,
            )

        except json.JSONDecodeError:
            return Response(
                content=body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type,
            )
