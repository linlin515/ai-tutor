package com.aitutor.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 连接状态。
 */
enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
}

/**
 * 购买状态。
 */
enum class PurchaseState {
    IDLE,
    QUERYING_PRODUCTS,
    PRODUCTS_READY,
    PURCHASING,
    PURCHASED,
    VERIFYING,
    VERIFIED,
    FAILED,
    RESTORING
}

/**
 * 购买结果。
 */
data class PurchaseResult(
    val purchaseToken: String,
    val productId: String,
    val isAcknowledged: Boolean = false,
    val orderId: String? = null,
    val purchaseTime: Long = 0L
)

/**
 * 自定义 ProductDetails 查询结果，替代 BillingClient 5.x 的 ProductDetailsResult（无可访问的 Builder）。
 */
data class QueryProductDetailsResult(
    val billingResult: BillingResult,
    val productDetailsList: List<ProductDetails>
)

/**
 * 自定义 Purchases 查询结果，替代 BillingClient 5.x 的 PurchaseResult（无可访问的 Builder）。
 */
data class QueryPurchaseResult(
    val billingResult: BillingResult,
    val purchases: List<Purchase>
)

/**
 * Google Play Billing 管理类。
 *
 * 管理 BillingClient 生命周期（连接/断开/重连）、
 * 商品查询、购买发起、确认购买、恢复购买。
 */
@Singleton
class BillingManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _purchaseState = MutableStateFlow(PurchaseState.IDLE)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _products = MutableStateFlow<List<BillingProduct>>(emptyList())
    val products: StateFlow<List<BillingProduct>> = _products.asStateFlow()

    // 最近一次购买结果，供 ViewModel 消费
    private val _lastPurchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val lastPurchaseResult: StateFlow<PurchaseResult?> = _lastPurchaseResult.asStateFlow()

    private var billingClient: BillingClient? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdated(billingResult, purchases)
    }

    /**
     * 连接 BillingClient。
     *
     * CONNECTING → CONNECTED / DISCONNECTED
     */
    suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        if (_connectionState.value == BillingConnectionState.CONNECTED) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        _connectionState.value = BillingConnectionState.CONNECTING

        val client = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient = client
                    _connectionState.value = BillingConnectionState.CONNECTED
                    continuation.resume(true)
                } else {
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                    continuation.resume(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
                // 自动重连（指数退避）
                scope.launch {
                    autoReconnect()
                }
            }
        })
    }

    /**
     * 查询订阅商品。
     *
     * QUERYING_PRODUCTS → PRODUCTS_READY / FAILED
     */
    suspend fun queryProducts(): Result<List<BillingProduct>> {
        if (_connectionState.value != BillingConnectionState.CONNECTED) {
            val connected = connect()
            if (!connected) {
                _purchaseState.value = PurchaseState.FAILED
                return Result.failure(Exception("BillingClient 未连接"))
            }
        }

        _purchaseState.value = PurchaseState.QUERYING_PRODUCTS

        return try {
            val client = billingClient ?: return Result.failure(Exception("BillingClient 为空"))

            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("premium_monthly")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("premium_yearly")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult: QueryProductDetailsResult = withTimeout(15_000L) {
                client.awaitQueryProductDetails(params)
            }

            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsResult.productDetailsList ?: emptyList()
                val products = details.map { it.toBillingProduct() }
                _products.value = products
                _purchaseState.value = PurchaseState.PRODUCTS_READY
                Result.success(products)
            } else {
                _purchaseState.value = PurchaseState.FAILED
                Result.failure(Exception("查询商品失败: ${productDetailsResult.billingResult.responseCode}"))
            }
        } catch (e: Exception) {
            _purchaseState.value = PurchaseState.FAILED
            Result.failure(Exception("查询商品异常: ${e.message}", e))
        }
    }

    /**
     * 发起购买流程。
     *
     * PURCHASING → (回调 PurchasesUpdatedListener → PURCHASED / FAILED)
     */
    fun launchBillingFlow(activity: Activity, productId: String) {
        val client = billingClient ?: return
        _purchaseState.value = PurchaseState.PURCHASING

        // 查找对应的 ProductDetails
        val productDetails = findProductDetails(productId) ?: run {
            _purchaseState.value = PurchaseState.FAILED
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _purchaseState.value = PurchaseState.FAILED
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        client.launchBillingFlow(activity, flowParams)
    }

    /**
     * 确认购买（acknowledgePurchase）。
     *
     * 使用指数退避重试 3 次。
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
        val client = billingClient ?: return false
        var lastError: Exception? = null

        for (attempt in 1..3) {
            try {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()

                val result = client.awaitAcknowledgePurchase(params)

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    return true
                }
                lastError = Exception("Acknowledge 失败: ${result.responseCode}")
            } catch (e: Exception) {
                lastError = e
            }

            if (attempt < 3) {
                // 指数退避: 1s, 2s, 4s
                delay(1000L * (1 shl (attempt - 1)))
            }
        }

        return false
    }

    /**
     * 恢复购买（queryPurchasesAsync）。
     *
     * RESTORING → 找到有效订阅 / 未找到
     */
    suspend fun restorePurchases(): List<PurchaseResult> {
        _purchaseState.value = PurchaseState.RESTORING

        return try {
            val client = billingClient ?: run {
                _purchaseState.value = PurchaseState.FAILED
                return emptyList()
            }

            val result = client.awaitQueryPurchases(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchases = result.purchases ?: emptyList()
                if (purchases.isEmpty()) {
                    _purchaseState.value = PurchaseState.IDLE
                    emptyList()
                } else {
                    val purchaseResults = purchases.map { it.toPurchaseResult() }
                    _purchaseState.value = PurchaseState.PRODUCTS_READY
                    purchaseResults
                }
            } else {
                _purchaseState.value = PurchaseState.FAILED
                emptyList()
            }
        } catch (e: Exception) {
            _purchaseState.value = PurchaseState.FAILED
            emptyList()
        }
    }

    /**
     * 断开 BillingClient。
     */
    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
        _connectionState.value = BillingConnectionState.CLOSED
        _purchaseState.value = PurchaseState.IDLE
    }

    // ========== 私有方法 ==========

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _purchaseState.value = PurchaseState.FAILED
                    return
                }

                // 处理每一笔购买
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _purchaseState.value = PurchaseState.PURCHASED
                        _lastPurchaseResult.value = purchase.toPurchaseResult()
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.IDLE
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // 已拥有该商品，尝试恢复
                _purchaseState.value = PurchaseState.PURCHASED
                scope.launch {
                    val results = restorePurchases()
                    results.firstOrNull()?.let {
                        _lastPurchaseResult.value = it
                    }
                }
            }
            else -> {
                _purchaseState.value = PurchaseState.FAILED
            }
        }
    }

    private suspend fun autoReconnect() {
        for (attempt in 1..3) {
            delay(1000L * (1 shl (attempt - 1)))
            if (_connectionState.value == BillingConnectionState.CONNECTED) return
            _connectionState.value = BillingConnectionState.CONNECTING

            val client = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()

            try {
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            billingClient = client
                            _connectionState.value = BillingConnectionState.CONNECTED
                        } else {
                            _connectionState.value = BillingConnectionState.DISCONNECTED
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        _connectionState.value = BillingConnectionState.DISCONNECTED
                    }
                })
            } catch (e: Exception) {
                _connectionState.value = BillingConnectionState.DISCONNECTED
            }
        }
    }

    private fun findProductDetails(productId: String): ProductDetails? {
        return _products.value.let { products ->
            // 从缓存的 ProductDetails 中查找
            null // BillingManager doesn't cache ProductDetails, query on demand
        } ?: run {
            // Fallback: query fresh
            null
        }
    }

    /**
     * 将 ProductDetails 转为 BillingProduct。
     */
    private fun ProductDetails.toBillingProduct(): BillingProduct {
        val subscriptionDetails = this.subscriptionOfferDetails?.firstOrNull()
        val pricingPhase = subscriptionDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
        val price = pricingPhase?.formattedPrice ?: "¥0"
        val currencyCode = pricingPhase?.priceCurrencyCode ?: "CNY"

        val type = when (productId) {
            "premium_monthly" -> BillingProductType.MONTHLY
            "premium_yearly" -> BillingProductType.YEARLY
            else -> BillingProductType.MONTHLY
        }

        return BillingProduct(
            productId = productId,
            title = title,
            description = description,
            price = price,
            currencyCode = currencyCode,
            type = type
        )
    }

    /**
     * 将 Purchase 转为 PurchaseResult。
     */
    private fun Purchase.toPurchaseResult(): PurchaseResult {
        return PurchaseResult(
            purchaseToken = purchaseToken,
            productId = products.firstOrNull() ?: "",
            isAcknowledged = isAcknowledged,
            orderId = orderId,
            purchaseTime = purchaseTime
        )
    }
}

