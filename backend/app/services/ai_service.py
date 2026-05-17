"""
AI 学伴后端 - AI 服务模块
通过 new-api (http://localhost:3000) 代理调用 LLM，兼容 OpenAI API 格式
"""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# ============================================================
# System Prompt 设计：苏格拉底问答法
# 引导 AI 通过启发式问题帮助 8-14 岁孩子独立思考，
# 而不是直接给出答案
# ============================================================
SOCRATIC_SYSTEM_PROMPT = """你是"AI学伴"，一个面向8-14岁青少年的学习助手。

## 核心原则：苏格拉底问答法
你的任务是引导学生独立思考，而不是直接给答案。

### 如何做：
1. **先理解**：确认学生的题目和当前思路
2. **再引导**：用启发式问题帮助学生找到解题方向
3. **分步骤**：把复杂问题分解成小步骤，逐步引导
4. **多鼓励**：肯定学生的思考过程，建立信心

### 示例对话：
学生：3x + 5 = 20，x等于多少？
好的引导：我们来想想，如果3x + 5 = 20，那3x应该等于多少呢？🤔

### 注意事项：
- 不要直接给出完整答案
- 用 emoji 让对话更亲切
- 如果学生有明显的知识盲区，先帮ta补基础概念
- 始终用中文回答
- 保持耐心和鼓励的态度"""

# 内容安全检测 System Prompt
CONTENT_SAFETY_SYSTEM_PROMPT = """你是一个内容安全检测助手。请判断以下内容是否包含任何不当信息：
- 暴力、血腥内容
- 色情或淫秽内容
- 政治敏感内容
- 违法信息
- 辱骂、欺凌或人身攻击
- 其他不适宜 8-14 岁青少年的内容

请以 JSON 格式回复：
{"safe": true/false, "reason": "如果 unsafe，简要说明原因"}

注意：正常的数学题、语文题、科学问题等学习内容，即使包含"死亡"等词在科学语境中（如"细胞死亡"）也是安全的。"""


async def chat(
    messages: list[dict[str, str]],
    model: str | None = None,
    temperature: float = 0.7,
    max_tokens: int = 2000,
) -> dict[str, Any]:
    """
    通用对话接口

    Args:
        messages: 消息列表，格式 [{"role": "user", "content": "..."}]
        model: 模型名称，默认使用配置中的模型
        temperature: 温度参数
        max_tokens: 最大生成 token 数

    Returns:
        包含回答内容的字典: {"content": "...", "model": "...", "usage": {...}}
    """
    model = model or settings.default_ai_model

    headers = {
        "Authorization": f"Bearer {settings.new_api_key}",
        "Content-Type": "application/json",
    }

    payload = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
    }

    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            response = await client.post(
                f"{settings.new_api_base_url}/chat/completions",
                headers=headers,
                json=payload,
            )
            response.raise_for_status()
            result = response.json()

            content = result["choices"][0]["message"]["content"]
            usage = result.get("usage", {})
            total_tokens = usage.get("total_tokens", 0)

            logger.info(
                "AI 调用成功: model=%s, tokens=%d",
                model,
                total_tokens,
            )

            return {
                "content": content,
                "model": result.get("model", model),
                "usage": usage,
                "total_tokens": total_tokens,
            }

        except httpx.TimeoutException:
            logger.error("AI 调用超时")
            raise TimeoutError("AI 服务响应超时，请稍后再试")
        except httpx.HTTPStatusError as e:
            logger.error("AI 调用 HTTP 错误: %s", e)
            raise RuntimeError(f"AI 服务返回错误: {e.response.status_code}")
        except Exception as e:
            logger.error("AI 调用异常: %s", e)
            raise RuntimeError(f"AI 服务调用失败: {str(e)}")


async def solve_problem(
    problem_text: str,
    subject: str = "math",
    model: str | None = None,
) -> dict[str, Any]:
    """
    解题服务 — 使用苏格拉底问答法引导式解题

    Args:
        problem_text: 题目文本
        subject: 学科 (math/chinese/english/science)
        model: 模型名称

    Returns:
        {
            "answer": "引导式回答",
            "model": "使用的模型",
            "total_tokens": 消耗的 token 数
        }
    """
    subject_prompts = {
        "math": "你正在解答一道数学题。",
        "chinese": "你正在解答一道语文题。",
        "english": "你正在解答一道英语题。",
        "science": "你正在解答一道科学题。",
    }

    subject_hint = subject_prompts.get(subject, "")

    messages = [
        {"role": "system", "content": SOCRATIC_SYSTEM_PROMPT},
        {
            "role": "user",
            "content": f"{subject_hint}\n\n题目：{problem_text}\n\n请用苏格拉底问答法引导我得出答案，而不是直接告诉我答案。",
        },
    ]

    result = await chat(messages, model=model)
    return {
        "answer": result["content"],
        "model": result["model"],
        "total_tokens": result["total_tokens"],
    }


async def content_filter(text: str) -> dict[str, bool | str]:
    """
    内容安全过滤检查

    使用 AI 判断内容是否安全

    Args:
        text: 待检查的文本

    Returns:
        {"safe": True/False, "reason": "原因说明"}
    """
    messages = [
        {"role": "system", "content": CONTENT_SAFETY_SYSTEM_PROMPT},
        {"role": "user", "content": f"请检查以下内容是否安全：\n\n{text}"},
    ]

    try:
        result = await chat(messages, temperature=0.1, max_tokens=500)
        content = result["content"]

        # 尝试从返回中解析 JSON
        import json
        import re

        # 查找 JSON 块
        json_match = re.search(r"\{[^{}]*\}", content, re.DOTALL)
        if json_match:
            parsed = json.loads(json_match.group())
            return {
                "safe": parsed.get("safe", True),
                "reason": parsed.get("reason", ""),
            }

        # 如果无法解析，基于关键词做二次检查
        return _keyword_filter(text)

    except Exception as e:
        logger.warning("AI 内容过滤失败，回退到关键词过滤: %s", e)
        return _keyword_filter(text)


def _keyword_filter(text: str) -> dict[str, bool | str]:
    """
    基于关键词的简单内容过滤（作为 AI 过滤的备用方案）
    """
    unsafe_keywords = [
        # 暴力
        "杀人", "爆炸", "恐怖袭击", "枪支", "毒品",
        # 色情
        "色情", "裸体", "性交", "成人网站",
        # 政治敏感
        "反动", "颠覆国家",
        # 违法
        "赌博", "传销",
    ]

    for keyword in unsafe_keywords:
        if keyword in text:
            return {"safe": False, "reason": f"内容包含不当关键词：{keyword}"}

    return {"safe": True, "reason": ""}
