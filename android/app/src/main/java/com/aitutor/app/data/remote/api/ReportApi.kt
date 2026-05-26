package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ReportExportRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ReportApi {
    @Streaming
    @POST("api/v1/report/export")
    suspend fun exportReport(
        @Body request: ReportExportRequest
    ): Response<ResponseBody>
}
