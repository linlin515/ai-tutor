"""
AI 学伴后端 - OCR 服务模块
使用 PaddleOCR 实现图片文字识别
Pipeline: 接收图片 → OCR 提取文字 → 调用 AI 解题
"""

from __future__ import annotations

import logging
import tempfile
from pathlib import Path
from typing import BinaryIO

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# PaddleOCR 是否可用的标志
_ocr_available = False
_ocr_engine = None


def _get_ocr_engine():
    """
    延迟初始化 OCR 引擎
    只在首次调用时加载，避免影响启动速度
    """
    global _ocr_engine, _ocr_available

    if _ocr_engine is not None:
        return _ocr_engine

    if not settings.ocr_enabled:
        logger.info("OCR 功能已禁用（OCR_ENABLED=false）")
        _ocr_available = False
        return None

    try:
        from paddleocr import PaddleOCR

        # 初始化 PaddleOCR，只使用检测+识别（不启用方向分类器以加快速度）
        _ocr_engine = PaddleOCR(
            use_angle_cls=False,
            lang="ch",
            show_log=False,
            use_gpu=False,  # CPU 推理，后续可切换 GPU
        )
        _ocr_available = True
        logger.info("PaddleOCR 引擎初始化成功（CPU 模式）")
    except ImportError:
        logger.warning("PaddleOCR 未安装，OCR 功能不可用")
        _ocr_available = False
    except Exception as e:
        logger.warning("PaddleOCR 初始化失败: %s", e)
        _ocr_available = False

    return _ocr_engine


async def ocr_recognize(image_data: bytes) -> str:
    """
    对图片进行 OCR 文字识别

    Args:
        image_data: 图片二进制数据

    Returns:
        识别出的文字内容

    Raises:
        RuntimeError: OCR 引擎不可用或识别失败
    """
    engine = _get_ocr_engine()

    if not _ocr_available or engine is None:
        raise RuntimeError("OCR 引擎不可用，请检查配置或安装 PaddleOCR")

    try:
        # 将图片数据保存到临时文件（PaddleOCR 需要文件路径）
        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
            tmp.write(image_data)
            tmp_path = tmp.name

        # 执行 OCR
        result = engine.ocr(tmp_path, cls=False)

        # 清理临时文件
        Path(tmp_path).unlink(missing_ok=True)

        # 提取文字
        if not result or not result[0]:
            return ""

        lines = []
        for line in result[0]:
            text = line[1][0]  # result[0][i] = [bbox, (text, confidence)]
            lines.append(text)

        recognized_text = "\n".join(lines)
        logger.info("OCR 识别完成: %d 字符", len(recognized_text))
        return recognized_text

    except Exception as e:
        logger.error("OCR 识别失败: %s", e)
        raise RuntimeError(f"OCR 识别失败: {str(e)}")


async def solve_by_photo(image_data: bytes, subject: str = "math") -> dict:
    """
    完整拍照解题 Pipeline

    Args:
        image_data: 图片二进制数据
        subject: 学科

    Returns:
        {
            "recognized_text": "OCR 识别的文字",
            "answer": "AI 解题回答",
            "model": "使用的模型",
            "total_tokens": 消耗的 token 数
        }
    """
    from app.services.ai_service import solve_problem

    # Step 1: OCR 识别
    recognized_text = await ocr_recognize(image_data)

    if not recognized_text:
        raise ValueError("未能从图片中识别出文字，请确认图片清晰且包含题目")

    # Step 2: AI 解题
    result = await solve_problem(recognized_text, subject=subject)

    return {
        "recognized_text": recognized_text,
        "answer": result["answer"],
        "model": result["model"],
        "total_tokens": result["total_tokens"],
    }
