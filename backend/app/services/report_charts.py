"""
AI 学伴后端 - 学习报告图表生成
使用 Matplotlib 生成柱状图、曲线图、雷达图、饼图
"""
from __future__ import annotations

import io
import logging
from typing import Any

import matplotlib
matplotlib.use("Agg")  # 非交互后端
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
import numpy as np

logger = logging.getLogger(__name__)

# 尝试加载中文字体
_ZH_FONT = None
_ZH_FONT_LOADED = False


def _load_zh_font():
    """加载中文字体，支持 Noto Sans CJK 或系统自带中文字体"""
    global _ZH_FONT, _ZH_FONT_LOADED
    if _ZH_FONT_LOADED:
        return _ZH_FONT

    # 常见中文字体路径
    font_candidates = [
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc",
        "/System/Library/Fonts/PingFang.ttc",  # macOS
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
    ]

    for font_path in font_candidates:
        try:
            if __import__("os").path.exists(font_path):
                prop = fm.FontProperties(fname=font_path)
                _ZH_FONT = prop
                _ZH_FONT_LOADED = True
                logger.info("中文字体已加载: %s", font_path)
                return _ZH_FONT
        except Exception:
            continue

    # 尝试从 matplotlib 自带字体中找
    for f in fm.findSystemFonts():
        try:
            if "cjk" in f.lower() or "noto" in f.lower() or "wqy" in f.lower():
                prop = fm.FontProperties(fname=f)
                _ZH_FONT = prop
                _ZH_FONT_LOADED = True
                logger.info("中文字体已加载: %s", f)
                return _ZH_FONT
        except Exception:
            continue

    logger.warning("未找到中文字体，PDF 中文可能显示异常")
    _ZH_FONT_LOADED = True
    return None


def _get_zh_font():
    """获取中文字体属性"""
    font = _load_zh_font()
    if font:
        return font
    return None


def _set_zh_style():
    """设置 matplotlib 全局中文字体"""
    font = _get_zh_font()
    if font:
        plt.rcParams["font.family"] = font.get_name()
    plt.rcParams["axes.unicode_minus"] = False


def generate_daily_bar_chart(
    daily_stats: list[dict[str, Any]],
    dpi: int = 100,
) -> io.BytesIO:
    """
    生成每日学习时长柱状图

    Args:
        daily_stats: [{"date": "2024-01-01", "study_minutes": 30}, ...]
        dpi: 图片分辨率

    Returns:
        BytesIO 图片数据
    """
    _set_zh_style()

    fig, ax = plt.subplots(figsize=(8, 4))

    dates = [d.get("date", "")[-5:] for d in daily_stats]  # 只取 MM-DD
    minutes = [d.get("study_minutes", 0) for d in daily_stats]

    bars = ax.bar(dates, minutes, color="#4A90D9", width=0.6)
    ax.set_xlabel("日期", fontproperties=_get_zh_font())
    ax.set_ylabel("学习时长 (分钟)", fontproperties=_get_zh_font())
    ax.set_title("每日学习时长", fontproperties=_get_zh_font(), fontsize=14)

    # 在柱子上标数字
    for bar, val in zip(bars, minutes):
        if val > 0:
            ax.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() + 0.5,
                str(val),
                ha="center", va="bottom", fontsize=8,
            )

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=dpi)
    plt.close(fig)
    buf.seek(0)
    return buf


def generate_accuracy_curve(
    daily_stats: list[dict[str, Any]],
    dpi: int = 100,
) -> io.BytesIO:
    """
    生成正确率曲线图

    Args:
        daily_stats: [{"date": "...", "accuracy": 0.85}, ...]

    Returns:
        BytesIO 图片数据
    """
    _set_zh_style()

    fig, ax = plt.subplots(figsize=(8, 4))

    dates = [d.get("date", "")[-5:] for d in daily_stats]
    accuracy = [d.get("accuracy", 0) * 100 for d in daily_stats]

    ax.plot(dates, accuracy, marker="o", color="#E74C3C", linewidth=2, markersize=6)
    ax.set_xlabel("日期", fontproperties=_get_zh_font())
    ax.set_ylabel("正确率 (%)", fontproperties=_get_zh_font())
    ax.set_title("正确率趋势", fontproperties=_get_zh_font(), fontsize=14)
    ax.set_ylim(0, 105)
    ax.axhline(y=60, color="gray", linestyle="--", alpha=0.5, label="60% 基准线")
    ax.legend(prop=_get_zh_font())

    # 在点上标数字
    for i, val in enumerate(accuracy):
        ax.annotate(
            f"{val:.0f}%",
            (dates[i], val),
            textcoords="offset points",
            xytext=(0, 10),
            ha="center",
            fontsize=8,
        )

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=dpi)
    plt.close(fig)
    buf.seek(0)
    return buf


