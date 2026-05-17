"""
AI 学伴后端 - 拍照解题路由
上传图片 → OCR 识别 → AI 解题
"""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.question_record import QuestionRecord
from app.models.user import User
from app.schemas.chat import SolvePhotoResponse
from app.schemas.common import success
from app.services.ocr_service import solve_by_photo

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/solve", tags=["拍照解题"])

# 允许的图片类型
ALLOWED_CONTENT_TYPES = {
    "image/jpeg",
    "image/png",
    "image/webp",
}

# 最大文件大小：10MB
MAX_FILE_SIZE = 10 * 1024 * 1024


@router.post("/photo")
async def solve_photo(
    file: UploadFile = File(..., description="题目图片"),
    subject: str = Form(
        default="math",
        pattern=r"^(math|chinese|english|science)$",
        description="学科",
    ),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """拍照解题 — 上传图片，OCR 识别后由 AI 解答"""
    # 验证文件类型
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的图片格式：{file.content_type}，仅支持 JPEG/PNG/WebP",
        )

    # 读取文件内容
    image_data = await file.read()

    if len(image_data) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail="图片文件过大，请压缩后上传（最大 10MB）",
        )

    if len(image_data) == 0:
        raise HTTPException(status_code=400, detail="上传的图片为空")

    # 调用 OCR + AI 解题 Pipeline
    try:
        result = await solve_by_photo(image_data, subject=subject)

        # 记录提问
        record = QuestionRecord(
            user_id=current_user.id,
            subject=subject,
            question_type="photo",
            question_content=result["recognized_text"],
            answer_content=result["answer"],
            ai_model_used=result.get("model"),
            tokens_used=result.get("total_tokens"),
        )
        db.add(record)

        return success(
            data=SolvePhotoResponse(
                recognized_text=result["recognized_text"],
                answer=result["answer"],
                subject=subject,
                model_used=result.get("model"),
                tokens_used=result.get("total_tokens"),
            )
        )

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
