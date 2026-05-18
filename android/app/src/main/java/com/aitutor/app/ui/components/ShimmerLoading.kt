package com.aitutor.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading animation brush. Sweeps a highlight across a surface
 * to indicate that content is loading.
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - targetValue, translateAnim - targetValue),
            end = Offset(translateAnim, translateAnim)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

/**
 * A single shimmer placeholder line with configurable width and height.
 * Used inside skeleton loading screens.
 */
@Composable
fun ShimmerPlaceholder(
    width: Dp = 160.dp,
    height: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush())
    )
}

/**
 * Skeleton loading card mimicking a dashboard stats card.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerPlaceholder(width = 80.dp, height = 14.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerPlaceholder(width = 60.dp, height = 28.dp)
        }
    }
}

/**
 * Skeleton loading for a list row.
 */
@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ShimmerPlaceholder(width = 40.dp, height = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerPlaceholder(width = 120.dp, height = 14.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerPlaceholder(width = 80.dp, height = 12.dp)
        }
    }
}

/**
 * Full page loading skeleton for Dashboard.
 */
@Composable
fun DashboardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats card placeholder
        ShimmerCard(height = 80.dp)

        // Trend chart placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush())
        )

        // Section header
        ShimmerPlaceholder(width = 100.dp, height = 18.dp)

        // Knowledge graph placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush())
        )

        // Achievements placeholder
        ShimmerCard(height = 120.dp)

        // Leaderboard placeholder
        ShimmerCard(height = 200.dp)
    }
}

/**
 * Loading skeleton for Settings.
 */
@Composable
fun SettingsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section group placeholders
        repeat(4) {
            ShimmerRow()
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerRow()
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerPlaceholder(width = 160.dp, height = 18.dp)
        Spacer(modifier = Modifier.height(8.dp))
        repeat(3) {
            ShimmerRow()
        }
    }
}
