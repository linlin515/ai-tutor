package com.aitutor.app.ui.flashcard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.Flashcard
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

// ===== Content composable (pure UI, no ViewModel) =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreenContent(
    state: FlashcardViewModel.UiState,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onReviewCard: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("复习卡片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is FlashcardViewModel.UiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("加载今日卡片...")
                    }
                }
                is FlashcardViewModel.UiState.Cards -> {
                    val card = s.cards[s.currentIndex]
                    val rotation by animateFloatAsState(
                        targetValue = if (isFlipped) 180f else 0f,
                        animationSpec = tween(300),
                        label = "flip"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "第 ${s.currentIndex + 1} / ${s.totalCards} 张",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // Flip card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .heightIn(min = 260.dp)
                                .clickable { onFlip() },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 260.dp)
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 8f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (rotation <= 90f) {
                                    FlashcardFront(card = card)
                                } else {
                                    FlashcardBack(card = card, modifier = Modifier.graphicsLayer { scaleX = -1f })
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "点击卡片翻转",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onReviewCard(card.id, "unfamiliar") }
                            ) {
                                Text("← 不熟练")
                            }
                            Button(
                                onClick = { onReviewCard(card.id, "mastered") }
                            ) {
                                Text("已掌握 →")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "今日已完成: ${s.completedToday} / ${s.dailyLimit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                is FlashcardViewModel.UiState.AllDone -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "今日复习已完成！",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "明天再来继续巩固吧",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is FlashcardViewModel.UiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😵", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("加载出错: ${s.message}")
                    }
                }
            }
        }
    }
}

// ===== Screen composable (bridges ViewModel -> Content) =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var isFlipped by remember { mutableStateOf(false) }

    FlashcardScreenContent(
        state = state,
        isFlipped = isFlipped,
        onFlip = { isFlipped = !isFlipped },
        onReviewCard = viewModel::reviewCard,
        onNavigateBack = onNavigateBack
    )
}

// ===== Preview =====

@Preview(
    name = "Flashcard 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Flashcard 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FlashcardScreenPreview() {
    AiTutorTheme {
        FlashcardScreenContent(
            state = FlashcardViewModel.UiState.Loading,
            isFlipped = false,
            onFlip = {},
            onReviewCard = { _, _ -> },
            onNavigateBack = {}
        )
    }
}
