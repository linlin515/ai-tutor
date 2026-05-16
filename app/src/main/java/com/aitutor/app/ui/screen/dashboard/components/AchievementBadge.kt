package com.aitutor.app.ui.screen.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.app.domain.model.Achievement
import com.aitutor.app.domain.model.AchievementStatus
import com.aitutor.app.domain.model.AchievementWithStatus

@Composable
fun AchievementBadge(
    achievementWithStatus: AchievementWithStatus,
    onClick: (Achievement) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUnlocked = achievementWithStatus.status == AchievementStatus.UNLOCKED
    val achievement = achievementWithStatus.achievement

    // 解锁动画
    val animatedScale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    Card(
        modifier = modifier
            .size(100.dp)
            .scale(animatedScale)
            .clickable { onClick(achievement) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Gray.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = if (isUnlocked) 22.sp else 18.sp,
                    modifier = Modifier.then(
                        if (!isUnlocked) Modifier
                        else Modifier
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 标题
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
                color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // 状态标记
            if (!isUnlocked) {
                Text(
                    text = "🔒",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AchievementGrid(
    achievements: List<AchievementWithStatus>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "成就徽章",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements) { aws ->
                    AchievementBadge(
                        achievementWithStatus = aws,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}
