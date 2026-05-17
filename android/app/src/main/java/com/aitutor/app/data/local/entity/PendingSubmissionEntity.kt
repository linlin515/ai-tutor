package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pending_submissions")
data class PendingSubmissionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val answers: String,
    val durationSeconds: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
