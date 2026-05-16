package com.aitutor.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 订阅页面
 * MVP 版本以 UI 展示为主，实际支付后续接入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "订阅方案",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = "选择适合你的学习方案",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "解锁全部功能，让学习更高效",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 免费版卡片
            PlanCard(
                title = "免费版",
                price = "¥0",
                period = "永久免费",
                features = listOf(
                    "每日 5 次提问",
                    "基础 AI 解答",
                    "拍照解题（每日3次）",
                    "语文 / 数学 / 英语"
                ),
                isRecommended = false,
                buttonText = "当前方案",
                buttonEnabled = false,
                accentColor = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 高级版卡片
            PlanCard(
                title = "高级版",
                price = "¥29",
                period = "每月",
                features = listOf(
                    "无限次提问",
                    "更精准的 AI 解答",
                    "无限拍照解题",
                    "所有科目支持",
                    "优先排队",
                    "学习报告分析"
                ),
                isRecommended = true,
                buttonText = "立即订阅",
                buttonEnabled = true,
                accentColor = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 功能对比
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "功能对比",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 对比表格
            FeatureComparisonRow(feature = "每日提问次数", free = "5次", pro = "无限次")
            FeatureComparisonRow(feature = "拍照解题", free = "3次/天", pro = "无限次")
            FeatureComparisonRow(feature = "科目支持", free = "基础4科", pro = "全部科目")
            FeatureComparisonRow(feature = "AI 模型", free = "标准版", pro = "高级版")
            FeatureComparisonRow(feature = "学习报告", free = "✗", pro = "✓")
            FeatureComparisonRow(feature = "优先服务", free = "✗", pro = "✓")

            Spacer(modifier = Modifier.height(24.dp))

            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "订阅服务将定期自动续费，你可以随时在设置中取消。\n支付功能将在后续版本中接入。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 订阅方案卡片
 */
@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    isRecommended: Boolean,
    buttonText: String,
    buttonEnabled: Boolean,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 推荐标签
            if (isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accentColor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "推荐",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 方案名称
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 价格
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ $period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 功能列表
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isRecommended) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 订阅按钮
            Button(
                onClick = { /* MVP: 仅 UI 展示 */ },
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 功能对比行
 */
@Composable
fun FeatureComparisonRow(
    feature: String,
    free: String,
    pro: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = feature,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.5f),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = free,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = pro,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
