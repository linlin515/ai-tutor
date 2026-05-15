package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AiTutorApi {

    // Auth
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(): Response<ApiResponse<TokenRefreshResponse>>

    // User
    @GET("api/v1/user/profile")
    suspend fun getProfile(): Response<ApiResponse<UserDto>>

    @PATCH("api/v1/user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserDto>>

    // Chat (non-streaming)
    @POST("api/v1/chat/ask")
    suspend fun ask(@Body request: ChatAskRequest): Response<ApiResponse<ChatAskResponse>>

    @GET("api/v1/chat/history")
    suspend fun getChatHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("conversation_id") conversationId: Long?
    ): Response<ApiResponse<PaginatedData<ChatHistoryItem>>>

    // Models
    @GET("api/v1/models")
    suspend fun getModels(): Response<ApiResponse<ModelListResponse>>

    // Solve photo
    @Multipart
    @POST("api/v1/solve/photo")
    suspend fun solvePhoto(@Part photo: MultipartBody.Part): Response<ApiResponse<SolvePhotoResponse>>

    // Streaming image upload (with conversation_id)
    @Multipart
    @POST("api/v1/chat/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("conversation_id") conversationId: okhttp3.RequestBody
    ): Response<ApiResponse<SolvePhotoResponse>>

    // Subscription
    @GET("api/v1/subscription/status")
    suspend fun getSubscriptionStatus(): Response<ApiResponse<SubscriptionStatusDto>>

    // Health
    @GET("api/v1/health")
    suspend fun health(): Response<ApiResponse<Map<String, String>>>
}
