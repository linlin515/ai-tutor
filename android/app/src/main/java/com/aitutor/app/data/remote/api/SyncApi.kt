package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.SyncPullResponse
import com.aitutor.app.data.remote.dto.SyncPushRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {
    @GET("api/v1/sync")
    suspend fun pullIncremental(
        @Query("since") since: Long
    ): Response<SyncPullResponse>

    @POST("api/v1/sync")
    suspend fun pushOfflineActions(
        @Body request: SyncPushRequest
    ): Response<Unit>
}
