package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val conditionType: String,
    val conditionValue: Int,
    val status: String = "LOCKED", // LOCKED / UNLOCKED
    val unlockedAt: Long? = null
)
