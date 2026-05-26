package com.aitutor.app.data.billing

/**
 * Google Play 订阅商品模型。
 */
data class BillingProduct(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val currencyCode: String = "CNY",
    val type: BillingProductType = BillingProductType.MONTHLY
)

/**
 * 订阅周期类型。
 */
enum class BillingProductType(val period: String) {
    MONTHLY("每月"),
    YEARLY("每年")
}
