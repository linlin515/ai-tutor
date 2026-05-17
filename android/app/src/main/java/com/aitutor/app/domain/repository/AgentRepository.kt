package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

/**
 * Repository that manages agent configuration and tool definitions.
 */
interface AgentRepository {

    /**
     * Returns a [Flow] that emits the current enabled/disabled state of the AI agent.
     */
    fun getAgentEnabled(): Flow<Boolean>

    /**
     * Persists the enabled/disabled state of the AI agent.
     */
    suspend fun setAgentEnabled(enabled: Boolean)

    /**
     * Returns OpenAI-compatible tool definitions for the built-in tools.
     *
     * Currently provides definitions for:
     * - web_search  : Searches the web for current information.
     * - calculator  : Evaluates mathematical expressions.
     * - datetime    : Returns the current date and time.
     */
    fun getToolsDefinitions(): List<Map<String, Any>>
}
