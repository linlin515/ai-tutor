package com.aitutor.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aitutor.app.data.local.dao.*
import com.aitutor.app.data.local.entity.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        LearningRecordEntity::class,
        KnowledgePointEntity::class,
        QuizRecordEntity::class,
        PendingSubmissionEntity::class,
        WrongAnswerEntity::class,
        AchievementEntity::class,
        UserScoreEntity::class,
        ScoreLogEntity::class,
        SubscriptionCacheEntity::class,
        PendingMessageEntity::class,
        CachedQuestionEntity::class,
        CachedWrongAnswerEntity::class,
        CachedConversationEntity::class,
        OfflineActionEntity::class,
        FlashcardEntity::class,
        FlashcardReviewLogEntity::class,
        SyncMetadataEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AiTutorDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun quizRecordDao(): QuizRecordDao
    abstract fun pendingSubmissionDao(): PendingSubmissionDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userScoreDao(): UserScoreDao
    abstract fun subscriptionCacheDao(): SubscriptionCacheDao
    abstract fun scoreLogDao(): ScoreLogDao
    abstract fun studyReportDao(): StudyReportDao
    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun cachedQuestionDao(): CachedQuestionDao
    abstract fun cachedWrongAnswerDao(): CachedWrongAnswerDao
    abstract fun cachedConversationDao(): CachedConversationDao
    abstract fun offlineActionDao(): OfflineActionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
