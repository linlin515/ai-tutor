package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("phone") val phone: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("grade") val grade: String?,
    @SerializedName("daily_quota") val dailyQuota: Int,
    @SerializedName("daily_used") val dailyUsed: Int,
    @SerializedName("is_subscribed") val isSubscribed: Boolean,
    @SerializedName("subscription_expire") val subscriptionExpire: String?
)

data class TokenRefreshResponse(
    @SerializedName("token") val token: String
)

data class UpdateProfileRequest(
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("grade") val grade: String?,
    @SerializedName("avatar") val avatar: String?
)
