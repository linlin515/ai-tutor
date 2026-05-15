package com.aitutor.app.ui.camera

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: String? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null,
    val showPreview: Boolean = false,
    val conversationId: Long = -1L  // The conversation created for this analysis
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    @ApplicationContext private val context: Context
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
            errorMessage = null,
            conversationId = -1L
        )
    }

    fun confirmPhoto() {
        val uri = uiState.capturedImageUri ?: return
        uiState = uiState.copy(isAnalyzing = true, errorMessage = null)

        viewModelScope.launch {
            // Create a temporary conversation for the photo solving
            val conversationId = chatRepository.createConversation("拍照解题")
            uiState = uiState.copy(conversationId = conversationId)

            val result = chatRepository.solvePhoto(
                imageUri = uri,
                conversationId = conversationId,
                grade = null
            )

            result.fold(
                onSuccess = { answer ->
                    // Save the user's image message
                    val userMsg = ChatMessage(
                        conversationId = conversationId,
                        content = "[拍照解题] 照片已上传并分析",
                        isUser = true,
                        contentType = MessageType.IMAGE,
                        status = MessageStatus.SENT
                    )
                    chatRepository.insertMessage(userMsg)

                    // Save the AI's analysis response
                    val aiMsg = ChatMessage(
                        conversationId = conversationId,
                        content = answer,
                        isUser = false,
                        status = MessageStatus.SENT
                    )
                    chatRepository.insertMessage(aiMsg)

                    // Update conversation title with the analysis summary
                    val title = answer.take(30) + if (answer.length > 30) "..." else ""
                    chatRepository.updateConversationTitle(conversationId, title)

                    uiState = uiState.copy(
                        isAnalyzing = false,
                        analysisResult = answer,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isAnalyzing = false,
                        errorMessage = error.message ?: "分析失败"
                    )
                }
            )
        }
    }

    fun resetState() {
        uiState = CameraUiState()
    }
}
