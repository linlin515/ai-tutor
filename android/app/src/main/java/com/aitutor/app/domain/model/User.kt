package com.aitutor.app.domain.model

data class User(
    val id: String,
    val phone: String,
    val nickname: String,
    val avatar: String?,
    val grade: String?,
    val dailyQuota: Int,
    val dailyUsed: Int,
    val isSubscribed: Boolean,
    val subscriptionExpire: String?
)
