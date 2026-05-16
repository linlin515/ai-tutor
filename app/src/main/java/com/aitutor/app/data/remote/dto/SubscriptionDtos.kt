package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 订阅状态 API 响应数据。
 *
 * GET /api/v1/subscription/status
 */
data class SubscriptionStatusDto(
    @SerializedName("plan_type") val planType: String = "free",
    @SerializedName("status") val status: String = "active",
    @SerializedName("features") val features: Map<String, FeatureQuotaDto> = emptyMap(),
    @SerializedName("daily_quota") val dailyQuota: DailyQuotaDto? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("trial_available") val trialAvailable: Boolean = false
)

/**
 * 单个功能的配额 DTO。
 */
data class FeatureQuotaDto(
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("used") val used: Int = 0
)

/**
 * 日配额 DTO。
 */
data class DailyQuotaDto(
    @SerializedName("total_queries") val totalQueries: Int = 0,
    @SerializedName("used_queries") val usedQueries: Int = 0,
    @SerializedName("reset_at") val resetAt: String? = null
)

/**
 * 消耗配额请求。
 *
 * POST /api/v1/subscription/consume
 */
data class ConsumeQuotaRequest(
    @SerializedName("feature") val feature: String
)

/**
 * 消耗配额响应。
 */
data class ConsumeQuotaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("daily_quota") val dailyQuota: DailyQuotaDto? = null
)
