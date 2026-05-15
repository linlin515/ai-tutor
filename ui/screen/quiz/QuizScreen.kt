package com.aitutor.app.ui.screen.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.QuizConfig
import com.aitutor.app.domain.model.Question
import com.aitutor.app.ui.screen.quiz.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.phase == com.aitutor.app.domain.model.QuizPhase.IDLE) {
            viewModel.loadMockQuiz(
                QuizConfig(subject = "math", questionCount = 3, difficulty = "medium")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("交互式测验") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.phase == com.aitutor.app.domain.model.QuizPhase.DONE) {
                            viewModel.reset()
                            onBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.phase) {
                com.aitutor.app.domain.model.QuizPhase.IDLE,
                com.aitutor.app.domain.model.QuizPhase.GENERATING -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在生成题目...")
                    }
                }
                com.aitutor.app.domain.model.QuizPhase.ANSWERING -> {
                    QuizAnsweringContent(
                        questions = uiState.questions,
                        answers = uiState.answers,
                        answeredCount = uiState.answers.size,
                        totalCount = uiState.questions.size,
                        onSubmitAnswer = { qId, answer -> viewModel.submitAnswer(qId, answer) },
                        onSubmitAll = { viewModel.submitAllAnswers() }
                    )
                }
                com.aitutor.app.domain.model.QuizPhase.SUBMITTING,
                com.aitutor.app.domain.model.QuizPhase.GRADING -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在批改...")
                    }
                }
                com.aitutor.app.domain.model.QuizPhase.DONE -> {
                    uiState.result?.let { result ->
                        QuizResultCard(
                            result = result,
                            onRetry = { viewModel.retryGenerate() },
                            onBack = onBack
                        )
                    }
                }
                com.aitutor.app.domain.model.QuizPhase.ERROR -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "出错了",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryGenerate() }) {
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
        // Progress bar
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

        // Questions list
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

        // Submit button
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
