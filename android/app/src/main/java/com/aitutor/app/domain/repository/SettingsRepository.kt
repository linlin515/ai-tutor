package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateModelId(modelId: String)
    suspend fun updateTemperature(temperature: Float)
    suspend fun updateTopP(topP: Float)
    suspend fun updateMaxTokens(maxTokens: Int)
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateTtsSpeed(speed: Float)
    suspend fun updateTtsVoice(voice: String)
    suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int)
    suspend fun getSyncSettings(): AppSettings
}
