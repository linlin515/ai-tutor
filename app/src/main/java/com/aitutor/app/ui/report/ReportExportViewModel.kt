package com.aitutor.app.ui.report

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.ReportType
import com.aitutor.app.domain.usecase.ShareStudyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportExportUiState(
    val reportType: ReportType = ReportType.WEEKLY,
    val isLoading: Boolean = false,
    val progressMessage: String = "",
    val pdfUri: Uri? = null,
    val errorMessage: String? = null,
    val shareIntent: Intent? = null
)

@HiltViewModel
class ReportExportViewModel @Inject constructor(
    private val shareStudyReportUseCase: ShareStudyReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportExportUiState())
    val uiState: StateFlow<ReportExportUiState> = _uiState.asStateFlow()

    fun setReportType(type: ReportType) {
        _uiState.value = _uiState.value.copy(reportType = type)
    }

    fun generateReport() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                progressMessage = "正在准备...",
                errorMessage = null,
                pdfUri = null,
                shareIntent = null
            )

            val result = shareStudyReportUseCase.generateReport(
                reportType = state.reportType,
                nickname = "",
                onProgress = { msg ->
                    _uiState.value = _uiState.value.copy(progressMessage = msg)
                }
            )

            result.onSuccess { uri ->
                val shareIntent = shareStudyReportUseCase.createShareIntent(uri)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progressMessage = "报告已生成",
                    pdfUri = uri,
                    shareIntent = shareIntent
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progressMessage = "",
                    errorMessage = error.message ?: "生成失败"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetState() {
        _uiState.value = ReportExportUiState()
    }
}
