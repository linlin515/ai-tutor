package com.aitutor.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

/**
 * 崩溃日志页面 — 列出所有崩溃日志，支持查看详情、删除、分享。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogScreen(
    onBack: () -> Unit = {},
    viewModel: CrashLogViewModel = viewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    val deleteResult by viewModel.deleteResult.collectAsState()
    val context = LocalContext.current

    var showDetail by remember { mutableStateOf<File?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<File?>(null) }

    // 处理删除/清空结果 Toast
    LaunchedEffect(deleteResult) {
        deleteResult?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearDeleteResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("崩溃日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!listState.isEmpty && showDetail == null) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空全部")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showDetail != null) {
            // 详情页面
            CrashLogDetailContent(
                detailState = detailState,
                onBack = {
                    showDetail = null
                    viewModel.refreshList()
                },
                onShare = {
                    shareCrashLog(context, showDetail!!, detailState.content)
                },
                onDelete = {
                    showDeleteDialog = showDetail
                }
            )
        } else {
            // 列表页面
            CrashLogListContent(
                listState = listState,
                onItemClick = { file ->
                    showDetail = file
                    viewModel.loadDetail(file)
                },
                onDeleteClick = { file ->
                    showDeleteDialog = file
                },
                modifier = Modifier.padding(padding)
            )
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除崩溃日志") },
            text = { Text("确定要删除 ${file.name} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    showDetail = null
                    viewModel.deleteCrash(file)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空崩溃日志") },
            text = { Text("确定要删除所有崩溃日志吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllCrashes()
                }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 崩溃日志列表内容。
 */
@Composable
private fun CrashLogListContent(
    listState: CrashLogListState,
    onItemClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            listState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            listState.isEmpty -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无崩溃日志",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "应用运行稳定，未记录到崩溃信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(listState.items, key = { it.file.absolutePath }) { item ->
                        CrashLogListItem(
                            item = item,
                            onClick = { onItemClick(item.file) },
                            onDelete = { onDeleteClick(item.file) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个崩溃日志条目。
 */
@Composable
private fun CrashLogListItem(
    item: CrashLogItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dateTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.exceptionType,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.exceptionSummary.isNotEmpty()) {
                    Text(
                        text = item.exceptionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 崩溃日志详情内容。
 */
@Composable
private fun CrashLogDetailContent(
    detailState: CrashLogDetailState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("分享")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }

        HorizontalDivider()

        when {
            detailState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Text(
                    text = detailState.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = detailState.content.ifEmpty { "暂无内容" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

/**
 * 分享崩溃日志内容。
 */
private fun shareCrashLog(context: android.content.Context, file: File, content: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "崩溃日志 - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "分享崩溃日志")
        )
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "分享失败: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
