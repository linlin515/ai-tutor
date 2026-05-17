package com.aitutor.app.data.tool.impl

import com.aitutor.app.data.tool.engine.Tool
import com.aitutor.app.domain.model.ToolResult
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Web search tool that uses OkHttpClient to call a configurable search API.
 * Falls back to current date/time info when the API is unavailable.
 */
class WebSearchTool(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String = "https://api.example.com"
) : Tool {

    override val name: String = "web_search"

    override val description: String =
        "Search the web for current information. Returns search results for a given query."

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "query" to mapOf(
                "type" to "string",
                "description" to "The search query to look up"
            )
        ),
        "required" to listOf("query")
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val query = args["query"]?.toString() ?: return ToolResult(
            toolName = name, query = null, result = "",
            durationMs = 0, isError = true
        )
        val startTime = System.currentTimeMillis()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AiTutor/1.0")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "No response body"
            ToolResult(
                toolName = name, query = query, result = body,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            // Fallback: return current date/time as mock search result
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = dateFormat.format(Date())
            ToolResult(
                toolName = name, query = query,
                result = "Mock search result for '$query':\nCurrent server time: $currentTime\n(Web search API unavailable, using mock data)",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
