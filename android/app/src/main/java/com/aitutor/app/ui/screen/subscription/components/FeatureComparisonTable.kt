package com.aitutor.app.ui.screen.subscription.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 功能对比表格的一行。
 */
@Composable
fun FeatureComparisonRow(
    feature: String,
    free: String,
    premium: String,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
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
                text = premium,
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

/**
 * 完整功能对比表格。
 */
@Composable
fun FeatureComparisonTable(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 表头
        Surface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "功能",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "免费版",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "高级版",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider()

        FeatureComparisonRow(feature = "AI 对话", free = "5次/天", premium = "无限次")
        FeatureComparisonRow(feature = "拍照解题", free = "3次/天", premium = "无限次")
        FeatureComparisonRow(feature = "科目支持", free = "基础4科", premium = "全部科目")
        FeatureComparisonRow(feature = "AI 模型", free = "标准版", premium = "高级版")
        FeatureComparisonRow(feature = "学习报告", free = "✗", premium = "✓")
        FeatureComparisonRow(feature = "语音识别", free = "✗", premium = "✓")
        FeatureComparisonRow(feature = "优先服务", free = "✗", premium = "✓")
    }
}
