package com.aitutor.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.local.CacheManager
import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.data.remote.datastore.LanguagePreferences
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.AgentRepository
import com.aitutor.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cacheManager: CacheManager,
    private val languagePreferences: LanguagePreferences,
    private val agentRepository: AgentRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    private val _cacheSize = MutableStateFlow(CacheSize())
    val cacheSize: StateFlow<CacheSize> = _cacheSize.asStateFlow()

    private val _cacheClearing = MutableStateFlow(false)
    val cacheClearing: StateFlow<Boolean> = _cacheClearing.asStateFlow()

    private val _cacheProgress = MutableStateFlow(0f)
    val cacheProgress: StateFlow<Float> = _cacheProgress.asStateFlow()

    private val _cacheClearedBytes = MutableStateFlow(0L)
    val cacheClearedBytes: StateFlow<Long> = _cacheClearedBytes.asStateFlow()

    // Language preference (F51)
    private val _currentLanguage = MutableStateFlow(Locale("zh"))
    val currentLanguage: StateFlow<Locale> = _currentLanguage.asStateFlow()

    // v2.0 Agent: Agent 模式状态
    private val _agentEnabled = MutableStateFlow(false)
    val agentEnabled: StateFlow<Boolean> = _agentEnabled.asStateFlow()

    val availableLanguages = listOf(
        Locale("zh"),
        Locale("en")
    )

    init {
        loadCacheSize()
        // Observe current language preference
        viewModelScope.launch {
            languagePreferences.currentLocale.collect { locale ->
                _currentLanguage.value = locale
            }
        }
        // Observe agent enabled state
        viewModelScope.launch {
            agentRepository.getAgentEnabled().collect { enabled ->
                _agentEnabled.value = enabled
            }
        }
    }

    fun loadCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = cacheManager.calculateSize()
        }
    }

    fun clearCache() {
        if (_cacheClearing.value) return
        _cacheClearing.value = true
        _cacheProgress.value = 0f
        viewModelScope.launch {
            cacheManager.clearAll { progress ->
                _cacheProgress.value = progress
            }
            val cleared = _cacheSize.value.total
            _cacheClearedBytes.value = cleared
            _cacheSize.value = CacheSize()
            _cacheClearing.value = false
        }
    }

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

    // F51: Language switching
    fun setLanguage(locale: Locale) {
        viewModelScope.launch {
            languagePreferences.setLocale(locale)
        }
    }

    fun getLanguageDisplayName(locale: Locale): String {
        return when (locale.language) {
            "zh" -> "中文"
            "en" -> "English"
            else -> locale.displayName
        }
    }

    // v2.0 Agent: 切换 Agent 模式
    fun toggleAgentMode() {
        viewModelScope.launch {
            val newValue = !_agentEnabled.value
            agentRepository.setAgentEnabled(newValue)
        }
    }
}
