package com.aitutor.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CacheManagementSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val cacheSize by viewModel.cacheSize.collectAsState()
    val cacheClearing by viewModel.cacheClearing.collectAsState()
    val cacheProgress by viewModel.cacheProgress.collectAsState()
    val cacheClearedBytes by viewModel.cacheClearedBytes.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "缓存管理",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Database cache info
        ListItem(
            headlineContent = { Text("数据库缓存") },
            supportingContent = {
                Text(formatCacheSize(cacheSize.database))
            },
            leadingContent = {
                Icon(Icons.Default.Storage, contentDescription = null)
            }
        )

        // Image cache info
        ListItem(
            headlineContent = { Text("图片缓存") },
            supportingContent = {
                Text(formatCacheSize(cacheSize.imageDisk))
            },
            leadingContent = {
                Icon(Icons.Default.Storage, contentDescription = null)
            }
        )

        // Clear cache button
        ListItem(
            headlineContent = { Text("清除缓存") },
            supportingContent = {
                Text(
                    if (cacheClearing) "正在清除…" else formatCacheSize(cacheSize.total)
                )
            },
            leadingContent = {
                Icon(Icons.Default.Delete, contentDescription = null)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Clear progress indicator
        if (cacheClearing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { cacheProgress },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "${(cacheProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Clear button at bottom
        TextButton(
            onClick = {
                if (cacheSize.total > 0) {
                    showClearDialog = true
                } else {
                    Toast.makeText(context, "缓存已清空", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !cacheClearing,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("清除所有缓存")
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除缓存") },
            text = {
                Text("确定要清除 ${formatCacheSize(cacheSize.total)} 的缓存数据吗？")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCache()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Toast when clear completes
    LaunchedEffect(cacheClearedBytes) {
        if (!cacheClearing && cacheClearedBytes > 0) {
            val mb = cacheClearedBytes / (1024.0 * 1024.0)
            Toast.makeText(
                context,
                "已清除 ${"%.1f".format(mb)} MB 缓存",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
}

private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
