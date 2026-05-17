package com.aitutor.app.ui.screen.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.WrongAnswerItem
import com.aitutor.app.domain.repository.WrongAnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val items: List<WrongAnswerItem> = emptyList(),
    val dueCount: Int = 0,
    val currentReviewIndex: Int = -1,
    val showAnswer: Boolean = false,
    val isLoading: Boolean = true,
    val selectedSubject: String = "all",
    val filterSubjects: List<String> = listOf("all", "math", "physics", "chemistry", "biology", "chinese", "english")
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadReviews()
    }

    fun loadReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            wrongAnswerRepository.getDueReviews().collect { items ->
                _uiState.update {
                    it.copy(
                        items = items,
                        dueCount = items.size,
                        isLoading = false,
                        currentReviewIndex = if (items.isNotEmpty()) 0 else -1,
                        showAnswer = false
                    )
                }
            }
        }
    }

    fun startReview() {
        _uiState.update {
            it.copy(
                currentReviewIndex = 0,
                showAnswer = false
            )
        }
    }

    fun toggleAnswer() {
        _uiState.update { it.copy(showAnswer = !it.showAnswer) }
    }

    fun markCorrect() {
        val index = _uiState.value.currentReviewIndex
        val items = _uiState.value.items
        if (index < 0 || index >= items.size) return

        viewModelScope.launch {
            wrongAnswerRepository.updateReview(items[index].id, isCorrect = true)
            advanceToNext()
        }
    }

    fun markIncorrect() {
        val index = _uiState.value.currentReviewIndex
        val items = _uiState.value.items
        if (index < 0 || index >= items.size) return

        viewModelScope.launch {
            wrongAnswerRepository.updateReview(items[index].id, isCorrect = false)
            advanceToNext()
        }
    }

    fun deleteItem() {
        val index = _uiState.value.currentReviewIndex
        val items = _uiState.value.items
        if (index < 0 || index >= items.size) return

        viewModelScope.launch {
            wrongAnswerRepository.deleteWrongAnswer(items[index].id)
            advanceToNext()
        }
    }

    fun selectSubject(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
        viewModelScope.launch {
            if (subject == "all") {
                wrongAnswerRepository.getDueReviews().collect { items ->
                    _uiState.update { it.copy(items = items, dueCount = items.size) }
                }
            } else {
                wrongAnswerRepository.getWrongAnswersBySubject(subject).collect { items ->
                    _uiState.update { it.copy(items = items, dueCount = items.size) }
                }
            }
        }
    }

    private fun advanceToNext() {
        val nextIndex = _uiState.value.currentReviewIndex + 1
        val items = _uiState.value.items
        if (nextIndex >= items.size) {
            // Review complete
            _uiState.update {
                it.copy(
                    currentReviewIndex = -1,
                    showAnswer = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentReviewIndex = nextIndex,
                    showAnswer = false
                )
            }
        }
    }
}
