package com.aitutor.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aitutor.app.domain.model.AppUpdateInfo

/**
 * 应用更新弹窗
 *
 * 显示新版本信息，提供"立即更新"和"稍后再说"按钮。
 * 强制更新模式下隐藏"稍后再说"按钮，且不可通过点击外部关闭。
 */
@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            // 强制更新时不允许关闭
            if (!updateInfo.forceUpdate) onDismiss()
        },
        title = { Text("发现新版本 v${updateInfo.versionName}") },
        text = {
            Text(
                text = updateInfo.releaseNotes.ifEmpty { "有新的应用版本可用，请更新以获取最新功能。" }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 跳转浏览器下载 APK
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("立即更新")
            }
        },
        dismissButton = {
            // 非强制更新时显示"稍后再说"
            if (!updateInfo.forceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("稍后再说")
                }
            }
        }
    )
}
