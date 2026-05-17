package com.aitutor.app.ui.screen.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val message: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        // 观察订阅状态
        viewModelScope.launch {
            subscriptionRepository.getSubscriptionState().collect { state ->
                _uiState.update { it.copy(subscription = state) }
            }
        }
        // 初始刷新
        refreshStatus()
    }

    /** 从服务器刷新订阅状态 */
    fun refreshStatus() {
        viewModelScope.launch {
            subscriptionRepository.refreshStatus()
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
