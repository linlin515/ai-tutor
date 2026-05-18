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
import com.aitutor.app.data.remote.dto.ToolDefinitionDto
import com.aitutor.app.data.remote.dto.ToolFunctionDto
import com.aitutor.app.data.remote.dto.ToolMessageDto
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.model.ChatMessage
import com.aitutor.app.domain.model.Conversation
import com.aitutor.app.domain.model.MessageStatus
import com.aitutor.app.domain.model.StreamEvent
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
            entities.map { it.toDomain() }.reversed() // DAO returns DESC, we want ASC for display
        }
    }

    override suspend fun getMessagesPaged(conversationId: Long, limit: Int, offset: Int): List<ChatMessage> {
        return messageDao.getByConversationPaged(conversationId, limit, offset)
            .map { it.toDomain() }
            .reversed() // DAO returns DESC, but we want chronological order
    }

    override suspend fun getMessageCount(conversationId: Long): Int {
        return messageDao.getMessageCount(conversationId)
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

    // ===== Streaming (v1.0 文本流) =====

    override fun streamChat(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        grade: String?,
        role: String?,
        systemPrompt: String?,
        language: String?
    ): Flow<String> {
        val chatMessages = buildMessageDtos(messages, role, systemPrompt, language)

        val request = ChatCompletionRequest(
            model = modelId,
            messages = chatMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            grade = grade,
            role = role,
            systemPrompt = systemPrompt,
            language = language
        )

        return chatStreamApi.streamChatText(request, tokenManager.getToken())
    }

    // ===== v2.0 Agent: 流式对话（返回 StreamEvent） =====

    override fun streamChatWithEvents(
        conversationId: Long,
        messages: List<ChatMessage>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        tools: List<Map<String, Any>>?,
        grade: String?,
        role: String?,
        systemPrompt: String?,
        language: String?
    ): Flow<StreamEvent> {
        val chatMessages = buildMessageDtos(messages, role, systemPrompt, language)
        val toolDtos = tools?.map { tool ->
            val func = tool["function"] as? Map<String, Any>
            ToolDefinitionDto(
                type = tool["type"]?.toString() ?: "function",
                function = ToolFunctionDto(
                    name = func?.get("name")?.toString() ?: "",
                    description = func?.get("description")?.toString() ?: "",
                    parameters = func?.get("parameters") as? Map<String, Any>
                )
            )
        }

        val request = ChatCompletionRequest(
            model = modelId,
            messages = chatMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            grade = grade,
            role = role,
            systemPrompt = systemPrompt,
            tools = toolDtos,
            toolChoice = if (toolDtos != null) "auto" else null,
            language = language
        )

        return chatStreamApi.streamChat(request, tokenManager.getToken())
    }

    // ===== v2.0 Agent: 工具结果回传 =====

    override fun streamChatWithToolResult(
        conversationId: Long,
        messages: List<ChatMessage>,
        toolCalls: List<Map<String, Any>>,
        toolResults: List<Map<String, Any>>,
        modelId: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        grade: String?,
        role: String?,
        systemPrompt: String?,
        language: String?
    ): Flow<StreamEvent> {
        val chatMessages = buildMessageDtos(messages, role, systemPrompt, language)

        // Append assistant message with tool_calls
        val assistantMsg = ChatMessageDto(
            role = "assistant",
            content = ""
        )
        chatMessages.add(assistantMsg)

        // Append tool result messages
        for (result in toolResults) {
            val toolMsg = ToolMessageDto(
                role = "tool",
                content = result["result"]?.toString() ?: "",
                toolCallId = result["tool_call_id"]?.toString() ?: ""
            )
            chatMessages.add(ChatMessageDto(role = toolMsg.role, content = toolMsg.content))
        }

        val request = ChatCompletionRequest(
            model = modelId,
            messages = chatMessages,
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            grade = grade,
            role = role,
            systemPrompt = systemPrompt,
            language = language
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
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开图片文件"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("无法解码图片"))
            }

            // Compress
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

            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "photo_${System.currentTimeMillis()}.jpg", requestBody)
            val conversationIdPart = conversationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val oldPhotoPart = MultipartBody.Part.createFormData("photo", "photo_${System.currentTimeMillis()}.jpg", requestBody)

            var analysisResult: String? = null

            try {
                val response = aiTutorApi.uploadImage(imagePart, conversationIdPart)
                val body = response.body()
                if (response.isSuccessful && body?.code == 0 && body.data != null) {
                    analysisResult = body.data.answer
                }
            } catch (_: Exception) { }

            if (analysisResult == null) {
                try {
                    val fallbackResponse = aiTutorApi.solvePhoto(oldPhotoPart)
                    val fallbackBody = fallbackResponse.body()
                    if (fallbackResponse.isSuccessful && fallbackBody?.code == 0 && fallbackBody.data != null) {
                        analysisResult = fallbackBody.data.answer
                    }
                } catch (_: Exception) { }
            }

            if (analysisResult != null) {
                Result.success(analysisResult!!)
            } else {
                Result.failure(Exception("图片上传失败，请检查网络连接"))
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

    // ===== Helpers =====

    private fun buildMessageDtos(
        messages: List<ChatMessage>,
        role: String?,
        systemPrompt: String?,
        language: String? = null
    ): MutableList<ChatMessageDto> {
        val chatMessages = mutableListOf<ChatMessageDto>()

        if (role == "tutor" || systemPrompt != null) {
            val prompt = systemPrompt ?: "你是一个AI学习助手，请用引导式教学方法帮助学生思考和解决问题。采用苏格拉底式提问，逐步引导学生自己找到答案，而不是直接给出答案。"
            // F51: 多语言 - 根据用户语言设置注入 AI 回复语言指令
            val finalPrompt = if (language == "en") {
                "$prompt\n\nPlease always answer in English."
            } else {
                prompt
            }
            chatMessages.add(ChatMessageDto(role = "system", content = finalPrompt))
        }

        chatMessages.addAll(messages.map { msg ->
            ChatMessageDto(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.content
            )
        })
        return chatMessages
    }
}
