package com.aitutor.app.data.tool.registry

import com.aitutor.app.data.tool.engine.Tool
import com.aitutor.app.domain.model.ToolResult

/**
 * Singleton registry for all registered AI-callable tools.
 * Tools must be registered here before they can be used by the agent.
 */
object ToolRegistry {
    private val registeredTools = mutableMapOf<String, Tool>()

    /**
     * Register a tool implementation.
     * @param tool The tool to register. Must have a unique name.
     */
    fun register(tool: Tool) {
        registeredTools[tool.name] = tool
    }

    /**
     * Get all registered tools in OpenAI-compatible format.
     */
    fun getToolDefinitions(): List<Map<String, Any>> {
        return registeredTools.values.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters
                )
            )
        }
    }

    /**
     * Execute a registered tool by name with the given arguments.
     */
    suspend fun execute(name: String, args: Map<String, Any>): ToolResult {
        val tool = registeredTools[name]
            ?: return ToolResult(
                toolName = name,
                query = args.toString(),
                result = "",
                durationMs = 0,
                isError = true
            )
        return tool.execute(args)
    }
}
