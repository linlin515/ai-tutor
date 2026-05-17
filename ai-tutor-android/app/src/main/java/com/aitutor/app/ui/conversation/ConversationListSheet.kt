package com.aitutor.app.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.app.domain.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListSheet(
    conversations: List<Conversation>,
    currentConversationId: Long,
    searchKeyword: String,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 600.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "会话列表",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    IconButton(onClick = onNewConversation) {
                        Icon(Icons.Default.Add, contentDescription = "新建会话")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                placeholder = { Text("搜索历史对话…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation list
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无会话",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = conversations,
                        key = { it.id }
                    ) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            isSelected = conversation.id == currentConversationId,
                            onClick = { onSelectConversation(conversation.id) },
                            onDelete = { onDeleteConversation(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title.ifEmpty { "新对话" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除会话",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    HorizontalDivider()
}
