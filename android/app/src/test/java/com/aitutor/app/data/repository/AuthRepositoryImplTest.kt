package com.aitutor.app.data.repository

import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.dto.*
import com.aitutor.app.data.remote.interceptor.TokenManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private val api: AiTutorApi = mockk()
    private val tokenManager: TokenManager = mockk()
    private lateinit var repository: AuthRepositoryImpl

    private val mockAuthResponse = AuthResponse(
        accessToken = "eyJhbGciOiJIUzI1NiJ9.token",
        tokenType = "bearer",
        userId = "12345",
        nickname = "测试用户",
        dailyQuota = 20,
        dailyUsed = 3
    )

    private val mockApiResponse = ApiResponse(
        code = 0,
        message = "success",
        data = mockAuthResponse
    )

    @Before
    fun setUp() {
        repository = AuthRepositoryImpl(api, tokenManager)
    }

    @Test
    fun login_success_shouldReturnUserAndSaveToken() = runTest {
        coEvery { api.login(any<AuthRequest>()) } returns Response.success(mockApiResponse)
        every { tokenManager.saveToken(any()) } returns Unit

        val result = repository.login("13800138000", "correctPass123")

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("12345", user!!.id)
        assertEquals("13800138000", user.phone)
        assertEquals("测试用户", user.nickname)
        assertEquals(20, user.dailyQuota)
        assertEquals(3, user.dailyUsed)
        verify { tokenManager.saveToken("eyJhbGciOiJIUzI1NiJ9.token") }
        coVerify { api.login(AuthRequest("13800138000", "correctPass123")) }
    }

    @Test
    fun login_apiError_shouldReturnFailure() = runTest {
        coEvery { api.login(any<AuthRequest>()) } returns Response.success(
            ApiResponse(code = 1001, message = "手机号或密码错误", data = null)
        )

        val result = repository.login("13800138000", "wrongPass")

        assertTrue(result.isFailure)
        assertEquals("手机号或密码错误", result.exceptionOrNull()?.message)
    }

    @Test
    fun login_networkError_shouldReturnFailure() = runTest {
        coEvery { api.login(any<AuthRequest>()) } throws Exception("网络连接失败")

        val result = repository.login("13800138000", "correctPass123")

        assertTrue(result.isFailure)
        assertEquals("网络连接失败", result.exceptionOrNull()?.message)
    }

    @Test
    fun login_responseNullBody_shouldReturnFailure() = runTest {
        coEvery { api.login(any<AuthRequest>()) } returns Response.success(null as ApiResponse<AuthResponse>?)

        val result = repository.login("13800138000", "correctPass123")

        assertTrue(result.isFailure)
    }

    @Test
    fun login_responseNotSuccessful_shouldReturnFailure() = runTest {
        coEvery { api.login(any<AuthRequest>()) } returns Response.error(
            401,
            okhttp3.ResponseBody.create(null, "Unauthorized")
        )

        val result = repository.login("13800138000", "correctPass123")

        assertTrue(result.isFailure)
    }

    @Test
    fun register_success_shouldReturnUserAndSaveToken() = runTest {
        coEvery { api.register(any<AuthRequest>()) } returns Response.success(mockApiResponse)
        every { tokenManager.saveToken(any()) } returns Unit

        val result = repository.register("13800138000", "newPass123", "test@example.com")

        assertTrue(result.isSuccess)
        assertEquals("12345", result.getOrNull()!!.id)
        verify { tokenManager.saveToken(any()) }
        coVerify { api.register(AuthRequest("13800138000", "newPass123", "test@example.com")) }
    }

    @Test
    fun register_withoutEmail_shouldWork() = runTest {
        coEvery { api.register(any<AuthRequest>()) } returns Response.success(mockApiResponse)
        every { tokenManager.saveToken(any()) } returns Unit

        val result = repository.register("13800138000", "newPass123", null)

        assertTrue(result.isSuccess)
        coVerify { api.register(AuthRequest("13800138000", "newPass123", null)) }
    }

    @Test
    fun register_apiError_shouldReturnFailure() = runTest {
        coEvery { api.register(any<AuthRequest>()) } returns Response.success(
            ApiResponse(code = 1002, message = "该手机号已注册", data = null)
        )

        val result = repository.register("13800138000", "newPass123", null)

        assertTrue(result.isFailure)
        assertEquals("该手机号已注册", result.exceptionOrNull()?.message)
    }

    @Test
    fun refreshToken_success_shouldReturnNewToken() = runTest {
        val refreshData = TokenRefreshResponse(token = "new.token.here")
        val refreshResponse = ApiResponse(code = 0, message = "success", data = refreshData)
        coEvery { api.refreshToken() } returns Response.success(refreshResponse)
        every { tokenManager.saveToken(any()) } returns Unit

        val result = repository.refreshToken()

        assertTrue(result.isSuccess)
        assertEquals("new.token.here", result.getOrNull())
        verify { tokenManager.saveToken("new.token.here") }
    }

    @Test
    fun refreshToken_failure_shouldClearToken() = runTest {
        coEvery { api.refreshToken() } returns Response.success(
            ApiResponse(code = 401, message = "Token 已过期", data = null)
        )
        every { tokenManager.clearToken() } returns Unit

        val result = repository.refreshToken()

        assertTrue(result.isFailure)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun getProfile_success_shouldReturnUser() = runTest {
        val userData = UserDto(
            id = "12345",
            phone = "13800138000",
            nickname = "测试用户",
            grade = "高中",
            avatar = null,
            dailyQuota = 20,
            dailyUsed = 3,
            isSubscribed = false,
            subscriptionExpire = null
        )
        coEvery { api.getProfile() } returns Response.success(
            ApiResponse(code = 0, message = "success", data = userData)
        )

        val result = repository.getProfile()

        assertTrue(result.isSuccess)
        assertEquals("测试用户", result.getOrNull()!!.nickname)
        assertEquals("高中", result.getOrNull()!!.grade)
    }

    @Test
    fun getProfile_failure_shouldReturnError() = runTest {
        coEvery { api.getProfile() } throws Exception("网络错误")

        val result = repository.getProfile()

        assertTrue(result.isFailure)
    }

    @Test
    fun getSavedToken_shouldDelegateToTokenManager() {
        every { tokenManager.getToken() } returns "saved-token"

        val token = repository.getSavedToken()

        assertEquals("saved-token", token)
        verify { tokenManager.getToken() }
    }

    @Test
    fun isLoggedIn_shouldDelegateToTokenManager() {
        every { tokenManager.isLoggedIn() } returns true
        assertTrue(repository.isLoggedIn())

        every { tokenManager.isLoggedIn() } returns false
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun saveToken_shouldDelegate() = runTest {
        every { tokenManager.saveToken(any()) } returns Unit
        repository.saveToken("new-token")
        verify { tokenManager.saveToken("new-token") }
    }

    @Test
    fun clearToken_shouldDelegate() = runTest {
        every { tokenManager.clearToken() } returns Unit
        repository.clearToken()
        verify { tokenManager.clearToken() }
    }
}
