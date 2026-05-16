package com.aitutor.app.data.tool.impl

import com.aitutor.app.data.tool.engine.Tool
import com.aitutor.app.domain.model.ToolResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date/Time tool that returns current date, time, day of week, and timezone info.
 */
class DateTimeTool : Tool {

    override val name: String = "datetime"

    override val description: String =
        "Get the current date, time, day of week, and timezone information. " +
            "Useful for answering questions about today's date, current time, or day of the week."

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "format" to mapOf(
                "type" to "string",
                "description" to "Optional: 'date', 'time', 'datetime', 'weekday', or 'all' (default: 'all')",
                "enum" to listOf("date", "time", "datetime", "weekday", "all")
            )
        )
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val startTime = System.currentTimeMillis()
        val format = args["format"]?.toString() ?: "all"
        val now = Date()
        val tz = TimeZone.getDefault()

        val result = when (format) {
            "date" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            "time" -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            "datetime" -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(now)
            "weekday" -> {
                val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val dayOfWeek = SimpleDateFormat("u", Locale.getDefault()).format(now).toInt()
                dayNames[dayOfWeek % 7]
            }
            else -> {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
                val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val dayOfWeek = SimpleDateFormat("u", Locale.getDefault()).format(now).toInt()
                buildString {
                    appendLine("Current date: $dateStr")
                    appendLine("Current time: $timeStr")
                    appendLine("Day of week: ${dayNames[dayOfWeek % 7]}")
                    appendLine("Timezone: ${tz.displayName} (${tz.getDisplayName(false, TimeZone.SHORT)})")
                    append("Unix timestamp: ${System.currentTimeMillis() / 1000}")
                }
            }
        }

        return ToolResult(
            toolName = name,
            query = "format=$format",
            result = result,
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}
