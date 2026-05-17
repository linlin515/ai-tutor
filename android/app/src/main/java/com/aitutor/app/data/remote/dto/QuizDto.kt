package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class QuizGenerateRequest(
    val subject: String,
    @SerializedName("knowledge_points") val knowledgePoints: List<String> = emptyList(),
    val difficulty: String = "medium",
    @SerializedName("question_count") val questionCount: Int = 5,
    @SerializedName("question_types") val questionTypes: List<String> = listOf("single_choice", "multiple_choice", "fill_blank"),
    val grade: String? = null
)

data class QuizGenerateResponse(
    @SerializedName("quiz_id") val quizId: String,
    val questions: List<QuestionDto>,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("estimated_time_min") val estimatedTimeMin: Int
)

data class QuestionDto(
    val id: String,
    val type: String,
    val content: String,
    val options: List<String>? = null,
    @SerializedName("knowledge_point") val knowledgePoint: String,
    val difficulty: String = "medium"
)

data class QuizSubmitRequest(
    @SerializedName("quiz_id") val quizId: String,
    val answers: Map<String, String>,
    @SerializedName("duration_seconds") val durationSeconds: Int
)

data class QuizSubmitResponse(
    @SerializedName("quiz_id") val quizId: String,
    val results: List<QuestionResultDto>,
    @SerializedName("overall_score") val overallScore: Float,
    @SerializedName("mastery_update") val masteryUpdate: Map<String, Float> = emptyMap(),
    val suggestions: List<String> = emptyList(),
    @SerializedName("wrong_questions_added") val wrongQuestionsAdded: List<String> = emptyList()
)

data class QuestionResultDto(
    @SerializedName("question_id") val questionId: String,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("user_answer") val userAnswer: String,
    val explanation: String,
    val score: Float,
    @SerializedName("knowledge_point") val knowledgePoint: String
)
