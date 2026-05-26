package com.aitutor.app.data.repository.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.data.remote.dto.LeaderboardItem
import com.aitutor.app.data.remote.dto.PaginatedData
import com.aitutor.app.domain.model.LeaderboardType
import com.aitutor.app.domain.model.RankEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.Response
import java.io.IOException

/**
 * 单元测试：LeaderboardPagingSource
 *
 * 覆盖场景：
 * - load 第一页（prevKey=null, nextKey=有值）
 * - load 中间页
 * - load 最后一页（nextKey=null）
 * - load HTTP 错误
 * - load 空 Response Body
 * - load 空 Response Data
 * - load API 异常
 * - isMe 标记正确
 * - getRefreshKey 各种状态
 */
@DisplayName("LeaderboardPagingSource")
class LeaderboardPagingSourceTest {

    private val gamificationApi: GamificationApi = mockk()
    private lateinit var pagingSource: LeaderboardPagingSource

    private val sampleItems = listOf(
        LeaderboardItem(userId = "user_a", nickname = "Alice", score = 100, rank = 1),
        LeaderboardItem(userId = "user_b", nickname = "Bob", score = 90, rank = 2),
        LeaderboardItem(userId = "user_me", nickname = "Me", score = 80, rank = 3)
    )

    @BeforeEach
    fun setUp() {
        pagingSource = LeaderboardPagingSource(
            gamificationApi = gamificationApi,
            type = LeaderboardType.GLOBAL,
            currentUserId = "user_me"
        )
    }

    @Nested
    @DisplayName("load(params)")
    inner class Load {

        @Test
        @DisplayName("第一页：prevKey 为 null，nextKey 有值")
        fun `page 1 returns prevKey null and nextKey set`() = runTest {
            val paginatedData = PaginatedData(
                items = sampleItems,
                total = 50,
                page = 1,
                pageSize = 20,
                totalPages = 3
            )
            val apiResponse = ApiResponse(code = 0, message = "success", data = paginatedData)
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.success(apiResponse)

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            val page = result as PagingSource.LoadResult.Page
            assertEquals(3, page.data.size)
            assertNull(page.prevKey)
            assertEquals(2, page.nextKey) // page 1, totalPages 3 -> nextKey = 2
        }

        @Test
        @DisplayName("中间页：prevKey 和 nextKey 都有值")
        fun `middle page returns both prevKey and nextKey`() = runTest {
            val paginatedData = PaginatedData(
                items = sampleItems,
                total = 50,
                page = 2,
                pageSize = 20,
                totalPages = 3
            )
            val apiResponse = ApiResponse(code = 0, message = "success", data = paginatedData)
            coEvery {
                gamificationApi.getLeaderboard(page = 2, pageSize = 20, type = "GLOBAL")
            } returns Response.success(apiResponse)

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = 2, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            val page = result as PagingSource.LoadResult.Page
            assertEquals(1, page.prevKey)
            assertEquals(3, page.nextKey)
        }

        @Test
        @DisplayName("最后一页：nextKey 为 null")
        fun `last page returns nextKey null`() = runTest {
            val paginatedData = PaginatedData(
                items = sampleItems,
                total = 50,
                page = 3,
                pageSize = 20,
                totalPages = 3
            )
            val apiResponse = ApiResponse(code = 0, message = "success", data = paginatedData)
            coEvery {
                gamificationApi.getLeaderboard(page = 3, pageSize = 20, type = "GLOBAL")
            } returns Response.success(apiResponse)

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = 3, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            val page = result as PagingSource.LoadResult.Page
            assertEquals(2, page.prevKey)
            assertNull(page.nextKey)
        }

        @Test
        @DisplayName("HTTP 错误应返回 LoadResult.Error")
        fun `http error returns LoadResult Error`() = runTest {
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.error(500, okhttp3.ResponseBody.create(null, "{}"))

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Error)
            val error = result as PagingSource.LoadResult.Error
            assertTrue(error.throwable.message?.contains("HTTP 500") == true)
        }

