package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String? = null
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("user_id") val userId: String,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("daily_quota") val dailyQuota: Int = 5,
    @SerializedName("daily_used") val dailyUsed: Int = 0
)

data class UserDto(
    @SerializedName("id") val id: String,
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
