package com.aitutor.app.ui.screen.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.QuizRepository
import com.aitutor.app.domain.usecase.quiz.GenerateQuizUseCase
import com.aitutor.app.domain.usecase.quiz.SubmitQuizUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val submitQuizUseCase: SubmitQuizUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var quizId: String = ""
    private var startTime: Long = System.currentTimeMillis()

    fun generateQuiz(config: QuizConfig) {
        _uiState.update { it.copy(phase = QuizPhase.GENERATING, config = config, error = null) }
        startTime = System.currentTimeMillis()
        viewModelScope.launch {
            val result = generateQuizUseCase(config)
            result.fold(
                onSuccess = { questions ->
                    _uiState.update {
                        it.copy(
                            phase = QuizPhase.ANSWERING,
                            questions = questions,
                            answers = emptyMap()
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(phase = QuizPhase.ERROR, error = e.message ?: "出题失败")
                    }
                }
            )
        }
    }

    fun submitAnswer(questionId: String, answer: String) {
        val currentAnswers = _uiState.value.answers.toMutableMap()
        currentAnswers[questionId] = answer
        _uiState.update { it.copy(answers = currentAnswers) }
    }

    fun submitAllAnswers() {
        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        _uiState.update { it.copy(phase = QuizPhase.SUBMITTING) }

        viewModelScope.launch {
            submitQuizUseCase(quizId, _uiState.value.answers, duration).collect { result ->
                when (result) {
                    is QuizSubmitResult.Success -> {
                        _uiState.update {
                            it.copy(phase = QuizPhase.DONE, result = result.result)
                        }
                    }
                    is QuizSubmitResult.PendingLocal -> {
                        _uiState.update {
                            it.copy(phase = QuizPhase.DONE, retryPending = true)
                        }
                    }
                    is QuizSubmitResult.Error -> {
                        _uiState.update {
                            it.copy(phase = QuizPhase.ERROR, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun retryGenerate() {
        generateQuiz(_uiState.value.config)
    }

    fun reset() {
        _uiState.value = QuizUiState()
    }

    fun loadMockQuiz(config: QuizConfig) {
        _uiState.update { it.copy(phase = QuizPhase.GENERATING) }
        val mockQuestions = listOf(
            Question(
                id = "q1",
                type = QuestionType.SINGLE_CHOICE,
                content = "二次函数 y = ax² + bx + c 的图像是什么？",
                options = listOf("直线", "抛物线", "双曲线", "椭圆"),
                knowledgePoint = "二次函数"
            ),
            Question(
                id = "q2",
                type = QuestionType.MULTIPLE_CHOICE,
                content = "以下哪些是勾股数？",
                options = listOf("3, 4, 5", "5, 12, 13", "6, 8, 10", "1, 2, 3"),
                knowledgePoint = "勾股定理"
            ),
            Question(
                id = "q3",
                type = QuestionType.FILL_BLANK,
                content = "三角形内角和等于 ____ 度。",
                knowledgePoint = "三角形"
            )
        )
        quizId = "mock-quiz-${System.currentTimeMillis()}"
        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                phase = QuizPhase.ANSWERING,
                questions = mockQuestions,
                config = config
            )
        }
    }

    fun loadMockResult() {
        val questions = _uiState.value.questions
        val answers = _uiState.value.answers

        val results = questions.map { q ->
            val userAnswer = answers[q.id] ?: ""
            val isCorrect = when (q.id) {
                "q1" -> userAnswer == "B" || userAnswer == "抛物线"
                "q2" -> userAnswer.contains("3") && userAnswer.contains("4") && userAnswer.contains("5")
                "q3" -> userAnswer == "180"
                else -> false
            }
            val correctAns = when (q.id) {
                "q1" -> "B"
                "q2" -> "3, 4, 5; 5, 12, 13"
                "q3" -> "180"
                else -> ""
            }
            val expl = when (q.id) {
                "q1" -> "抛物线正确！二次函数的图像是开口向上或向下的抛物线。"
                "q2" -> "勾股数满足 a² + b² = c²"
                "q3" -> "三角形内角和恒等于180度。"
                else -> ""
            }
            QuestionResult(
                questionId = q.id,
                isCorrect = isCorrect,
                correctAnswer = correctAns,
                userAnswer = userAnswer,
                explanation = expl,
                score = if (isCorrect) 1f else 0f,
                knowledgePoint = q.knowledgePoint
            )
        }
        val correctCount = results.count { it.isCorrect }
        val overallScore = if (results.isNotEmpty()) correctCount.toFloat() / results.size else 0f

        _uiState.update {
            it.copy(
                phase = QuizPhase.DONE,
                result = QuizResult(
                    quizId = quizId,
                    results = results,
                    overallScore = overallScore,
                    suggestions = if (overallScore < 0.8f) listOf("建议复习相关知识点") else listOf("掌握得不错！"),
                    wrongQuestionsAdded = results.filter { !it.isCorrect }.map { it.questionId }
                )
            )
        }
    }
}
