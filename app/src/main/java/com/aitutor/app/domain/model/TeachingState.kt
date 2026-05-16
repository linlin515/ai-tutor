package com.aitutor.app.domain.model

/**
 * TeachingState represents the current state of the Socratic teaching process.
 */
sealed class TeachingState {

    /** No active teaching session. */
    data object Idle : TeachingState()

    /** The AI is asking a guiding question. */
    data class Questioning(
        val question: String,
        val step: Int = 1,
        val totalSteps: Int = 1
    ) : TeachingState()

    /** The AI is evaluating the student's answer. */
    data class Evaluating(
        val isCorrect: Boolean,
        val feedback: String,
        val step: Int = 1,
        val totalSteps: Int = 1
    ) : TeachingState()

    /** The teaching session is complete. */
    data object Complete : TeachingState()

    val isActive: Boolean
        get() = this !is Idle

    val displayText: String
        get() = when (this) {
            is Idle -> ""
            is Questioning -> "引导提问中..."
            is Evaluating -> if (isCorrect) "回答正确！" else "再想想..."
            is Complete -> "教学完成"
        }
}
