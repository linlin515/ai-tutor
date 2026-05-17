package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ApiResponse
import com.aitutor.app.data.remote.dto.ConsumeQuotaRequest
import com.aitutor.app.data.remote.dto.ConsumeQuotaResponse
import com.aitutor.app.data.remote.dto.SubscriptionStatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 订阅管理 API。
 */
interface SubscriptionApi {

    @GET("api/v1/subscription/status")
    suspend fun getStatus(): Response<ApiResponse<SubscriptionStatusDto>>

    @POST("api/v1/subscription/consume")
    suspend fun consumeQuota(@Body request: ConsumeQuotaRequest): Response<ApiResponse<ConsumeQuotaResponse>>
}
