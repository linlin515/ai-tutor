package com.aitutor.app.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.aitutor.app.ui.common.MarkdownText

@Composable
fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor"
    )

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        if (text.isNotEmpty()) {
            MarkdownText(text = text)
        }

        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        ) {
            if (isStreaming) {
                Text(
                    text = "_",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(cursorAlpha)
                )
            }
        }

        if (text.isEmpty() && isStreaming) {
            Text(
                text = "AI 正在输入...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
