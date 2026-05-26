package com.aitutor.app.ui.flashcard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.Flashcard

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
