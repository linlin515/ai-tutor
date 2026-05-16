package com.aitutor.app.di

import com.aitutor.app.data.repository.AgentRepositoryImpl
import com.aitutor.app.data.repository.AnalyticsRepositoryImpl
import com.aitutor.app.data.repository.AuthRepositoryImpl
import com.aitutor.app.data.repository.ChatRepositoryImpl
import com.aitutor.app.data.repository.GamificationRepositoryImpl
import com.aitutor.app.data.repository.QuizRepositoryImpl
import com.aitutor.app.data.repository.SettingsRepositoryImpl
import com.aitutor.app.data.repository.SolveRepositoryImpl
import com.aitutor.app.data.repository.StudyReportRepositoryImpl
import com.aitutor.app.data.repository.SubscriptionRepositoryImpl
import com.aitutor.app.data.repository.VoiceRepositoryImpl
import com.aitutor.app.data.repository.WrongAnswerRepositoryImpl
import com.aitutor.app.domain.repository.AgentRepository
import com.aitutor.app.domain.repository.AnalyticsRepository
import com.aitutor.app.domain.repository.AuthRepository
import com.aitutor.app.domain.repository.ChatRepository
import com.aitutor.app.domain.repository.GamificationRepository
import com.aitutor.app.domain.repository.QuizRepository
import com.aitutor.app.domain.repository.SettingsRepository
import com.aitutor.app.domain.repository.SolveRepository
import com.aitutor.app.domain.repository.StudyReportRepository
import com.aitutor.app.domain.repository.SubscriptionRepository
import com.aitutor.app.domain.repository.VoiceRepository
import com.aitutor.app.domain.repository.WrongAnswerRepository
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

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    @Binds
    @Singleton
    abstract fun bindWrongAnswerRepository(impl: WrongAnswerRepositoryImpl): WrongAnswerRepository

    @Binds
    @Singleton
    abstract fun bindSolveRepository(impl: SolveRepositoryImpl): SolveRepository

    @Binds
    @Singleton
    abstract fun bindGamificationRepository(impl: GamificationRepositoryImpl): GamificationRepository

    @Binds
    @Singleton
    abstract fun bindAgentRepository(impl: AgentRepositoryImpl): AgentRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindStudyReportRepository(impl: StudyReportRepositoryImpl): StudyReportRepository
}
