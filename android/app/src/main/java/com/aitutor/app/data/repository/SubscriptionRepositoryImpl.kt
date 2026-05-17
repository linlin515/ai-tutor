package com.aitutor.app.data.repository

import com.aitutor.app.data.local.dao.SubscriptionCacheDao
import com.aitutor.app.data.local.entity.SubscriptionCacheEntity
import com.aitutor.app.data.remote.api.SubscriptionApi
import com.aitutor.app.data.remote.dto.SubscriptionStatusDto
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.SubscriptionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionApi: SubscriptionApi,
    private val subscriptionCacheDao: SubscriptionCacheDao,
    private val gson: Gson
) : SubscriptionRepository {

    override fun getSubscriptionState(): Flow<SubscriptionState> {
        return subscriptionCacheDao.observe().map { entity ->
            entity?.toDomain() ?: SubscriptionState(isLoading = true)
        }
    }

    override suspend fun checkQuota(feature: FeatureType): QuotaResult = withContext(Dispatchers.IO) {
        val cache = subscriptionCacheDao.get()

        // 先检查功能级配额
        val featureQuota = cache?.let { parseFeatureQuota(it.featuresJson, feature.key) }
        if (featureQuota != null) {
            if (!featureQuota.enabled) return@withContext QuotaResult.Disabled(feature.displayName)
            if (featureQuota.isExceeded) return@withContext QuotaResult.Exceeded(
                limit = featureQuota.limit ?: 0,
                used = featureQuota.used
            )
        }

        // 再检查日配额
        if (cache != null && cache.dailyQuotaTotal > 0 && cache.dailyQuotaUsed >= cache.dailyQuotaTotal) {
            return@withContext QuotaResult.Exceeded(
                limit = cache.dailyQuotaTotal,
                used = cache.dailyQuotaUsed
            )
        }

        val remaining = if (cache != null && cache.dailyQuotaTotal > 0) {
            (cache.dailyQuotaTotal - cache.dailyQuotaUsed).coerceAtLeast(0)
        } else Int.MAX_VALUE

        QuotaResult.Allowed(remaining = remaining)
    }

    override suspend fun consumeQuota(feature: FeatureType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = subscriptionApi.consumeQuota(
                com.aitutor.app.data.remote.dto.ConsumeQuotaRequest(feature = feature.key)
            )
            val body = response.body()
            if (response.isSuccessful && body?.data?.success == true) {
                // 更新缓存中的已用次数
                body.data.dailyQuota?.let { quotaDto ->
                    subscriptionCacheDao.get()?.let { existing ->
                        val updated = existing.copy(
                            dailyQuotaUsed = quotaDto.usedQueries,
                            updatedAt = System.currentTimeMillis()
                        )
                        subscriptionCacheDao.insert(updated)
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.message ?: "消耗配额失败 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("消耗配额请求失败: ${e.message}", e))
        }
    }

    override suspend fun refreshStatus(): Result<SubscriptionState> = withContext(Dispatchers.IO) {
        try {
            val response = subscriptionApi.getStatus()
            val body = response.body()
            if (response.isSuccessful && body?.data != null) {
                val dto = body.data
                val entity = dto.toEntity()
                subscriptionCacheDao.insert(entity)
                Result.success(entity.toDomain())
            } else {
                // 保留缓存数据，但标记刷新失败
                val fallback = subscriptionCacheDao.get()?.toDomain() ?: SubscriptionState()
                Result.success(fallback)
            }
        } catch (e: Exception) {
            // 网络错误：返回缓存数据
            val cached = subscriptionCacheDao.get()?.toDomain() ?: SubscriptionState()
            Result.success(cached)
        }
    }

    // --- DTO 转换 ---

    private fun SubscriptionStatusDto.toEntity(): SubscriptionCacheEntity {
        return SubscriptionCacheEntity(
            planType = planType,
            status = status,
            featuresJson = gson.toJson(features),
            dailyQuotaTotal = dailyQuota?.totalQueries ?: 0,
            dailyQuotaUsed = dailyQuota?.usedQueries ?: 0,
            validUntil = validUntil,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun SubscriptionCacheEntity.toDomain(): SubscriptionState {
        val featureMap = mutableMapOf<String, FeatureQuota>()
        try {
            val type = object : TypeToken<Map<String, com.aitutor.app.data.remote.dto.FeatureQuotaDto>>() {}.type
            val parsed: Map<String, com.aitutor.app.data.remote.dto.FeatureQuotaDto>? = gson.fromJson(featuresJson, type)
            parsed?.forEach { (key, dto) ->
                featureMap[key] = FeatureQuota(
                    enabled = dto.enabled,
                    limit = dto.limit,
                    used = dto.used
                )
            }
        } catch (_: Exception) {
            // JSON 解析失败，使用空 map
        }

        return SubscriptionState(
            planType = planType,
            status = status,
            features = featureMap,
            dailyQuota = DailyQuota(
                totalQueries = dailyQuotaTotal,
                usedQueries = dailyQuotaUsed
            ),
            validUntil = validUntil,
            isLoading = false
        )
    }

    private fun parseFeatureQuota(json: String, featureKey: String): FeatureQuota? {
        return try {
            val type = object : TypeToken<Map<String, com.aitutor.app.data.remote.dto.FeatureQuotaDto>>() {}.type
            val parsed: Map<String, com.aitutor.app.data.remote.dto.FeatureQuotaDto>? = gson.fromJson(json, type)
            parsed?.get(featureKey)?.let {
                FeatureQuota(enabled = it.enabled, limit = it.limit, used = it.used)
            }
        } catch (_: Exception) {
            null
        }
    }
}
