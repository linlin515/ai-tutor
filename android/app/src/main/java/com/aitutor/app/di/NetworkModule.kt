package com.aitutor.app.di

import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.AnalyticsApi
import com.aitutor.app.data.remote.api.ChatStreamApi
import com.aitutor.app.data.remote.api.GamificationApi
import com.aitutor.app.data.remote.api.QuizApi
import com.aitutor.app.data.remote.api.SolveApi
import com.aitutor.app.data.remote.api.SubscriptionApi
import com.aitutor.app.data.remote.interceptor.AuthInterceptor
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.aitutor.app.data.tool.impl.WebSearchTool
import com.aitutor.app.data.tool.registry.ToolRegistry
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

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi {
        return retrofit.create(AnalyticsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuizApi(retrofit: Retrofit): QuizApi {
        return retrofit.create(QuizApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSolveApi(okHttpClient: OkHttpClient): SolveApi {
        return SolveApi(okHttpClient, BASE_URL)
    }

    @Provides
    @Singleton
    fun provideGamificationApi(retrofit: Retrofit): GamificationApi {
        return retrofit.create(GamificationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSubscriptionApi(retrofit: Retrofit): SubscriptionApi {
        return retrofit.create(SubscriptionApi::class.java)
    }

    /**
     * 注册 WebSearchTool 到 ToolRegistry（需要 OkHttpClient 实例）
     */
    @Provides
    @Singleton
    fun provideWebSearchTool(okHttpClient: OkHttpClient): WebSearchTool {
        val tool = WebSearchTool(okHttpClient, "https://aitutor.googlecloud.ccwu.cc")
        // 注册到全局 ToolRegistry
        ToolRegistry.register(tool)
        return tool
    }
}
