package com.aitutor.app.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatInputBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onAddAttachment: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Add attachment button
            IconButton(onClick = onAddAttachment) {
                Icon(Icons.Default.Add, contentDescription = "附加")
            }

            // Text input
            OutlinedTextField(
                value = inputText,
                onValueChange = { if (it.length <= 2000) onTextChange(it) },
                placeholder = { Text("输入你的问题…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                enabled = enabled
            )

            // Character count
            if (inputText.isNotEmpty()) {
                Text(
                    text = "\${inputText.length}/2000",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inputText.length >= 1900) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Voice button or Send button
            if (inputText.isBlank()) {
                IconButton(onClick = onVoiceClick) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "语音输入",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        }
    }
}
