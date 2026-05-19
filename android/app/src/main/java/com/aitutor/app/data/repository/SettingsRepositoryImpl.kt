package com.aitutor.app.data.repository

import com.aitutor.app.data.remote.datastore.SettingsDataStore
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }

    override suspend fun updateModelId(modelId: String) {
        settingsDataStore.updateModelId(modelId)
    }

    override suspend fun updateTemperature(temperature: Float) {
        settingsDataStore.updateTemperature(temperature)
    }

    override suspend fun updateTopP(topP: Float) {
        settingsDataStore.updateTopP(topP)
    }

    override suspend fun updateMaxTokens(maxTokens: Int) {
        settingsDataStore.updateMaxTokens(maxTokens)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        settingsDataStore.updateThemeMode(themeMode)
    }

    override suspend fun updateTtsSpeed(speed: Float) {
        settingsDataStore.updateTtsSpeed(speed)
    }

    override suspend fun updateTtsVoice(voice: String) {
        settingsDataStore.updateTtsVoice(voice)
    }

    override suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        settingsDataStore.updateDailyReminder(enabled, hour, minute)
    }

    override suspend fun getSyncSettings(): AppSettings {
        return settingsDataStore.settingsFlow.first()
    }
}
