package com.aitutor.app.ui.chat

import android.content.Context
import com.aitutor.app.data.remote.datastore.LanguagePreferences
import com.aitutor.app.data.repository.UserProfileRepository
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.*
import com.aitutor.app.domain.usecase.chat.ProcessTeachingResponseUseCase
import com.aitutor.app.util.NetworkMonitor
import com.aitutor.app.utils.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatViewModelTest : BaseViewModelTest() {

    private val chatRepository: ChatRepository = mockk()
    private val voiceRepository: VoiceRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val userProfileRepository: UserProfileRepository = mockk()
    private val processTeachingResponseUseCase: ProcessTeachingResponseUseCase = mockk()
    private val solveRepository: SolveRepository = mockk()
    private val agentRepository: AgentRepository = mockk()
    private lateinit var languagePreferences: LanguagePreferences
    private lateinit var networkMonitor: NetworkMonitor
    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: ChatViewModel

    private val defaultSettings = AppSettings()

    @Before
    override fun setUp() {
        super.setUp()
        every { chatRepository.getAllConversations() } returns flowOf(emptyList())
        coEvery { authRepository.getProfile() } returns Result.success(
            User("1", "13800138000", "用户", null, "高中", 20, 0, false, null)
        )
        every { agentRepository.getAgentEnabled() } returns flowOf(false)
        every { settingsRepository.getSettings() } returns flowOf(defaultSettings)
        every { userProfileRepository.getGradeSystemPrompt(any()) } returns "grade prompt"
        every { processTeachingResponseUseCase.stripTeachingMarkers(any()) } answers { firstArg() }
        every { processTeachingResponseUseCase.parseTeachingEvents(any()) } returns emptyList()
        every { processTeachingResponseUseCase.countTeachingSteps(any()) } returns 0
        every { agentRepository.getToolsDefinitions() } returns emptyList()
        // Prevent notification path from calling Android-only APIs
        every { context.packageManager.getLaunchIntentForPackage(any()) } returns null

        languagePreferences = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        every { networkMonitor.isOnline } returns MutableStateFlow(true)

        viewModel = ChatViewModel(
            chatRepository, voiceRepository, settingsRepository, authRepository,
            userProfileRepository, processTeachingResponseUseCase, solveRepository,
            agentRepository, languagePreferences, networkMonitor, context
        )
    }

    @Test
    fun initialUiState_shouldHaveDefaults() {
        val state = viewModel.uiState
        assertTrue(state.messages.isEmpty())
        assertEquals(-1L, state.currentConversationId)
        assertEquals("", state.inputText)
        assertFalse(state.isStreaming)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.agentEnabled)
        assertEquals(ChatMode.ASSISTANT, state.chatMode)
        assertEquals(AgentState.IDLE, state.agentState)
        assertEquals("auto", state.difficultyLevel)
        assertNotNull(state.conversations)
    }

    @Test
    fun updateInputText_shouldSetText() {
        viewModel.updateInputText("你好，AI助手")
        assertEquals("你好，AI助手", viewModel.uiState.inputText)
    }

    @Test
    fun updateInputText_shouldAllowEmpty() {
        viewModel.updateInputText("some text")
        viewModel.updateInputText("")
        assertEquals("", viewModel.uiState.inputText)
    }

    @Test
    fun toggleAgentMode_shouldCallRepository() = runTest {
        coEvery { agentRepository.setAgentEnabled(any()) } returns Unit
        viewModel.toggleAgentMode()
        coVerify { agentRepository.setAgentEnabled(true) }
    }

    @Test
    fun toggleAgentSwitch_shouldToggleVisibility() {
        assertFalse(viewModel.uiState.showAgentSwitch)
        viewModel.toggleAgentSwitch()
        assertTrue(viewModel.uiState.showAgentSwitch)
        viewModel.toggleAgentSwitch()
        assertFalse(viewModel.uiState.showAgentSwitch)
    }

    @Test
    fun sendMessage_withEmptyText_shouldNotSend() {
        viewModel.sendMessage()
        assertFalse(viewModel.uiState.isStreaming)
    }

    @Test
    fun createNewConversation_shouldCreateAndSelect() = runTest {
        val newConvId = 42L
        val messagesFlow = MutableStateFlow(emptyList<ChatMessage>())
        coEvery { chatRepository.createConversation(any()) } returns newConvId
        every { chatRepository.getMessagesByConversation(any()) } returns messagesFlow
        viewModel.createNewConversation()
        assertEquals(newConvId, viewModel.uiState.currentConversationId)
        coVerify { chatRepository.createConversation("") }
    }

    @Test
    fun selectConversation_shouldLoadMessages() = runTest {
        val convId = 10L
        val messages = listOf(
            ChatMessage(conversationId = convId, content = "Hello", isUser = true),
            ChatMessage(conversationId = convId, content = "Hi there!", isUser = false)
        )
        val messagesFlow = MutableStateFlow(messages)
        every { chatRepository.getMessagesByConversation(convId) } returns messagesFlow
        viewModel.selectConversation(convId)
        assertEquals(convId, viewModel.uiState.currentConversationId)
        assertEquals(2, viewModel.uiState.messages.size)
    }

    @Test
    fun deleteConversation_shouldRemoveAndResetIfCurrent() = runTest {
        val convId = 5L
        coEvery { chatRepository.deleteConversation(convId) } returns Unit
        every { chatRepository.getAllConversations() } returns flowOf(emptyList())
        val messagesFlow = MutableStateFlow(emptyList<ChatMessage>())
        every { chatRepository.getMessagesByConversation(any()) } returns messagesFlow
        viewModel.selectConversation(convId)
        viewModel.deleteConversation(convId)
        assertEquals(-1L, viewModel.uiState.currentConversationId)
        assertTrue(viewModel.uiState.messages.isEmpty())
        coVerify { chatRepository.deleteConversation(convId) }
    }

    @Test
    fun setDifficultyLevel_shouldUpdate() {
        viewModel.setDifficultyLevel("高中")
        assertEquals("高中", viewModel.uiState.difficultyLevel)
        viewModel.setDifficultyLevel("auto")
        assertEquals("auto", viewModel.uiState.difficultyLevel)
    }

    @Test
    fun toggleDifficultySwitcher_shouldToggle() {
        assertFalse(viewModel.uiState.showDifficultySwitcher)
        viewModel.toggleDifficultySwitcher()
        assertTrue(viewModel.uiState.showDifficultySwitcher)
        viewModel.toggleDifficultySwitcher()
        assertFalse(viewModel.uiState.showDifficultySwitcher)
    }

    @Test
    fun toggleTutorMode_shouldSwitchBetweenAssistantAndTutor() {
        assertEquals(ChatMode.ASSISTANT, viewModel.uiState.chatMode)
        viewModel.toggleTutorMode()
        assertEquals(ChatMode.TUTOR, viewModel.uiState.chatMode)
        assertTrue(viewModel.uiState.tutorMode)
        viewModel.toggleTutorMode()
        assertEquals(ChatMode.ASSISTANT, viewModel.uiState.chatMode)
        assertFalse(viewModel.uiState.tutorMode)
    }

    @Test
    fun setChatMode_toQuiz_shouldUpdateState() {
        viewModel.setChatMode(ChatMode.QUIZ)
        assertEquals(ChatMode.QUIZ, viewModel.uiState.chatMode)
        assertTrue(viewModel.uiState.tutorMode)
        assertEquals(TeachingState.Idle, viewModel.uiState.teachingState)
    }

    @Test
    fun setChatMode_toAssistant_shouldClearTeachingState() {
        viewModel.setChatMode(ChatMode.TUTOR)
        assertEquals(ChatMode.TUTOR, viewModel.uiState.chatMode)
        viewModel.setChatMode(ChatMode.ASSISTANT)
        assertEquals(ChatMode.ASSISTANT, viewModel.uiState.chatMode)
        assertFalse(viewModel.uiState.tutorMode)
        assertEquals(TeachingState.Idle, viewModel.uiState.teachingState)
    }

    @Test
    fun retrySend_withNoFailedMessage_shouldDoNothing() {
        viewModel.retrySend()
    }

    @Test
    fun retrySend_withFailedMessage_shouldRetry() = runTest {
        val convId = 1L
        val messagesFlow = MutableStateFlow(
            listOf(ChatMessage(id = 10L, conversationId = convId, content = "Failed message", isUser = true, status = MessageStatus.FAILED))
        )
        every { chatRepository.getMessagesByConversation(any()) } returns messagesFlow
        coEvery { chatRepository.updateMessageStatus(10L, MessageStatus.SENT) } returns Unit
        coEvery { chatRepository.createConversation(any()) } returns convId
        coEvery { chatRepository.insertMessage(any()) } returns 100L
        coEvery { chatRepository.getConversationById(any()) } returns Conversation(id = convId, title = "")
        coEvery { chatRepository.updateConversationTitle(any(), any()) } returns Unit
        coEvery { chatRepository.updateMessageStatus(any(), any()) } returns Unit
        every { chatRepository.streamChat(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns emptyFlow()
        viewModel.selectConversation(convId)
        viewModel.retrySend()
        coVerify { chatRepository.updateMessageStatus(10L, MessageStatus.SENT) }
    }

    @Test
    fun clearConversation_withValidId_shouldDeleteMessages() = runTest {
        val convId = 5L
        coEvery { chatRepository.deleteAllMessages(convId) } returns Unit
        val messagesFlow = MutableStateFlow(emptyList<ChatMessage>())
        every { chatRepository.getMessagesByConversation(any()) } returns messagesFlow
        viewModel.selectConversation(convId)
        viewModel.clearConversation()
        coVerify { chatRepository.deleteAllMessages(convId) }
    }

    @Test
    fun clearConversation_withInvalidId_shouldDoNothing() {
        viewModel.clearConversation()
        coVerify(exactly = 0) { chatRepository.deleteAllMessages(any()) }
    }

    @Test
    fun speakText_shouldCallVoiceRepository() = runTest {
        every { voiceRepository.speak(any(), any()) } returns Unit
        viewModel.speakText("Hello world")
        verify { voiceRepository.speak("Hello world", defaultSettings.ttsSpeed) }
    }

    @Test
    fun stopSpeaking_shouldCallVoiceRepository() {
        every { voiceRepository.stopSpeaking() } returns Unit
        viewModel.stopSpeaking()
        verify { voiceRepository.stopSpeaking() }
    }

    @Test
    fun updateSearchKeyword_shouldUpdate() {
        viewModel.updateSearchKeyword("数学")
        assertEquals("数学", viewModel.uiState.searchKeyword)
    }

    @Test
    fun toggleConversationSheet_shouldToggle() {
        assertFalse(viewModel.uiState.showConversationSheet)
        viewModel.toggleConversationSheet()
        assertTrue(viewModel.uiState.showConversationSheet)
        viewModel.toggleConversationSheet()
        assertFalse(viewModel.uiState.showConversationSheet)
    }

    @Test
    fun hideConversationSheet_shouldSetFalse() {
        viewModel.toggleConversationSheet()
        assertTrue(viewModel.uiState.showConversationSheet)
        viewModel.hideConversationSheet()
        assertFalse(viewModel.uiState.showConversationSheet)
    }
}