def generate_knowledge_radar(
    subject_breakdown: list[dict[str, Any]],
    dpi: int = 100,
) -> io.BytesIO:
    """
    生成知识雷达图（学科正确率蜘蛛网图）

    Args:
        subject_breakdown: [{"subject": "math", "accuracy": 0.8}, ...]

    Returns:
        BytesIO 图片数据
    """
    _set_zh_style()

    if not subject_breakdown:
        # 空数据返回简单占位图
        fig, ax = plt.subplots(figsize=(5, 5))
        ax.text(0.5, 0.5, "暂无数据", ha="center", va="center",
                fontsize=16, fontproperties=_get_zh_font())
        ax.set_xlim(0, 1)
        ax.set_ylim(0, 1)
        plt.tight_layout()
        buf = io.BytesIO()
        fig.savefig(buf, format="png", dpi=dpi)
        plt.close(fig)
        buf.seek(0)
        return buf

    subjects = [d.get("subject", "") for d in subject_breakdown]
    values = [d.get("accuracy", 0) * 100 for d in subject_breakdown]

    # 学科名中文化
    subject_labels = {
        "math": "数学", "physics": "物理", "chemistry": "化学",
        "biology": "生物", "chinese": "语文", "english": "英语",
    }
    labels = [subject_labels.get(s, s) for s in subjects]

    num_vars = len(labels)
    angles = np.linspace(0, 2 * np.pi, num_vars, endpoint=False).tolist()
    values += values[:1]  # 闭合
    angles += angles[:1]

    fig, ax = plt.subplots(figsize=(5, 5), subplot_kw=dict(polar=True))
    ax.plot(angles, values, "o-", linewidth=2, color="#2ECC71")
    ax.fill(angles, values, alpha=0.25, color="#2ECC71")
    ax.set_xticks(angles[:-1])
    safe_labels: list[str] = [str(l) if l else "" for l in labels]
    ax.set_xticklabels(safe_labels, fontproperties=_get_zh_font())
    ax.set_ylim(0, 100)
    ax.set_title("知识掌握雷达图", fontproperties=_get_zh_font(), fontsize=14, pad=20)

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=dpi)
    plt.close(fig)
    buf.seek(0)
    return buf


def generate_wrong_answer_pie(
    weak_areas: list[dict[str, Any]],
    dpi: int = 100,
) -> io.BytesIO:
    """
    生成错题饼图（薄弱学科/知识点分布）

    Args:
        weak_areas: [{"topic": "分数运算", "mastery": 0.3, "subject": "math"}, ...]

    Returns:
        BytesIO 图片数据
    """
    _set_zh_style()

    if not weak_areas:
        fig, ax = plt.subplots(figsize=(5, 5))
        ax.text(0.5, 0.5, "暂无薄弱知识点", ha="center", va="center",
                fontsize=14, fontproperties=_get_zh_font())
        ax.set_xlim(0, 1)
        ax.set_ylim(0, 1)
        plt.tight_layout()
        buf = io.BytesIO()
        fig.savefig(buf, format="png", dpi=dpi)
        plt.close(fig)
        buf.seek(0)
        return buf

    # 按学科聚合
    subject_errors: dict[str, int] = {}
    for w in weak_areas:
        subj = w.get("subject", "unknown")
        # 掌握度越低，错误权重越高
        weight = int((1 - w.get("mastery", 0.5)) * 10) + 1
        subject_errors[subj] = subject_errors.get(subj, 0) + weight

    subject_labels = {
        "math": "数学", "physics": "物理", "chemistry": "化学",
        "biology": "生物", "chinese": "语文", "english": "英语",
    }
    labels = [subject_labels.get(s, s) for s in subject_errors.keys()]
    sizes = list(subject_errors.values())
    colors = plt.colormaps["Set3"](np.linspace(0, 1, len(labels)))

    fig, ax = plt.subplots(figsize=(5, 5))
    wedges, texts, autotexts = ax.pie(
        sizes, labels=labels, autopct="%1.1f%%",
        colors=colors, startangle=90,
        textprops={"fontproperties": _get_zh_font()} if _get_zh_font() else {},
    )
    ax.set_title("薄弱知识点分布", fontproperties=_get_zh_font(), fontsize=14)

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=dpi)
    plt.close(fig)
    buf.seek(0)
    return buf
