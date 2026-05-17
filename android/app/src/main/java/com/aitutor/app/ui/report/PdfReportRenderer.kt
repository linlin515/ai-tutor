package com.aitutor.app.ui.report

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.aitutor.app.domain.model.ReportData
import com.aitutor.app.domain.model.ReportType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF 渲染引擎 - 使用 Android 原生 PdfDocument API 逐页 Canvas 绘制学习报告。
 * 
 * 设计规范：
 * - A4 尺寸: 595 × 842 pt
 * - 边距: 40pt 左右, 36pt 上下
 * - 主色: #1565C0 (蓝色系)
 * - 内容区域宽度: 515pt
 */
@Singleton
class PdfReportRenderer @Inject constructor() {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 40
        private const val MARGIN_TOP = 36
        private const val MARGIN_RIGHT = 40
        private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

        private val COLOR_PRIMARY = Color.parseColor("#1565C0")
        private val COLOR_PRIMARY_LIGHT = Color.parseColor("#E3F2FD")
        private val COLOR_ACCENT = Color.parseColor("#FF6F00")
        private val COLOR_TEXT_PRIMARY = Color.parseColor("#212121")
        private val COLOR_TEXT_SECONDARY = Color.parseColor("#757575")
        private val COLOR_DIVIDER = Color.parseColor("#E0E0E0")
        private val COLOR_CARD_BG = Color.parseColor("#F5F5F5")
        private val COLOR_GREEN = Color.parseColor("#4CAF50")
        private val COLOR_ORANGE = Color.parseColor("#FF9800")
        private val COLOR_RED = Color.parseColor("#F44336")
    }

    fun render(reportData: ReportData): PdfDocument {
        val document = PdfDocument()

        // Page 1: Cover page
        drawCoverPage(document, reportData)

        // Page 2: Overview
        drawOverviewPage(document, reportData)

        // Page 3: Learning trend (bar chart)
        drawTrendChartPage(document, reportData)

        // Page 4: Wrong answers & achievements
        drawWrongAnswersPage(document, reportData)

        // Page 5: Subject distribution & summary
        drawSubjectDistributionPage(document, reportData)

        return document
    }

    // ===== Page 1: Cover =====

    private fun drawCoverPage(document: PdfDocument, data: ReportData) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background header area
        val headerPaint = Paint().apply {
            color = COLOR_PRIMARY
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 280f, headerPaint)

        // White circle decoration
        val circlePaint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawCircle(480f, 60f, 120f, circlePaint)
        canvas.drawCircle(80f, 220f, 80f, circlePaint)

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("📊 学习报告", MARGIN_LEFT.toFloat(), 150f, titlePaint)

        // Report type
        val subtitlePaint = Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 20f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        val reportTypeLabel = when (data.reportType) {
            ReportType.WEEKLY -> "本周报告"
            ReportType.MONTHLY -> "本月报告"
        }
        canvas.drawText(reportTypeLabel, MARGIN_LEFT.toFloat(), 182f, subtitlePaint)

        // Period
        canvas.drawText(data.period, MARGIN_LEFT.toFloat(), 212f, subtitlePaint)

        // User info area (below header)
        val nicknamePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 28f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        val nameText = if (data.nickname.isNotBlank()) data.nickname else "AI 学伴用户"
        canvas.drawText(nameText, MARGIN_LEFT.toFloat(), 330f, nicknamePaint)

        // Generation date
        val datePaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        val dateLabel = "报告生成日期: ${data.generationDate}"
        canvas.drawText(dateLabel, MARGIN_LEFT.toFloat(), 358f, datePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), 380f, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), 380f, dividerPaint)

        // Quick stats row
        drawStatCard(canvas, 0, "📅", "总活跃天数", "${data.totalActiveDays}天", 400f)
        drawStatCard(canvas, 1, "💬", "总对话数", "${data.totalConversations}", 400f)
        drawStatCard(canvas, 2, "🔥", "最长连胜", "${data.streakDays}天", 400f)
        drawStatCard(canvas, 3, "📝", "今日学习", "${data.todayMessages}条", 400f)

        // Footer
        val footerPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("由 AI 学伴 App 生成", MARGIN_LEFT.toFloat(), 810f, footerPaint)

        document.finishPage(page)
    }

    // ===== Page 2: Learning Overview =====

    private fun drawOverviewPage(document: PdfDocument, data: ReportData) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Title
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("📈 学习概览", MARGIN_LEFT.toFloat(), MARGIN_TOP + 24f, titlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), MARGIN_TOP + 36f, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), MARGIN_TOP + 36f, dividerPaint)

        // Section 1: Study time & accuracy
        var yPos = MARGIN_TOP + 60f

        // Card: Study duration
        val cardPaint = Paint().apply {
            color = COLOR_CARD_BG
            isAntiAlias = true
        }
        val cardRect = RectF(
            MARGIN_LEFT.toFloat(), yPos,
            (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos + 80f
        )
        canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

        val sectionTitlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("⏱ 学习时长", MARGIN_LEFT + 12f, yPos + 24f, sectionTitlePaint)

        val valuePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 28f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        val hours = data.totalStudyMinutes / 60
        val mins = data.totalStudyMinutes % 60
        val timeStr = if (hours > 0) "${hours}小时${mins}分钟" else "${mins}分钟"
        canvas.drawText(timeStr, MARGIN_LEFT + 12f, yPos + 62f, valuePaint)

        yPos += 96f

        // Card: Solving stats
        val cardRect2 = RectF(
            MARGIN_LEFT.toFloat(), yPos,
            (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos + 100f
        )
        canvas.drawRoundRect(cardRect2, 8f, 8f, cardPaint)

        canvas.drawText("📝 解题统计", MARGIN_LEFT + 12f, yPos + 24f, sectionTitlePaint)

        val detailPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("解题总数: ${data.totalSolved} 题", MARGIN_LEFT + 12f, yPos + 48f, detailPaint)
        canvas.drawText("正确: ${data.totalCorrect} 题  |  错误: ${data.totalWrong} 题", MARGIN_LEFT + 12f, yPos + 68f, detailPaint)

        // Accuracy bar
        val accuracyColor = when {
            data.accuracyRate >= 80f -> COLOR_GREEN
            data.accuracyRate >= 60f -> COLOR_ORANGE
            else -> COLOR_RED
        }
        val accValuePaint = Paint().apply {
            color = accuracyColor
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("正确率: ${"%.1f".format(data.accuracyRate)}%", MARGIN_LEFT + 12f, yPos + 90f, accValuePaint)

        // Accuracy bar background
        val barBgPaint = Paint().apply {
            color = COLOR_DIVIDER
        }
        val barLeft = MARGIN_LEFT + 160f
        val barTop = yPos + 80f
        val barWidth = CONTENT_WIDTH - 172f
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + 8f, barBgPaint)

        // Accuracy bar fill
        val barFillPaint = Paint().apply {
            color = accuracyColor
        }
        val fillWidth = barWidth * (data.accuracyRate / 100f)
        canvas.drawRect(barLeft, barTop, barLeft + fillWidth, barTop + 8f, barFillPaint)

        yPos += 116f

        // Mastery card
        val cardRect3 = RectF(
            MARGIN_LEFT.toFloat(), yPos,
            (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos + 80f
        )
        canvas.drawRoundRect(cardRect3, 8f, 8f, cardPaint)

        canvas.drawText("🎯 知识点掌握度", MARGIN_LEFT + 12f, yPos + 24f, sectionTitlePaint)
        canvas.drawText(
            "已掌握 ${data.masteredKnowledgePoints} / ${data.totalKnowledgePoints} 个知识点",
            MARGIN_LEFT + 12f, yPos + 48f, detailPaint
        )

        val masteryColor = when {
            data.masteryRate >= 70f -> COLOR_GREEN
            data.masteryRate >= 40f -> COLOR_ORANGE
            else -> COLOR_RED
        }
        val masteryValuePaint = Paint().apply {
            color = masteryColor
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("掌握率: ${"%.1f".format(data.masteryRate)}%", MARGIN_LEFT + 12f, yPos + 72f, masteryValuePaint)

        yPos += 96f

        // Streak card
        val cardRect4 = RectF(
            MARGIN_LEFT.toFloat(), yPos,
            (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos + 60f
        )
        canvas.drawRoundRect(cardRect4, 8f, 8f, cardPaint)

        canvas.drawText("🔥 连续学习", MARGIN_LEFT + 12f, yPos + 24f, sectionTitlePaint)

        val streakValuePaint = Paint().apply {
            color = COLOR_ACCENT
            textSize = 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("最长连续学习 ${data.streakDays} 天", MARGIN_LEFT + 12f, yPos + 52f, streakValuePaint)

        // Footer
        val footerPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("第 2 页 | 由 AI 学伴 App 生成", MARGIN_LEFT.toFloat(), 820f, footerPaint)

        document.finishPage(page)
    }

    // ===== Page 3: Learning Trend (Bar Chart) =====

    private fun drawTrendChartPage(document: PdfDocument, data: ReportData) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Title
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }

        val trendTitle = when (data.reportType) {
            ReportType.WEEKLY -> "📊 近 7 天学习趋势"
            ReportType.MONTHLY -> "📊 近 30 天学习趋势"
        }
        canvas.drawText(trendTitle, MARGIN_LEFT.toFloat(), MARGIN_TOP + 24f, titlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), MARGIN_TOP + 36f, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), MARGIN_TOP + 36f, dividerPaint)

        // Draw bar chart if we have data
        if (data.dailyStats.isNotEmpty()) {
            val chartLeft = MARGIN_LEFT + 30f
            val chartTop = MARGIN_TOP + 60f
            val chartWidth = CONTENT_WIDTH - 60f
            val chartHeight = 400f
            val chartBottom = chartTop + chartHeight

            // Y-axis
            val axisPaint = Paint().apply {
                color = COLOR_TEXT_SECONDARY
                strokeWidth = 1f
            }
            canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
            canvas.drawLine(chartLeft, chartBottom, chartLeft + chartWidth, chartBottom, axisPaint)

            // Find max value for scaling
            val maxMsgCount = data.dailyStats.maxOfOrNull { it.msgCount } ?: 1

            // Draw bars
            val barCount = data.dailyStats.size
            val totalBarArea = chartWidth - 20f
            val barSpacing = 4f
            val barWidth = (totalBarArea - barSpacing * (barCount - 1)) / barCount

            val barFillPaint = Paint().apply {
                color = COLOR_PRIMARY
                isAntiAlias = true
            }
            val barLabelPaint = Paint().apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 10f
                isAntiAlias = true
            }
            val barValuePaint = Paint().apply {
                color = COLOR_PRIMARY
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }

            data.dailyStats.forEachIndexed { index, stat ->
                val barHeight = if (maxMsgCount > 0) {
                    (stat.msgCount.toFloat() / maxMsgCount) * (chartHeight - 30f)
                } else 0f

                val barX = chartLeft + 10f + index * (barWidth + barSpacing)
                val barY = chartBottom - barHeight

                // Draw bar
                canvas.drawRoundRect(
                    barX, barY, barX + barWidth, chartBottom,
                    3f, 3f, barFillPaint
                )

                // Draw value on top
                if (stat.msgCount > 0) {
                    canvas.drawText(
                        "${stat.msgCount}",
                        barX + 2f, barY - 6f, barValuePaint
                    )
                }

                // Draw date label (show only last 2 chars of day for weekly, or date for monthly)
                val label = when (data.reportType) {
                    ReportType.WEEKLY -> {
                        // Show MM/DD
                        if (stat.day.length >= 10) stat.day.substring(5) else stat.day
                    }
                    ReportType.MONTHLY -> {
                        // Show MM/DD
                        if (stat.day.length >= 10) stat.day.substring(5) else stat.day
                    }
                }
                canvas.drawText(
                    label,
                    barX - 2f, chartBottom + 14f, barLabelPaint
                )
            }

            // Summary text
            val summaryPaint = Paint().apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 14f
                isAntiAlias = true
            }
            val totalMsgTrend = data.dailyStats.sumOf { it.msgCount }
            val avgDaily = if (data.dailyStats.isNotEmpty()) totalMsgTrend / data.dailyStats.size else 0
            canvas.drawText("统计周期内总消息数: $totalMsgTrend 条", MARGIN_LEFT.toFloat(), chartBottom + 50f, summaryPaint)
            canvas.drawText("日均消息数: $avgDaily 条", MARGIN_LEFT.toFloat(), chartBottom + 72f, summaryPaint)
        } else {
            val noDataPaint = Paint().apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 18f
                isAntiAlias = true
            }
            canvas.drawText("暂无趋势数据", PAGE_WIDTH / 2f - 60f, PAGE_HEIGHT / 2f, noDataPaint)
        }

        // Footer
        val footerPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("第 3 页 | 由 AI 学伴 App 生成", MARGIN_LEFT.toFloat(), 820f, footerPaint)

        document.finishPage(page)
    }

    // ===== Page 4: Wrong Answers & Achievements =====

    private fun drawWrongAnswersPage(document: PdfDocument, data: ReportData) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 4).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Title
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("❌ 错题汇总", MARGIN_LEFT.toFloat(), MARGIN_TOP + 24f, titlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), MARGIN_TOP + 36f, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), MARGIN_TOP + 36f, dividerPaint)

        val detailPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }

        var yPos = MARGIN_TOP + 56f

        // Top wrong answers
        if (data.topWrongAnswers.isNotEmpty()) {
            val sectionTitlePaint = Paint().apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 16f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("高频错题 TOP5", MARGIN_LEFT.toFloat(), yPos, sectionTitlePaint)
            yPos += 28f

            data.topWrongAnswers.forEachIndexed { index, answer ->
                val itemPaint = Paint().apply {
                    color = COLOR_TEXT_PRIMARY
                    textSize = 13f
                    isAntiAlias = true
                }
                val subjectColor = when (answer.subject) {
                    "数学" -> COLOR_PRIMARY
                    "物理" -> COLOR_GREEN
                    "化学" -> COLOR_ORANGE
                    "英语" -> COLOR_RED
                    else -> COLOR_TEXT_PRIMARY
                }
                val subjectPaint = Paint().apply {
                    color = subjectColor
                    textSize = 13f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    isAntiAlias = true
                }

                val text = "${index + 1}. ${answer.question.take(30)}"
                val suffix = if (answer.question.length > 30) "..." else ""
                canvas.drawText(text + suffix, MARGIN_LEFT.toFloat(), yPos, itemPaint)

                // Subject tag
                canvas.drawText("[${answer.subject}]", MARGIN_LEFT + CONTENT_WIDTH - 120f, yPos, subjectPaint)

                // Wrong count badge
                val badgePaint = Paint().apply {
                    color = Color.parseColor("#FFEBEE")
                    isAntiAlias = true
                }
                canvas.drawRoundRect(
                    MARGIN_LEFT + CONTENT_WIDTH - 70f, yPos - 11f,
                    (MARGIN_LEFT + CONTENT_WIDTH).toFloat(), yPos + 4f,
                    10f, 10f, badgePaint
                )
                val countPaint = Paint().apply {
                    color = COLOR_RED
                    textSize = 11f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("错${answer.wrongCount}次", MARGIN_LEFT + CONTENT_WIDTH - 66f, yPos, countPaint)

                yPos += 26f
            }

            yPos += 10f
        } else {
            val emptyPaint = Paint().apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 14f
                isAntiAlias = true
            }
            canvas.drawText("暂无错题记录，继续保持！", MARGIN_LEFT.toFloat(), yPos, emptyPaint)
            yPos += 24f
        }

        // Divider
        val hDivider = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), yPos, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos, hDivider)
        yPos += 16f

        // Achievements section
        val sectionTitlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("🏆 学习之星评价", MARGIN_LEFT.toFloat(), yPos, sectionTitlePaint)
        yPos += 28f

        // Generate commentary based on data
        val commentary = generateCommentary(data)
        val commentaryPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 13f
            isAntiAlias = true
        }

        // Word wrap commentary
        val words = commentary.toCharArray()
        val lineWidth = CONTENT_WIDTH - 20f
        val lineHeight = 20f
        var line = StringBuilder()
        var lineCount = 0

        for (char in words) {
            val testLine = line.toString() + char
            val textWidth = commentaryPaint.measureText(testLine)
            if (textWidth > lineWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), MARGIN_LEFT.toFloat(), yPos, commentaryPaint)
                yPos += lineHeight
                lineCount++
                line = StringBuilder().append(char)
            } else {
                line.append(char)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), MARGIN_LEFT.toFloat(), yPos, commentaryPaint)
            yPos += lineHeight
        }

        // Footer
        val footerPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("第 4 页 | 由 AI 学伴 App 生成", MARGIN_LEFT.toFloat(), 820f, footerPaint)

        document.finishPage(page)
    }

    // ===== Page 5: Subject Distribution & Final Summary =====

    private fun drawSubjectDistributionPage(document: PdfDocument, data: ReportData) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 5).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Title
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("📚 学科分布与摘要", MARGIN_LEFT.toFloat(), MARGIN_TOP + 24f, titlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), MARGIN_TOP + 36f, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), MARGIN_TOP + 36f, dividerPaint)

        val sectionTitlePaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        val detailPaint = Paint().apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }

        var yPos = MARGIN_TOP + 56f

        // Subject distribution
        canvas.drawText("错题学科分布", MARGIN_LEFT.toFloat(), yPos, sectionTitlePaint)
        yPos += 28f

        if (data.subjectDistribution.isNotEmpty()) {
            val barBgPaint = Paint().apply {
                color = COLOR_CARD_BG
            }
            val subjectColors = listOf(COLOR_PRIMARY, COLOR_GREEN, COLOR_ORANGE, COLOR_RED,
                Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"))
            val barWidth = CONTENT_WIDTH - 100f

            data.subjectDistribution.forEachIndexed { index, subject ->
                if (index >= 6) return@forEachIndexed // Max 6 subjects

                val subjectPaint = Paint().apply {
                    color = COLOR_TEXT_PRIMARY
                    textSize = 13f
                    isAntiAlias = true
                }
                canvas.drawText(subject.subject, MARGIN_LEFT.toFloat(), yPos, subjectPaint)

                // Background bar
                canvas.drawRect(
                    MARGIN_LEFT + 80f, yPos - 8f,
                    MARGIN_LEFT + 80f + barWidth, yPos + 2f,
                    barBgPaint
                )

                // Fill bar
                val fillWidth = barWidth * (subject.percentage / 100f)
                val fillColor = subjectColors[index % subjectColors.size]
                val fillPaint = Paint().apply {
                    color = fillColor
                }
                canvas.drawRect(
                    MARGIN_LEFT + 80f, yPos - 8f,
                    MARGIN_LEFT + 80f + fillWidth, yPos + 2f,
                    fillPaint
                )

                // Percentage
                val pctPaint = Paint().apply {
                    color = fillColor
                    textSize = 12f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("${"%.1f".format(subject.percentage)}%",
                    MARGIN_LEFT + 85f + barWidth, yPos, pctPaint)

                yPos += 22f
            }
        } else {
            canvas.drawText("暂无学科分布数据", MARGIN_LEFT.toFloat(), yPos, detailPaint)
            yPos += 24f
        }

        yPos += 16f

        // Divider
        val hDivider = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_LEFT.toFloat(), yPos, (PAGE_WIDTH - MARGIN_RIGHT).toFloat(), yPos, hDivider)
        yPos += 16f

        // Final summary
        canvas.drawText("📋 综合摘要", MARGIN_LEFT.toFloat(), yPos, sectionTitlePaint)
        yPos += 28f

        val summaryItems = listOf(
            "本${if (data.reportType == ReportType.WEEKLY) "周" else "月"}共学习了 ${data.totalStudyMinutes} 分钟",
            "解题 ${data.totalSolved} 题，正确率 ${"%.1f".format(data.accuracyRate)}%",
            "已掌握 ${data.masteredKnowledgePoints}/${data.totalKnowledgePoints} 个知识点",
            "累计 ${data.totalConversations} 次对话，最长连续学习 ${data.streakDays} 天",
            if (data.accuracyRate >= 80f) "🎉 继续保持！学习效果非常棒！" else "💪 继续加油，多练习提高正确率！"
        )

        for (item in summaryItems) {
            canvas.drawText("• $item", MARGIN_LEFT.toFloat(), yPos, detailPaint)
            yPos += 22f
        }

        // Footer
        val footerPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("第 5 页 | 由 AI 学伴 App 生成", MARGIN_LEFT.toFloat(), 820f, footerPaint)

        document.finishPage(page)
    }

    // ===== Helper methods =====

    private fun drawStatCard(canvas: Canvas, index: Int, icon: String, label: String, value: String, yPos: Float) {
        val cardWidth = (CONTENT_WIDTH - 12f) / 2f
        val xPos = MARGIN_LEFT + index * (cardWidth + 12f)

        // Calculate row
        val row = index / 2
        val col = index % 2
        val cardX = MARGIN_LEFT + col * (cardWidth + 12f)
        val cardY = yPos + row * 70f

        // Card background
        val cardPaint = Paint().apply {
            color = COLOR_CARD_BG
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardX, cardY, cardX + cardWidth, cardY + 60f, 8f, 8f, cardPaint)

        // Label
        val labelPaint = Paint().apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText(icon + "  " + label, cardX + 8f, cardY + 22f, labelPaint)

        // Value
        val valuePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 20f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, cardX + 8f, cardY + 50f, valuePaint)
    }

    private fun generateCommentary(data: ReportData): String {
        val parts = mutableListOf<String>()

        if (data.accuracyRate >= 90f) {
            parts.add("学习之星🌟 你的表现令人惊叹！")
        } else if (data.accuracyRate >= 80f) {
            parts.add("优秀学习者🎯 继续保持这个势头！")
        } else if (data.accuracyRate >= 60f) {
            parts.add("稳健进步中📈 有提升空间，继续加油！")
        } else {
            parts.add("学习是一场马拉松🏃 多回顾错题，稳步提升！")
        }

        if (data.streakDays >= 7) {
            parts.add("连续学习${data.streakDays}天，自律性超强！")
        } else if (data.streakDays >= 3) {
            parts.add("已连续学习${data.streakDays}天，保持良好的学习节奏！")
        }

        if (data.totalSolved > 50) {
            parts.add("解题量丰富，积累了大量的实战经验。")
        }

        return parts.joinToString(" ")
    }
}
