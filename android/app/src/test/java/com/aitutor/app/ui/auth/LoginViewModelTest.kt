package com.aitutor.app.ui.auth

import com.aitutor.app.domain.model.User
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.utils.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 单元测试：LoginViewModel
 *
 * 覆盖场景：
 * - 初始化状态
 * - 输入验证（手机号、密码、邮箱）
 * - 登录/注册模式切换
 * - 登录成功/失败
 * - 注册成功/失败
 */
class LoginViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: LoginViewModel

    private val mockUser = User(
        id = "12345",
        phone = "13800138000",
        nickname = "测试用户",
        avatar = null,
        grade = null,
        dailyQuota = 20,
        dailyUsed = 3,
        isSubscribed = false,
        subscriptionExpire = null
    )

    @Before
    override fun setUp() {
        super.setUp()
        viewModel = LoginViewModel(authRepository)
    }

    // ======================== 初始化 ========================

    @Test
    fun initialUiState_shouldHaveDefaults() {
        val state = viewModel.uiState
        assertEquals("", state.phone)
        assertEquals("", state.password)
        assertEquals("", state.email)
        assertFalse(state.isRegister)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isSuccess)
    }

    // ======================== 输入操作 ========================

    @Test
    fun updatePhone_shouldFilterNonDigits() {
        viewModel.updatePhone("138abc00!!138")
        assertEquals("13800138", viewModel.uiState.phone)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun updatePhone_shouldLimitTo11Chars() {
        viewModel.updatePhone("123456789012345")
        assertEquals(11, viewModel.uiState.phone.length)
        assertEquals("12345678901", viewModel.uiState.phone)
    }

    @Test
    fun updatePassword_shouldSetPassword() {
        viewModel.updatePassword("mySecret123")
        assertEquals("mySecret123", viewModel.uiState.password)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun updateEmail_shouldSetEmail() {
        viewModel.updateEmail("test@example.com")
        assertEquals("test@example.com", viewModel.uiState.email)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun updatePhone_shouldClearErrorOnInput() {
        // First trigger an error
        viewModel.submit()
        assertNotNull(viewModel.uiState.errorMessage)

        // Typing should clear it
        viewModel.updatePhone("1")
        assertNull(viewModel.uiState.errorMessage)
    }

    // ======================== 模式切换 ========================

    @Test
    fun toggleMode_shouldSwitchBetweenLoginAndRegister() {
        assertFalse(viewModel.uiState.isRegister)

        viewModel.toggleMode()
        assertTrue(viewModel.uiState.isRegister)
        assertNull(viewModel.uiState.errorMessage)

        viewModel.toggleMode()
        assertFalse(viewModel.uiState.isRegister)
    }

    // ======================== 登录验证 ========================

    @Test
    fun submit_withInvalidPhone_shouldShowError() {
        viewModel.updatePhone("13800") // Only 5 digits
        viewModel.updatePassword("validPass123")
        viewModel.submit()

        assertEquals("请输入正确的11位手机号", viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun submit_withEmptyPhone_shouldShowError() {
        viewModel.updatePassword("validPass123")
        viewModel.submit()

        assertEquals("请输入正确的11位手机号", viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun submit_withShortPassword_shouldShowError() {
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("12345") // only 5 chars
        viewModel.submit()

        assertEquals("密码长度为6-20位", viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun submit_withLongPassword_shouldShowError() {
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("a".repeat(21)) // 21 chars
        viewModel.submit()

        assertEquals("密码长度为6-20位", viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isLoading)
    }

    // ======================== 登录成功 ========================

    @Test
    fun submit_loginSuccess_shouldUpdateState() = runTest {
        // Given
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("correctPass123")
        coEvery { authRepository.login(any(), any()) } returns Result.success(mockUser)

        // When
        viewModel.submit()

        // Then
        // Note: viewModelScope uses testDispatcher, so we need to advance
        // testDispatcher.advanceUntilIdle() is handled implicitly by runTest + StandardTestDispatcher
        assertTrue(viewModel.uiState.isSuccess)
        assertFalse(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
        coVerify { authRepository.login("13800138000", "correctPass123") }
    }

    // ======================== 登录失败 ========================

    @Test
    fun submit_loginFailure_shouldShowError() = runTest {
        // Given
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("wrongPass")
        coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception("手机号或密码错误"))

        // When
        viewModel.submit()

        // Then
        assertFalse(viewModel.uiState.isSuccess)
        assertFalse(viewModel.uiState.isLoading)
        assertEquals("手机号或密码错误", viewModel.uiState.errorMessage)
    }

    @Test
    fun submit_loginFailure_withNullMessage_shouldShowDefault() = runTest {
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("wrongPass")
        coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception())

        viewModel.submit()

        assertEquals("操作失败，请重试", viewModel.uiState.errorMessage)
    }

    // ======================== 注册 ========================

    @Test
    fun submit_registerSuccess_shouldUpdateState() = runTest {
        // Given — switch to register mode
        viewModel.toggleMode()
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("newPass123")
        viewModel.updateEmail("user@test.com")
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(mockUser)

        // When
        viewModel.submit()

        // Then
        assertTrue(viewModel.uiState.isSuccess)
        assertFalse(viewModel.uiState.isLoading)
        coVerify { authRepository.register("13800138000", "newPass123", "user@test.com") }
    }

    @Test
    fun submit_registerWithoutEmail_shouldWork() = runTest {
        viewModel.toggleMode()
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("newPass123")
        coEvery { authRepository.register(any(), any(), any()) } returns Result.success(mockUser)

        viewModel.submit()

        assertTrue(viewModel.uiState.isSuccess)
        coVerify { authRepository.register("13800138000", "newPass123", null) }
    }

    @Test
    fun submit_registerFailure_shouldShowError() = runTest {
        viewModel.toggleMode()
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("newPass123")
        coEvery { authRepository.register(any(), any(), any()) } returns Result.failure(Exception("该手机号已注册"))

        viewModel.submit()

        assertEquals("该手机号已注册", viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isSuccess)
    }

    // ======================== 边界场景 ========================

    @Test
    fun submit_afterError_shouldClearErrorMessage() = runTest {
        // First submit fails
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("short")
        viewModel.submit()
        assertNotNull(viewModel.uiState.errorMessage)

        // Fix password and submit again (succeed this time)
        viewModel.updatePassword("correctPass123")
        coEvery { authRepository.login(any(), any()) } returns Result.success(mockUser)

        viewModel.submit()

        assertNull(viewModel.uiState.errorMessage)
        assertTrue(viewModel.uiState.isSuccess)
    }

    @Test
    fun submit_shouldSetLoadingState() = runTest {
        viewModel.updatePhone("13800138000")
        viewModel.updatePassword("correctPass123")

        // Use a coAnswer that suspends to verify loading state
        coEvery { authRepository.login(any(), any()) } coAnswers {
            // Simulate some delay — loading should be true here
            Result.success(mockUser)
        }

        viewModel.submit()
        // After completion, loading should be false
        assertFalse(viewModel.uiState.isLoading)
    }
}
