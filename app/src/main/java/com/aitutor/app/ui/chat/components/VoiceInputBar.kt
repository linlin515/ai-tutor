package com.aitutor.app.ui.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.VoiceState
import com.aitutor.app.domain.model.VoiceStatus

/**
 * 语音输入面板 (F16/F20)
 *
 * 支持打断交互：
 * - TTS 播放中点击麦克风 → 立即打断 TTS → 切换到 LISTENING 状态
 * - 点击其他消息 TTS 按钮 → 打断当前 → 播放新的
 * - 打断延迟 <300ms
 */
@Composable
fun VoiceInputBar(
    voiceState: VoiceState,
    onDismiss: () -> Unit,
    onInterruptAndListen: (() -> Unit)? = null,  // F20: 打断 TTS 并开始听
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val statusText = when (voiceState.status) {
        VoiceStatus.LISTENING -> "正在听..."
        VoiceStatus.PROCESSING -> "识别中..."
        VoiceStatus.SPEAKING -> "播放中..."
        VoiceStatus.IDLE -> ""
    }

    val isInteractive = voiceState.status == VoiceStatus.LISTENING ||
            voiceState.status == VoiceStatus.SPEAKING

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Mic/Stop button with pulse animation
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (voiceState.status == VoiceStatus.LISTENING) scale else 1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = isInteractive) {
                        when (voiceState.status) {
                            VoiceStatus.SPEAKING -> {
                                // F20: TTS 播放中触发 → 打断 TTS 并开始听
                                onInterruptAndListen?.invoke()
                            }
                            VoiceStatus.LISTENING -> {
                                // 正在听 → 停止听
                                onDismiss()
                            }
                            else -> {}
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // TTS 播放中显示停止图标，其他情况显示麦克风
                    imageVector = if (voiceState.status == VoiceStatus.SPEAKING)
                        Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (voiceState.status == VoiceStatus.SPEAKING)
                        "停止播放并开始录音" else "录音中",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // F20: 打断提示文字
            if (voiceState.status == VoiceStatus.SPEAKING) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击即可打断朗读并开始语音输入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (voiceState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = voiceState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
