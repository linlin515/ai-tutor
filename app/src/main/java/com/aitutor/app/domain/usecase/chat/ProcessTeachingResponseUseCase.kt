package com.aitutor.app.domain.usecase.chat

import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for processing teaching responses in Socratic teaching mode (F42).
 *
 * Handles:
 * - Parsing teaching events from AI responses (mode_switch, socratic_question, correctness, teaching_complete)
 * - Tracking step progression within a teaching session
 * - Determining when to transition between teaching states
 */
class ProcessTeachingResponseUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    companion object {
        // SSE event markers that the AI may embed in responses
        const val MARKER_MODE_SWITCH = "[mode_switch]"
        const val MARKER_SOCRATIC_QUESTION = "[socratic_question]"
        const val MARKER_CORRECTNESS = "[correctness]"
        const val MARKER_TEACHING_COMPLETE = "[teaching_complete]"

        // Timeout: auto-exit TUTOR mode after 30 minutes of inactivity
        const val TUTOR_IDLE_TIMEOUT_MS = 30 * 60 * 1000L
    }

    /**
     * Data class representing a parsed teaching event from streaming content.
     */
    data class TeachingEvent(
        val type: EventType,
        val content: String,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class EventType {
        NORMAL_CONTENT,
        MODE_SWITCH,
        SOCRATIC_QUESTION,
        CORRECTNESS,
        TEACHING_COMPLETE
    }

    /**
     * Parse the accumulated streaming content for teaching-related events.
     * Returns a list of teaching events found in the content.
     */
    fun parseTeachingEvents(content: String): List<TeachingEvent> {
        val events = mutableListOf<TeachingEvent>()

        if (content.contains(MARKER_MODE_SWITCH)) {
            events.add(
                TeachingEvent(
                    type = EventType.MODE_SWITCH,
                    content = content.substringAfter(MARKER_MODE_SWITCH).substringBefore("\n").trim()
                )
            )
        }

        if (content.contains(MARKER_SOCRATIC_QUESTION)) {
            val question = content.substringAfter(MARKER_SOCRATIC_QUESTION)
                .substringBefore("\n").trim()
            events.add(
                TeachingEvent(
                    type = EventType.SOCRATIC_QUESTION,
                    content = question.ifEmpty { content }
                )
            )
        }

        if (content.contains(MARKER_CORRECTNESS)) {
            val correctnessBlock = content.substringAfter(MARKER_CORRECTNESS)
                .substringBefore("\n").trim()
            val isCorrect = correctnessBlock.contains("正确", ignoreCase = true) ||
                    correctnessBlock.contains("correct", ignoreCase = true)
            events.add(
                TeachingEvent(
                    type = EventType.CORRECTNESS,
                    content = correctnessBlock,
                    metadata = mapOf("isCorrect" to isCorrect.toString())
                )
            )
        }

        if (content.contains(MARKER_TEACHING_COMPLETE)) {
            events.add(
                TeachingEvent(
                    type = EventType.TEACHING_COMPLETE,
                    content = "教学完成"
                )
            )
        }

        return events
    }

    /**
     * Strip teaching markers from content for display.
     */
    fun stripTeachingMarkers(content: String): String {
        var cleaned = content
        val markers = listOf(
            MARKER_MODE_SWITCH,
            MARKER_SOCRATIC_QUESTION,
            MARKER_CORRECTNESS,
            MARKER_TEACHING_COMPLETE
        )
        for (marker in markers) {
            cleaned = cleaned.replace(marker, "")
        }
        return cleaned.trim()
    }

    /**
     * Check if the content indicates this is a teaching session response.
     */
    fun isTeachingResponse(content: String): Boolean {
        return content.contains(MARKER_SOCRATIC_QUESTION) ||
                content.contains(MARKER_CORRECTNESS) ||
                content.contains(MARKER_TEACHING_COMPLETE)
    }

    /**
     * Count the number of teaching steps in the conversation history.
     * Steps are inferred from socratic_question markers.
     */
    fun countTeachingSteps(messages: List<ChatMessage>): Int {
        return messages.count { msg ->
            !msg.isUser && (msg.content.contains(MARKER_SOCRATIC_QUESTION) ||
                    msg.content.contains(MARKER_CORRECTNESS))
        }
    }
}
