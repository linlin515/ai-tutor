package com.aitutor.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.ui.common.MarkdownText

@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp,
            bottomStart = 16.dp, bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp,
            bottomStart = 4.dp, bottomEnd = 16.dp
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // AI message - TTS button
        if (!isUser && message.content.isNotEmpty() && onSpeak != null) {
            IconButton(
                onClick = onSpeak,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom)
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "朗读",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Message content
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.contentType == com.aitutor.app.domain.model.MessageType.TEXT) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        MarkdownText(text = message.content)
                    }
                }
            }

            // Status indicators
            if (isUser) {
                when (message.status) {
                    MessageStatus.FAILED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "发送失败",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "重试",
                                modifier = Modifier.size(16.dp).clickable { onRetry?.invoke() },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    MessageStatus.SENDING -> {
                        Text(
                            text = "发送中...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {}
                }
            }
        }

        // User avatar placeholder
        if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
