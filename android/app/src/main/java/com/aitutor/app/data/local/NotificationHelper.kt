package com.aitutor.app.data.local

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 通知辅助工具，负责发送和取消各类通知。
 */
object NotificationHelper {

    private var notificationIdCounter = 1000

    private fun nextId(): Int = notificationIdCounter++

    /**
     * 发送消息通知。
     *
     * @param context        上下文
     * @param title          通知标题
     * @param content        通知内容
     * @param conversationId 对话 ID（用于点击后打开指定对话）
     */
    fun sendMessageNotification(
        context: Context,
        title: String,
        content: String,
        conversationId: Long
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversation_id", conversationId)
        } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MESSAGE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, conversationId.toInt(), notification)
    }

    /**
     * 发送复习提醒通知。
     *
     * @param context 上下文
     * @param count   待复习题目数
     */
    fun sendReviewReminder(
        context: Context,
        count: Int
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "review")
        } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REVIEW)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("复习提醒")
            .setContentText("你有 $count 道错题需要复习")
            .setStyle(NotificationCompat.BigTextStyle().bigText("你有 $count 道错题需要复习，点击查看详情"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, 2001, notification)
    }

    /**
     * 发送鼓励通知（没有待复习题目时）。
     */
    fun sendEncouragementNotification(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_SYSTEM)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("学习提醒")
            .setContentText("今日错题已全部掌握，继续保持！")
            .setStyle(NotificationCompat.BigTextStyle().bigText("今日错题已全部掌握，继续保持！去学习新知识吧~"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, 2002, notification)
    }

    /**
     * 发送配额超限通知。
     */
    fun sendQuotaExceededNotification(
        context: Context,
        featureName: String
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "subscription")
        } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_SYSTEM)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("配额提醒")
            .setContentText("今日 $featureName 次数已用尽")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, 3001, notification)
    }

    /**
     * 取消所有通知。
     */
    fun cancelAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }

    /**
     * 取消指定对话的消息通知。
     */
    fun cancelConversation(context: Context, conversationId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(conversationId.toInt())
    }

    /**
     * 安全发送通知，检查 POST_NOTIFICATIONS 权限（Android 13+）。
     */
    private fun notifySafe(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return  // 无权限，静默忽略
            }
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
