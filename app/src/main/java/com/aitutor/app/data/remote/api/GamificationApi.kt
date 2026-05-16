package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.data.remote.dto.LeaderboardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface GamificationApi {

    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboard(): Response<ApiResponse<LeaderboardResponse>>

    @POST("api/v1/game/sync/score")
    suspend fun syncScore(): Response<ApiResponse<Unit>>
}
