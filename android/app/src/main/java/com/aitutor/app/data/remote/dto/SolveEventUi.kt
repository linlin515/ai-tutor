package com.aitutor.app.data.remote.dto

/**
 * UI-friendly representation of a solve SSE event for rendering in the camera screen.
 */
sealed class SolveEventUi {
    data class Ocr(
        val text: String,
        val subject: String,
        val confidence: Float
    ) : SolveEventUi()

    data class Step(
        val step: Int,
        val total: Int,
        val title: String,
        val content: String
    ) : SolveEventUi()

    data class Answer(
        val answer: String,
        val explanation: String
    ) : SolveEventUi()
}
