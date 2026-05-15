package com.aitutor.app.ui.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: String? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null,
    val showPreview: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
) : ViewModel() {

    var uiState by mutableStateOf(CameraUiState())
        private set

    fun onPhotoCaptured(uri: String) {
        uiState = uiState.copy(
            capturedImageUri = uri,
            showPreview = true,
            errorMessage = null
        )
    }

    fun retakePhoto() {
        uiState = uiState.copy(
            capturedImageUri = null,
            showPreview = false,
            analysisResult = null,
            errorMessage = null
        )
    }

    fun confirmPhoto() {
        val uri = uiState.capturedImageUri ?: return
        uiState = uiState.copy(isAnalyzing = true)

        viewModelScope.launch {
            // TODO: Implement photo upload and analysis
            // For now, simulate with a delay
            kotlinx.coroutines.delay(2000)
            uiState = uiState.copy(
                isAnalyzing = false,
                analysisResult = "分析结果",
                errorMessage = null
            )
        }
    }

    fun resetState() {
        uiState = CameraUiState()
    }
}
