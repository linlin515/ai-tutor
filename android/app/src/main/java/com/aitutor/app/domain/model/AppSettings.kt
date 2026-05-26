package com.aitutor.app.domain.model

import com.aitutor.app.domain.repository.TtsMode

data class AppSettings(
    val modelId: String = "default",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
    val darkTheme: ThemeMode = ThemeMode.SYSTEM,
    val ttsSpeed: Float = 1.0f,
    val ttsVoice: String = "default",
    val ttsMode: TtsMode = TtsMode.LOCAL,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
