package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.User

interface AuthRepository {
    suspend fun login(phone: String, password: String): Result<User>
    suspend fun register(phone: String, password: String, email: String? = null): Result<User>
    suspend fun refreshToken(): Result<String>
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(nickname: String?, grade: String?, avatar: String?): Result<User>
    fun getSavedToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    fun isLoggedIn(): Boolean

    // [v30] 获取当前已登录用户 ID（用于排行榜"我"的定位）
    fun getCurrentUserId(): String?
}
