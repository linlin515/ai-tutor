package com.aitutor.app.domain.model

/**
 * SSE events emitted by the solve photo streaming endpoint.
 */
sealed class SolveEvent {
    data class OcrResult(
        val text: String,
        val subject: String,
        val confidence: Float
    ) : SolveEvent()

    data class StepProgress(
        val step: Int,
        val total: Int,
        val title: String,
        val content: String
    ) : SolveEvent()

    data class AnswerResult(
        val answer: String,
        val explanation: String
    ) : SolveEvent()

    data class Complete(
        val status: String,
        val solveId: String
    ) : SolveEvent()

    data class SolveError(
        val code: String,
        val message: String
    ) : SolveEvent()
}
