package com.aitutor.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.ui.common.MarkdownText

/**
 * A collapsible step card for displaying adaptive step-by-step explanations (F41).
 *
 * Each card represents one step in a multi-step explanation.
 * Users can expand/collapse steps to control their learning pace.
 *
 * @param stepNumber The step number (1-based)
 * @param totalSteps Total number of steps
 * @param title Step title
 * @param content Step content body (supports Markdown)
 * @param isCompleted Whether this step has been completed
 * @param isLocked Whether this step is locked (sequential unlocking)
 * @param isExpanded Whether this step is initially expanded
 * @param onToggle Called when the card is expanded/collapsed
 */
@Composable
fun CollapsibleStepCard(
    stepNumber: Int,
    totalSteps: Int,
    title: String,
    content: String,
    isCompleted: Boolean = false,
    isLocked: Boolean = false,
    isExpanded: Boolean = stepNumber == 1,
    onToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(isExpanded) }

    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isLocked -> MaterialTheme.colorScheme.outlineVariant
        expanded -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 2.dp else 0.dp
        )
    ) {
        // Header — clickable to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLocked) {
                    expanded = !expanded
                    onToggle?.invoke()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isLocked -> MaterialTheme.colorScheme.outlineVariant
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已完成",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "未解锁",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title and progress info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifEmpty { "第 $stepNumber 步" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "第 $stepNumber / $totalSteps 步",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expand/collapse icon
            if (!isLocked) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "未解锁",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = expanded && !isLocked,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                MarkdownText(
                    text = content.ifEmpty { "（暂无详细内容）" }
                )
            }
        }
    }
}
