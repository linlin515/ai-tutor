package com.aitutor.app.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.aitutor.app.data.local.dao.AchievementDao;
import com.aitutor.app.data.local.dao.AchievementDao_Impl;
import com.aitutor.app.data.local.dao.AnalyticsDao;
import com.aitutor.app.data.local.dao.AnalyticsDao_Impl;
import com.aitutor.app.data.local.dao.ConversationDao;
import com.aitutor.app.data.local.dao.ConversationDao_Impl;
import com.aitutor.app.data.local.dao.MessageDao;
import com.aitutor.app.data.local.dao.MessageDao_Impl;
import com.aitutor.app.data.local.dao.PendingSubmissionDao;
import com.aitutor.app.data.local.dao.PendingSubmissionDao_Impl;
import com.aitutor.app.data.local.dao.QuizRecordDao;
import com.aitutor.app.data.local.dao.QuizRecordDao_Impl;
import com.aitutor.app.data.local.dao.ScoreLogDao;
import com.aitutor.app.data.local.dao.ScoreLogDao_Impl;
import com.aitutor.app.data.local.dao.StudyReportDao;
import com.aitutor.app.data.local.dao.StudyReportDao_Impl;
import com.aitutor.app.data.local.dao.SubscriptionCacheDao;
import com.aitutor.app.data.local.dao.SubscriptionCacheDao_Impl;
import com.aitutor.app.data.local.dao.UserScoreDao;
import com.aitutor.app.data.local.dao.UserScoreDao_Impl;
import com.aitutor.app.data.local.dao.WrongAnswerDao;
import com.aitutor.app.data.local.dao.WrongAnswerDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AiTutorDatabase_Impl extends AiTutorDatabase {
  private volatile ConversationDao _conversationDao;

  private volatile MessageDao _messageDao;

  private volatile AnalyticsDao _analyticsDao;

  private volatile QuizRecordDao _quizRecordDao;

  private volatile PendingSubmissionDao _pendingSubmissionDao;

  private volatile WrongAnswerDao _wrongAnswerDao;

  private volatile AchievementDao _achievementDao;

  private volatile UserScoreDao _userScoreDao;

  private volatile SubscriptionCacheDao _subscriptionCacheDao;

  private volatile ScoreLogDao _scoreLogDao;

  private volatile StudyReportDao _studyReportDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `modelId` TEXT NOT NULL, `systemPrompt` TEXT, `messageCount` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `content` TEXT NOT NULL, `isUser` INTEGER NOT NULL, `contentType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, `metadata` TEXT, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `learning_records` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `learnDurationMin` INTEGER NOT NULL, `solveCount` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, `streakDays` INTEGER NOT NULL, `totalKnowledgePoints` INTEGER NOT NULL, `masteredPoints` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `knowledge_points` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `subject` TEXT NOT NULL, `parentId` TEXT, `status` TEXT NOT NULL, `confidence` REAL NOT NULL, `lastReviewedAt` INTEGER, `wrongCount` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_records` (`quizId` TEXT NOT NULL, `subject` TEXT NOT NULL, `knowledgePoints` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `questionCount` INTEGER NOT NULL, `score` REAL, `durationSeconds` INTEGER, `createdAt` INTEGER NOT NULL, `syncedToCloud` INTEGER NOT NULL, PRIMARY KEY(`quizId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_submissions` (`id` TEXT NOT NULL, `quizId` TEXT NOT NULL, `answers` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `wrong_answers` (`id` TEXT NOT NULL, `question` TEXT NOT NULL, `correctAnswer` TEXT NOT NULL, `userAnswer` TEXT NOT NULL, `subject` TEXT NOT NULL, `knowledgePoint` TEXT NOT NULL, `source` TEXT NOT NULL, `intervalDays` INTEGER NOT NULL, `consecutiveCorrect` INTEGER NOT NULL, `isMastered` INTEGER NOT NULL, `nextReviewAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL, `conditionType` TEXT NOT NULL, `conditionValue` INTEGER NOT NULL, `status` TEXT NOT NULL, `unlockedAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_score` (`id` TEXT NOT NULL, `totalScore` INTEGER NOT NULL, `currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastLearningDate` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `score_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventType` TEXT NOT NULL, `score` INTEGER NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `subscription_cache` (`id` INTEGER NOT NULL, `planType` TEXT NOT NULL, `status` TEXT NOT NULL, `featuresJson` TEXT NOT NULL, `dailyQuotaTotal` INTEGER NOT NULL, `dailyQuotaUsed` INTEGER NOT NULL, `validUntil` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4a9a1c5226f643086feba3d8f4cbd029')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `conversations`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `learning_records`");
        db.execSQL("DROP TABLE IF EXISTS `knowledge_points`");
        db.execSQL("DROP TABLE IF EXISTS `quiz_records`");
        db.execSQL("DROP TABLE IF EXISTS `pending_submissions`");
        db.execSQL("DROP TABLE IF EXISTS `wrong_answers`");
        db.execSQL("DROP TABLE IF EXISTS `achievements`");
        db.execSQL("DROP TABLE IF EXISTS `user_score`");
        db.execSQL("DROP TABLE IF EXISTS `score_logs`");
        db.execSQL("DROP TABLE IF EXISTS `subscription_cache`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsConversations = new HashMap<String, TableInfo.Column>(7);
        _columnsConversations.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("modelId", new TableInfo.Column("modelId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("systemPrompt", new TableInfo.Column("systemPrompt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversations.put("messageCount", new TableInfo.Column("messageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConversations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConversations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoConversations = new TableInfo("conversations", _columnsConversations, _foreignKeysConversations, _indicesConversations);
        final TableInfo _existingConversations = TableInfo.read(db, "conversations");
        if (!_infoConversations.equals(_existingConversations)) {
          return new RoomOpenHelper.ValidationResult(false, "conversations(com.aitutor.app.data.local.entity.ConversationEntity).\n"
                  + " Expected:\n" + _infoConversations + "\n"
                  + " Found:\n" + _existingConversations);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(8);
        _columnsMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("conversationId", new TableInfo.Column("conversationId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isUser", new TableInfo.Column("isUser", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("contentType", new TableInfo.Column("contentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("metadata", new TableInfo.Column("metadata", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysMessages.add(new TableInfo.ForeignKey("conversations", "CASCADE", "NO ACTION", Arrays.asList("conversationId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(1);
        _indicesMessages.add(new TableInfo.Index("index_messages_conversationId", false, Arrays.asList("conversationId"), Arrays.asList("ASC")));
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.aitutor.app.data.local.entity.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsLearningRecords = new HashMap<String, TableInfo.Column>(11);
        _columnsLearningRecords.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("learnDurationMin", new TableInfo.Column("learnDurationMin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("solveCount", new TableInfo.Column("solveCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("correctCount", new TableInfo.Column("correctCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("wrongCount", new TableInfo.Column("wrongCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("streakDays", new TableInfo.Column("streakDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("totalKnowledgePoints", new TableInfo.Column("totalKnowledgePoints", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("masteredPoints", new TableInfo.Column("masteredPoints", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLearningRecords.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLearningRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLearningRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLearningRecords = new TableInfo("learning_records", _columnsLearningRecords, _foreignKeysLearningRecords, _indicesLearningRecords);
        final TableInfo _existingLearningRecords = TableInfo.read(db, "learning_records");
        if (!_infoLearningRecords.equals(_existingLearningRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "learning_records(com.aitutor.app.data.local.entity.LearningRecordEntity).\n"
                  + " Expected:\n" + _infoLearningRecords + "\n"
                  + " Found:\n" + _existingLearningRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsKnowledgePoints = new HashMap<String, TableInfo.Column>(8);
        _columnsKnowledgePoints.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("parentId", new TableInfo.Column("parentId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("lastReviewedAt", new TableInfo.Column("lastReviewedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnowledgePoints.put("wrongCount", new TableInfo.Column("wrongCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKnowledgePoints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKnowledgePoints = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKnowledgePoints = new TableInfo("knowledge_points", _columnsKnowledgePoints, _foreignKeysKnowledgePoints, _indicesKnowledgePoints);
        final TableInfo _existingKnowledgePoints = TableInfo.read(db, "knowledge_points");
        if (!_infoKnowledgePoints.equals(_existingKnowledgePoints)) {
          return new RoomOpenHelper.ValidationResult(false, "knowledge_points(com.aitutor.app.data.local.entity.KnowledgePointEntity).\n"
                  + " Expected:\n" + _infoKnowledgePoints + "\n"
                  + " Found:\n" + _existingKnowledgePoints);
        }
        final HashMap<String, TableInfo.Column> _columnsQuizRecords = new HashMap<String, TableInfo.Column>(9);
        _columnsQuizRecords.put("quizId", new TableInfo.Column("quizId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("knowledgePoints", new TableInfo.Column("knowledgePoints", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("difficulty", new TableInfo.Column("difficulty", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("questionCount", new TableInfo.Column("questionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("score", new TableInfo.Column("score", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("durationSeconds", new TableInfo.Column("durationSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuizRecords.put("syncedToCloud", new TableInfo.Column("syncedToCloud", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysQuizRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesQuizRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoQuizRecords = new TableInfo("quiz_records", _columnsQuizRecords, _foreignKeysQuizRecords, _indicesQuizRecords);
        final TableInfo _existingQuizRecords = TableInfo.read(db, "quiz_records");
        if (!_infoQuizRecords.equals(_existingQuizRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "quiz_records(com.aitutor.app.data.local.entity.QuizRecordEntity).\n"
                  + " Expected:\n" + _infoQuizRecords + "\n"
                  + " Found:\n" + _existingQuizRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsPendingSubmissions = new HashMap<String, TableInfo.Column>(6);
        _columnsPendingSubmissions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingSubmissions.put("quizId", new TableInfo.Column("quizId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingSubmissions.put("answers", new TableInfo.Column("answers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingSubmissions.put("durationSeconds", new TableInfo.Column("durationSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingSubmissions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingSubmissions.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingSubmissions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingSubmissions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPendingSubmissions = new TableInfo("pending_submissions", _columnsPendingSubmissions, _foreignKeysPendingSubmissions, _indicesPendingSubmissions);
        final TableInfo _existingPendingSubmissions = TableInfo.read(db, "pending_submissions");
        if (!_infoPendingSubmissions.equals(_existingPendingSubmissions)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_submissions(com.aitutor.app.data.local.entity.PendingSubmissionEntity).\n"
                  + " Expected:\n" + _infoPendingSubmissions + "\n"
                  + " Found:\n" + _existingPendingSubmissions);
        }
        final HashMap<String, TableInfo.Column> _columnsWrongAnswers = new HashMap<String, TableInfo.Column>(13);
        _columnsWrongAnswers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("question", new TableInfo.Column("question", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("correctAnswer", new TableInfo.Column("correctAnswer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("userAnswer", new TableInfo.Column("userAnswer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("knowledgePoint", new TableInfo.Column("knowledgePoint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("intervalDays", new TableInfo.Column("intervalDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("consecutiveCorrect", new TableInfo.Column("consecutiveCorrect", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("isMastered", new TableInfo.Column("isMastered", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("nextReviewAt", new TableInfo.Column("nextReviewAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWrongAnswers.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWrongAnswers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWrongAnswers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWrongAnswers = new TableInfo("wrong_answers", _columnsWrongAnswers, _foreignKeysWrongAnswers, _indicesWrongAnswers);
        final TableInfo _existingWrongAnswers = TableInfo.read(db, "wrong_answers");
        if (!_infoWrongAnswers.equals(_existingWrongAnswers)) {
          return new RoomOpenHelper.ValidationResult(false, "wrong_answers(com.aitutor.app.data.local.entity.WrongAnswerEntity).\n"
                  + " Expected:\n" + _infoWrongAnswers + "\n"
                  + " Found:\n" + _existingWrongAnswers);
        }
        final HashMap<String, TableInfo.Column> _columnsAchievements = new HashMap<String, TableInfo.Column>(8);
        _columnsAchievements.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("conditionType", new TableInfo.Column("conditionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("conditionValue", new TableInfo.Column("conditionValue", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievements.put("unlockedAt", new TableInfo.Column("unlockedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAchievements = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAchievements = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAchievements = new TableInfo("achievements", _columnsAchievements, _foreignKeysAchievements, _indicesAchievements);
        final TableInfo _existingAchievements = TableInfo.read(db, "achievements");
        if (!_infoAchievements.equals(_existingAchievements)) {
          return new RoomOpenHelper.ValidationResult(false, "achievements(com.aitutor.app.data.local.entity.AchievementEntity).\n"
                  + " Expected:\n" + _infoAchievements + "\n"
                  + " Found:\n" + _existingAchievements);
        }
        final HashMap<String, TableInfo.Column> _columnsUserScore = new HashMap<String, TableInfo.Column>(6);
        _columnsUserScore.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserScore.put("totalScore", new TableInfo.Column("totalScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserScore.put("currentStreak", new TableInfo.Column("currentStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserScore.put("longestStreak", new TableInfo.Column("longestStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserScore.put("lastLearningDate", new TableInfo.Column("lastLearningDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserScore.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserScore = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserScore = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserScore = new TableInfo("user_score", _columnsUserScore, _foreignKeysUserScore, _indicesUserScore);
        final TableInfo _existingUserScore = TableInfo.read(db, "user_score");
        if (!_infoUserScore.equals(_existingUserScore)) {
          return new RoomOpenHelper.ValidationResult(false, "user_score(com.aitutor.app.data.local.entity.UserScoreEntity).\n"
                  + " Expected:\n" + _infoUserScore + "\n"
                  + " Found:\n" + _existingUserScore);
        }
        final HashMap<String, TableInfo.Column> _columnsScoreLogs = new HashMap<String, TableInfo.Column>(6);
        _columnsScoreLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoreLogs.put("eventType", new TableInfo.Column("eventType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoreLogs.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoreLogs.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoreLogs.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoreLogs.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysScoreLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesScoreLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoScoreLogs = new TableInfo("score_logs", _columnsScoreLogs, _foreignKeysScoreLogs, _indicesScoreLogs);
        final TableInfo _existingScoreLogs = TableInfo.read(db, "score_logs");
        if (!_infoScoreLogs.equals(_existingScoreLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "score_logs(com.aitutor.app.data.local.entity.ScoreLogEntity).\n"
                  + " Expected:\n" + _infoScoreLogs + "\n"
                  + " Found:\n" + _existingScoreLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsSubscriptionCache = new HashMap<String, TableInfo.Column>(8);
        _columnsSubscriptionCache.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("planType", new TableInfo.Column("planType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("featuresJson", new TableInfo.Column("featuresJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("dailyQuotaTotal", new TableInfo.Column("dailyQuotaTotal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("dailyQuotaUsed", new TableInfo.Column("dailyQuotaUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("validUntil", new TableInfo.Column("validUntil", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubscriptionCache.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSubscriptionCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSubscriptionCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSubscriptionCache = new TableInfo("subscription_cache", _columnsSubscriptionCache, _foreignKeysSubscriptionCache, _indicesSubscriptionCache);
        final TableInfo _existingSubscriptionCache = TableInfo.read(db, "subscription_cache");
        if (!_infoSubscriptionCache.equals(_existingSubscriptionCache)) {
          return new RoomOpenHelper.ValidationResult(false, "subscription_cache(com.aitutor.app.data.local.entity.SubscriptionCacheEntity).\n"
                  + " Expected:\n" + _infoSubscriptionCache + "\n"
                  + " Found:\n" + _existingSubscriptionCache);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4a9a1c5226f643086feba3d8f4cbd029", "57d43d310b97c89c2e0dc237b4d2a529");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "conversations","messages","learning_records","knowledge_points","quiz_records","pending_submissions","wrong_answers","achievements","user_score","score_logs","subscription_cache");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `conversations`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `learning_records`");
      _db.execSQL("DELETE FROM `knowledge_points`");
      _db.execSQL("DELETE FROM `quiz_records`");
      _db.execSQL("DELETE FROM `pending_submissions`");
      _db.execSQL("DELETE FROM `wrong_answers`");
      _db.execSQL("DELETE FROM `achievements`");
      _db.execSQL("DELETE FROM `user_score`");
      _db.execSQL("DELETE FROM `score_logs`");
      _db.execSQL("DELETE FROM `subscription_cache`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ConversationDao.class, ConversationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AnalyticsDao.class, AnalyticsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(QuizRecordDao.class, QuizRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PendingSubmissionDao.class, PendingSubmissionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WrongAnswerDao.class, WrongAnswerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AchievementDao.class, AchievementDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserScoreDao.class, UserScoreDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SubscriptionCacheDao.class, SubscriptionCacheDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ScoreLogDao.class, ScoreLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StudyReportDao.class, StudyReportDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ConversationDao conversationDao() {
    if (_conversationDao != null) {
      return _conversationDao;
    } else {
      synchronized(this) {
        if(_conversationDao == null) {
          _conversationDao = new ConversationDao_Impl(this);
        }
        return _conversationDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public AnalyticsDao analyticsDao() {
    if (_analyticsDao != null) {
      return _analyticsDao;
    } else {
      synchronized(this) {
        if(_analyticsDao == null) {
          _analyticsDao = new AnalyticsDao_Impl(this);
        }
        return _analyticsDao;
      }
    }
  }

  @Override
  public QuizRecordDao quizRecordDao() {
    if (_quizRecordDao != null) {
      return _quizRecordDao;
    } else {
      synchronized(this) {
        if(_quizRecordDao == null) {
          _quizRecordDao = new QuizRecordDao_Impl(this);
        }
        return _quizRecordDao;
      }
    }
  }

  @Override
  public PendingSubmissionDao pendingSubmissionDao() {
    if (_pendingSubmissionDao != null) {
      return _pendingSubmissionDao;
    } else {
      synchronized(this) {
        if(_pendingSubmissionDao == null) {
          _pendingSubmissionDao = new PendingSubmissionDao_Impl(this);
        }
        return _pendingSubmissionDao;
      }
    }
  }

  @Override
  public WrongAnswerDao wrongAnswerDao() {
    if (_wrongAnswerDao != null) {
      return _wrongAnswerDao;
    } else {
      synchronized(this) {
        if(_wrongAnswerDao == null) {
          _wrongAnswerDao = new WrongAnswerDao_Impl(this);
        }
        return _wrongAnswerDao;
      }
    }
  }

  @Override
  public AchievementDao achievementDao() {
    if (_achievementDao != null) {
      return _achievementDao;
    } else {
      synchronized(this) {
        if(_achievementDao == null) {
          _achievementDao = new AchievementDao_Impl(this);
        }
        return _achievementDao;
      }
    }
  }

  @Override
  public UserScoreDao userScoreDao() {
    if (_userScoreDao != null) {
      return _userScoreDao;
    } else {
      synchronized(this) {
        if(_userScoreDao == null) {
          _userScoreDao = new UserScoreDao_Impl(this);
        }
        return _userScoreDao;
      }
    }
  }

  @Override
  public SubscriptionCacheDao subscriptionCacheDao() {
    if (_subscriptionCacheDao != null) {
      return _subscriptionCacheDao;
    } else {
      synchronized(this) {
        if(_subscriptionCacheDao == null) {
          _subscriptionCacheDao = new SubscriptionCacheDao_Impl(this);
        }
        return _subscriptionCacheDao;
      }
    }
  }

  @Override
  public ScoreLogDao scoreLogDao() {
    if (_scoreLogDao != null) {
      return _scoreLogDao;
    } else {
      synchronized(this) {
        if(_scoreLogDao == null) {
          _scoreLogDao = new ScoreLogDao_Impl(this);
        }
        return _scoreLogDao;
      }
    }
  }

  @Override
  public StudyReportDao studyReportDao() {
    if (_studyReportDao != null) {
      return _studyReportDao;
    } else {
      synchronized(this) {
        if(_studyReportDao == null) {
          _studyReportDao = new StudyReportDao_Impl(this);
        }
        return _studyReportDao;
      }
    }
  }
}