        @Test
        @DisplayName("响应 body 为 null 应返回 LoadResult.Error")
        fun `null body returns LoadResult Error`() = runTest {
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.success(null as ApiResponse<PaginatedData<LeaderboardItem>>?)

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Error)
            val error = result as PagingSource.LoadResult.Error
            assertTrue(error.throwable.message?.contains("Response body is null") == true)
        }

        @Test
        @DisplayName("响应 data 为 null 应返回 LoadResult.Error")
        fun `null data returns LoadResult Error`() = runTest {
            val apiResponse: ApiResponse<PaginatedData<LeaderboardItem>> = ApiResponse(
                code = 0, message = "success", data = null
            )
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.success(apiResponse)

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Error)
            val error = result as PagingSource.LoadResult.Error
            assertTrue(error.throwable.message?.contains("Response data is null") == true)
        }

        @Test
        @DisplayName("API 异常应返回 LoadResult.Error")
        fun `api exception returns LoadResult Error`() = runTest {
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } throws IOException("网络连接失败")

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Error)
            assertEquals("网络连接失败", (result as PagingSource.LoadResult.Error).throwable.message)
        }

        @Test
        @DisplayName("isMe 标记：当前用户的条目应标记为 isMe=true")
        fun `isMe flag set correctly for current user`() = runTest {
            val items = listOf(
                LeaderboardItem(userId = "user_me", nickname = "Me", score = 80, rank = 1),
                LeaderboardItem(userId = "other", nickname = "Other", score = 70, rank = 2)
            )
            val paginatedData = PaginatedData(
                items = items, total = 2, page = 1,
                pageSize = 20, totalPages = 1
            )
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.success(ApiResponse(code = 0, message = "success", data = paginatedData))

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            val page = result as PagingSource.LoadResult.Page
            assertTrue(page.data[0].isMe)
            assertTrue(!page.data[1].isMe)
        }

        @Test
        @DisplayName("空 items 应返回空数据页")
        fun `empty items returns empty page`() = runTest {
            val paginatedData: PaginatedData<LeaderboardItem> = PaginatedData(
                items = emptyList(), total = 0, page = 1,
                pageSize = 20, totalPages = 0
            )
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 20, type = "GLOBAL")
            } returns Response.success(ApiResponse(code = 0, message = "success", data = paginatedData))

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            assertTrue((result as PagingSource.LoadResult.Page).data.isEmpty())
        }

        @Test
        @DisplayName("loadSize 超过 100 时被限制为 100")
        fun `loadSize capped at 100`() = runTest {
            val paginatedData: PaginatedData<LeaderboardItem> = PaginatedData(
                items = sampleItems, total = 200, page = 1,
                pageSize = 100, totalPages = 2
            )
            coEvery {
                gamificationApi.getLeaderboard(page = 1, pageSize = 100, type = "GLOBAL")
            } returns Response.success(ApiResponse(code = 0, message = "success", data = paginatedData))

            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 200, placeholdersEnabled = false)
            )

            assertTrue(result is PagingSource.LoadResult.Page)
            coVerify {
                gamificationApi.getLeaderboard(page = 1, pageSize = 100, type = "GLOBAL")
            }
        }
    }

    @Nested
    @DisplayName("getRefreshKey(state)")
    inner class GetRefreshKey {

        @Test
        @DisplayName("有 anchorPosition 时应返回正确的刷新键")
        fun `with anchor position returns correct key`() {
            val state: PagingState<Int, RankEntry> = PagingState(
                pages = listOf(
                    PagingSource.LoadResult.Page(
                        data = listOf(
                            RankEntry(rank = 1, nickname = "A", score = 100),
                            RankEntry(rank = 2, nickname = "B", score = 90)
                        ),
                        prevKey = null,
                        nextKey = 2
                    )
                ),
                anchorPosition = 0,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )

            val key = pagingSource.getRefreshKey(state)

            // anchorPosition=0, closestPageToPosition returns page with nextKey=2
            // nextKey?.minus(1) = 1
            assertEquals(1, key)
        }

        @Test
        @DisplayName("anchorPosition 为 null 时应返回 null")
        fun `null anchor position returns null`() {
            val state: PagingState<Int, RankEntry> = PagingState(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )

            val key = pagingSource.getRefreshKey(state)

            assertNull(key)
        }

        @Test
        @DisplayName("anchorPosition 在最后一页时应正确处理")
        fun `anchor position on last page works`() {
            val state: PagingState<Int, RankEntry> = PagingState(
                pages = listOf(
                    PagingSource.LoadResult.Page(
                        data = listOf(RankEntry(rank = 41, nickname = "X", score = 10)),
                        prevKey = 2,
                        nextKey = null
                    )
                ),
                anchorPosition = 0,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )

            val key = pagingSource.getRefreshKey(state)

            // prevKey=2, prevKey+1 = 3
            assertEquals(3, key)
        }
    }
}
