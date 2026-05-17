"""
AI 学伴后端 - 自适应分步讲解路由
按年级和学科调整讲解深度，生成结构化分步讲解
"""

from __future__ import annotations

import json
import logging
import re

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.common import success
from app.services.ai_service import chat as ai_chat

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/solve", tags=["分步讲解"])

# 年级 → 讲解深度映射
GRADE_DEPTH = {
    "primary-1": "小学一年级，使用最简单的语言和大量比喻，避免复杂术语",
    "primary-2": "小学二年级，使用简单语言和比喻，少量术语需要解释",
    "primary-3": "小学三年级，可以引入基本术语，但需要解释",
    "primary-4": "小学四年级，可以使用适当术语，步骤详细",
    "primary-5": "小学五年级，讲解较详细，可以适当省略基础概念",
    "primary-6": "小学六年级，讲解简洁，重点放在解题方法",
    "junior-1": "初一，讲解简洁，侧重方法和思路",
    "junior-2": "初二，讲解精炼，注重逻辑推导",
    "junior-3": "初三，讲解精炼，直接切入核心方法",
}

# 学科提示
SUBJECT_PROMPTS = {
    "math": "数学题",
    "chinese": "语文题",
    "english": "英语题",
    "science": "科学题",
}

# 分步讲解 System Prompt
STEPS_SYSTEM_PROMPT = """你是"AI学伴"，一个面向8-14岁青少年的学习助手。
现在需要你为一道{subject_desc}生成分步讲解。

## 讲解深度
当前学生年级：{grade_desc}

## 输出要求
请严格按照以下 JSON 格式输出，不要输出任何其他内容：
```json
{{
  "steps": [
    {{
      "title": "步骤标题（简短概括）",
      "content": "详细讲解内容（适合该年级学生的语言）",
      "formula": "涉及的关键公式（如果没有则为null）"
    }}
  ],
  "difficulty": "easy/medium/hard"
}}
```

## 讲解原则
1. 根据年级调整语言深度和步骤数量：低年级更多步骤、更通俗；高年级更精炼
2. 每个步骤的标题要简短有力，内容要清晰易懂
3. 如果涉及公式，务必在 formula 字段中给出
4. difficulty 根据题目实际难度判断，不受年级影响"""


def _parse_steps_response(content: str) -> dict:
    """从 AI 返回内容中解析结构化步骤"""
    # 尝试提取 JSON 块
    json_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", content, re.DOTALL)
    if json_match:
        text = json_match.group(1)
    else:
        # 尝试直接匹配最外层 JSON 对象
        json_match = re.search(r"\{[\s\S]*\}", content)
        text = json_match.group(0) if json_match else content

    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        # 解析失败，返回单步兜底
        return {
            "steps": [{"title": "解答", "content": content, "formula": None}],
            "difficulty": "medium",
        }

    # 校验必要字段
    steps = parsed.get("steps", [])
    if not steps:
        return {
            "steps": [{"title": "解答", "content": content, "formula": None}],
            "difficulty": parsed.get("difficulty", "medium"),
        }

    # 标准化每个步骤
    normalized_steps = []
    for step in steps:
        normalized_steps.append({
            "title": step.get("title", ""),
            "content": step.get("content", ""),
            "formula": step.get("formula") or None,
        })

    return {
        "steps": normalized_steps,
        "difficulty": parsed.get("difficulty", "medium"),
    }


@router.post("/steps")
async def solve_steps(
    question: str,
    grade: str,
    subject: str = "math",
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """自适应分步讲解 — 按年级和学科调整讲解深度"""
    grade_desc = GRADE_DEPTH.get(grade, "中学生，讲解简洁清晰")
    subject_desc = SUBJECT_PROMPTS.get(subject, "题目")

    system_prompt = STEPS_SYSTEM_PROMPT.format(
        subject_desc=subject_desc,
        grade_desc=grade_desc,
    )

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"题目：{question}"},
    ]

    try:
        result = await ai_chat(messages, temperature=0.3, max_tokens=3000)
        parsed = _parse_steps_response(result["content"])

        return success(data=parsed)

    except TimeoutError:
        raise HTTPException(status_code=504, detail="AI 服务响应超时，请稍后再试")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
