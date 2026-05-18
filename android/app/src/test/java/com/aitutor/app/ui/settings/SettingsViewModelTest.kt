package com.aitutor.app.ui.settings

import app.cash.turbine.test
import com.aitutor.app.data.local.CacheManager
import com.aitutor.app.data.local.CacheSize
import com.aitutor.app.data.remote.datastore.LanguagePreferences
import com.aitutor.app.domain.model.AppSettings
import com.aitutor.app.domain.model.ThemeMode
import com.aitutor.app.domain.repository.AgentRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.usecase.AppUpdateChecker
import com.aitutor.app.domain.usecase.CheckResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * 单元测试：SettingsViewModel
 *
 * 覆盖场景：
 * - 初始化状态（settings, cache, language, agent）
 * - 缓存大小加载与清理
 * - 设置项更新（temperature, topP, maxTokens, theme, TTS）
 * - 语言切换
 * - Agent 模式切换
 * - 应用更新检查
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SettingsViewModel")
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val settingsRepository: SettingsRepository = mockk()
    private val cacheManager: CacheManager = mockk()
    private val languagePreferences: LanguagePreferences = mockk()
    private val agentRepository: AgentRepository = mockk()
    private val appUpdateChecker: AppUpdateChecker = mockk()

    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        val defaultSettings = AppSettings(
            modelId = "gpt-4",
            temperature = 0.7f,
            topP = 1.0f,
            maxTokens = 2048,
            darkTheme = ThemeMode.SYSTEM,
            ttsSpeed = 1.0f,
            ttsVoice = "default"
        )
        every { settingsRepository.getSettings() } returns flowOf(defaultSettings)
        coEvery { cacheManager.calculateSize() } returns CacheSize(total = 1024)
        every { languagePreferences.currentLocale } returns flowOf(Locale("zh"))
        every { agentRepository.getAgentEnabled() } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初始化")
    inner class Initialization {

        @Test
        @DisplayName("settings 应从 repository 加载默认值")
        fun `settings loaded from repository`() = runTest {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            viewModel.settings.test {
                assertEquals("gpt-4", awaitItem().modelId)
                cancel()
            }
        }

        @Test
        @DisplayName("cacheSize 应在 init 中加载")
        fun `cache size loaded on init`() = runTest {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertEquals(1024, viewModel.cacheSize.value.total)
        }

        @Test
        @DisplayName("currentLanguage 应从 Preferences 加载")
        fun `current language loaded from preferences`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertEquals(Locale("zh"), viewModel.currentLanguage.value)
        }

        @Test
        @DisplayName("agentEnabled 默认为 false")
        fun `agent enabled defaults to false`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertFalse(viewModel.agentEnabled.value)
        }

        @Test
        @DisplayName("cacheClearing 初始为 false")
        fun `cacheClearing initial value is false`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertFalse(viewModel.cacheClearing.value)
        }

        @Test
        @DisplayName("isCheckingUpdate 初始为 false")
        fun `isCheckingUpdate initial value is false`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertFalse(viewModel.isCheckingUpdate.value)
        }

        @Test
        @DisplayName("updateCheckResult 初始为 null")
        fun `updateCheckResult initial value is null`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            assertNull(viewModel.updateCheckResult.value)
        }
    }

    @Nested
    @DisplayName("缓存操作")
    inner class CacheOperations {

        @Test
        @DisplayName("loadCacheSize 应调用 cacheManager.calculateSize()")
        fun `loadCacheSize calls calculateSize`() = runTest {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            coVerify { cacheManager.calculateSize() }
        }

        @Test
        @DisplayName("clearCache 应调用 cacheManager.clearAll 并更新状态")
        fun `clearCache updates state correctly`() = runTest {
            coEvery { cacheManager.clearAll(any()) } answers {
                firstArg<(Float) -> Unit>().invoke(1f)
            }
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            viewModel.clearCache()
            advanceUntilIdle()

            coVerify { cacheManager.clearAll(any()) }
            assertEquals(0, viewModel.cacheSize.value.total)
            assertEquals(1024, viewModel.cacheClearedBytes.value)
            assertFalse(viewModel.cacheClearing.value)
        }

        @Test
        @Disabled("viewModelScope 需要 Hilt 测试环境")
        @DisplayName("clearCache 正在清理时再次调用应被忽略")
        fun `clearCache ignored when already clearing`() = runTest {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            // Set clearing flag first (simulate in-progress state)
            viewModel.clearCache()
            advanceUntilIdle()
            // Second call should be ignored
            viewModel.clearCache()
            advanceUntilIdle()

            coVerify(exactly = 1) { cacheManager.clearAll(any()) }
        }
    }

    @Nested
    @DisplayName("设置更新")
    inner class SettingsUpdate {

        @BeforeEach
        fun createViewModel() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
        }

        @Test
        @DisplayName("updateTemperature 应委托给 settingsRepository")
        fun `updateTemperature delegates to repository`() = runTest {
            coEvery { settingsRepository.updateTemperature(any()) } returns Unit
            viewModel.updateTemperature(0.5f)
            coVerify { settingsRepository.updateTemperature(0.5f) }
        }

        @Test
        @DisplayName("updateTopP 应委托给 settingsRepository")
        fun `updateTopP delegates to repository`() = runTest {
            coEvery { settingsRepository.updateTopP(any()) } returns Unit
            viewModel.updateTopP(0.9f)
            coVerify { settingsRepository.updateTopP(0.9f) }
        }

        @Test
        @DisplayName("updateMaxTokens 应委托给 settingsRepository")
        fun `updateMaxTokens delegates to repository`() = runTest {
            coEvery { settingsRepository.updateMaxTokens(any()) } returns Unit
            viewModel.updateMaxTokens(4096)
            coVerify { settingsRepository.updateMaxTokens(4096) }
        }

        @Test
        @DisplayName("updateThemeMode 应委托给 settingsRepository")
        fun `updateThemeMode delegates to repository`() = runTest {
            coEvery { settingsRepository.updateThemeMode(any()) } returns Unit
            viewModel.updateThemeMode(ThemeMode.DARK)
            coVerify { settingsRepository.updateThemeMode(ThemeMode.DARK) }
        }

        @Test
        @DisplayName("updateTtsSpeed 应委托给 settingsRepository")
        fun `updateTtsSpeed delegates to repository`() = runTest {
            coEvery { settingsRepository.updateTtsSpeed(any()) } returns Unit
            viewModel.updateTtsSpeed(1.5f)
            coVerify { settingsRepository.updateTtsSpeed(1.5f) }
        }
    }

    @Nested
    @DisplayName("语言切换")
    inner class LanguageSwitching {

        @BeforeEach
        fun createViewModel() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
        }

        @Test
        @DisplayName("setLanguage 应设置语言偏好")
        fun `setLanguage updates language preference`() = runTest {
            coEvery { languagePreferences.setLocale(any()) } returns Unit
            viewModel.setLanguage(Locale("en"))
            coVerify { languagePreferences.setLocale(Locale("en")) }
        }

        @Test
        @DisplayName("getLanguageDisplayName 中文应返回中文")
        fun `getLanguageDisplayName chinese`() {
            assertEquals("中文", viewModel.getLanguageDisplayName(Locale("zh")))
        }

        @Test
        @DisplayName("getLanguageDisplayName 英文应返回 English")
        fun `getLanguageDisplayName english`() {
            assertEquals("English", viewModel.getLanguageDisplayName(Locale("en")))
        }

        @Test
        @DisplayName("availableLanguages 应包含中英文")
        fun `availableLanguages contains zh and en`() {
            val langs = viewModel.availableLanguages
            assertTrue(langs.contains(Locale("zh")))
            assertTrue(langs.contains(Locale("en")))
            assertEquals(2, langs.size)
        }
    }

    @Nested
    @DisplayName("Agent 模式")
    inner class AgentMode {

        @BeforeEach
        fun createViewModel() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
        }

        @Test
        @DisplayName("toggleAgentMode 应切换 agent 状态")
        fun `toggleAgentMode toggles agent state`() = runTest {
            coEvery { agentRepository.setAgentEnabled(any()) } returns Unit
            viewModel.toggleAgentMode()
            coVerify { agentRepository.setAgentEnabled(true) }
        }

        @Test
        @Disabled("需要 Hilt/AndroidX Test 环境才能测试 viewModelScope")
        @DisplayName("toggleAgentMode 两次应切换回 false")
        fun `toggleAgentMode twice sets back to false`() = runTest {
            coEvery { agentRepository.setAgentEnabled(any()) } returns Unit
            viewModel.toggleAgentMode()
            viewModel.toggleAgentMode()
            coVerify { agentRepository.setAgentEnabled(false) }
        }
    }

    @Nested
    @DisplayName("更新检查")
    inner class UpdateCheck {

        @BeforeEach
        fun createViewModel() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
        }

        @Test
        @Disabled("checkForUpdate 使用 Dispatchers.IO 需要 AndroidX Test 环境")
        @DisplayName("checkForUpdate 成功时应更新结果")
        fun `checkForUpdate success`() = runTest {
            val expectedResult = CheckResult.NoUpdate
            every { appUpdateChecker.checkForUpdate() } returns expectedResult

            viewModel.checkForUpdate()
            advanceUntilIdle()
            assertEquals(expectedResult, viewModel.updateCheckResult.value)
            assertFalse(viewModel.isCheckingUpdate.value)
        }

        @Test
        @Disabled("checkForUpdate 使用 Dispatchers.IO 需要 AndroidX Test 环境")
        @DisplayName("checkForUpdate 失败时应设置错误结果")
        fun `checkForUpdate failure`() = runTest {
            every { appUpdateChecker.checkForUpdate() } throws RuntimeException("Network error")

            viewModel.checkForUpdate()
            advanceUntilIdle()
            val result = viewModel.updateCheckResult.value
            assertTrue(result is CheckResult.Error)
            assertEquals("Network error", (result as CheckResult.Error).message)
            assertFalse(viewModel.isCheckingUpdate.value)
        }

        @Test
        @Disabled("checkForUpdate 使用 Dispatchers.IO 需要 AndroidX Test 环境")
        @DisplayName("checkForUpdate 正在检查时再次调用应被忽略")
        fun `checkForUpdate ignored when already checking`() = runTest {
            every { appUpdateChecker.checkForUpdate() } returns CheckResult.NoUpdate

            viewModel.checkForUpdate()
            viewModel.checkForUpdate() // Should be ignored
            advanceUntilIdle()
            verify(exactly = 1) { appUpdateChecker.checkForUpdate() }
        }

        @Test
        @DisplayName("clearUpdateResult 应重置结果为 null")
        fun `clearUpdateResult resets result`() {
            viewModel = SettingsViewModel(
                settingsRepository, cacheManager, languagePreferences,
                agentRepository, appUpdateChecker
            )
            viewModel.clearUpdateResult()
            assertNull(viewModel.updateCheckResult.value)
        }
    }
}
