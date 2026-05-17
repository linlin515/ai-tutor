package com.aitutor.app.data.mapper

import com.aitutor.app.data.remote.dto.UserDto
import com.aitutor.app.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    phone = phone,
    nickname = nickname,
    avatar = avatar,
    grade = grade,
    dailyQuota = dailyQuota,
    dailyUsed = dailyUsed,
    isSubscribed = isSubscribed,
    subscriptionExpire = subscriptionExpire
)
