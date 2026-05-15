package com.aitutor.app.di

import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.ChatStreamApi
import com.aitutor.app.data.remote.interceptor.AuthInterceptor
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://aitutor.googlecloud.ccwu.cc/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager,
        authRepositoryProvider: Provider<AuthRepository>
    ): AuthInterceptor {
        return AuthInterceptor(tokenManager, authRepositoryProvider)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 无限超时用于 SSE
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAiTutorApi(retrofit: Retrofit): AiTutorApi {
        return retrofit.create(AiTutorApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatStreamApi(okHttpClient: OkHttpClient): ChatStreamApi {
        return ChatStreamApi(okHttpClient, BASE_URL)
    }
}
