package com.aitutor.app.data.repository

import app.cash.turbine.test
import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.SolveApi
import com.aitutor.app.data.remote.dto.SolveStepsResponse
import com.aitutor.app.data.remote.dto.SolveRetryRequest
import com.aitutor.app.data.remote.dto.SolveStepsRequest
import com.aitutor.app.data.remote.dto.SolveStepDto
import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.domain.model.SolveEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.Response

/**
 * 单元测试：SolveRepositoryImpl
 *
 * 覆盖场景：
 * - streamSolvePhoto 委托给 SolveApi
 * - retryStep 成功/失败/异常
 * - getSolveSteps 成功/失败/异常
 */
@DisplayName("SolveRepositoryImpl")
class SolveRepositoryImplTest {

    private val solveApi: SolveApi = mockk()
    private val aiTutorApi: AiTutorApi = mockk()
    private lateinit var repository: SolveRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = SolveRepositoryImpl(solveApi, aiTutorApi)
    }

    @Nested
    @DisplayName("streamSolvePhoto")
    inner class StreamSolvePhoto {

        @Test
        @DisplayName("应委托给 solveApi.streamSolve")
        fun `delegates to solveApi`() = runTest {
            val imageBytes = byteArrayOf(0x01, 0x02, 0x03)
            val expectedFlow = flowOf(
                SolveEvent.OcrResult(text = "1+1=?", subject = "math", confidence = 0.95f),
                SolveEvent.Complete(status = "success", solveId = "sid_123")
            )
            every { solveApi.streamSolve(imageBytes, "math", "grade_8") } returns expectedFlow

            val result = repository.streamSolvePhoto(imageBytes, "math", "grade_8")

            result.test {
                assertInstanceOf(SolveEvent.OcrResult::class.java, awaitItem())
                assertInstanceOf(SolveEvent.Complete::class.java, awaitItem())
                awaitComplete()
            }
            verify { solveApi.streamSolve(imageBytes, "math", "grade_8") }
        }

        @Test
        @DisplayName("使用默认参数调用 solveApi")
        fun `uses default parameters`() = runTest {
            val imageBytes = byteArrayOf()
            every { solveApi.streamSolve(imageBytes, "auto", null) } returns flowOf()

            val result = repository.streamSolvePhoto(imageBytes)

            result.test { awaitComplete() }
            verify { solveApi.streamSolve(imageBytes, "auto", null) }
        }

        @Test
        @DisplayName("错误事件应透传")
        fun `error events are forwarded`() = runTest {
            val imageBytes = byteArrayOf()
            val errorEvent = SolveEvent.SolveError(code = "HTTP_500", message = "Server error")
            every { solveApi.streamSolve(imageBytes, "auto", null) } returns flowOf(errorEvent)

            val result = repository.streamSolvePhoto(imageBytes)

            result.test {
                val event = awaitItem()
                assertInstanceOf(SolveEvent.SolveError::class.java, event)
                assertEquals("HTTP_500", (event as SolveEvent.SolveError).code)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("retryStep")
    inner class RetryStep {

        @Test
        @DisplayName("成功时返回 success")
        fun `success returns success`() = runTest {
            val response: Response<ApiResponse<String>> = Response.success(
                ApiResponse(code = 0, message = "重试成功", data = null)
            )
            coEvery { aiTutorApi.solveRetryStep(any()) } returns response

            val result = repository.retryStep("sid_1", 2, "请详细解释")

            assertTrue(result.isSuccess)
            assertEquals("重试成功", result.getOrNull())
        }

        @Test
        @DisplayName("API 返回非 0 码时应返回 failure")
        fun `api non-zero code returns failure`() = runTest {
            val response: Response<ApiResponse<String>> = Response.success(
                ApiResponse(code = 1001, message = "题目不存在", data = null)
            )
            coEvery { aiTutorApi.solveRetryStep(any()) } returns response

            val result = repository.retryStep("sid_1", 2, "请详细解释")

            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }

        @Test
        @DisplayName("HTTP 请求失败时应返回 failure")
        fun `http error returns failure`() = runTest {
            val response: Response<ApiResponse<String>> = Response.error(
                500, okhttp3.ResponseBody.create(null, "{}")
            )
            coEvery { aiTutorApi.solveRetryStep(any()) } returns response

            val result = repository.retryStep("sid_1", 2, "请详细解释")

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("网络异常时应返回 failure")
        fun `network exception returns failure`() = runTest {
            coEvery { aiTutorApi.solveRetryStep(any()) } throws RuntimeException("连接超时")

            val result = repository.retryStep("sid_1", 2, "请详细解释")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("连接超时") == true)
        }

        @Test
        @DisplayName("应传递正确的请求参数")
        fun `passes correct request parameters`() = runTest {
            val response: Response<ApiResponse<String>> = Response.success(
                ApiResponse(code = 0, message = "成功", data = null)
            )
            coEvery { aiTutorApi.solveRetryStep(any()) } returns response

            repository.retryStep("sid_abc", 3, "再讲一遍")

            coVerify { aiTutorApi.solveRetryStep(any()) }
        }
    }

    @Nested
    @DisplayName("getSolveSteps")
    inner class GetSolveSteps {

        private val stepsResponse = SolveStepsResponse(
            steps = listOf(
                SolveStepDto(title = "步骤1", content = "第一步", formula = null),
                SolveStepDto(title = "步骤2", content = "第二步", formula = "x=1")
            ),
            difficulty = "easy"
        )

        @Test
        @DisplayName("成功时返回 steps 数据")
        fun `success returns steps data`() = runTest {
            val response: Response<ApiResponse<SolveStepsResponse>> = Response.success(
                ApiResponse(code = 0, message = "成功", data = stepsResponse)
            )
            coEvery { aiTutorApi.solveSteps(any()) } returns response

            val result = repository.getSolveSteps("1+1=?", "grade_1", "math")

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.steps?.size)
            assertEquals("步骤1", result.getOrNull()?.steps?.get(0)?.title)
        }

        @Test
        @DisplayName("API 返回 data 为 null 时应返回 failure")
        fun `null data returns failure`() = runTest {
            val response: Response<ApiResponse<SolveStepsResponse>> = Response.success(
                ApiResponse(code = 0, message = "成功", data = null)
            )
            coEvery { aiTutorApi.solveSteps(any()) } returns response

            val result = repository.getSolveSteps("1+1=?", "grade_1", "math")

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API 返回非 0 码时应返回 failure")
        fun `non-zero code returns failure`() = runTest {
            val response: Response<ApiResponse<SolveStepsResponse>> = Response.success(
                ApiResponse(code = 2001, message = "不支持的学科", data = null)
            )
            coEvery { aiTutorApi.solveSteps(any()) } returns response

            val result = repository.getSolveSteps("1+1=?", "grade_1", "math")

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("网络异常时应返回 failure")
        fun `network exception returns failure`() = runTest {
            coEvery { aiTutorApi.solveSteps(any()) } throws RuntimeException("网络不可用")

            val result = repository.getSolveSteps("1+1=?", "grade_1", "math")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("网络不可用") == true)
        }

        @Test
        @DisplayName("应传递正确的请求参数")
        fun `passes correct request parameters`() = runTest {
            val response: Response<ApiResponse<SolveStepsResponse>> = Response.success(
                ApiResponse(code = 0, message = "成功", data = stepsResponse)
            )
            coEvery { aiTutorApi.solveSteps(any()) } returns response

            repository.getSolveSteps("2+2=?", "grade_2", "math")

            coVerify { aiTutorApi.solveSteps(any()) }
        }
    }
}
