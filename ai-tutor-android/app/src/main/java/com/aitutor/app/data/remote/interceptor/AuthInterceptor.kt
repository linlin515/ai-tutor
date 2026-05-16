package com.aitutor.app.data.remote.interceptor

import com.aitutor.app.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepositoryProvider: Provider<AuthRepository>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth for login/register/refresh/health endpoints
        if (path.contains("/auth/") || path.contains("/health")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getToken()
        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        // Handle 401 — token expired, try to refresh and retry
        if (response.code == 401 && token != null) {
            response.close()

            val newToken = try {
                // Use Provider to break circular dependency:
                // AuthInterceptor → AuthRepository → Api → OkHttpClient → AuthInterceptor
                runBlocking { authRepositoryProvider.get().refreshToken().getOrNull() }
            } catch (e: Exception) {
                null
            }

            if (newToken != null) {
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            }
        }

        return response
    }
}
