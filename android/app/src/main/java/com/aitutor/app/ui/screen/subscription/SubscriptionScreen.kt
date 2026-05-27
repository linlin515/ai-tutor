package com.aitutor.app.ui.screen.subscription

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.data.billing.BillingConnectionState
import com.aitutor.app.data.billing.BillingProduct
import com.aitutor.app.data.billing.BillingProductType
import com.aitutor.app.data.billing.PurchaseState
import com.aitutor.app.domain.model.SubscriptionState
import com.aitutor.app.ui.screen.subscription.components.FeatureComparisonTable
import com.aitutor.app.ui.screen.subscription.components.PlanCard
import com.aitutor.app.ui.screen.subscription.components.QuotaExceededDialog
import com.aitutor.app.ui.screen.subscription.components.QuotaProgressBar
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // 监听待购买商品ID，触发 BillingFlow
    val pendingProductId by viewModel.pendingPurchaseProductId.collectAsState()
    LaunchedEffect(pendingProductId) {
        if (pendingProductId != null && activity != null) {
            viewModel.onBillingFlowReady(activity)
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    SubscriptionScreenContent(
        state = uiState,
        onPurchase = viewModel::purchase,
        onRestorePurchases = viewModel::restorePurchases,
        onRefreshStatus = viewModel::refreshStatus,
        onConnectAndQuery = viewModel::connectAndQuery,
        onDismissQuotaExceededDialog = viewModel::dismissQuotaExceededDialog,
        onBack = onBack,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreenContent(
    state: SubscriptionUiState,
    onPurchase: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onRefreshStatus: () -> Unit,
    onConnectAndQuery: () -> Unit,
    onDismissQuotaExceededDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // 配额超限对话框
    if (state.showQuotaExceededDialog) {
        val exceededFeature = state.exceededFeature ?: return
        QuotaExceededDialog(
            featureName = exceededFeature.displayName,
            usedQueries = state.subscription.dailyQuota.usedQueries,
            totalQueries = state.subscription.dailyQuota.totalQueries,
            onDismiss = onDismissQuotaExceededDialog,
            onViewPlans = {
                onDismissQuotaExceededDialog()
                // 页面已经在订阅页，所以什么都不做
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订阅方案", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 恢复购买按钮
                    TextButton(
                        onClick = onRestorePurchases,
                        enabled = !state.isRestoring
                    ) {
                        if (state.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复购买")
                    }
                    TextButton(onClick = onRefreshStatus) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("刷新")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
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

            // 连接状态提示
            if (state.connectionState == BillingConnectionState.DISCONNECTED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "支付服务未连接",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onConnectAndQuery) {
                            Text("重试")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 日配额进度条
            val daily = state.subscription.dailyQuota
            if (daily.totalQueries > 0) {
                QuotaProgressBar(
                    used = daily.usedQueries,
                    total = daily.totalQueries
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 加载中
            if (state.subscription.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 从 BillingManager 获取商品信息
            val billingProducts = state.products
            val monthlyProduct = billingProducts.find { it.type == BillingProductType.MONTHLY }
            val yearlyProduct = billingProducts.find { it.type == BillingProductType.YEARLY }

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
                isCurrentPlan = state.subscription.planType == "free",
                accentColor = MaterialTheme.colorScheme.outline,
                onSubscribe = { /* 免费版无需订阅 */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 高级版 - 月付卡片
            PlanCard(
                title = "高级版 · 月付",
                price = monthlyProduct?.price ?: "¥29",
                period = monthlyProduct?.type?.period ?: "每月",
                features = listOf(
                    "无限次提问",
                    "更精准的 AI 解答",
                    "无限拍照解题",
                    "所有科目支持",
                    "优先排队",
                    "学习报告分析"
                ),
                isRecommended = true,
                isCurrentPlan = state.subscription.planType == "premium",
                accentColor = MaterialTheme.colorScheme.secondary,
                onSubscribe = {
                    onPurchase("premium_monthly")
                },
                isLoading = state.isPurchasing && state.purchaseState == PurchaseState.PURCHASING
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 高级版 - 年付卡片（更优惠）
            PlanCard(
                title = "高级版 · 年付",
                price = yearlyProduct?.price ?: "¥198",
                period = yearlyProduct?.type?.period ?: "每年",
                features = listOf(
                    "无限次提问",
                    "更精准的 AI 解答",
                    "无限拍照解题",
                    "所有科目支持",
                    "优先排队",
                    "学习报告分析",
                    "约 55 折优惠"
                ),
                isRecommended = false,
                isCurrentPlan = state.subscription.planType == "premium",
                accentColor = MaterialTheme.colorScheme.tertiary,
                onSubscribe = {
                    onPurchase("premium_yearly")
                },
                isLoading = state.isPurchasing && state.purchaseState == PurchaseState.PURCHASING
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 功能对比
            Text(
                text = "功能对比",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureComparisonTable()

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
                        text = "订阅服务将定期自动续费，你可以随时在 Google Play 设置中取消。\n确认购买后，款项将通过你的 Google 账号支付。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ===== Preview =====
@Preview(
    name = "订阅页 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "订阅页 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewSubscriptionScreen() {
    AiTutorTheme {
        SubscriptionScreenContent(
            state = SubscriptionUiState(
                subscription = SubscriptionState(isLoading = true)
            ),
            onPurchase = {},
            onRestorePurchases = {},
            onRefreshStatus = {},
            onConnectAndQuery = {},
            onDismissQuotaExceededDialog = {},
            onBack = {}
        )
    }
}
