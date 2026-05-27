package com.aitutor.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

// ===== Content composable (pure UI) =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    showLogoutDialog: Boolean,
    onUpdateNickname: (String) -> Unit,
    onUpdateGrade: (String) -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
    onShowLogoutDialog: () -> Unit,
    onDismissLogoutDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.user?.nickname?.firstOrNull()?.toString() ?: "?",
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isEditing) {
                // Edit mode
                OutlinedTextField(
                    value = state.editNickname,
                    onValueChange = onUpdateNickname,
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.editGrade,
                    onValueChange = onUpdateGrade,
                    label = { Text("年级") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelEditing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onSaveProfile,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Text("保存")
                    }
                }
            } else {
                // Display mode
                Text(
                    text = state.user?.nickname ?: "加载中...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.user?.grade ?: "未设置年级",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("手机号") },
                        supportingContent = { Text(state.user?.phone ?: "") },
                        leadingContent = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quota card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("每日配额") },
                        supportingContent = {
                            val quota = state.user?.dailyQuota ?: 0
                            val used = state.user?.dailyUsed ?: 0
                            Text("已用 $used / $quota")
                        },
                        leadingContent = {
                            Icon(Icons.Default.BarChart, contentDescription = null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartEditing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("编辑资料")
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout button
                OutlinedButton(
                    onClick = onShowLogoutDialog,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录")
                }
            }
        }

        // Logout confirm dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = onDismissLogoutDialog,
                title = { Text("退出登录") },
                text = { Text("确定要退出登录吗？退出后需要重新登录才能使用。") },
                confirmButton = {
                    TextButton(onClick = onLogout) {
                        Text("确定退出")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissLogoutDialog) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// ===== Screen composable (ViewModel bridge) =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggingOut) {
        if (state.isLoggingOut) {
            onLogout()
        }
    }

    ProfileScreenContent(
        state = state,
        showLogoutDialog = showLogoutDialog,
        onUpdateNickname = viewModel::updateNickname,
        onUpdateGrade = viewModel::updateGrade,
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onSaveProfile = viewModel::saveProfile,
        onLogout = {
            showLogoutDialog = false
            viewModel.logout()
        },
        onShowLogoutDialog = { showLogoutDialog = true },
        onDismissLogoutDialog = { showLogoutDialog = false },
        onBack = onBack
    )
}

// ===== Preview =====
@Preview(
    name = "个人中心 预览",
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "个人中心 预览 (深色)",
    showBackground = true,
    backgroundColor = 0xFFFEFBFF,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ProfileScreenPreview() {
    AiTutorTheme {
        ProfileScreenContent(
            state = ProfileUiState(isLoading = true),
            showLogoutDialog = false,
            onUpdateNickname = {},
            onUpdateGrade = {},
            onStartEditing = {},
            onCancelEditing = {},
            onSaveProfile = {},
            onLogout = {},
            onShowLogoutDialog = {},
            onDismissLogoutDialog = {},
            onBack = {}
        )
    }
}
