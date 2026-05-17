"""
AI 学伴后端 - FastAPI 应用入口

启动命令：uvicorn app.main:app --reload
"""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.database import close_db, init_db
from app.middleware.content_safety import ContentSafetyMiddleware
from app.routers import audio, auth, chat, chat_completions, health, models, solve, steps, subscription, user
from app.schemas.common import error as api_error
from app.services.redis_service import close_redis

# 配置日志
settings = get_settings()
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format=settings.log_format,
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    logger.info("AI 学伴后端启动中...")
    logger.info("数据库初始化...")
    try:
        await init_db()
        logger.info("数据库表初始化完成")
        if settings.secret_key:
            logger.info("JWT 密钥: 已配置（自动生成/环境变量）")
    except Exception as e:
        logger.warning("数据库初始化可能已在之前完成: %s", e)

    yield

    logger.info("AI 学伴后端关闭中...")
    await close_db()
    await close_redis()
    logger.info("数据库连接已关闭")


app = FastAPI(
    title="AI 学伴后端 API",
    description="面向 8-14 岁青少年的 AI 学习助手后端服务",
    version="1.0.0",
    lifespan=lifespan,
)

# ============================================================
# CORS 中间件
# ============================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_allow_credentials,
    allow_methods=settings.cors_allow_methods,
    allow_headers=settings.cors_allow_headers,
)

# ============================================================
# 全局异常处理
# ============================================================


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """全局异常处理，返回统一格式"""
    logger.error("未捕获异常: %s", exc, exc_info=True)
    return JSONResponse(
        status_code=500,
        content=api_error(
            code=-1,
            message="服务器内部错误，请稍后再试",
        ).model_dump(),
    )


# ============================================================
# 中间件注册
# ============================================================
app.add_middleware(ContentSafetyMiddleware)

# ============================================================
# 路由注册
# ============================================================
app.include_router(health.router, prefix="/api/v1")
app.include_router(auth.router)
app.include_router(models.router)
app.include_router(user.router)
app.include_router(chat.router)
app.include_router(chat_completions.router)
app.include_router(solve.router)
app.include_router(steps.router)
app.include_router(subscription.router)
app.include_router(audio.router)


# ============================================================
# 根路径
# ============================================================
@app.get("/")
async def root():
    return {
        "service": "AI 学伴后端",
        "version": "1.0.0",
        "docs": "/docs",
    }
