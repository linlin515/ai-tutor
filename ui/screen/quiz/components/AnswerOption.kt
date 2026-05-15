package com.aitutor.app.ui.screen.quiz.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnswerOption(
    text: String,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    isMultiChoice: Boolean = false,
    showResult: Boolean = false,
    isCorrect: Boolean = false,
    isWrong: Boolean = false
) {
    val borderColor = when {
        showResult && isCorrect -> MaterialTheme.colorScheme.primary
        showResult && isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    val containerColor = when {
        showResult && isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        showResult && isWrong -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = !showResult) { onSelect() },
        border = BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiChoice) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    enabled = !showResult
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    enabled = !showResult
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
