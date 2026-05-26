package com.aitutor.app.ui.screen.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.ui.screen.review.components.ReviewCard
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("间隔复习") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.currentReviewIndex < 0) {
                // Home / selection screen
                ReviewHomeContent(
                    dueCount = uiState.dueCount,
                    items = uiState.items,
                    onStartReview = { viewModel.startReview() },
                    onDeleteItem = { id -> viewModel.deleteItem() },
                    filterSubjects = uiState.filterSubjects,
                    selectedSubject = uiState.selectedSubject,
                    onSubjectChange = { viewModel.selectSubject(it) }
                )
            } else {
                // Active review
                val currentItem = uiState.items.getOrNull(uiState.currentReviewIndex)
                if (currentItem != null) {
                    ReviewCard(
                        item = currentItem,
                        index = uiState.currentReviewIndex + 1,
                        total = uiState.items.size,
                        showAnswer = uiState.showAnswer,
                        onToggleAnswer = { viewModel.toggleAnswer() },
                        onCorrect = { viewModel.markCorrect() },
                        onIncorrect = { viewModel.markIncorrect() },
                        onDelete = { viewModel.deleteItem() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewHomeContent(
    dueCount: Int,
    items: List<com.aitutor.app.domain.model.WrongAnswerItem>,
    onStartReview: () -> Unit,
    onDeleteItem: (String) -> Unit,
    filterSubjects: List<String>,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Due count card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (dueCount > 0)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$dueCount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "道题目待复习",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartReview,
                    enabled = dueCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始复习", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject filter
        Text(
            text = "按学科筛选",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterSubjects.forEach { subject ->
                FilterChip(
                    selected = selectedSubject == subject,
                    onClick = { onSubjectChange(subject) },
                    label = { Text(formatSubject(subject)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // All wrong answers list
        Text(
            text = "错题本 (${items.size})",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无错题，继续学习吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.question.take(50) + if (item.question.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${formatSubject(item.subject)} · ${item.knowledgePoint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSubject(key: String): String = when (key) {
    "all" -> "全部"
    "math" -> "数学"
    "physics" -> "物理"
    "chemistry" -> "化学"
    "biology" -> "生物"
    "chinese" -> "语文"
    "english" -> "英语"
    else -> key
}

// ===== Preview =====
@Preview(
    name = "复习页 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "复习页 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ReviewScreenPreview() {
    AiTutorTheme {
        ReviewScreen(onBack = {})
    }
}
