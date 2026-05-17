package com.aitutor.app.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Understanding badge indicating the AI's assessment of the student's comprehension (F42).
 *
 * Used in Socratic teaching mode to show whether the student's answer
 * demonstrates understanding, needs improvement, or is incorrect.
 *
 * @param level Understanding level (0.0 - 1.0)
 * @param label Human-readable label
 * @param feedback Optional feedback text
 * @param showAnimation Whether to show entrance animation
 * @param modifier Modifier
 */
@Composable
fun UnderstandingBadge(
    level: Float,
    label: String = "",
    feedback: String? = null,
    showAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Determine visual properties based on level
    data class BadgeVisuals(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val color: androidx.compose.ui.graphics.Color,
        val textColor: androidx.compose.ui.graphics.Color,
        val label: String
    )
    val visuals: BadgeVisuals = when {
        level >= 0.8f -> BadgeVisuals(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "完全理解 ✓"
        )
        level >= 0.5f -> BadgeVisuals(
            Icons.Default.ThumbUp,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "基本理解"
        )
        level >= 0.3f -> BadgeVisuals(
            Icons.Default.TrendingUp,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "需要加强"
        )
        else -> BadgeVisuals(
            Icons.Default.Star,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "需要重学"
        )
    }
    val icon = visuals.icon
    val badgeColor = visuals.color
    val textColor = visuals.textColor
    val badgeLabel = visuals.label

    var visible by remember { mutableStateOf(!showAnimation) }
    LaunchedEffect(Unit) {
        if (showAnimation) {
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = badgeColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label.ifEmpty { badgeLabel },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )

                    if (level >= 0.3f) {
                        // Show mini progress bar for understanding level
                        LinearProgressIndicator(
                            progress = { level.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .padding(top = 4.dp)
                                .height(4.dp),
                            color = when {
                                level >= 0.8f -> MaterialTheme.colorScheme.primary
                                level >= 0.5f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            trackColor = textColor.copy(alpha = 0.15f)
                        )
                    }

                    if (feedback != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // Percentage display
                Text(
                    text = "${(level * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
