package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_logs")
data class ScoreLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val score: Int,
    val description: String = "",
    val date: String, // yyyy-MM-dd
    val createdAt: Long = System.currentTimeMillis()
)
