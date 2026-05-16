package com.aitutor.app.domain.model

/**
 * 单个功能的配额信息。
 */
data class FeatureQuota(
    val enabled: Boolean = true,
    val limit: Int? = null,   // null 表示无限
    val used: Int = 0
) {
    /** 剩余可用次数，无限则返回 Int.MAX_VALUE */
    val remaining: Int
        get() = if (limit == null) Int.MAX_VALUE else (limit - used).coerceAtLeast(0)

    /** 是否已达到限额 */
    val isExceeded: Boolean
        get() = limit != null && used >= limit
}

/**
 * 配额检查结果。
 */
sealed class QuotaResult {
    /** 允许执行 — 包含已用次数（消耗前检查） */
    data class Allowed(val remaining: Int = Int.MAX_VALUE) : QuotaResult()

    /** 已超限 — 包含配额信息 */
    data class Exceeded(val limit: Int, val used: Int) : QuotaResult()

    /** 功能未启用 */
    data class Disabled(val featureName: String = "该功能") : QuotaResult()

    val isAllowed: Boolean get() = this is Allowed
    val message: String
        get() = when (this) {
            is Allowed -> "可用"
            is Exceeded -> "今日次数已用尽 ($used/$limit)"
            is Disabled -> "$featureName 暂未开放"
        }
}
