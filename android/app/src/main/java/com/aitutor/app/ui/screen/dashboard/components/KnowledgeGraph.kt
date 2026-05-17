package com.aitutor.app.ui.screen.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.aitutor.app.domain.model.KnowledgeNode

@Composable
fun KnowledgeGraph(
    nodes: List<KnowledgeNode>,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) {
        Box(modifier = modifier) {
            androidx.compose.material3.Text(
                text = "暂无知识点数据，开始学习后这里会展示知识图谱",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val displayNodes = nodes.take(50) // Limit to 50 nodes
    
    val nodeColors = mapOf(
        "mastered" to Color(0xFF4CAF50),
        "learning" to Color(0xFFFFC107),
        "weak" to Color(0xFFF44336)
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val nodeWidth = 120f
        val nodeHeight = 40f
        val horizontalSpacing = 160f
        val verticalSpacing = 70f
        val startX = (canvasWidth - (minOf(displayNodes.size, 5) * horizontalSpacing)) / 2f + horizontalSpacing / 2
        val startY = 40f

        displayNodes.forEachIndexed { index, node ->
            val col = index % 5
            val row = index / 5
            val x = startX + col * horizontalSpacing
            val y = startY + row * verticalSpacing
            
            val color = nodeColors[node.status] ?: Color.Gray
            
            // Draw node rounded rect
            drawRoundRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(x - nodeWidth / 2, y - nodeHeight / 2),
                size = Size(nodeWidth, nodeHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x - nodeWidth / 2, y - nodeHeight / 2),
                size = Size(nodeWidth, nodeHeight),
                cornerRadius = CornerRadius(8f, 8f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            // Draw node text
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.parseColor("#FFFFFF")
                    this.textSize = 14f * density
                    this.textAlign = android.graphics.Paint.Align.CENTER
                    this.isAntiAlias = true
                }
                val text = if (node.name.length > 6) node.name.take(5) + ".." else node.name
                drawText(text, x, y + 5f, paint)
            }

            // Draw edge to parent
            if (node.parentId != null) {
                val parentIndex = displayNodes.indexOfFirst { it.id == node.parentId }
                if (parentIndex >= 0) {
                    val pCol = parentIndex % 5
                    val pRow = parentIndex / 5
                    val px = startX + pCol * horizontalSpacing
                    val py = startY + pRow * verticalSpacing + nodeHeight / 2
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(px, py),
                        end = Offset(x, y - nodeHeight / 2),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}
