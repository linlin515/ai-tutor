package com.aitutor.app.di

import com.aitutor.app.data.remote.api.ReportApi
import com.aitutor.app.data.repository.ReportRepositoryImpl
import com.aitutor.app.domain.repository.ReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReportModule {

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi {
        return retrofit.create(ReportApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReportRepository(impl: ReportRepositoryImpl): ReportRepository {
        return impl
    }
}
