package com.aitutor.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

@Singleton
class OnboardingDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val isOnboardingDone: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingDone() {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }
}
