package com.aitutor.app.data.remote.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val currentLocale: Flow<Locale> = context.languageDataStore.data.map { prefs ->
        val langCode = prefs[KEY_APP_LANGUAGE] ?: "zh"
        if (langCode == "zh") Locale.CHINA else Locale(langCode)
    }

    suspend fun setLocale(locale: Locale) {
        context.languageDataStore.edit { prefs ->
            val code = when {
                locale.language == "zh" -> "zh"
                else -> locale.language
            }
            prefs[KEY_APP_LANGUAGE] = code
        }
    }

    suspend fun getCurrentLanguageCode(): String {
        return context.languageDataStore.data.map { prefs ->
            prefs[KEY_APP_LANGUAGE] ?: "zh"
        }.let { flow ->
            var code = "zh"
            flow.collect { code = it }
            code
        }
    }
}
