"""
AI 学伴后端 - 学习报告导出服务
PDF生成(ReportLab 7章节) + CSV生成(UTF-8 BOM)
"""
from __future__ import annotations

import csv
import io
import logging
import os
from datetime import date, timedelta
from typing import Any

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch, mm
from reportlab.platypus import (
    Image,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)
from sqlalchemy import cast, Date, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.question_record import QuestionRecord
from app.models.quiz_record import QuizRecord
from app.models.wrong_answer import WrongAnswer
from app.services.report_charts import (
    generate_accuracy_curve,
    generate_daily_bar_chart,
    generate_knowledge_radar,
    generate_wrong_answer_pie,
)

logger = logging.getLogger(__name__)


def _get_period_range(period: str, today: date | None = None) -> tuple[date, date]:
    """
    根据周期返回起止日期

    Returns:
        (start_date, end_date)
    """
    if today is None:
        today = date.today()

    if period == "today":
        return today, today
    elif period == "week":
        return today - timedelta(days=6), today
    elif period == "month":
        return today - timedelta(days=29), today
    else:  # all
        return today - timedelta(days=90), today


async def _collect_stats(
    user_id: str,
    start_date: date,
    end_date: date,
    subject: str,
    db: AsyncSession,
) -> dict[str, Any]:
    """
    收集统计数据供报告使用
    """
    today = date.today()

    # --- 今日统计 ---
    today_q_count = (
        await db.execute(
            select(func.count()).select_from(QuestionRecord).where(
                QuestionRecord.user_id == user_id,
                cast(QuestionRecord.created_at, Date) == today,
            )
        )
    ).scalar() or 0

    today_wrong = (
        await db.execute(
            select(func.count()).select_from(WrongAnswer).where(
                WrongAnswer.user_id == user_id,
                cast(WrongAnswer.created_at, Date) == today,
            )
        )
    ).scalar() or 0

    total_today = today_q_count
    today_accuracy = 0.0
    if total_today > 0:
        correct_est = max(0, total_today - today_wrong)
        today_accuracy = round(correct_est / total_today, 2)

    # --- 周期内统计 ---
    q_count = (
        await db.execute(
            select(func.count()).select_from(QuestionRecord).where(
                QuestionRecord.user_id == user_id,
                cast(QuestionRecord.created_at, Date) >= start_date,
                cast(QuestionRecord.created_at, Date) <= end_date,
            )
        )
    ).scalar() or 0

    wrong_count = (
        await db.execute(
            select(func.count()).select_from(WrongAnswer).where(
                WrongAnswer.user_id == user_id,
                cast(WrongAnswer.created_at, Date) >= start_date,
                cast(WrongAnswer.created_at, Date) <= end_date,
            )
        )
    ).scalar() or 0

    period_accuracy = 0.0
    if q_count > 0:
        correct_est = max(0, q_count - wrong_count)
        period_accuracy = round(correct_est / q_count, 2)

    # --- 每日统计 ---
    daily_stats = []
    current = start_date
    while current <= end_date:
        d_q = (
            await db.execute(
                select(func.count()).select_from(QuestionRecord).where(
                    QuestionRecord.user_id == user_id,
                    cast(QuestionRecord.created_at, Date) == current,
                )
            )
        ).scalar() or 0

        d_w = (
            await db.execute(
                select(func.count()).select_from(WrongAnswer).where(
                    WrongAnswer.user_id == user_id,
                    cast(QuestionRecord.created_at, Date) == current,
                )
            )
        ).scalar() or 0

        d_acc = 0.0
        if d_q > 0:
            d_acc = round(max(0, d_q - d_w) / d_q, 2)

        daily_stats.append({
            "date": current.isoformat(),
            "study_minutes": d_q * 3,
            "questions_solved": d_q,
            "accuracy": d_acc,
        })
        current += timedelta(days=1)

    # --- 学科细分 ---
    subjects_list = ["math", "physics", "chemistry", "biology", "chinese", "english"]
    subject_breakdown = []
    for subj in subjects_list:
        sq = (
            await db.execute(
                select(func.count()).select_from(QuestionRecord).where(
                    QuestionRecord.user_id == user_id,
                    QuestionRecord.subject == subj,
                    cast(QuestionRecord.created_at, Date) >= start_date,
                    cast(QuestionRecord.created_at, Date) <= end_date,
                )
            )
        ).scalar() or 0

        sw = (
            await db.execute(
                select(func.count()).select_from(WrongAnswer).where(
                    WrongAnswer.user_id == user_id,
                    WrongAnswer.subject == subj,
                    cast(WrongAnswer.created_at, Date) >= start_date,
                    cast(WrongAnswer.created_at, Date) <= end_date,
                )
            )
        ).scalar() or 0

        s_acc = 0.0
        if sq > 0:
            s_acc = round(max(0, sq - sw) / sq, 2)

        if sq > 0:
            subject_breakdown.append({
                "subject": subj,
                "questions_solved": sq,
                "accuracy": s_acc,
                "study_minutes": sq * 3,
            })

    # --- 薄弱环节 ---
    result = await db.execute(
        select(
            WrongAnswer.topic,
            WrongAnswer.subject,
            func.avg(WrongAnswer.mastery_score).label("avg_mastery"),
        )
        .where(
            WrongAnswer.user_id == user_id,
            WrongAnswer.topic.isnot(None),
        )
        .group_by(WrongAnswer.topic, WrongAnswer.subject)
        .having(func.avg(WrongAnswer.mastery_score) < 0.6)
    )
    rows = result.all()
    weak_areas = [
        {
            "topic": row.topic,
            "mastery": round(float(row.avg_mastery), 2),
            "subject": row.subject,
        }
        for row in rows
        if row.topic
    ]

    # --- 知识点掌握 ---
    total_kp = len(weak_areas) + max(0, (
        await db.execute(
            select(func.count(func.distinct(WrongAnswer.topic))).where(
                WrongAnswer.user_id == user_id,
                WrongAnswer.mastery_score >= 0.6,
            )
        )
    ).scalar() or 0)

    mastered_kp = (
        await db.execute(
            select(func.count(func.distinct(WrongAnswer.topic))).where(
                WrongAnswer.user_id == user_id,
                WrongAnswer.mastery_score >= 0.6,
            )
        )
    ).scalar() or 0

    return {
        "today_questions": total_today,
        "today_accuracy": today_accuracy,
        "period_questions": q_count,
        "period_accuracy": period_accuracy,
        "daily_stats": daily_stats,
        "subject_breakdown": subject_breakdown,
        "weak_areas": weak_areas,
        "total_knowledge_points": total_kp + mastered_kp,  # approximate
        "mastered_points": mastered_kp,
        "mastery_rate": round(mastered_kp / max(1, total_kp + mastered_kp), 2),
        "study_minutes": q_count * 3,
        "total_wrong_answers": wrong_count,
    }


