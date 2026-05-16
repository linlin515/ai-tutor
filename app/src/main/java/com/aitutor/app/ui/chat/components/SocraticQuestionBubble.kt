package com.aitutor.app.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.app.ui.common.MarkdownText

/**
 * A bubble component for displaying Socratic guiding questions (F42).
 *
 * Used when the AI asks a probing question to guide the student toward the answer,
 * rather than directly providing it.
 *
 * @param question The guiding question text
 * @param stepNumber Current step in the teaching session (optional)
 * @param totalSteps Total steps in the teaching session (optional)
 * @param hint Optional hint to help the student think
 * @param modifier Modifier
 */
@Composable
fun SocraticQuestionBubble(
    question: String,
    stepNumber: Int = 1,
    totalSteps: Int = 1,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    var showHint by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "引导问题",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "引导思考",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Spacer(modifier = Modifier.weight(1f))

                if (totalSteps > 1) {
                    Text(
                        text = "第 $stepNumber 步 / 共 $totalSteps 步",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question content — shown in italic to denote it's a guiding question
            MarkdownText(
                text = question
            )

            // Hint toggle
            if (hint != null) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showHint = !showHint },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showHint) "隐藏提示" else "需要一点提示？",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                AnimatedVisibility(
                    visible = showHint,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MarkdownText(
                            text = hint,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Next action indicator
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "请在输入框回答这个问题",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}
