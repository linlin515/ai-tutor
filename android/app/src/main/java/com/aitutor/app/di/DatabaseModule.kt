package com.aitutor.app.di

import android.content.Context
import androidx.room.Room
import com.aitutor.app.data.local.dao.*
import com.aitutor.app.data.local.db.AiTutorDatabase
import com.aitutor.app.data.local.db.MIGRATION_1_2
import com.aitutor.app.data.local.db.MIGRATION_2_3
import com.aitutor.app.data.local.db.MIGRATION_3_4
import com.aitutor.app.data.local.db.MIGRATION_4_5
import com.aitutor.app.data.local.db.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiTutorDatabase {
        return Room.databaseBuilder(
            context,
            AiTutorDatabase::class.java,
            "aitutor_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @Provides
    fun provideConversationDao(database: AiTutorDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: AiTutorDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideAnalyticsDao(database: AiTutorDatabase): AnalyticsDao {
        return database.analyticsDao()
    }

    @Provides
    @Singleton
    fun provideQuizRecordDao(database: AiTutorDatabase): QuizRecordDao {
        return database.quizRecordDao()
    }

    @Provides
    @Singleton
    fun providePendingSubmissionDao(database: AiTutorDatabase): PendingSubmissionDao {
        return database.pendingSubmissionDao()
    }

    @Provides
    @Singleton
    fun provideWrongAnswerDao(database: AiTutorDatabase): WrongAnswerDao {
        return database.wrongAnswerDao()
    }

    @Provides
    @Singleton
    fun provideAchievementDao(database: AiTutorDatabase): AchievementDao {
        return database.achievementDao()
    }

    @Provides
    @Singleton
    fun provideUserScoreDao(database: AiTutorDatabase): UserScoreDao {
        return database.userScoreDao()
    }

    @Provides
    @Singleton
    fun provideScoreLogDao(database: AiTutorDatabase): ScoreLogDao {
        return database.scoreLogDao()
    }

    @Provides
    @Singleton
    fun provideSubscriptionCacheDao(database: AiTutorDatabase): SubscriptionCacheDao {
        return database.subscriptionCacheDao()
    }

    @Provides
    @Singleton
    fun provideStudyReportDao(database: AiTutorDatabase): StudyReportDao {
        return database.studyReportDao()
    }

    @Provides
    @Singleton
    fun providePendingMessageDao(database: AiTutorDatabase): PendingMessageDao {
        return database.pendingMessageDao()
    }

    // === v4.0 Sprint 2: New DAOs ===

    @Provides
    @Singleton
    fun provideCachedQuestionDao(database: AiTutorDatabase): CachedQuestionDao {
        return database.cachedQuestionDao()
    }

    @Provides
    @Singleton
    fun provideCachedWrongAnswerDao(database: AiTutorDatabase): CachedWrongAnswerDao {
        return database.cachedWrongAnswerDao()
    }

    @Provides
    @Singleton
    fun provideCachedConversationDao(database: AiTutorDatabase): CachedConversationDao {
        return database.cachedConversationDao()
    }

    @Provides
    @Singleton
    fun provideOfflineActionDao(database: AiTutorDatabase): OfflineActionDao {
        return database.offlineActionDao()
    }

    @Provides
    @Singleton
    fun provideFlashcardDao(database: AiTutorDatabase): FlashcardDao {
        return database.flashcardDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: AiTutorDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}
