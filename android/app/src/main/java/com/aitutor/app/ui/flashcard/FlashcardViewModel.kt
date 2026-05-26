package com.aitutor.app.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.Flashcard
import com.aitutor.app.domain.usecase.flashcard.GetTodayCardsUseCase
import com.aitutor.app.domain.usecase.flashcard.ReviewCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val getTodayCardsUseCase: GetTodayCardsUseCase,
    private val reviewCardUseCase: ReviewCardUseCase
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Cards(
            val cards: List<Flashcard>,
            val currentIndex: Int,
            val totalCards: Int,
            val completedToday: Int,
            val dailyLimit: Int
        ) : UiState()
        object AllDone : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadTodayCards() {
        viewModelScope.launch {
            try {
                val cards = getTodayCardsUseCase()
                if (cards.isEmpty()) {
                    _uiState.value = UiState.AllDone
                } else {
                    _uiState.value = UiState.Cards(cards, 0, cards.size, 0, 10)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun reviewCard(cardId: String, judgment: String) {
        viewModelScope.launch {
            try {
                reviewCardUseCase(cardId, judgment)
                val current = _uiState.value as? UiState.Cards ?: return@launch
                val nextIndex = current.currentIndex + 1
                if (nextIndex >= current.totalCards) {
                    _uiState.value = UiState.AllDone
                } else {
                    _uiState.value = current.copy(currentIndex = nextIndex)
                }
            } catch (e: Exception) { /* handled */ }
        }
    }

    init { loadTodayCards() }
}
