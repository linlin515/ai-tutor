package com.aitutor.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.AgentState

/**
 * A status indicator chip/badge that shows the current AI Agent state.
 *
 * Displays different icons and text for each [AgentState], with
 * animated transitions between states. Shows nothing when idle.
 *
 * @param agentState The current agent state to display
 * @param modifier Modifier
 */
@Composable
fun AgentStatusIndicator(
    agentState: AgentState,
    modifier: Modifier = Modifier
) {
    val isVisible = agentState != AgentState.IDLE

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        val containerColor = when (agentState) {
            AgentState.THINKING -> MaterialTheme.colorScheme.surfaceVariant
            AgentState.SEARCHING -> MaterialTheme.colorScheme.secondaryContainer
            AgentState.REASONING -> MaterialTheme.colorScheme.tertiaryContainer
            AgentState.RESPONDING -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val contentColor = when (agentState) {
            AgentState.THINKING -> MaterialTheme.colorScheme.onSurfaceVariant
            AgentState.SEARCHING -> MaterialTheme.colorScheme.onSecondaryContainer
            AgentState.REASONING -> MaterialTheme.colorScheme.onTertiaryContainer
            AgentState.RESPONDING -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        // Shimmer alpha animation for THINKING state
        val shimmerAlpha by if (agentState == AgentState.THINKING) {
            rememberInfiniteTransition(label = "shimmer").animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmerAlpha"
            )
        } else {
            remember { mutableFloatStateOf(1.0f) }
        }

        // Spinning rotation for SEARCHING state
        val rotation by if (agentState == AgentState.SEARCHING) {
            rememberInfiniteTransition(label = "spin").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spinRotation"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }

        Surface(
            modifier = modifier
                .padding(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(16.dp),
            color = containerColor.copy(alpha = shimmerAlpha)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Icon
                when (agentState) {
                    AgentState.THINKING -> {
                        Text(
                            text = "\uD83E\uDD14",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    AgentState.SEARCHING -> {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotation),
                            tint = contentColor
                        )
                    }
                    AgentState.REASONING -> {
                        Text(
                            text = "\uD83E\uDDE0",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    AgentState.RESPONDING -> {
                        Text(
                            text = "\uD83D\uDCDD",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    else -> {}
                }

                // Label text
                Text(
                    text = when (agentState) {
                        AgentState.THINKING -> "AI 思考中..."
                        AgentState.SEARCHING -> "搜索中..."
                        AgentState.REASONING -> "AI 推理中..."
                        AgentState.RESPONDING -> "AI 回答中..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}
