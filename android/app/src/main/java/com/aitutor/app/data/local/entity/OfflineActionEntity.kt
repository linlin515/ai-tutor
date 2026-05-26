package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val targetId: String,
    val payload: String, // JSON string
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
