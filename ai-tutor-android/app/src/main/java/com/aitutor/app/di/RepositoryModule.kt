package com.aitutor.app.di

import com.aitutor.app.data.repository.AuthRepositoryImpl
import com.aitutor.app.data.repository.ChatRepositoryImpl
import com.aitutor.app.data.repository.SettingsRepositoryImpl
import com.aitutor.app.data.repository.VoiceRepositoryImpl
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(impl: VoiceRepositoryImpl): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
