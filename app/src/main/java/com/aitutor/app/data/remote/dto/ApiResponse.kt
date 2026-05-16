package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import kotlin.jvm.JvmSuppressWildcards

@JvmSuppressWildcards
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

@JvmSuppressWildcards
data class PaginatedData<T>(
    @SerializedName("items") val items: List<T>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)
