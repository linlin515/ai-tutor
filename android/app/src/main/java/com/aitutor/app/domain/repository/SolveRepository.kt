package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.SolveEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository for photo-based problem solving with SSE streaming.
 */
interface SolveRepository {

    /**
     * Upload a photo and stream the solve process events.
     *
     * @param imageBytes JPEG-compressed image bytes
     * @param subject    subject hint ("math", "physics", "chemistry", "biology", "chinese", "english", "auto")
     * @param grade      optional student grade for contextualised explanations
     * @return flow of [SolveEvent] representing OCR results, step progress, final answer, etc.
     */
    fun streamSolvePhoto(
        imageBytes: ByteArray,
        subject: String = "auto",
        grade: String? = null
    ): Flow<SolveEvent>

    /**
     * Retry a specific step in the solve process.
     *
     * @param solveId   the solve ID returned in the [SolveEvent.Complete] event
     * @param stepIndex the 1-based index of the step to retry
     * @param question  additional question or clarification from the user
     * @return success with a message or failure
     */
    suspend fun retryStep(
        solveId: String,
        stepIndex: Int,
        question: String
    ): Result<String>

    /**
     * Get adaptive step-by-step explanation for a text question.
     *
     * @param question the question text
     * @param grade    student grade for adaptive difficulty
     * @param subject  subject category
     * @return success with steps or failure
     */
    suspend fun getSolveSteps(
        question: String,
        grade: String = "auto",
        subject: String = "auto"
    ): Result<com.aitutor.app.data.remote.dto.SolveStepsResponse>
}
