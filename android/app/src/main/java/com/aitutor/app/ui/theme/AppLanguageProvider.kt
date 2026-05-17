package com.aitutor.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.aitutor.app.data.remote.datastore.LanguagePreferences
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { Locale("zh") }

@Composable
fun AppLanguageProvider(
    languagePreferences: LanguagePreferences,
    content: @Composable () -> Unit
) {
    val locale by languagePreferences.currentLocale.collectAsState(initial = Locale("zh"))
    val context = LocalContext.current

    // Apply locale to the configuration for non-Compose resources
    val config = LocalConfiguration.current
    val updatedConfig = Configuration(config).apply {
        setLocale(locale)
    }

    // Create updated context with new locale
    val localizedContext = context.createConfigurationContext(updatedConfig)

    CompositionLocalProvider(LocalAppLanguage provides locale) {
        content()
    }
}

/**
 * Helper to get the localized context for string resources.
 */
@Composable
fun getLocalizedContext(languagePreferences: LanguagePreferences): Context {
    val locale by languagePreferences.currentLocale.collectAsState(initial = Locale("zh"))
    val context = LocalContext.current
    val config = Configuration(context.resources.configuration).apply {
        setLocale(locale)
    }
    return context.createConfigurationContext(config)
}
