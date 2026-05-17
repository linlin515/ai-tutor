package com.aitutor.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val color: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji = "\uD83D\uDCF7",
        title = "拍照解题",
        description = "遇到难题不要慌，拍张照片就能获得详细的解题步骤和思路讲解。支持数学、物理、化学等多种学科。",
        color = Color(0xFF4FC3F7)
    ),
    OnboardingPage(
        emoji = "\uD83C\uDFA4",
        title = "语音交互",
        description = "用语音与 AI 老师对话，就像请了一位私人导师。随时提问，自然交流，学习效率翻倍。",
        color = Color(0xFF81C784)
    ),
    OnboardingPage(
        emoji = "\uD83D\uDCCA",
        title = "学习进度",
        description = "追踪你的学习轨迹，查看知识点掌握情况。智能推荐薄弱环节，让每一分钟学习都有成效。",
        color = Color(0xFFFFB74D)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Skip button at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = {
                        viewModel.onOnboardingComplete()
                        onOnboardingComplete()
                    }) {
                        Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Content area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Emoji icon
                    Text(
                        text = page.emoji,
                        fontSize = 100.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Page title
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Page description
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Bottom section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = index == pagerState.currentPage
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next / Start button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.onOnboardingComplete()
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "下一步" else "开始使用",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
