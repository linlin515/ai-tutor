package com.aitutor.app.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.remote.dto.SolveEventUi
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.MessageType
import com.aitutor.app.domain.model.SolveEvent
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.SolveRepository
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: String? = null,
    val isAnalyzing: Boolean = false,
    val isCompressing: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null,
    val showPreview: Boolean = false,
    val conversationId: Long = -1L,
    val selectedSubject: String = "auto",
    val solveEvents: List<SolveEventUi> = emptyList()
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val solveRepository: SolveRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var uiState by mutableStateOf(CameraUiState())
        private set

    // ---- ML Kit OCR real-time detection state ----

    /** Detected text bounding boxes from the latest analyzed camera frame. */
    var ocrResults by mutableStateOf<List<OcrBoundingBox>>(emptyList())
        private set

    /** Width of the camera frame that was analyzed (raw image space). */
    var ocrFrameWidth by mutableIntStateOf(0)
        private set

    /** Height of the camera frame that was analyzed (raw image space). */
    var ocrFrameHeight by mutableIntStateOf(0)
        private set

    /** Rotation degrees of the analyzed camera frame (0, 90, 180, 270). */
    var ocrRotationDegrees by mutableIntStateOf(0)
        private set

    /** Reusable ML Kit TextRecognizer instance (on-device). */
    val textRecognizer: TextRecognizer = TextRecognition.getClient()

    // ---- end OCR state ----

    private var solveJob: Job? = null

    fun onPhotoCaptured(uri: String) {
        uiState = uiState.copy(
            capturedImageUri = uri,
            showPreview = true,
            errorMessage = null,
            solveEvents = emptyList()
        )
    }

    fun onSubjectChanged(subject: String) {
        uiState = uiState.copy(selectedSubject = subject)
    }

    fun onOcrResult(
        boxes: List<OcrBoundingBox>,
        frameWidth: Int,
        frameHeight: Int,
        rotationDegrees: Int
    ) {
        ocrResults = boxes
        ocrFrameWidth = frameWidth
        ocrFrameHeight = frameHeight
        ocrRotationDegrees = rotationDegrees
    }

    fun retakePhoto() {
        solveJob?.cancel()
        uiState = uiState.copy(
            capturedImageUri = null,
            showPreview = false,
            analysisResult = null,
            errorMessage = null,
            conversationId = -1L,
            solveEvents = emptyList()
        )
    }

    fun confirmPhoto() {
        val uri = uiState.capturedImageUri ?: return

        uiState = uiState.copy(
            isAnalyzing = true,
            isCompressing = true,
            errorMessage = null,
            solveEvents = emptyList()
        )

        viewModelScope.launch {
            // Compress image on IO dispatcher
            val imageBytes = compressImage(uri)
            uiState = uiState.copy(isCompressing = false)

            if (imageBytes == null) {
                uiState = uiState.copy(
                    isAnalyzing = false,
                    errorMessage = "图片压缩失败"
                )
                return@launch
            }

            // Create a temporary conversation for this solve
            val conversationId = chatRepository.createConversation("拍照解题")
            uiState = uiState.copy(conversationId = conversationId)

            // Start SSE solve streaming
            solveJob = viewModelScope.launch {
                solveRepository.streamSolvePhoto(
                    imageBytes = imageBytes,
                    subject = uiState.selectedSubject,
                    grade = null
                )
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        uiState = uiState.copy(
                            isAnalyzing = false,
                            errorMessage = e.message ?: "解题分析失败"
                        )
                    }
                    .collect { event ->
                        handleSolveEvent(event, conversationId, imageBytes)
                    }
            }
        }
    }

    private suspend fun handleSolveEvent(
        event: SolveEvent,
        conversationId: Long,
        imageBytes: ByteArray
    ) {
        when (event) {
            is SolveEvent.OcrResult -> {
                uiState = uiState.copy(
                    solveEvents = uiState.solveEvents + SolveEventUi.Ocr(
                        text = event.text,
                        subject = event.subject,
                        confidence = event.confidence
                    )
                )
            }

            is SolveEvent.StepProgress -> {
                uiState = uiState.copy(
                    solveEvents = uiState.solveEvents + SolveEventUi.Step(
                        step = event.step,
                        total = event.total,
                        title = event.title,
                        content = event.content
                    )
                )
            }

            is SolveEvent.AnswerResult -> {
                val fullAnswer = buildString {
                    append(event.explanation)
                    if (event.answer.isNotEmpty()) {
                        append("\n\n**答案:** ${event.answer}")
                    }
                }

                // Save user image message
                val userMsg = ChatMessage(
                    conversationId = conversationId,
                    content = "[拍照解题] 照片已上传并分析",
                    isUser = true,
                    contentType = MessageType.IMAGE,
                    status = MessageStatus.SENT
                )
                chatRepository.insertMessage(userMsg)

                // Save AI answer
                val aiMsg = ChatMessage(
                    conversationId = conversationId,
                    content = fullAnswer,
                    isUser = false,
                    status = MessageStatus.SENT
                )
                chatRepository.insertMessage(aiMsg)

                // Update conversation title
                val title = fullAnswer.take(30) + if (fullAnswer.length > 30) "..." else ""
                chatRepository.updateConversationTitle(conversationId, title)

                uiState = uiState.copy(
                    isAnalyzing = false,
                    analysisResult = fullAnswer,
                    solveEvents = uiState.solveEvents + SolveEventUi.Answer(
                        answer = event.answer,
                        explanation = event.explanation
                    ),
                    errorMessage = null
                )
            }

            is SolveEvent.Complete -> {
                // Solve complete — do nothing extra; answer should have arrived already
            }

            is SolveEvent.SolveError -> {
                uiState = uiState.copy(
                    isAnalyzing = false,
                    errorMessage = event.message
                )
            }
        }
    }

    /**
     * Read, decode, scale and compress the captured image to JPEG bytes.
     */
    private fun compressImage(uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // Scale down if larger than 1920px on the longest edge
            val maxDimension = 1920f
            val scale = minOf(
                maxDimension / bitmap.width,
                maxDimension / bitmap.height,
                1f
            )

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            bytes
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetState() {
        solveJob?.cancel()
        uiState = CameraUiState()
    }

    override fun onCleared() {
        super.onCleared()
        solveJob?.cancel()
        textRecognizer.close()
    }
}
