package com.aitutor.app.data.remote.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.TtsMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_MODEL_ID = stringPreferencesKey("model_id")
        private val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        private val KEY_TOP_P = floatPreferencesKey("top_p")
        private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
        private val KEY_TTS_MODE = stringPreferencesKey("tts_mode")
        private val KEY_DAILY_REMINDER = booleanPreferencesKey("daily_reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            modelId = prefs[KEY_MODEL_ID] ?: "default",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.7f,
            topP = prefs[KEY_TOP_P] ?: 1.0f,
            maxTokens = prefs[KEY_MAX_TOKENS] ?: 2048,
            darkTheme = parseThemeMode(prefs[KEY_THEME_MODE] ?: "SYSTEM"),
            ttsSpeed = prefs[KEY_TTS_SPEED] ?: 1.0f,
            ttsVoice = prefs[KEY_TTS_VOICE] ?: "default",
            ttsMode = parseTtsMode(prefs[KEY_TTS_MODE] ?: "LOCAL"),
            dailyReminderEnabled = prefs[KEY_DAILY_REMINDER] ?: false,
            reminderHour = prefs[KEY_REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[KEY_REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun updateModelId(modelId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_MODEL_ID] = modelId
        }
    }

    suspend fun updateTemperature(temperature: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_TEMPERATURE] = temperature
        }
    }

    suspend fun updateTopP(topP: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_TOP_P] = topP
        }
    }

    suspend fun updateMaxTokens(maxTokens: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_MAX_TOKENS] = maxTokens
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_TTS_SPEED] = speed
        }
    }

    suspend fun updateTtsVoice(voice: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_TTS_VOICE] = voice
        }
    }

    suspend fun updateTtsMode(mode: TtsMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_TTS_MODE] = mode.name
        }
    }

    suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DAILY_REMINDER] = enabled
            prefs[KEY_REMINDER_HOUR] = hour
            prefs[KEY_REMINDER_MINUTE] = minute
        }
    }

    private fun parseThemeMode(mode: String): ThemeMode {
        return try {
            ThemeMode.valueOf(mode)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun parseTtsMode(mode: String): TtsMode {
        return try {
            TtsMode.valueOf(mode)
        } catch (e: Exception) {
            TtsMode.LOCAL
        }
    }
}
