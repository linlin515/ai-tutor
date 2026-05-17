package com.aitutor.app.domain.model

/**
 * 订阅状态聚合模型，从 API + 本地缓存构建。
 */
data class SubscriptionState(
    val planType: String = "free",         // "free" / "premium"
    val status: String = "active",         // "active" / "expired" / "cancelled"
    val features: Map<String, FeatureQuota> = emptyMap(),
    val dailyQuota: DailyQuota = DailyQuota(),
    val validUntil: String? = null,
    val trialAvailable: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * 日配额信息。
 */
data class DailyQuota(
    val totalQueries: Int = 0,
    val usedQueries: Int = 0,
    val resetAt: String? = null
) {
    val remaining: Int get() = (totalQueries - usedQueries).coerceAtLeast(0)
    val isExceeded: Boolean get() = totalQueries > 0 && usedQueries >= totalQueries
    val progress: Float get() = if (totalQueries > 0) (usedQueries.toFloat() / totalQueries).coerceIn(0f, 1f) else 0f
}

/**
 * 订阅状态 UI 封装，包含刷新动作。
 */
sealed class SubscriptionUiEvent {
    data class ShowMessage(val message: String) : SubscriptionUiEvent()
    data class QuotaExceeded(val feature: FeatureType, val quota: DailyQuota) : SubscriptionUiEvent()
}
