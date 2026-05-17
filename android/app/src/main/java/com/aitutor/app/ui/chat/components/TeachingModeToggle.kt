package com.aitutor.app.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.ChatMode

/**
 * A toggle component for switching between teaching modes (F42).
 *
 * Provides ASSISTANT (normal), TUTOR (Socratic teaching), and QUIZ modes.
 *
 * @param currentMode Currently selected ChatMode
 * @param onModeChange Callback when mode is changed
 * @param enabled Whether the toggle is interactive
 * @param modifier Modifier
 */
@Composable
fun TeachingModeToggle(
    currentMode: ChatMode,
    onModeChange: (ChatMode) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(ChatMode.ASSISTANT, Icons.Default.AutoAwesome, "问答"),
        Triple(ChatMode.TUTOR, Icons.Default.Psychology, "教学"),
        Triple(ChatMode.QUIZ, Icons.Default.Quiz, "测验")
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, icon, label) ->
            val isSelected = currentMode == mode

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    when (mode) {
                        ChatMode.ASSISTANT -> MaterialTheme.colorScheme.surfaceVariant
                        ChatMode.TUTOR -> MaterialTheme.colorScheme.tertiaryContainer
                        ChatMode.QUIZ -> MaterialTheme.colorScheme.secondaryContainer
                    }
                } else {
                    MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(200),
                label = "modeColor"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    when (mode) {
                        ChatMode.ASSISTANT -> MaterialTheme.colorScheme.onSurfaceVariant
                        ChatMode.TUTOR -> MaterialTheme.colorScheme.onTertiaryContainer
                        ChatMode.QUIZ -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                animationSpec = tween(200),
                label = "modeContentColor"
            )

            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onModeChange(mode) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(20.dp),
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = containerColor,
                    selectedLabelColor = contentColor,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = contentColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) {
                        when (mode) {
                            ChatMode.ASSISTANT -> MaterialTheme.colorScheme.outline
                            ChatMode.TUTOR -> MaterialTheme.colorScheme.tertiary
                            ChatMode.QUIZ -> MaterialTheme.colorScheme.secondary
                        }
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    selectedBorderColor = when (mode) {
                        ChatMode.ASSISTANT -> MaterialTheme.colorScheme.outline
                        ChatMode.TUTOR -> MaterialTheme.colorScheme.tertiary
                        ChatMode.QUIZ -> MaterialTheme.colorScheme.secondary
                    },
                    enabled = enabled,
                    selected = isSelected
                ),
                modifier = Modifier.height(32.dp)
            )
        }
    }
}
