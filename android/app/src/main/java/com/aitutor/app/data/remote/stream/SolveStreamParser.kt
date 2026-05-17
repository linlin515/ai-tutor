package com.aitutor.app.data.remote.stream

import com.aitutor.app.data.remote.dto.AnswerResultDto
import com.aitutor.app.data.remote.dto.OcrResultDto
import com.aitutor.app.data.remote.dto.SolveCompleteDto
import com.aitutor.app.data.remote.dto.SolveErrorDto
import com.aitutor.app.data.remote.dto.StepProgressDto
import com.aitutor.app.domain.model.SolveEvent
import com.google.gson.Gson

/**
 * Parses raw SSE event type + data lines into [SolveEvent] domain objects.
 * Stateless — safe to share / inject as a singleton.
 */
class SolveStreamParser(
    private val gson: Gson = Gson()
) {
    /**
     * Parse a single SSE event.
     *
     * @param eventType the SSE event name (e.g. "ocr_result", "step", "answer", "complete", "error")
     * @param data      the JSON payload of the data: line
     * @return the corresponding [SolveEvent] or null if the event type is unknown / payload is malformed
     */
    fun parseEvent(eventType: String, data: String): SolveEvent? {
        return try {
            when (eventType) {
                "ocr_result" -> {
                    val dto = gson.fromJson(data, OcrResultDto::class.java)
                    SolveEvent.OcrResult(
                        text = dto.text,
                        subject = dto.subject,
                        confidence = dto.confidence
                    )
                }
                "step" -> {
                    val dto = gson.fromJson(data, StepProgressDto::class.java)
                    SolveEvent.StepProgress(
                        step = dto.step,
                        total = dto.total,
                        title = dto.title,
                        content = dto.content
                    )
                }
                "answer" -> {
                    val dto = gson.fromJson(data, AnswerResultDto::class.java)
                    SolveEvent.AnswerResult(
                        answer = dto.answer,
                        explanation = dto.explanation
                    )
                }
                "complete" -> {
                    val dto = gson.fromJson(data, SolveCompleteDto::class.java)
                    SolveEvent.Complete(
                        status = dto.status,
                        solveId = dto.solveId
                    )
                }
                "error" -> {
                    val dto = gson.fromJson(data, SolveErrorDto::class.java)
                    SolveEvent.SolveError(
                        code = dto.code,
                        message = dto.message
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
