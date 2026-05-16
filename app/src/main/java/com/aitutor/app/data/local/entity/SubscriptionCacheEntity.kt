package com.aitutor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 订阅缓存实体，用于离线快速展示配额状态。
 */
@Entity(tableName = "subscription_cache")
data class SubscriptionCacheEntity(
    @PrimaryKey val id: Int = 1,  // 单行记录
    val planType: String = "free",
    val status: String = "active",
    val featuresJson: String = "{}",       // 序列化的 Map<String, FeatureQuotaDto>
    val dailyQuotaTotal: Int = 0,
    val dailyQuotaUsed: Int = 0,
    val validUntil: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
