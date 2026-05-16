package com.aitutor.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.ui.chat.components.ChatInputBar
import com.aitutor.app.ui.chat.components.MessageBubble
import com.aitutor.app.ui.chat.components.StreamingText
import com.aitutor.app.ui.chat.components.VoiceInputBar
import com.aitutor.app.ui.common.EmptyStateView
import com.aitutor.app.ui.conversation.ConversationListSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long? = null,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val listState = rememberLazyListState()
    var showVoicePanel by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // RECORD_AUDIO runtime permission
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) showVoicePanel = true
    }

    fun handleVoiceClick() {
        if (hasAudioPermission) {
            showVoicePanel = !showVoicePanel
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Select conversation if provided
    LaunchedEffect(conversationId) {
        if (conversationId != null && conversationId > 0) {
            viewModel.selectConversation(conversationId)
        }
    }

    // Auto scroll to bottom on new messages
    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = state.conversations
                        .find { it.id == state.currentConversationId }?.title
                        ?: "AI 对话"
                    Text(
                        text = if (title.isEmpty()) "AI 对话" else title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleConversationSheet() }) {
                        Icon(Icons.Default.Menu, contentDescription = "会话列表")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建对话")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "个人中心")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                onTextChange = { viewModel.updateInputText(it) },
                onSend = { viewModel.sendMessage() },
                onVoiceClick = { handleVoiceClick() },
                onAddAttachment = onNavigateToCamera,
                enabled = !state.isStreaming
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.messages.isEmpty() && !state.isStreaming) {
                // Empty state
                EmptyStateView(
                    title = "开始一段新的对话吧",
                    subtitle = "输入问题或点击麦克风语音输入"
                )
            } else {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            onRetry = {
                                if (message.status == MessageStatus.FAILED) {
                                    viewModel.retrySend()
                                }
                            },
                            onSpeak = {
                                if (!message.isUser && message.content.isNotEmpty()) {
                                    viewModel.speakText(message.content)
                                }
                            }
                        )
                    }

                    // Streaming AI message
                    if (state.isStreaming) {
                        item(key = "streaming") {
                            StreamingText(
                                text = state.streamingContent,
                                isStreaming = true
                            )
                        }
                    }
                }
            }

            // Error
            if (state.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(state.errorMessage)
                }
            }

            // Voice panel overlay
            if (showVoicePanel) {
                VoiceInputBar(
                    voiceState = state.voiceState,
                    onDismiss = { showVoicePanel = false }
                )
            }

            // Conversation list bottom sheet
            if (state.showConversationSheet) {
                ConversationListSheet(
                    conversations = state.conversations,
                    currentConversationId = state.currentConversationId,
                    searchKeyword = state.searchKeyword,
                    onSelectConversation = { id ->
                        viewModel.selectConversation(id)
                        viewModel.hideConversationSheet()
                    },
                    onDeleteConversation = { id ->
                        viewModel.deleteConversation(id)
                    },
                    onNewConversation = {
                        viewModel.createNewConversation()
                        viewModel.hideConversationSheet()
                    },
                    onSearchKeywordChange = { viewModel.updateSearchKeyword(it) },
                    onDismiss = { viewModel.hideConversationSheet() }
                )
            }
        }
    }
}
