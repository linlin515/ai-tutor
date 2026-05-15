package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.data.remote.dto.AnalyticsDto
import retrofit2.Response
import retrofit2.http.GET

interface AnalyticsApi {
    @GET("api/v1/analytics/stats")
    suspend fun getAnalyticsStats(): Response<ApiResponse<AnalyticsDto>>
}