def _get_subject_cn(subject: str) -> str:
    """学科英文转中文"""
    mapping = {
        "math": "数学", "physics": "物理", "chemistry": "化学",
        "biology": "生物", "chinese": "语文", "english": "英语",
    }
    return mapping.get(subject, subject)


# ============================================================
# PDF 生成（ReportLab）
# ============================================================

def generate_pdf_report(
    stats: dict[str, Any],
    period: str,
    subject: str,
    charts_dpi: int = 100,
    max_size_mb: float = 10.0,
) -> io.BytesIO:
    """
    生成 7 章节 PDF 学习报告

    章节：
    1. 报告概要
    2. 每日时长柱状图
    3. 正确率曲线
    4. 知识雷达图
    5. 错题饼图
    6. 薄弱点列表
    7. 学习建议
    """
    buf = io.BytesIO()

    doc = SimpleDocTemplate(
        buf,
        pagesize=A4,
        topMargin=20 * mm,
        bottomMargin=20 * mm,
        leftMargin=15 * mm,
        rightMargin=15 * mm,
    )

    styles = getSampleStyleSheet()
    # 自定义中文字体样式
    title_style = ParagraphStyle(
        "ZhTitle",
        parent=styles["Title"],
        fontSize=22,
        leading=28,
        spaceAfter=12,
    )
    heading_style = ParagraphStyle(
        "ZhHeading",
        parent=styles["Heading2"],
        fontSize=14,
        leading=20,
        spaceBefore=16,
        spaceAfter=8,
    )
    normal_style = ParagraphStyle(
        "ZhNormal",
        parent=styles["Normal"],
        fontSize=10,
        leading=16,
        spaceAfter=6,
    )
    small_style = ParagraphStyle(
        "ZhSmall",
        parent=styles["Normal"],
        fontSize=9,
        leading=14,
        spaceAfter=4,
    )

    elements: list = []

    # --- 封面/标题 ---
    period_names = {"today": "今日", "week": "本周", "month": "本月", "all": "全部"}
    period_cn = period_names.get(period, period)
    elements.append(Paragraph("AI 学伴学习报告", title_style))
    elements.append(Paragraph(f"统计周期：{period_cn}", normal_style))
    elements.append(Spacer(1, 12))

    # === 章节 1: 报告概要 ===
    elements.append(Paragraph("一、报告概要", heading_style))
    overview_data = [
        ["统计周期", period_cn],
        ["学科过滤", _get_subject_cn(subject) if subject != "all" else "全部"],
        ["学习时长", f'{stats["study_minutes"]} 分钟'],
        ["答题总数", f'{stats["period_questions"]} 题'],
        ["周期正确率", f'{stats["period_accuracy"] * 100:.1f}%'],
        ["今日正确率", f'{stats["today_accuracy"] * 100:.1f}%'],
        ["知识点总数", f'{stats["total_knowledge_points"]} 个'],
        ["已掌握", f'{stats["mastered_points"]} 个'],
        ["掌握率", f'{stats["mastery_rate"] * 100:.1f}%'],
        ["错题总数", f'{stats["total_wrong_answers"]} 题'],
    ]
    t = Table(overview_data, colWidths=[120, 300])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), colors.HexColor("#F0F0F0")),
        ("TEXTCOLOR", (0, 0), (-1, -1), colors.HexColor("#333333")),
        ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 0), (-1, -1), 10),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CCCCCC")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("PADDING", (0, 0), (-1, -1), 6),
    ]))
    elements.append(t)
    elements.append(Spacer(1, 12))

    # === 章节 2: 每日时长柱状图 ===
    elements.append(Paragraph("二、每日学习时长", heading_style))
    try:
        chart_buf = generate_daily_bar_chart(stats["daily_stats"], dpi=charts_dpi)
        img = Image(chart_buf, width=6 * inch, height=3 * inch)
        elements.append(img)
    except Exception as e:
        logger.warning("生成每日时长柱状图失败: %s", e)
        elements.append(Paragraph("（图表生成失败）", normal_style))
    elements.append(Spacer(1, 8))

    # === 章节 3: 正确率曲线 ===
    elements.append(Paragraph("三、正确率趋势", heading_style))
    try:
        chart_buf = generate_accuracy_curve(stats["daily_stats"], dpi=charts_dpi)
        img = Image(chart_buf, width=6 * inch, height=3 * inch)
        elements.append(img)
    except Exception as e:
        logger.warning("生成正确率曲线失败: %s", e)
        elements.append(Paragraph("（图表生成失败）", normal_style))
    elements.append(Spacer(1, 8))

    # === 章节 4: 知识雷达图 ===
    elements.append(Paragraph("四、知识掌握雷达图", heading_style))
    try:
        chart_buf = generate_knowledge_radar(stats["subject_breakdown"], dpi=charts_dpi)
        img = Image(chart_buf, width=4 * inch, height=4 * inch)
        elements.append(img)
    except Exception as e:
        logger.warning("生成知识雷达图失败: %s", e)
        elements.append(Paragraph("（图表生成失败）", normal_style))
    elements.append(Spacer(1, 8))

    # === 章节 5: 错题饼图 ===
    elements.append(Paragraph("五、薄弱知识点分布", heading_style))
    try:
        chart_buf = generate_wrong_answer_pie(stats["weak_areas"], dpi=charts_dpi)
        img = Image(chart_buf, width=4 * inch, height=4 * inch)
        elements.append(img)
    except Exception as e:
        logger.warning("生成错题饼图失败: %s", e)
        elements.append(Paragraph("（图表生成失败）", normal_style))
    elements.append(Spacer(1, 8))

    # === 章节 6: 薄弱点列表 ===
    elements.append(Paragraph("六、薄弱知识点列表", heading_style))
    if stats["weak_areas"]:
        weak_data = [["序号", "学科", "知识点", "掌握度"]]
        for i, wa in enumerate(stats["weak_areas"], start=1):
            weak_data.append([
                str(i),
                _get_subject_cn(wa["subject"]),
                wa["topic"] or "未知",
                f'{wa["mastery"] * 100:.0f}%',
            ])
        t = Table(weak_data, colWidths=[40, 80, 200, 60])
        t.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#4A90D9")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
            ("FONTSIZE", (0, 0), (-1, -1), 9),
            ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CCCCCC")),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ("PADDING", (0, 0), (-1, -1), 5),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#FFFFFF"),
                                                  colors.HexColor("#F5F8FC")]),
        ]))
        elements.append(t)
    else:
        elements.append(Paragraph("暂无薄弱知识点，继续保持！", normal_style))
    elements.append(Spacer(1, 12))

    # === 章节 7: 学习建议 ===
    elements.append(Paragraph("七、学习建议", heading_style))
    accuracy = stats["period_accuracy"]
    weak_count = len(stats["weak_areas"])

    suggestions = []
    if weak_count == 0:
        suggestions.append("表现优秀！所有知识点均已掌握，建议挑战更高难度的题目。")
    else:
        suggestions.append(f"当前有 {weak_count} 个薄弱知识点需要加强。")
        suggestions.append("建议针对薄弱知识点进行专项练习，每天复习 2-3 道错题。")

    if accuracy < 0.5:
        suggestions.append("正确率偏低，建议先复习基础知识，再做针对性练习。")
    elif accuracy < 0.7:
        suggestions.append("正确率处于中等水平，建议巩固薄弱环节，查漏补缺。")
    else:
        suggestions.append("正确率良好，继续保持！建议适当增加学习量。")

    suggestions.append("")
    suggestions.append("学习小贴士：")
    suggestions.append("• 每天坚持 15-30 分钟学习，效果最佳")
    suggestions.append("• 定期回顾错题本，加深记忆")
    suggestions.append("• 同一知识点间隔 1-3 天再复习，提高掌握度")
    suggestions.append("• 多学科交叉学习，避免疲劳")

    for s in suggestions:
        elements.append(Paragraph(s, normal_style if not s.startswith("•") else small_style))

    # 构建 PDF
    doc.build(elements)
    buf.seek(0)

    # 检查文件大小，如果 > max_size_mb 则降低 DPI 重新生成
    file_size_mb = len(buf.getvalue()) / (1024 * 1024)
    if file_size_mb > max_size_mb and charts_dpi > 60:
        logger.info("PDF 大小 %.1fMB 超过限制 %.1fMB，降低图表质量重试", file_size_mb, max_size_mb)
        return generate_pdf_report(stats, period, subject, charts_dpi=60, max_size_mb=max_size_mb)

    return buf


