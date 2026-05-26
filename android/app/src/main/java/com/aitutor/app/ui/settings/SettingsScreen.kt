package com.aitutor.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.AppUpdateInfo
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.TtsMode
import com.aitutor.app.domain.usecase.CheckResult
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToUserAgreement: () -> Unit = {},
    onNavigateToCrashLog: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile entry
        ListItem(
            headlineContent = { Text("个人中心") },
            leadingContent = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToProfile() }
        )
        HorizontalDivider()

        // Subscription entry
        ListItem(
            headlineContent = { Text("订阅管理") },
            leadingContent = {
                Icon(Icons.Default.Star, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToSubscription() }
        )
        HorizontalDivider()

        // Study Report entry (F50)
        ListItem(
            headlineContent = { Text("学习报告") },
            leadingContent = {
                Icon(Icons.Default.Assessment, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToReport() }
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Model section
        Text(
            text = "模型与参数",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SettingsSlider(
            label = "模型选择",
            value = "当前: ${settings.modelId}",
            enabled = false
        )

        SettingsSlider(
            label = "Temperature",
            value = String.format("%.1f", settings.temperature),
            onValueChange = { viewModel.updateTemperature(it) },
            valueRange = 0f..2f,
            steps = 19,
            currentValue = settings.temperature
        )

        SettingsSlider(
            label = "Top-P",
            value = String.format("%.2f", settings.topP),
            onValueChange = { viewModel.updateTopP(it) },
            valueRange = 0f..1f,
            steps = 19,
            currentValue = settings.topP
        )

        SettingsSlider(
            label = "最大 Token 数",
            value = "${settings.maxTokens}",
            onValueChange = { viewModel.updateMaxTokens(it.roundToInt()) },
            valueRange = 256f..4096f,
            steps = 14,
            currentValue = settings.maxTokens.toFloat()
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Language section (F51)
        Text(
            text = "语言 / Language",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Language selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.availableLanguages.forEach { locale ->
                FilterChip(
                    selected = currentLanguage.language == locale.language,
                    onClick = { viewModel.setLanguage(locale) },
                    label = {
                        Text(viewModel.getLanguageDisplayName(locale))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Theme section
        Text(
            text = "主题与显示",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = "主题模式",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = settings.darkTheme == mode,
                    onClick = { viewModel.updateThemeMode(mode) },
                    label = {
                        Text(
                            when (mode) {
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                                ThemeMode.SYSTEM -> "跟随系统"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Agent section (v2.0)
        Text(
            text = "AI Agent",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Agent 模式") },
            supportingContent = {
                Text(if (viewModel.agentEnabled.collectAsState().value) "已开启 - AI可联网搜索" else "已关闭")
            },
            leadingContent = {
                Icon(Icons.Default.Psychology, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = viewModel.agentEnabled.collectAsState().value,
                    onCheckedChange = { viewModel.toggleAgentMode() }
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Voice section
        Text(
            text = "语音",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SettingsSlider(
            label = "TTS 语速",
            value = String.format("%.1f", settings.ttsSpeed),
            onValueChange = { viewModel.updateTtsSpeed(it) },
            valueRange = 0.5f..2.0f,
            steps = 14,
            currentValue = settings.ttsSpeed
        )

        // TTS 引擎切换
        Text(
            text = "TTS 引擎",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TtsMode.values().forEach { mode ->
                FilterChip(
                    selected = settings.ttsMode == mode,
                    onClick = { viewModel.updateTtsMode(mode) },
                    label = {
                        Text(
                            when (mode) {
                                TtsMode.LOCAL -> "本地"
                                TtsMode.CLOUD -> "云端"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // General section
        Text(
            text = "通用",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // F4: 每日学习提醒开关
        val reminderEnabled = settings.dailyReminderEnabled
        ListItem(
            headlineContent = { Text("每日学习提醒") },
            supportingContent = {
                Text(
                    if (reminderEnabled) "每日 ${"%02d:%02d".format(settings.reminderHour, settings.reminderMinute)} 推送复习提醒"
                    else "关闭后将不再推送学习提醒"
                )
            },
            leadingContent = {
                Icon(Icons.Default.Notifications, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { viewModel.toggleDailyReminder(it) }
                )
            }
        )

        HorizontalDivider()

        // v2.5 F3: 我的收藏入口
        ListItem(
            headlineContent = { Text("我的收藏") },
            supportingContent = { Text("查看收藏的消息") },
            leadingContent = {
                Icon(Icons.Default.Favorite, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToFavorites() }
        )

        HorizontalDivider()

        // App version display
        val packageInfo = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) { null }
        }
        val versionName = packageInfo?.versionName ?: "1.0.0"
        val versionCode = packageInfo?.let {
            try {
                androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it)
            } catch (e: Exception) { 1L }
        } ?: 1L

        ListItem(
            headlineContent = { Text("版本") },
            supportingContent = { Text("v$versionName (build $versionCode)") }
        )

        HorizontalDivider()

        // Check Update button
        val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
        ListItem(
            headlineContent = {
                if (isCheckingUpdate) {
                    Text("正在检查更新…")
                } else {
                    Text("检查更新")
                }
            },
            leadingContent = {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                }
            },
            modifier = Modifier.clickable(enabled = !isCheckingUpdate) {
                viewModel.checkForUpdate()
            }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("崩溃日志") },
            supportingContent = {
                Text("查看应用崩溃记录")
            },
            leadingContent = {
                Icon(Icons.Default.BugReport, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToCrashLog() }
        )

        HorizontalDivider()
        Text(
            text = "法律信息",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("隐私政策") },
            leadingContent = {
                Icon(Icons.Default.Security, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToPrivacyPolicy() }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("用户协议") },
            leadingContent = {
                Icon(Icons.Default.Description, contentDescription = null)
            },
            modifier = Modifier.clickable { onNavigateToUserAgreement() }
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Cache clearing
        val cacheSize by viewModel.cacheSize.collectAsState()
        val cacheClearing by viewModel.cacheClearing.collectAsState()
        val cacheProgress by viewModel.cacheProgress.collectAsState()
        val cacheClearedBytes by viewModel.cacheClearedBytes.collectAsState()
        var showClearDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier.clickable(enabled = !cacheClearing) {
                if (cacheSize.total > 0) {
                    showClearDialog = true
                } else {
                    Toast.makeText(context, "缓存已清空", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Clear progress indicator
        if (cacheClearing) {
            LinearProgressIndicator(
                progress = { cacheProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
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
                    "已清除 ${
                        "%.1f".format(mb)
                    } MB 缓存",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ============================================================
        // P0-3 应用内更新检测
        // ============================================================

        val updateCheckResult by viewModel.updateCheckResult.collectAsState()
        var showUpdateDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }

        // Handle update check results
        LaunchedEffect(updateCheckResult) {
            when (val result = updateCheckResult) {
                is CheckResult.NoUpdate -> {
                    Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                    viewModel.clearUpdateResult()
                }
                is CheckResult.UpdateAvailable -> {
                    showUpdateDialog = result.info
                }
                is CheckResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    viewModel.clearUpdateResult()
                }
                null -> { /* no result yet */ }
            }
        }

        // Update dialog
        showUpdateDialog?.let { info ->
            UpdateDialog(
                updateInfo = info,
                onDismiss = {
                    showUpdateDialog = null
                    viewModel.clearUpdateResult()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: ((Float) -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    currentValue: Float = 0f,
    enabled: Boolean = true
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (onValueChange != null) {
            Slider(
                value = currentValue,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled
            )
        }
    }
}
