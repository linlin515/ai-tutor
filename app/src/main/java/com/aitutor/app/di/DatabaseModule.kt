package com.aitutor.app.di

import android.content.Context
import androidx.room.Room
import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.AnalyticsDao
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.dao.PendingSubmissionDao
import com.aitutor.app.data.local.dao.QuizRecordDao
import com.aitutor.app.data.local.dao.SubscriptionCacheDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.local.dao.WrongAnswerDao
import com.aitutor.app.data.local.db.AiTutorDatabase
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
            // TODO: Replace with proper Migration objects for version upgrades
            // .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
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
    fun provideSubscriptionCacheDao(database: AiTutorDatabase): SubscriptionCacheDao {
        return database.subscriptionCacheDao()
    }
}
