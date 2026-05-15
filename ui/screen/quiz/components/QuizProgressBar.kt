package com.aitutor.app.ui.screen.quiz.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuizProgressBar(
    answered: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) answered.toFloat() / total else 0f
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.height(8.dp),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
