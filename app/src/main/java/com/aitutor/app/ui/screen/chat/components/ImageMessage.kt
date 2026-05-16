package com.aitutor.app.ui.screen.chat.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * 图片消息组件 (F24)
 *
 * 在聊天气泡中显示缩略图（最大 240dp 宽/高，保持宽高比）。
 * 支持：
 * - Coil 异步加载（内存缓存 + 磁盘缓存）
 * - 加载中 shimmer 占位动画
 * - 加载成功 Crossfade 淡入
 * - 加载失败错误占位图 + 重试按钮
 * - 点击触发全屏预览
 *
 * @param url 图片 URL
 * @param isMine 是否为用户发送的消息（决定对齐方向）
 * @param onRetry 重新加载回调
 * @param onClick 点击图片触发全屏预览
 */
@Composable
fun ImageMessage(
    url: String,
    isMine: Boolean,
    onRetry: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(url)
            .size(480)
            .crossfade(300)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    )

    val state = painter.state

    Box(
        modifier = modifier
            .widthIn(max = 240.dp)
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = state is AsyncImagePainter.State.Success) { onClick() }
    ) {
        Crossfade(
            targetState = state,
            animationSpec = tween(300)
        ) { currentState ->
            when (currentState) {
                is AsyncImagePainter.State.Loading -> {
                    ShimmerPlaceholder()
                }

                is AsyncImagePainter.State.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = "图片消息",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                is AsyncImagePainter.State.Error -> {
                    ErrorPlaceholder(onRetry = onRetry)
                }

                else -> {
                    ShimmerPlaceholder()
                }
            }
        }
    }
}

/**
 * Shimmer 占位动画——加载中的骨架屏效果
 */
@Composable
private fun ShimmerPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "加载中...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * 加载失败占位组件——显示错误图标 + 重试按钮
 */
@Composable
private fun ErrorPlaceholder(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "图片加载失败",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "图片加载失败",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重新加载",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
