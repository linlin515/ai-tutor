package com.aitutor.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aitutor.app.data.local.dao.AchievementDao
import com.aitutor.app.data.local.dao.AnalyticsDao
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.dao.PendingSubmissionDao
import com.aitutor.app.data.local.dao.QuizRecordDao
import com.aitutor.app.data.local.dao.ScoreLogDao
import com.aitutor.app.data.local.dao.StudyReportDao
import com.aitutor.app.data.local.dao.SubscriptionCacheDao
import com.aitutor.app.data.local.dao.UserScoreDao
import com.aitutor.app.data.local.dao.WrongAnswerDao
import com.aitutor.app.data.local.entity.AchievementEntity
import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.local.entity.KnowledgePointEntity
import com.aitutor.app.data.local.entity.LearningRecordEntity
import com.aitutor.app.data.local.entity.MessageEntity
import com.aitutor.app.data.local.entity.PendingSubmissionEntity
import com.aitutor.app.data.local.entity.QuizRecordEntity
import com.aitutor.app.data.local.entity.ScoreLogEntity
import com.aitutor.app.data.local.entity.SubscriptionCacheEntity
import com.aitutor.app.data.local.entity.UserScoreEntity
import com.aitutor.app.data.local.entity.WrongAnswerEntity

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
        SubscriptionCacheEntity::class
    ],
    version = 3,
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
}
