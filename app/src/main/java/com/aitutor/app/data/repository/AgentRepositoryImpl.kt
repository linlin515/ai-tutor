package com.aitutor.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.aitutor.app.data.tool.registry.ToolRegistry
import com.aitutor.app.domain.repository.AgentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "agent_preferences"
)

@Singleton
class AgentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AgentRepository {

    companion object {
        private val AGENT_ENABLED_KEY = booleanPreferencesKey("agent_enabled")
    }

    override fun getAgentEnabled(): Flow<Boolean> {
        return context.agentDataStore.data.map { preferences ->
            preferences[AGENT_ENABLED_KEY] ?: false
        }
    }

    override suspend fun setAgentEnabled(enabled: Boolean) {
        context.agentDataStore.edit { preferences ->
            preferences[AGENT_ENABLED_KEY] = enabled
        }
    }

    override fun getToolsDefinitions(): List<Map<String, Any>> {
        return ToolRegistry.getToolDefinitions()
    }
}
