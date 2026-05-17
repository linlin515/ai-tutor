package com.aitutor.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.User
import com.aitutor.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editNickname: String = "",
    val editGrade: String = "",
    val errorMessage: String? = null,
    val isLoggingOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            authRepository.getProfile().fold(
                onSuccess = { user ->
                    uiState = uiState.copy(
                        user = user,
                        isLoading = false,
                        editNickname = user.nickname,
                        editGrade = user.grade ?: ""
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            )
        }
    }

    fun startEditing() {
        uiState = uiState.copy(
            isEditing = true,
            editNickname = uiState.user?.nickname ?: "",
            editGrade = uiState.user?.grade ?: ""
        )
    }

    fun cancelEditing() {
        uiState = uiState.copy(isEditing = false)
    }

    fun updateNickname(nickname: String) {
        uiState = uiState.copy(editNickname = nickname)
    }

    fun updateGrade(grade: String) {
        uiState = uiState.copy(editGrade = grade)
    }

    fun saveProfile() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            authRepository.updateProfile(
                nickname = uiState.editNickname,
                grade = uiState.editGrade.ifBlank { null },
                avatar = null
            ).fold(
                onSuccess = { user ->
                    uiState = uiState.copy(
                        user = user,
                        isLoading = false,
                        isEditing = false
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            )
        }
    }

    fun logout() {
        uiState = uiState.copy(isLoggingOut = true)
        viewModelScope.launch {
            authRepository.clearToken()
            uiState = uiState.copy(isLoggingOut = false)
        }
    }
}
