package com.aitutor.app.ui.screen.quiz.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.app.domain.model.QuizResult

@Composable
fun QuizResultCard(
    result: QuizResult,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.overallScore >= 0.6f)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "批改结果",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(result.overallScore * 100).toInt()}分",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.overallScore >= 0.6f)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.suggestions.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        itemsIndexed(result.results) { index, qResult ->
            QuestionCard(
                index = index + 1,
                question = com.aitutor.app.domain.model.Question(
                    id = qResult.questionId,
                    type = com.aitutor.app.domain.model.QuestionType.SINGLE_CHOICE,
                    content = "题目 ${index + 1}",
                    options = null,
                    knowledgePoint = qResult.knowledgePoint
                ),
                selectedAnswer = qResult.userAnswer,
                showResult = true,
                isCorrect = qResult.isCorrect,
                correctAnswer = qResult.correctAnswer,
                explanation = qResult.explanation
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("返回")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("再来一组")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
