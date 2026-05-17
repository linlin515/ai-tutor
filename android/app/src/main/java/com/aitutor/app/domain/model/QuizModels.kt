package com.aitutor.app.domain.model

data class QuizConfig(
    val subject: String,
    val knowledgePoints: List<String> = emptyList(),
    val difficulty: String = "medium",
    val questionCount: Int = 5,
    val questionTypes: List<String> = listOf("single_choice", "multiple_choice", "fill_blank"),
    val grade: String? = null
)

data class Question(
    val id: String,
    val type: QuestionType,
    val content: String,
    val options: List<String>? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val isCorrect: Boolean? = null,
    val knowledgePoint: String = ""
)

data class QuestionResult(
    val questionId: String,
    val isCorrect: Boolean,
    val correctAnswer: String,
    val userAnswer: String,
    val explanation: String,
    val score: Float,
    val knowledgePoint: String
)

data class QuizResult(
    val quizId: String,
    val results: List<QuestionResult>,
    val overallScore: Float,
    val masteryUpdate: Map<String, Float> = emptyMap(),
    val suggestions: List<String> = emptyList(),
    val wrongQuestionsAdded: List<String> = emptyList()
)

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.IDLE,
    val config: QuizConfig = QuizConfig(subject = "math"),
    val questions: List<Question> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
    val result: QuizResult? = null,
    val error: String? = null,
    val retryPending: Boolean = false
)

enum class QuizPhase {
    IDLE,
    GENERATING,
    ANSWERING,
    SUBMITTING,
    GRADING,
    DONE,
    ERROR
}

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    FILL_BLANK
}

sealed class QuizSubmitResult {
    data class Success(val result: QuizResult) : QuizSubmitResult()
    data object PendingLocal : QuizSubmitResult()
    data class Error(val message: String) : QuizSubmitResult()
}