# ============================================================
# CSV 生成（UTF-8 BOM）
# ============================================================

def generate_csv_report(stats: dict[str, Any]) -> io.BytesIO:
    """
    生成 CSV 格式学习报告（UTF-8 BOM 编码，兼容 Excel）

    包含 3 个工作表（用多段 CSV 合并）：
    - 概要
    - 每日统计
    - 薄弱知识点
    """
    buf = io.BytesIO()
    # UTF-8 BOM
    buf.write(b"\xef\xbb\xbf")

    writer = csv.writer(buf)

    # === Sheet 1: 报告概要 ===
    writer.writerow(["=== 报告概要 ==="])
    writer.writerow(["指标", "值"])
    writer.writerow(["学习时长（分钟）", stats["study_minutes"]])
    writer.writerow(["答题总数", stats["period_questions"]])
    writer.writerow(["周期正确率", f'{stats["period_accuracy"] * 100:.1f}%'])
    writer.writerow(["今日正确率", f'{stats["today_accuracy"] * 100:.1f}%'])
    writer.writerow(["知识点总数", stats["total_knowledge_points"]])
    writer.writerow(["已掌握知识点", stats["mastered_points"]])
    writer.writerow(["掌握率", f'{stats["mastery_rate"] * 100:.1f}%'])
    writer.writerow(["错题总数", stats["total_wrong_answers"]])
    writer.writerow([])  # 空行分隔

    # === Sheet 2: 每日统计 ===
    writer.writerow(["=== 每日学习统计 ==="])
    writer.writerow(["日期", "学习时长(分钟)", "答题数", "正确率"])
    for d in stats["daily_stats"]:
        writer.writerow([
            d["date"],
            d["study_minutes"],
            d["questions_solved"],
            f'{d["accuracy"] * 100:.1f}%',
        ])
    writer.writerow([])

    # === Sheet 3: 薄弱知识点 ===
    writer.writerow(["=== 薄弱知识点 ==="])
    writer.writerow(["序号", "学科", "知识点", "掌握度"])
    for i, wa in enumerate(stats["weak_areas"], start=1):
        writer.writerow([
            i,
            _get_subject_cn(wa["subject"]),
            wa["topic"] or "未知",
            f'{wa["mastery"] * 100:.0f}%',
        ])

    buf.seek(0)
    return buf


# ============================================================
# 公共入口
# ============================================================

async def generate_report(
    user_id: str,
    format: str,
    period: str,
    subject: str,
    db: AsyncSession,
) -> tuple[io.BytesIO, str, str]:
    """
    生成学习报告

    Args:
        user_id: 用户 ID
        format: pdf / csv
        period: today / week / month / all
        subject: 学科过滤
        db: 数据库会话

    Returns:
        (data_bytes, content_type, filename)

    Raises:
        ValueError: 不支持的格式
    """
    start_date, end_date = _get_period_range(period)
    stats = await _collect_stats(user_id, start_date, end_date, subject, db)

    if format == "pdf":
        data = generate_pdf_report(stats, period, subject)
        content_type = "application/pdf"
        filename = f"学习报告_{date.today().isoformat()}.pdf"
    elif format == "csv":
        data = generate_csv_report(stats)
        content_type = "text/csv; charset=utf-8"
        filename = f"学习报告_{date.today().isoformat()}.csv"
    else:
        raise ValueError(f"不支持的导出格式: {format}")

    return data, content_type, filename