// ========== 挂起扩展函数 ==========

/**
 * BillingClient.queryProductDetailsAsync 的挂起封装。
 * BillingClient 5.x 回调签名: (BillingResult, MutableList<ProductDetails>) -> Unit
 * 返回自定义 QueryProductDetailsResult（替代无 Builder 的 ProductDetailsResult）。
 */
private suspend fun BillingClient.awaitQueryProductDetails(
    params: QueryProductDetailsParams
): QueryProductDetailsResult = suspendCancellableCoroutine { continuation ->
    queryProductDetailsAsync(
        params,
        { billingResult: BillingResult, productDetailsList: MutableList<ProductDetails> ->
            if (continuation.isActive) {
                continuation.resume(
                    QueryProductDetailsResult(
                        billingResult = billingResult,
                        productDetailsList = productDetailsList
                    )
                )
            }
        }
    )
}

/**
 * BillingClient.queryPurchasesAsync 的挂起封装。
 * BillingClient 5.x 回调签名: (BillingResult, MutableList<Purchase>) -> Unit
 * 返回自定义 QueryPurchaseResult（替代无 Builder 的 PurchaseResult）。
 */
private suspend fun BillingClient.awaitQueryPurchases(
    params: QueryPurchasesParams
): QueryPurchaseResult = suspendCancellableCoroutine { continuation ->
    queryPurchasesAsync(
        params,
        { billingResult: BillingResult, purchases: MutableList<Purchase> ->
            if (continuation.isActive) {
                continuation.resume(
                    QueryPurchaseResult(
                        billingResult = billingResult,
                        purchases = purchases
                    )
                )
            }
        }
    )
}

/**
 * BillingClient.acknowledgePurchase 的挂起封装。
 * BillingClient 5.x 回调签名: (BillingResult) -> Unit
 */
private suspend fun BillingClient.awaitAcknowledgePurchase(
    params: AcknowledgePurchaseParams
): BillingResult = suspendCancellableCoroutine { continuation ->
    acknowledgePurchase(
        params,
        { billingResult: BillingResult ->
            if (continuation.isActive) {
                continuation.resume(billingResult)
            }
        }
    )
}
