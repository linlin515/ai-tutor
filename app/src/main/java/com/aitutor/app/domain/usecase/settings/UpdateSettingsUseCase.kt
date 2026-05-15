package com.aitutor.app.domain.usecase.settings

import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun getSettings(): Flow<AppSettings> = settingsRepository.getSettings()

    suspend fun updateModelId(modelId: String) =
        settingsRepository.updateModelId(modelId)

    suspend fun updateTemperature(temperature: Float) =
        settingsRepository.updateTemperature(temperature)

    suspend fun updateTopP(topP: Float) =
        settingsRepository.updateTopP(topP)

    suspend fun updateMaxTokens(maxTokens: Int) =
        settingsRepository.updateMaxTokens(maxTokens)

    suspend fun updateThemeMode(themeMode: ThemeMode) =
        settingsRepository.updateThemeMode(themeMode)

    suspend fun updateTtsSpeed(speed: Float) =
        settingsRepository.updateTtsSpeed(speed)

    suspend fun updateTtsVoice(voice: String) =
        settingsRepository.updateTtsVoice(voice)
}
