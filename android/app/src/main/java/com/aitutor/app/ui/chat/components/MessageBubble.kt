package com.aitutor.app.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.FeedbackType
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.ui.common.MarkdownText
import com.aitutor.app.ui.screen.chat.components.ImageMessage
import com.aitutor.app.ui.screen.chat.components.PhotoPreviewDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    onImageLoadRetry: (() -> Unit)? = null,  // F24: 图片重新加载
    // v2.5 F1: Long-press menu callbacks
    onCopy: ((String) -> Unit)? = null,
    onFavorite: ((Long) -> Unit)? = null,
    onFeedback: ((Long) -> Unit)? = null,
    // v2.5 F2: Thumbs up/down callbacks
    onThumbsUp: ((Long) -> Unit)? = null,
    onThumbsDown: ((Long) -> Unit)? = null,
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

    // F24: 全屏预览弹窗控制
    var showPhotoPreview by remember { mutableStateOf(false) }

    // v2.5 F1: Long-press context menu state
    var showContextMenu by remember { mutableStateOf(false) }

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
            // Message content area with long-press support
            Box {
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(bubbleColor)
                        .then(
                            if (onCopy != null || onFavorite != null || onFeedback != null) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { showContextMenu = true }
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    when (message.contentType) {
                        MessageType.TEXT -> {
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
                        MessageType.IMAGE -> {
                            // F24: 图片消息分支
                            ImageMessage(
                                url = message.content,
                                isMine = isUser,
                                onRetry = { onImageLoadRetry?.invoke() },
                                onClick = { showPhotoPreview = true }
                            )
                        }
                        MessageType.AUDIO -> {
                            // AUDIO 类型的占位渲染
                            Text(
                                text = "[语音消息]",
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        MessageType.AGENT_STEP -> {
                            // Agent 步骤消息由 ChatScreen 单独处理，这里作为兜底
                            Text(
                                text = "[Agent 步骤]",
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // v2.5 F1: Long-press context menu (DropdownMenu)
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    // Copy content
                    if (onCopy != null && message.content.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("复制内容") },
                            onClick = {
                                showContextMenu = false
                                onCopy(message.content)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                    }

                    // Favorite / unfavorite
                    if (onFavorite != null) {
                        DropdownMenuItem(
                            text = { Text(if (message.isFavorite) "取消收藏" else "收藏消息") },
                            onClick = {
                                showContextMenu = false
                                onFavorite(message.id)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            }
                        )
                    }

                    // Feedback (only for AI messages)
                    if (!isUser && onFeedback != null) {
                        DropdownMenuItem(
                            text = { Text("反馈") },
                            onClick = {
                                showContextMenu = false
                                onFeedback(message.id)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Feedback, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // v2.5 F2: Thumbs up/down for AI messages
            if (!isUser && message.content.isNotEmpty() && (onThumbsUp != null || onThumbsDown != null)) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val hasFeedback = message.feedback != null

                    if (onThumbsUp != null) {
                        IconButton(
                            onClick = { onThumbsUp(message.id) },
                            modifier = Modifier.size(24.dp),
                            enabled = !hasFeedback
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = "有用",
                                modifier = Modifier.size(16.dp),
                                tint = if (message.feedback == FeedbackType.POSITIVE)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (onThumbsDown != null) {
                        IconButton(
                            onClick = { onThumbsDown(message.id) },
                            modifier = Modifier.size(24.dp),
                            enabled = !hasFeedback
                        ) {
                            Icon(
                                Icons.Default.ThumbDown,
                                contentDescription = "没用",
                                modifier = Modifier.size(16.dp),
                                tint = if (message.feedback == FeedbackType.NEGATIVE)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
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

    // F24: 全屏预览弹窗
    if (showPhotoPreview && message.contentType == MessageType.IMAGE) {
        PhotoPreviewDialog(
            url = message.content,
            onDismiss = { showPhotoPreview = false }
        )
    }
}
