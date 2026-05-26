package com.aitutor.app.ui.flashcard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.Flashcard
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@Composable
fun FlashcardFront(card: Flashcard) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(card.question, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("难度: ${card.difficulty}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { card.masteryLevel },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text("掌握度: ${(card.masteryLevel * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}

// ===== Preview =====
@Preview(
    name = "Flashcard 正面 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Flashcard 正面 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FlashcardFrontPreview() {
    AiTutorTheme {
        FlashcardFront(
            card = Flashcard(
                id = "preview-1",
                sourceId = "source-1",
                question = "什么是牛顿第一定律？",
                correctAnswer = "一切物体在没有受到力的作用时，总保持静止状态或匀速直线运动状态。",
                explanation = "牛顿第一定律也称为惯性定律，描述了物体在没有外力作用时的运动状态。",
                difficulty = "中等",
                category = "物理",
                masteryLevel = 0.6f
            )
        )
    }
}
