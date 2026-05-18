package com.aitutor.app.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.ui.common.EmptyStateView
import com.aitutor.app.ui.components.DashboardSkeleton
import com.aitutor.app.ui.screen.dashboard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习进度") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.isEmpty) {
                // Skeleton loading state
                DashboardSkeleton()
            } else if (uiState.isEmpty && !uiState.isLoading) {
                EmptyStateView(
                    title = "开始学习吧！",
                    subtitle = "去聊天或解题，这里将展示你的学习统计数据。",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats overview
                    StatsOverviewCard(stats = uiState.stats)

                    // Trend chart
                    TrendChart(
                        trends = uiState.trends,
                        selectedDays = uiState.trendDays,
                        onDaysChange = { viewModel.setTrendDays(it) }
                    )

                    // Knowledge graph section header
                    Text(
                        text = "知识掌握度",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Subject filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("全部", "数学", "物理", "化学", "生物", "语文", "英语").forEach { subject ->
                            FilterChip(
                                selected = uiState.selectedSubject == getSubjectKey(subject),
                                onClick = { viewModel.selectSubject(getSubjectKey(subject)) },
                                label = { Text(subject, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Knowledge graph
                    KnowledgeGraph(
                        nodes = uiState.knowledgeNodes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )

                    // F46 游戏化数据展示
                    // 连胜指示器
                    StreakIndicator(
                        streak = uiState.streak
                    )

                    // 成就徽章区
                    AchievementGrid(
                        achievements = uiState.achievements
                    )

                    // 排行榜
                    LeaderboardView(
                        rankings = uiState.rankings,
                        isLoading = uiState.isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun getSubjectKey(displayName: String): String = when (displayName) {
    "全部" -> "all"
    "数学" -> "math"
    "物理" -> "physics"
    "化学" -> "chemistry"
    "生物" -> "biology"
    "语文" -> "chinese"
    "英语" -> "english"
    else -> displayName.lowercase()
}
