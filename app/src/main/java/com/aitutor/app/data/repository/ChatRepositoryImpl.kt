package com.aitutor.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.mapper.toDomain
import com.aitutor.app.data.mapper.toEntity
import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.ChatStreamApi
import com.aitutor.app.data.remote.dto.ChatCompletionRequest
import com.aitutor.app.data.remote.dto.ChatMessageDto
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val chatStreamApi: ChatStreamApi,
    private val tokenManager: TokenManager,
    private val aiTutorApi: AiTutorApi,
    @ApplicationContext private val context: Context
) : ChatRepository {

    // ===== Conversation Operations =====

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(id: Long): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    override suspend fun createConversation(title: String, modelId: String): Long {
        val entity = ConversationEntity(
            title = title,
            modelId = modelId
        )
        return conversationDao.insert(entity)
    }

    override suspend fun updateConversationTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
        conversationDao.updateTimestamp(id, System.currentTimeMillis())
    }

    override suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    // ===== Message Operations =====

    override fun getMessagesByConversation(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.getByConversationFlow(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertMessage(message: ChatMessage): Long {
        val entity = message.toEntity()
        val id = messageDao.insert(entity)

        // Update conversation timestamp and message count
        conversationDao.updateTimestamp(message.conversationId, System.currentTimeMillis())

        return id
    }

    override suspend fun updateMessageStatus(id: Long, status: MessageStatus) {
        messageDao.updateStatus(id, status.name)
    }

    override suspend fun deleteAllMessages(conversationId: Long) {
        messageDao.deleteByConversation(conversationId)
    }

    // ===== Streaming =====

    override fun streamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        grade: String?,
        role: String?,
        systemPrompt: String?
    ): Flow<String> {
        val chatMessages = mutableListOf<ChatMessageDto>()

        // Prepend system prompt if in tutor mode
        if (role == "tutor" || systemPrompt != null) {
            val prompt = systemPrompt ?: "你是一个AI学习助手，请用引导式教学方法帮助学生思考和解决问题。采用苏格拉底式提问，逐步引导学生自己找到答案，而不是直接给出答案。"
            chatMessages.add(ChatMessageDto(role = "system", content = prompt))
        }

        chatMessages.addAll(messages.map { msg ->
            ChatMessageDto(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.content
            )
        })

        val request = ChatCompletionRequest(
            model = modelId,
            messages = chatMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            grade = grade,
            role = role,
            systemPrompt = systemPrompt
        )

        return chatStreamApi.streamChat(request, tokenManager.getToken())
    }

    // ===== Image Solving =====

    override suspend fun solvePhoto(
        imageUri: String,
        conversationId: Long,
        grade: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Read and compress image
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开图片文件"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("无法解码图片"))
            }

            // Compress: scale down if larger than 1920px, then compress to JPEG quality 80
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
            val imageBytes = outputStream.toByteArray()

            if (!scaledBitmap.equals(bitmap)) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            // Create multipart body
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "photo_${System.currentTimeMillis()}.jpg", requestBody)
            val conversationIdPart = conversationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // Upload via API
            val response = aiTutorApi.uploadImage(imagePart, conversationIdPart)
            val body = response.body()

            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                Result.success(body.data.answer)
            } else {
                val errorMsg = body?.message ?: "图片上传失败 (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("图片处理失败: ${e.message}", e))
        }
    }

    // ===== Search =====

    override fun searchMessagesByKeyword(keyword: String): Flow<List<Conversation>> {
        return conversationDao.searchByTitle(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ===== Clear All =====

    override suspend fun clearAll() {
        conversationDao.deleteAll()
    }
}
