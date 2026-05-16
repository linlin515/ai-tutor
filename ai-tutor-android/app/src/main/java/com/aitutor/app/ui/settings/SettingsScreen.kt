package com.aitutor.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

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

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // General section
        Text(
            text = "通用",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("版本") },
            supportingContent = { Text("v1.0.0 (build 1)") }
        )

        Spacer(modifier = Modifier.height(32.dp))
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
