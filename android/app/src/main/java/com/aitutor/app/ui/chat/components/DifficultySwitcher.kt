package com.aitutor.app.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Difficulty level switcher for adaptive step-by-step teaching (F41).
 *
 * Allows the user to switch between different teaching difficulty levels:
 * 小学 (Elementary), 初中 (Middle School), 高中 (High School), 大学 (University).
 *
 * When set to "auto", the system uses the user's profile grade.
 *
 * @param currentLevel Current difficulty level ("auto", "小学", "初中", "高中", "大学")
 * @param onLevelChange Callback when a level is selected
 * @param enabled Whether the switcher is interactive
 */
@Composable
fun DifficultySwitcher(
    currentLevel: String,
    onLevelChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val levels = listOf(
        "auto" to "自动",
        "小学" to "小学",
        "初中" to "初中",
        "高中" to "高中",
        "大学" to "大学"
    )

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Label
        Text(
            text = "讲解难度",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        // Difficulty chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            levels.forEach { (value, label) ->
                val isSelected = currentLevel == value ||
                        (currentLevel == null && value == "auto")

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (enabled) {
                            onLevelChange(value)
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    enabled = enabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Hint text explaining current difficulty
        val hint = when (currentLevel) {
            "小学" -> "极其简单的语言，多用比喻"
            "初中" -> "适中难度，适当使用术语"
            "高中" -> "标准学术语言"
            "大学" -> "深入原理，专业术语"
            else -> "根据个人资料自动调整"
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}
