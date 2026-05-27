package com.aitutor.app.ui.screen.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.QuizConfig
import com.aitutor.app.domain.model.QuizPhase
import com.aitutor.app.domain.model.QuizUiState
import com.aitutor.app.domain.model.Question
import com.aitutor.app.ui.screen.quiz.components.*
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.phase == QuizPhase.IDLE) {
            viewModel.loadMockQuiz(
                QuizConfig(subject = "math", questionCount = 3, difficulty = "medium")
            )
        }
    }

    QuizScreenContent(
        state = uiState,
        onSubmitAnswer = { qId, answer -> viewModel.submitAnswer(qId, answer) },
        onSubmitAll = { viewModel.submitAllAnswers() },
        onRetry = { viewModel.retryGenerate() },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreenContent(
    state: QuizUiState,
    onSubmitAnswer: (String, String) -> Unit,
    onSubmitAll: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("交互式测验") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.phase == QuizPhase.DONE) {
                            onBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                QuizPhase.IDLE,
                QuizPhase.GENERATING -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在生成题目...")
                    }
                }
                QuizPhase.ANSWERING -> {
                    QuizAnsweringContent(
                        questions = state.questions,
                        answers = state.answers,
                        answeredCount = state.answers.size,
                        totalCount = state.questions.size,
                        onSubmitAnswer = onSubmitAnswer,
                        onSubmitAll = onSubmitAll
                    )
                }
                QuizPhase.SUBMITTING,
                QuizPhase.GRADING -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在批改...")
                    }
                }
                QuizPhase.DONE -> {
                    state.result?.let { result ->
                        QuizResultCard(
                            result = result,
                            onRetry = onRetry,
                            onBack = onBack
                        )
                    }
                }
                QuizPhase.ERROR -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "出错了",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizAnsweringContent(
    questions: List<Question>,
    answers: Map<String, String>,
    answeredCount: Int,
    totalCount: Int,
    onSubmitAnswer: (String, String) -> Unit,
    onSubmitAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        QuizProgressBar(
            answered = answeredCount,
            total = totalCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = "已答 $answeredCount/$totalCount 题",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(questions) { index, question ->
                QuestionCard(
                    index = index + 1,
                    question = question,
                    selectedAnswer = answers[question.id] ?: "",
                    onAnswerSelected = { answer -> onSubmitAnswer(question.id, answer) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onSubmitAll,
                enabled = answeredCount == totalCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    if (answeredCount == totalCount) "提交批改" else "请完成所有题目($answeredCount/$totalCount)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ===== Preview =====
@Preview(
    name = "测验页 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "测验页 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuizScreenPreview() {
    AiTutorTheme {
        QuizScreenContent(
            state = QuizUiState(
                phase = QuizPhase.IDLE,
                config = QuizConfig(subject = "math"),
                questions = emptyList(),
                answers = emptyMap(),
                result = null,
                error = null,
                retryPending = false
            ),
            onSubmitAnswer = { _, _ -> },
            onSubmitAll = {},
            onRetry = {},
            onBack = {}
        )
    }
}
