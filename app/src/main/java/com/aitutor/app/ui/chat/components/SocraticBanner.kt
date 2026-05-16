package com.aitutor.app.ui.chat.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.ChatMode

/**
 * Teaching mode banner displayed at the top of the chat screen when in TUTOR or QUIZ mode (F42).
 *
 * Shows the current teaching mode, a brief description, and an indicator.
 *
 * @param chatMode Current chat mode
 * @param onDismiss Callback to dismiss/exit the current teaching mode
 * @param modifier Modifier
 */
@Composable
fun SocraticBanner(
    chatMode: ChatMode,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (chatMode == ChatMode.ASSISTANT) return

    val bannerColor = when (chatMode) {
        ChatMode.TUTOR -> MaterialTheme.colorScheme.tertiaryContainer
        ChatMode.QUIZ -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val bannerContentColor = when (chatMode) {
        ChatMode.TUTOR -> MaterialTheme.colorScheme.onTertiaryContainer
        ChatMode.QUIZ -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when (chatMode) {
        ChatMode.TUTOR -> Icons.Default.Psychology
        ChatMode.QUIZ -> Icons.Default.Quiz
        else -> Icons.Default.AutoAwesome
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = bannerContentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatMode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = bannerContentColor
                )
                Text(
                    text = chatMode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = bannerContentColor.copy(alpha = 0.8f)
                )
            }

            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = bannerContentColor
                    )
                ) {
                    Text("退出", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
}
