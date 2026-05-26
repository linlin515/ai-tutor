package com.aitutor.app.ui.flashcard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.Flashcard

@Composable
fun FlashcardBack(card: Flashcard, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("正确答案: ${card.correctAnswer}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(card.explanation, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text("类别: ${card.category}", style = MaterialTheme.typography.bodySmall)
    }
}
