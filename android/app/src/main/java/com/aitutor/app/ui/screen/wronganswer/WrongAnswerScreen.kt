package com.aitutor.app.ui.screen.wronganswer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.ui.common.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswerScreen(
    onBack: () -> Unit = {},
    viewModel: WrongAnswerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WrongAnswerScreenContent(
        state = uiState,
        onSelectSubject = viewModel::selectSubject,
        onDeleteItem = viewModel::deleteItem,
        onMarkMastered = viewModel::markMastered,
        onClearError = viewModel::clearError,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswerScreenContent(
    state: WrongAnswerUiState,
    onSelectSubject: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onMarkMastered: (String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("错题本") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Subject filter chips
            SubjectFilterRow(
                subjects = state.subjects,
                selectedSubject = state.selectedSubject,
                onSubjectSelected = onSelectSubject
            )

            // Error snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.items.isEmpty() -> {
                    EmptyStateView(
                        title = "暂无错题",
                        subtitle = "答题错误的题目将会出现在这里",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.items,
                            key = { it.id }
                        ) { item ->
                            WrongAnswerCard(
                                item = item,
                                onDelete = { onDeleteItem(item.id) },
                                onMarkMastered = { onMarkMastered(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectFilterRow(
    subjects: List<String>,
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit
) {
    val displayNames = mapOf(
        "all" to "全部",
        "math" to "数学",
        "physics" to "物理",
        "chemistry" to "化学",
        "biology" to "生物",
        "chinese" to "语文",
        "english" to "英语"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subjects.forEach { subject ->
            FilterChip(
                selected = selectedSubject == subject,
                onClick = { onSubjectSelected(subject) },
                label = {
                    Text(
                        text = displayNames[subject] ?: subject,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun WrongAnswerCard(
    item: com.aitutor.app.domain.model.WrongAnswerItem,
    onDelete: () -> Unit,
    onMarkMastered: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isMastered)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: subject badge + mastered status + review time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subject badge
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = getSubjectDisplay(item.subject),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(4.dp)
                    )

                    // Mastered badge
                    if (item.isMastered) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "已掌握",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Next review time
                Text(
                    text = "复习: ${formatTime(item.nextReviewAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = getReviewTimeColor(item.nextReviewAt)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question
            Text(
                text = item.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // User answer (wrong) — red
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "你的答案: ${item.userAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Correct answer — green
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "正确答案: ${item.correctAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Knowledge point if available
            if (item.knowledgePoint.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "知识点: ${item.knowledgePoint}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.isMastered) {
                    TextButton(onClick = onMarkMastered) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("标记已掌握", style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这道错题吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun getSubjectDisplay(key: String): String = when (key) {
    "math" -> "数学"
    "physics" -> "物理"
    "chemistry" -> "化学"
    "biology" -> "生物"
    "chinese" -> "语文"
    "english" -> "英语"
    else -> key
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun getReviewTimeColor(timestamp: Long): Color {
    val now = System.currentTimeMillis()
    return if (timestamp <= now) {
        MaterialTheme.colorScheme.error
    } else if (timestamp - now < 24 * 60 * 60 * 1000L) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// ===== Preview =====
@Preview(
    name = "错题本 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "错题本 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewWrongAnswerScreen() {
    AiTutorTheme {
        WrongAnswerScreenContent(
            state = WrongAnswerUiState(isLoading = true),
            onSelectSubject = {},
            onDeleteItem = {},
            onMarkMastered = {},
            onClearError = {},
            onBack = {}
        )
    }
}
