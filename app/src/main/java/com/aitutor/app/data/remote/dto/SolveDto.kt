package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for multipart form data request to solve/photo endpoint.
 */
data class SolvePhotoRequest(
    val subject: String = "auto",
    val grade: String? = null
)

/**
 * SSE payload: ocr_result event
 */
data class OcrResultDto(
    @SerializedName("text") val text: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("confidence") val confidence: Float
)

/**
 * SSE payload: step event
 */
data class StepProgressDto(
    @SerializedName("step") val step: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String
)

/**
 * SSE payload: answer event
 */
data class AnswerResultDto(
    @SerializedName("answer") val answer: String,
    @SerializedName("explanation") val explanation: String
)

/**
 * SSE payload: complete event
 */
data class SolveCompleteDto(
    @SerializedName("status") val status: String,
    @SerializedName("solve_id") val solveId: String
)

/**
 * SSE payload: error event
 */
data class SolveErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)

/**
 * Request body for step retry.
 */
data class SolveRetryRequest(
    @SerializedName("solve_id") val solveId: String,
    @SerializedName("step_index") val stepIndex: Int,
    @SerializedName("question") val question: String
)
