package com.aitutor.app.data.repository

import com.aitutor.app.data.mapper.toDomain
import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.dto.AuthRequest
import com.aitutor.app.data.remote.dto.UpdateProfileRequest
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.model.User
import com.aitutor.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AiTutorApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(phone: String, password: String): Result<User> {
        return try {
            val response = api.login(AuthRequest(phone, password))
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                val authData = body.data
                tokenManager.saveToken(authData.accessToken)
                Result.success(
                    User(
                        id = authData.userId,
                        phone = phone,
                        nickname = authData.nickname ?: "用户${phone.takeLast(4)}",
                        avatar = null,
                        grade = null,
                        dailyQuota = authData.dailyQuota,
                        dailyUsed = authData.dailyUsed,
                        isSubscribed = false,
                        subscriptionExpire = null
                    )
                )
            } else {
                Result.failure(Exception(body?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(phone: String, password: String, email: String? = null): Result<User> {
        return try {
            val response = api.register(AuthRequest(phone, password, email))
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                val authData = body.data
                tokenManager.saveToken(authData.accessToken)
                Result.success(
                    User(
                        id = authData.userId,
                        phone = phone,
                        nickname = authData.nickname ?: "用户${phone.takeLast(4)}",
                        avatar = null,
                        grade = null,
                        dailyQuota = authData.dailyQuota,
                        dailyUsed = authData.dailyUsed,
                        isSubscribed = false,
                        subscriptionExpire = null
                    )
                )
            } else {
                Result.failure(Exception(body?.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            val response = api.refreshToken()
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                tokenManager.saveToken(body.data.token)
                Result.success(body.data.token)
            } else {
                tokenManager.clearToken()
                Result.failure(Exception("Token 刷新失败，请重新登录"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(): Result<User> {
        return try {
            val response = api.getProfile()
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                Result.success(body.data.toDomain())
            } else {
                Result.failure(Exception(body?.message ?: "获取用户信息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        nickname: String?,
        grade: String?,
        avatar: String?
    ): Result<User> {
        return try {
            val response = api.updateProfile(
                UpdateProfileRequest(nickname, grade, avatar)
            )
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                Result.success(body.data.toDomain())
            } else {
                Result.failure(Exception(body?.message ?: "更新资料失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedToken(): String? = tokenManager.getToken()

    override suspend fun saveToken(token: String) {
        tokenManager.saveToken(token)
    }

    override suspend fun clearToken() {
        tokenManager.clearToken()
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
