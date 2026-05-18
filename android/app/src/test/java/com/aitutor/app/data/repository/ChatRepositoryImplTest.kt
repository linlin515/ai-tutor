package com.aitutor.app.data.repository

import android.content.Context
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.local.entity.MessageEntity
import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.ChatStreamApi
import com.aitutor.app.data.remote.dto.ChatCompletionRequest
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRepositoryImplTest {

    private val conversationDao: ConversationDao = mockk()
    private val messageDao: MessageDao = mockk()
    private val chatStreamApi: ChatStreamApi = mockk()
    private val tokenManager: TokenManager = mockk()
    private val aiTutorApi: AiTutorApi = mockk()
    private val context: Context = mockk()
    private lateinit var repository: ChatRepositoryImpl
    private val token = "test-token-123"

    @Before
    fun setUp() {
        every { tokenManager.getToken() } returns token
        repository = ChatRepositoryImpl(
            conversationDao, messageDao, chatStreamApi, tokenManager, aiTutorApi, context
        )
    }

    @Test
    fun getAllConversations_shouldMapToDomain() = runTest {
        val entities = listOf(
            ConversationEntity(id = 1, title = "数学", messageCount = 5),
            ConversationEntity(id = 2, title = "物理", messageCount = 3)
        )
        every { conversationDao.getAllFlow() } returns flowOf(entities)
        val result = repository.getAllConversations().first()
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("数学", result[0].title)
        assertEquals(2L, result[1].id)
    }

    @Test
    fun getConversationById_existing_shouldReturnDomain() = runTest {
        val entity = ConversationEntity(id = 10, title = "化学", messageCount = 2)
        coEvery { conversationDao.getById(10L) } returns entity
        val result = repository.getConversationById(10L)
        assertNotNull(result)
        assertEquals(10L, result!!.id)
        assertEquals("化学", result.title)
    }

    @Test
    fun getConversationById_notFound_shouldReturnNull() = runTest {
        coEvery { conversationDao.getById(999L) } returns null
        val result = repository.getConversationById(999L)
        assertNull(result)
    }

    @Test
    fun createConversation_shouldInsertAndReturnId() = runTest {
        coEvery { conversationDao.insert(any()) } returns 42L
        val id = repository.createConversation("新对话", "default")
        assertEquals(42L, id)
        coVerify { conversationDao.insert(any<ConversationEntity>()) }
    }

    @Test
    fun updateConversationTitle_shouldUpdateTitleAndTimestamp() = runTest {
        coEvery { conversationDao.updateTitle(any(), any()) } returns Unit
        coEvery { conversationDao.updateTimestamp(any(), any()) } returns Unit
        repository.updateConversationTitle(5L, "更新标题")
        coVerify { conversationDao.updateTitle(5L, "更新标题") }
        coVerify { conversationDao.updateTimestamp(5L, any()) }
    }

    @Test
    fun deleteConversation_shouldDelete() = runTest {
        coEvery { conversationDao.deleteById(any()) } returns Unit
        repository.deleteConversation(3L)
        coVerify { conversationDao.deleteById(3L) }
    }

    @Test
    fun getMessagesByConversation_shouldMapToDomain() = runTest {
        val entities = listOf(
            MessageEntity(conversationId = 1, content = "你好", isUser = true, status = "SENT"),
            MessageEntity(conversationId = 1, content = "你好！有什么可以帮助你的？", isUser = false, status = "SENT")
        )
        every { messageDao.getByConversationFlow(1L) } returns flowOf(entities)
        val result = repository.getMessagesByConversation(1L).first()
        assertEquals(2, result.size)
        assertFalse(result[0].isUser) // reversed: last message (AI reply) now first
        assertEquals("你好！有什么可以帮助你的？", result[0].content)
        assertTrue(result[1].isUser)
    }

    @Test
    fun insertMessage_shouldInsertAndUpdateConversationTimestamp() = runTest {
        val message = ChatMessage(conversationId = 1L, content = "测试消息", isUser = true, status = MessageStatus.SENDING)
        coEvery { messageDao.insert(any()) } returns 100L
        coEvery { conversationDao.updateTimestamp(any(), any()) } returns Unit
        val id = repository.insertMessage(message)
        assertEquals(100L, id)
        coVerify { messageDao.insert(any<MessageEntity>()) }
        coVerify { conversationDao.updateTimestamp(1L, any()) }
    }

    @Test
    fun updateMessageStatus_shouldUpdate() = runTest {
        coEvery { messageDao.updateStatus(any(), any()) } returns Unit
        repository.updateMessageStatus(50L, MessageStatus.SENT)
        coVerify { messageDao.updateStatus(50L, "SENT") }
    }

    @Test
    fun deleteAllMessages_shouldDelete() = runTest {
        coEvery { messageDao.deleteByConversation(any()) } returns Unit
        repository.deleteAllMessages(2L)
        coVerify { messageDao.deleteByConversation(2L) }
    }

    @Test
    fun streamChat_shouldBuildCorrectRequest() = runTest {
        val messages = listOf(ChatMessage(conversationId = 1L, content = "Hello", isUser = true))
        every { chatStreamApi.streamChatText(any(), any()) } returns flowOf("response")
        repository.streamChat(1L, messages, "gpt-4o", 0.7f, 1.0f, 2048, grade = "高中", role = "assistant")
        val slot = slot<ChatCompletionRequest>()
        verify { chatStreamApi.streamChatText(capture(slot), eq(token)) }
        assertEquals("gpt-4o", slot.captured.model)
        assertEquals(true, slot.captured.stream)
        assertEquals(0.7f, slot.captured.temperature)
        assertEquals(1.0f, slot.captured.topP)
        assertEquals(2048, slot.captured.maxTokens)
        assertEquals("高中", slot.captured.grade)
        assertEquals("assistant", slot.captured.role)
    }

    @Test
    fun streamChat_withTutorRole_shouldIncludeSystemMessage() = runTest {
        val messages = listOf(ChatMessage(conversationId = 1L, content = "What is 2+2?", isUser = true))
        every { chatStreamApi.streamChatText(any(), any()) } returns flowOf("")
        repository.streamChat(1L, messages, "gpt-4o", 0.7f, 1.0f, 2048, role = "tutor")
        val slot = slot<ChatCompletionRequest>()
        verify { chatStreamApi.streamChatText(capture(slot), any()) }
        val firstMessage = slot.captured.messages.first()
        assertEquals("system", firstMessage.role)
        assertTrue(firstMessage.content!!.contains("苏格拉底"))
    }

    @Test
    fun streamChatWithEvents_shouldBuildToolRequest() = runTest {
        val messages = listOf(ChatMessage(conversationId = 1L, content = "Search the web", isUser = true))
        val tools = listOf(
            mapOf("type" to "function", "function" to mapOf("name" to "web_search", "description" to "Search the web", "parameters" to mapOf("type" to "object")))
        )
        every { chatStreamApi.streamChat(any(), any()) } returns emptyFlow()
        repository.streamChatWithEvents(1L, messages, "gpt-4o", 0.7f, 1.0f, 2048, tools = tools)
        val slot = slot<ChatCompletionRequest>()
        verify { chatStreamApi.streamChat(capture(slot), any()) }
        assertNotNull(slot.captured.tools)
        assertEquals(1, slot.captured.tools!!.size)
        assertEquals("web_search", slot.captured.tools!![0].function.name)
    }

    @Test
    fun searchMessagesByKeyword_shouldReturnResults() = runTest {
        val entities = listOf(ConversationEntity(id = 1, title = "数学题", messageCount = 3))
        every { conversationDao.searchByTitle("数学") } returns flowOf(entities)
        val result = repository.searchMessagesByKeyword("数学").first()
        assertEquals(1, result.size)
        assertEquals("数学题", result[0].title)
    }

    @Test
    fun searchMessagesByKeyword_noResults_shouldReturnEmpty() = runTest {
        every { conversationDao.searchByTitle("不存在") } returns flowOf(emptyList())
        val result = repository.searchMessagesByKeyword("不存在").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun clearAll_shouldDeleteAllConversations() = runTest {
        coEvery { conversationDao.deleteAll() } returns Unit
        repository.clearAll()
        coVerify { conversationDao.deleteAll() }
    }
}
