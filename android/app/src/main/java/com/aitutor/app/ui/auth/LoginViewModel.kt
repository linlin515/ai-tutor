package com.aitutor.app.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val email: String = "",
    val isRegister: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun updatePhone(phone: String) {
        // Only allow digits, max 11 chars
        val filtered = phone.filter { it.isDigit() }.take(11)
        uiState = uiState.copy(phone = filtered, errorMessage = null)
    }

    fun updatePassword(password: String) {
        uiState = uiState.copy(password = password, errorMessage = null)
    }

    fun updateEmail(email: String) {
        uiState = uiState.copy(email = email, errorMessage = null)
    }

    fun toggleMode() {
        uiState = uiState.copy(
            isRegister = !uiState.isRegister,
            errorMessage = null
        )
    }

    fun submit() {
        val phone = uiState.phone
        val password = uiState.password

        // Validation
        if (phone.length != 11) {
            uiState = uiState.copy(errorMessage = "请输入正确的11位手机号")
            return
        }
        if (password.length < 6 || password.length > 20) {
            uiState = uiState.copy(errorMessage = "密码长度为6-20位")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = if (uiState.isRegister) {
                authRepository.register(phone, password, uiState.email.ifBlank { null })
            } else {
                authRepository.login(phone, password)
            }

            result.fold(
                onSuccess = {
                    uiState = uiState.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "操作失败，请重试"
                    )
                }
            )
        }
    }
}
