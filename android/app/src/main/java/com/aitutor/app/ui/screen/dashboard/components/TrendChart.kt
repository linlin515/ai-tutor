package com.aitutor.app.ui.screen.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.TrendPoint

@Composable
fun TrendChart(
    trends: List<TrendPoint>,
    selectedDays: Int = 7,
    onDaysChange: (Int) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "学习趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedDays == 7,
                        onClick = { onDaysChange(7) },
                        label = { Text("7天") }
                    )
                    FilterChip(
                        selected = selectedDays == 30,
                        onClick = { onDaysChange(30) },
                        label = { Text("30天") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (trends.isEmpty()) {
                Text(
                    text = "暂无趋势数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LineChart(
                    points = trends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gradientColor = lineColor.copy(alpha = 0.1f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val paddingLeft = 40f
        val paddingBottom = 30f
        val chartWidth = canvasWidth - paddingLeft - 10f
        val chartHeight = canvasHeight - paddingBottom - 10f

        val maxValue = points.maxOf { it.solveCount.coerceAtLeast(1) }.toFloat()
        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

        // Draw grid lines
        for (i in 0..4) {
            val y = 10f + chartHeight * (1f - i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 0.5f
            )
        }

        // Draw line
        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val y = 10f + chartHeight * (1f - point.solveCount / maxValue)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Draw fill
            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(paddingLeft + (points.size - 1) * stepX, 10f + chartHeight)
            fillPath.lineTo(paddingLeft, 10f + chartHeight)
            fillPath.close()
            drawPath(fillPath, brush = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                endY = 10f + chartHeight
            ))
        }
    }
}
