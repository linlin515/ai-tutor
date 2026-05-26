package com.aitutor.app.domain.repository

import com.aitutor.app.data.billing.PurchaseResult
import com.aitutor.app.domain.model.FeatureType
import com.aitutor.app.domain.model.QuotaResult
import com.aitutor.app.domain.model.SubscriptionState
import kotlinx.coroutines.flow.Flow

/**
 * 订阅管理仓库接口。
 */
interface SubscriptionRepository {

    /** 观察订阅状态（优先缓存，后台刷新） */
    fun getSubscriptionState(): Flow<SubscriptionState>

    /** 检查指定功能是否可用 */
    suspend fun checkQuota(feature: FeatureType): QuotaResult

    /** 消耗一次配额（在实际使用后调用） */
    suspend fun consumeQuota(feature: FeatureType): Result<Unit>

    /** 从服务器刷新订阅状态 */
    suspend fun refreshStatus(): Result<SubscriptionState>

    /** 验证购买（调后端 verify API） */
    suspend fun verifyPurchase(purchaseToken: String, productId: String): Result<SubscriptionState>

    /** 恢复已购买的订阅 */
    suspend fun restorePurchases(): Result<List<PurchaseResult>>
}
