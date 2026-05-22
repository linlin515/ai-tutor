package com.aitutor.app.ui.screen.wronganswer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.WrongAnswerItem
import com.aitutor.app.domain.repository.WrongAnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WrongAnswerUiState(
    val items: List<WrongAnswerItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSubject: String = "all",
    val subjects: List<String> = listOf(
        "all", "math", "physics", "chemistry", "biology", "chinese", "english"
    ),
    val error: String? = null
)

@HiltViewModel
class WrongAnswerViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongAnswerUiState())
    val uiState: StateFlow<WrongAnswerUiState> = _uiState.asStateFlow()

    init {
        loadWrongAnswers()
    }

    fun loadWrongAnswers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val subject = _uiState.value.selectedSubject
                val flow = if (subject == "all") {
                    wrongAnswerRepository.getAllWrongAnswers()
                } else {
                    wrongAnswerRepository.getWrongAnswersBySubject(subject)
                }
                flow.collect { items ->
                    _uiState.update {
                        it.copy(items = items, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    fun selectSubject(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
        loadWrongAnswers()
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                wrongAnswerRepository.deleteWrongAnswer(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除失败")
                }
            }
        }
    }

    fun markMastered(id: String) {
        viewModelScope.launch {
            try {
                wrongAnswerRepository.updateReview(id, isCorrect = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "操作失败")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
