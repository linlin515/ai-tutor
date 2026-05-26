package com.aitutor.app.ui.screen.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.billing.BillingConnectionState
import com.aitutor.app.data.billing.BillingManager
import com.aitutor.app.data.billing.BillingProduct
import com.aitutor.app.data.billing.PurchaseResult
import com.aitutor.app.data.billing.PurchaseState
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val subscription: SubscriptionState = SubscriptionState(),
    val showQuotaExceededDialog: Boolean = false,
    val exceededFeature: FeatureType? = null,
    val message: String? = null,

    // Billing 状态
    val connectionState: BillingConnectionState = BillingConnectionState.DISCONNECTED,
    val purchaseState: PurchaseState = PurchaseState.IDLE,
    val products: List<BillingProduct> = emptyList(),
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    /** 待购买的商品 ID，由 UI 层消费并触发 launchBillingFlow */
    private val _pendingPurchaseProductId = MutableStateFlow<String?>(null)
    val pendingPurchaseProductId: StateFlow<String?> = _pendingPurchaseProductId.asStateFlow()

    init {
        // 观察订阅状态
        viewModelScope.launch {
            subscriptionRepository.getSubscriptionState().collect { state ->
                _uiState.update { it.copy(subscription = state) }
            }
        }

        // 观察 BillingManager 连接状态
        viewModelScope.launch {
            billingManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // 观察 BillingManager 购买状态
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                _uiState.update {
                    it.copy(
                        purchaseState = state,
                        isPurchasing = state == PurchaseState.PURCHASING || state == PurchaseState.VERIFYING,
                        isRestoring = state == PurchaseState.RESTORING
                    )
                }
            }
        }

        // 观察商品列表
        viewModelScope.launch {
            billingManager.products.collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }

        // 观察购买结果，自动调 verify
        viewModelScope.launch {
            billingManager.lastPurchaseResult.collect { result ->
                if (result != null) {
                    // 购买成功 → 调后端 verify
                    verifyAfterPurchase(result)
                }
            }
        }

        // 初始连接 BillingClient + 查询商品
        connectAndQuery()
    }

    /** 连接 BillingClient 并查询商品 */
    fun connectAndQuery() {
        viewModelScope.launch {
            val connected = billingManager.connect()
            if (connected) {
                billingManager.queryProducts()
            } else {
                _uiState.update { it.copy(message = "支付服务连接失败，请检查 Google Play 服务") }
            }
        }
    }

    /** 从服务器刷新订阅状态 */
    fun refreshStatus() {
        viewModelScope.launch {
            subscriptionRepository.refreshStatus()
        }
    }

    /** 用户点击购买按钮 */
    fun purchase(productId: String) {
        _pendingPurchaseProductId.value = productId
    }

    /** UI 层调用此方法，传入 Activity 以启动 billing flow */
    fun onBillingFlowReady(activity: Activity) {
        val productId = _pendingPurchaseProductId.value ?: return
        _pendingPurchaseProductId.value = null
        billingManager.launchBillingFlow(activity, productId)
    }

    /** 恢复购买 */
    fun restorePurchases() {
        viewModelScope.launch {
            val result = subscriptionRepository.restorePurchases()
            result.onSuccess { purchases ->
                if (purchases.isEmpty()) {
                    _uiState.update { it.copy(message = "未找到可恢复的订阅") }
                } else {
                    _uiState.update { it.copy(message = "已恢复 ${purchases.size} 个订阅") }
                    refreshStatus()
                }
            }.onFailure { e ->
                _uiState.update { it.copy(message = "恢复购买失败: ${e.message}") }
            }
        }
    }

    /** 购买成功后调后端 verify */
    private suspend fun verifyAfterPurchase(purchaseResult: PurchaseResult) {
        _uiState.update { it.copy(purchaseState = PurchaseState.VERIFYING) }

        val result = subscriptionRepository.verifyPurchase(
            purchaseToken = purchaseResult.purchaseToken,
            productId = purchaseResult.productId
        )

        result.onSuccess {
            _uiState.update {
                it.copy(
                    message = "订阅成功！",
                    purchaseState = PurchaseState.VERIFIED
                )
            }
            refreshStatus()
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    message = "订阅验证失败，请联系客服: ${e.message}",
                    purchaseState = PurchaseState.FAILED
                )
            }
        }
    }

    /** 在执行需要配额的 action 前调用 — 检查通过则返回 true */
    suspend fun checkQuotaBeforeAction(feature: FeatureType): Boolean {
        val result = subscriptionRepository.checkQuota(feature)
        return when (result) {
            is QuotaResult.Allowed -> true
            is QuotaResult.Exceeded -> {
                _uiState.update {
                    it.copy(
                        showQuotaExceededDialog = true,
                        exceededFeature = feature
                    )
                }
                false
            }
            is QuotaResult.Disabled -> {
                _uiState.update {
                    it.copy(message = result.message)
                }
                false
            }
        }
    }

    /** 在 action 成功后调用，消耗一次配额 */
    fun consumeQuota(feature: FeatureType) {
        viewModelScope.launch {
            subscriptionRepository.consumeQuota(feature)
                .onFailure { e ->
                    _uiState.update { it.copy(message = "配额更新失败: ${e.message}") }
                }
        }
    }

    fun dismissQuotaExceededDialog() {
        _uiState.update { it.copy(showQuotaExceededDialog = false, exceededFeature = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
