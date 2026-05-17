package com.aitutor.app.data.model

import com.aitutor.app.domain.model.FeatureType
import com.aitutor.app.domain.model.QuotaResult
import com.aitutor.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局配额守卫，用于在执行需要消耗配额的操作前进行检查和消耗。
 *
 * 提供统一的配额检查入口，避免各 ViewModel 直接耦合 SubscriptionRepository。
 */
@Singleton
class QuotaGuard @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {

    /**
     * 检查指定功能的配额是否可用。
     *
     * @param feature 要检查的功能类型
     * @return QuotaResult — Allowed / Exceeded / Disabled
     */
    fun checkQuota(feature: FeatureType): QuotaResult {
        return runBlocking {
            subscriptionRepository.checkQuota(feature)
        }
    }

    /**
     * 消耗一次配额（在实际使用后调用）。
     *
     * @param feature 要消耗配额的功能类型
     * @return true 表示消耗成功，false 表示失败
     */
    fun consumeQuota(feature: FeatureType): Boolean {
        return runBlocking {
            subscriptionRepository.consumeQuota(feature)
                .onSuccess { return@runBlocking true }
                .onFailure { return@runBlocking false }
            false
        }
    }
}
