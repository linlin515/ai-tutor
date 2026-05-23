package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.data.remote.dto.LeaderboardItem
import com.aitutor.app.data.remote.dto.PaginatedData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GamificationApi {

    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboard(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("type") type: String = "GLOBAL"
    ): Response<ApiResponse<PaginatedData<LeaderboardItem>>>

    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboardAll(): Response<ApiResponse<List<LeaderboardItem>>>

    @POST("api/v1/game/sync/score")
    suspend fun syncScore(): Response<ApiResponse<Unit>>
}
