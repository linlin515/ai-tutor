package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.PendingSubmissionDao
import com.aitutor.app.data.local.dao.QuizRecordDao
import com.aitutor.app.data.local.entity.PendingSubmissionEntity
import com.aitutor.app.data.local.entity.QuizRecordEntity
import com.aitutor.app.data.remote.api.QuizApi
import com.aitutor.app.data.remote.dto.*
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.QuizRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val quizApi: QuizApi,
    private val quizRecordDao: QuizRecordDao,
    private val pendingSubmissionDao: PendingSubmissionDao
) : QuizRepository {

    override suspend fun generateQuiz(config: QuizConfig): Result<List<Question>> {
        return try {
            val response = quizApi.generateQuiz(
                QuizGenerateRequest(
                    subject = config.subject,
                    knowledgePoints = config.knowledgePoints,
                    difficulty = config.difficulty,
                    questionCount = config.questionCount,
                    questionTypes = config.questionTypes,
                    grade = config.grade
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val questions = body.data.questions.map { dto ->
                        Question(
                            id = dto.id,
                            type = parseQuestionType(dto.type),
                            content = dto.content,
                            options = dto.options,
                            knowledgePoint = dto.knowledgePoint
                        )
                    }
                    // Save quiz record
                    quizRecordDao.insertRecord(
                        QuizRecordEntity(
                            quizId = body.data.quizId,
                            subject = config.subject,
                            knowledgePoints = Gson().toJson(config.knowledgePoints),
                            difficulty = config.difficulty,
                            questionCount = questions.size
                        )
                    )
                    Result.success(questions)
                } else {
                    Result.failure(Exception(body?.message ?: "出题失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun submitAnswers(quizId: String, answers: Map<String, String>, duration: Int): Flow<QuizSubmitResult> = flow {
        try {
            val response = quizApi.submitQuiz(
                QuizSubmitRequest(
                    quizId = quizId,
                    answers = answers,
                    durationSeconds = duration
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val result = QuizResult(
                        quizId = body.data.quizId,
                        results = body.data.results.map { r ->
                            QuestionResult(
                                questionId = r.questionId,
                                isCorrect = r.isCorrect,
                                correctAnswer = r.correctAnswer,
                                userAnswer = r.userAnswer,
                                explanation = r.explanation,
                                score = r.score,
                                knowledgePoint = r.knowledgePoint
                            )
                        },
                        overallScore = body.data.overallScore,
                        masteryUpdate = body.data.masteryUpdate,
                        suggestions = body.data.suggestions,
                        wrongQuestionsAdded = body.data.wrongQuestionsAdded
                    )
                    // Update local record
                    quizRecordDao.insertRecord(
                        QuizRecordEntity(
                            quizId = quizId,
                            subject = "",
                            score = result.overallScore,
                            durationSeconds = duration
                        )
                    )
                    emit(QuizSubmitResult.Success(result))
                } else {
                    emit(QuizSubmitResult.Error(body?.message ?: "批改失败"))
                }
            } else {
                // Network failure -> cache locally
                pendingSubmissionDao.insert(
                    PendingSubmissionEntity(
                        quizId = quizId,
                        answers = Gson().toJson(answers),
                        durationSeconds = duration
                    )
                )
                emit(QuizSubmitResult.PendingLocal)
            }
        } catch (e: IOException) {
            pendingSubmissionDao.insert(
                PendingSubmissionEntity(
                    quizId = quizId,
                    answers = Gson().toJson(answers),
                    durationSeconds = duration
                )
            )
            emit(QuizSubmitResult.PendingLocal)
        } catch (e: Exception) {
            emit(QuizSubmitResult.Error(e.message ?: "未知错误"))
        }
    }

    override suspend fun syncPendingSubmissions() {
        val pendings = pendingSubmissionDao.getAll()
        for (pending in pendings) {
            try {
                val answers = Gson().fromJson(pending.answers, Map::class.java) as? Map<String, String> ?: emptyMap()
                val response = quizApi.submitQuiz(
                    QuizSubmitRequest(
                        quizId = pending.quizId,
                        answers = answers,
                        durationSeconds = pending.durationSeconds
                    )
                )
                if (response.isSuccessful) {
                    pendingSubmissionDao.deleteById(pending.id)
                }
            } catch (_: IOException) {
                if (pending.retryCount >= 3) {
                    pendingSubmissionDao.deleteById(pending.id)
                } else {
                    pendingSubmissionDao.updateRetryCount(pending.id, pending.retryCount + 1)
                }
                break
            }
        }
    }

    private fun parseQuestionType(type: String): QuestionType = when (type) {
        "single_choice" -> QuestionType.SINGLE_CHOICE
        "multiple_choice" -> QuestionType.MULTIPLE_CHOICE
        "fill_blank" -> QuestionType.FILL_BLANK
        else -> QuestionType.SINGLE_CHOICE
    }
}
