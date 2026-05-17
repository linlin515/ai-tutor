package com.aitutor.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * P1-2: Network disconnected banner.
 * Shows an orange/red banner at the top when offline with animated slide-in/out.
 */
@Composable
fun NetworkBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE65100)) // Deep orange
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "网络已断开，部分功能不可用",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
