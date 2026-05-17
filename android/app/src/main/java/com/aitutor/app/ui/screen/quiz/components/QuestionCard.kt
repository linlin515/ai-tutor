package com.aitutor.app.ui.screen.quiz.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.Question
import com.aitutor.app.domain.model.QuestionType

@Composable
fun QuestionCard(
    index: Int,
    question: Question,
    selectedAnswer: String = "",
    onAnswerSelected: (String) -> Unit = {},
    showResult: Boolean = false,
    isCorrect: Boolean? = null,
    correctAnswer: String? = null,
    explanation: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (showResult && isCorrect != null) {
            CardDefaults.cardColors(
                containerColor = if (isCorrect)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "第 $index 题",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when (question.type) {
                                QuestionType.SINGLE_CHOICE -> "单选题"
                                QuestionType.MULTIPLE_CHOICE -> "多选题"
                                QuestionType.FILL_BLANK -> "填空题"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (question.type) {
                QuestionType.SINGLE_CHOICE ->
                    question.options?.forEach { option ->
                        AnswerOption(
                            text = option,
                            isSelected = selectedAnswer == option,
                            onSelect = { onAnswerSelected(option) },
                            showResult = showResult,
                            isCorrect = showResult && option == correctAnswer,
                            isWrong = showResult && selectedAnswer == option && option != correctAnswer
                        )
                    }
                QuestionType.MULTIPLE_CHOICE ->
                    question.options?.forEach { option ->
                        AnswerOption(
                            text = option,
                            isSelected = selectedAnswer.contains(option.firstOrNull()?.toString() ?: ""),
                            onSelect = { onAnswerSelected(option) },
                            isMultiChoice = true,
                            showResult = showResult,
                            isCorrect = showResult && option == correctAnswer,
                            isWrong = showResult && selectedAnswer == option && option != correctAnswer
                        )
                    }
                QuestionType.FILL_BLANK ->
                    FillBlankInput(
                        value = selectedAnswer,
                        onValueChange = onAnswerSelected,
                        enabled = !showResult
                    )
            }

            if (showResult && explanation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "解析: $explanation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
