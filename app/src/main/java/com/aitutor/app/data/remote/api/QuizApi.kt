package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface QuizApi {
    @POST("api/v1/quiz/generate")
    suspend fun generateQuiz(@Body request: QuizGenerateRequest): Response<ApiResponse<QuizGenerateResponse>>

    @POST("api/v1/quiz/submit")
    suspend fun submitQuiz(@Body request: QuizSubmitRequest): Response<ApiResponse<QuizSubmitResponse>>
}
