package com.aitutor.app.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 通知渠道定义。
 */
object NotificationChannels {

    const val CHANNEL_MESSAGE = "channel_message"
    const val CHANNEL_REVIEW = "channel_review"
    const val CHANNEL_SYSTEM = "channel_system"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_MESSAGE,
                "新消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI 助手的新消息通知"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_REVIEW,
                "复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "错题复习提醒"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_SYSTEM,
                "系统通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "系统运行状态通知"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}
