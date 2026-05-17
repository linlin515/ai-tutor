package com.aitutor.app.domain.usecase

import com.aitutor.app.domain.model.AppUpdateInfo

/**
 * 更新检查结果
 */
sealed class CheckResult {
    /**
     * 有新版本可用
     */
    data class UpdateAvailable(val info: AppUpdateInfo) : CheckResult()

    /**
     * 当前已是最新版本
     */
    data object NoUpdate : CheckResult()

    /**
     * 检查失败
     */
    data class Error(val message: String) : CheckResult()
}
