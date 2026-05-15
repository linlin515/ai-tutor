package com.aitutor.app.di

import android.content.Context
import androidx.room.Room
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
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
}
