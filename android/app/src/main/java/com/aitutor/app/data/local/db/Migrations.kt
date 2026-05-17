package com.aitutor.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration from version 1 to 2.
 *
 * v1: conversations, messages
 * v2: +learning_records, knowledge_points, quiz_records, pending_submissions, wrong_answers
 */
val MIGRATION_1_2 = Migration(1, 2) { db ->
    // learning_records
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `learning_records` (
            `id` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `learnDurationMin` INTEGER NOT NULL DEFAULT 0,
            `solveCount` INTEGER NOT NULL DEFAULT 0,
            `correctCount` INTEGER NOT NULL DEFAULT 0,
            `wrongCount` INTEGER NOT NULL DEFAULT 0,
            `streakDays` INTEGER NOT NULL DEFAULT 0,
            `totalKnowledgePoints` INTEGER NOT NULL DEFAULT 0,
            `masteredPoints` INTEGER NOT NULL DEFAULT 0,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )

    // knowledge_points
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `knowledge_points` (
            `id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `subject` TEXT NOT NULL,
            `parentId` TEXT,
            `status` TEXT NOT NULL DEFAULT 'weak',
            `confidence` REAL NOT NULL DEFAULT 0.0,
            `lastReviewedAt` INTEGER,
            `wrongCount` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )

    // quiz_records
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `quiz_records` (
            `quizId` TEXT NOT NULL,
            `subject` TEXT NOT NULL,
            `knowledgePoints` TEXT NOT NULL DEFAULT '[]',
            `difficulty` TEXT NOT NULL DEFAULT 'medium',
            `questionCount` INTEGER NOT NULL DEFAULT 0,
            `score` REAL,
            `durationSeconds` INTEGER,
            `createdAt` INTEGER NOT NULL,
            `syncedToCloud` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`quizId`)
        )
        """.trimIndent()
    )

    // pending_submissions
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `pending_submissions` (
            `id` TEXT NOT NULL,
            `quizId` TEXT NOT NULL,
            `answers` TEXT NOT NULL,
            `durationSeconds` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `retryCount` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )

    // wrong_answers (with indices that mimic Room's generated schema)
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `wrong_answers` (
            `id` TEXT NOT NULL,
            `question` TEXT NOT NULL,
            `correctAnswer` TEXT NOT NULL,
            `userAnswer` TEXT NOT NULL,
            `subject` TEXT NOT NULL,
            `knowledgePoint` TEXT NOT NULL DEFAULT '',
            `source` TEXT NOT NULL DEFAULT 'quiz',
            `intervalDays` INTEGER NOT NULL DEFAULT 1,
            `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0,
            `isMastered` INTEGER NOT NULL DEFAULT 0,
            `nextReviewAt` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
}

/**
 * Room database migration from version 2 to 3.
 *
 * v2: conversations, messages, learning_records, knowledge_points, quiz_records,
 *      pending_submissions, wrong_answers
 * v3: +achievements, user_score, score_logs, subscription_cache
 */
val MIGRATION_2_3 = Migration(2, 3) { db ->
    // achievements
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `achievements` (
            `id` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `icon` TEXT NOT NULL,
            `conditionType` TEXT NOT NULL,
            `conditionValue` INTEGER NOT NULL,
            `status` TEXT NOT NULL DEFAULT 'LOCKED',
            `unlockedAt` INTEGER,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )

    // user_score (single row, keyed by id='user_score')
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `user_score` (
            `id` TEXT NOT NULL,
            `totalScore` INTEGER NOT NULL DEFAULT 0,
            `currentStreak` INTEGER NOT NULL DEFAULT 0,
            `longestStreak` INTEGER NOT NULL DEFAULT 0,
            `lastLearningDate` TEXT NOT NULL DEFAULT '',
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )

    // score_logs
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `score_logs` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `eventType` TEXT NOT NULL,
            `score` INTEGER NOT NULL,
            `description` TEXT NOT NULL DEFAULT '',
            `date` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL
        )
        """.trimIndent()
    )

    // subscription_cache (single row, keyed by id=1)
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `subscription_cache` (
            `id` INTEGER NOT NULL,
            `planType` TEXT NOT NULL DEFAULT 'free',
            `status` TEXT NOT NULL DEFAULT 'active',
            `featuresJson` TEXT NOT NULL DEFAULT '{}',
            `dailyQuotaTotal` INTEGER NOT NULL DEFAULT 0,
            `dailyQuotaUsed` INTEGER NOT NULL DEFAULT 0,
            `validUntil` TEXT,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
}

/**
 * Room database migration from version 3 to 4.
 *
 * v3: all previous tables
 * v4: +pending_messages for P1-2 offline message queue
 */
val MIGRATION_3_4 = Migration(3, 4) { db ->
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `pending_messages` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `conversationId` INTEGER NOT NULL,
            `content` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `retryCount` INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
}
