package com.aitutor.app.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the conversation list.
 * Used by ConversationListSheet when integrated as a standalone component.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword

    val conversations: StateFlow<List<Conversation>> = chatRepository.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun createConversation() {
        viewModelScope.launch {
            chatRepository.createConversation("")
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
        }
    }
}
