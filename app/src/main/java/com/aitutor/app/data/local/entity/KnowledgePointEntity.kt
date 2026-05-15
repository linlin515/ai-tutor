package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_points")
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val parentId: String? = null,
    val status: String = "weak",
    val confidence: Float = 0f,
    val lastReviewedAt: Long? = null,
    val wrongCount: Int = 0
)
