package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.FlashcardPullResponse
import com.aitutor.app.data.remote.dto.FlashcardReviewRequest
import com.aitutor.app.data.remote.dto.FlashcardSyncRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FlashcardApi {
    @GET("api/v1/flashcard/today")
    suspend fun getTodayCards(): Response<FlashcardPullResponse>

    @POST("api/v1/flashcard/review")
    suspend fun submitReview(
        @Body request: FlashcardReviewRequest
    ): Response<Unit>

    @POST("api/v1/flashcard/sync")
    suspend fun syncReviewLogs(
        @Body request: FlashcardSyncRequest
    ): Response<Unit>
}
