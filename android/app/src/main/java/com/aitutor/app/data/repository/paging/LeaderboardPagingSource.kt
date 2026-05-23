package com.aitutor.app.data.repository.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.data.remote.dto.PaginatedData
import com.aitutor.app.data.remote.dto.LeaderboardItem
import com.aitutor.app.domain.model.LeaderboardType
import com.aitutor.app.domain.model.RankEntry
import java.io.IOException

/**
 * PagingSource for leaderboard remote pagination.
 *
 * 每页调用一次云端 API，将 [PaginatedData<LeaderboardItem>] 映射为
 * [RankEntry]，达到数据推算域保护时直接返回 Error。
 */
class LeaderboardPagingSource(
    private val gamificationApi: GamificationApi,
    private val type: LeaderboardType,
    private val currentUserId: String  // [v30] 用于标记"我"的行高亮
) : PagingSource<Int, RankEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RankEntry> {
        val page = params.key ?: 1
        val pageSize = params.loadSize.coerceAtMost(100)

        return try {
            val response = gamificationApi.getLeaderboard(
                page = page,
                pageSize = pageSize,
                type = type.name
            )

            if (!response.isSuccessful) {
                return LoadResult.Error(
                    IOException("HTTP ${response.code()}: ${response.message()}")
                )
            }

            val body = response.body()
                ?: return LoadResult.Error(IOException("Response body is null"))

            val paginated: PaginatedData<LeaderboardItem> = body.data
                ?: return LoadResult.Error(IOException("Response data is null"))

            val rankEntries = paginated.items
                .mapIndexed { index, item ->
                    item.toRankEntry(isMe = item.userId == currentUserId)
                }

            LoadResult.Page(
                data = rankEntries,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < paginated.totalPages) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, RankEntry>): Int? {
        return state.anchorPosition?.let { anchorPos ->
            state.closestPageToPosition(anchorPos)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }
}
