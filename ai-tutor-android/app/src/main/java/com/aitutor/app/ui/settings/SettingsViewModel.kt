package com.aitutor.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun updateTemperature(temperature: Float) {
        viewModelScope.launch {
            settingsRepository.updateTemperature(temperature)
        }
    }

    fun updateTopP(topP: Float) {
        viewModelScope.launch {
            settingsRepository.updateTopP(topP)
        }
    }

    fun updateMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxTokens(maxTokens)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.updateTtsSpeed(speed)
        }
    }
}
