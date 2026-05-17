package com.aitutor.app.ui.screen.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.app.domain.model.StreakResult

@Composable
fun StreakIndicator(
    streak: StreakResult,
    modifier: Modifier = Modifier
) {
    val streakColor by animateColorAsState(
        targetValue = when {
            streak.currentStreak >= 30 -> MaterialTheme.colorScheme.error
            streak.currentStreak >= 7 -> MaterialTheme.colorScheme.tertiary
            streak.currentStreak >= 3 -> MaterialTheme.colorScheme.secondary
            streak.currentStreak > 0 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "streak_color"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = streakColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：火焰图标 + 连续天数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        streak.currentStreak >= 30 -> "🔥🔥🔥"
                        streak.currentStreak >= 7 -> "🔥🔥"
                        streak.currentStreak >= 3 -> "🔥"
                        streak.currentStreak > 0 -> "✨"
                        else -> "💤"
                    },
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${streak.currentStreak} 天",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = streakColor
                    )
                    Text(
                        text = if (streak.hasLearnedToday) "今日已学习 ✓" else "今日尚未学习",
                        style = MaterialTheme.typography.bodySmall,
                        color = streakColor.copy(alpha = 0.7f)
                    )
                }
            }

            // 右侧：最长连续
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "最长",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${streak.longestStreak} 天",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
