package com.aitutor.app.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.aitutor.app.domain.model.AppUpdateInfo
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用内更新检测
 *
 * 检查当前版本并对比后端 API 返回的最新版本，判断是否有更新可用。
 *
 * 后端 API 端点: GET /api/v1/app/version
 * 响应格式:
 * ```json
 * {
 *   "version_code": 21,
 *   "version_name": "2.1.0",
 *   "download_url": "https://aitutor.googlecloud.ccwu.cc/download.apk",
 *   "release_notes": "新增崩溃监控、应用内更新、安全加固",
 *   "force_update": false
 * }
 * ```
 *
 * 带有 5 分钟冷却期防止重复请求。
 */
@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val VERSION_API_URL =
            "http://34.92.238.135:5100/api/v1/app/version"
        private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 分钟冷却期
    }

    private var lastCheckTime: Long = 0
    private var lastResult: CheckResult? = null

    /**
     * 检查更新
     *
     * @return [CheckResult] 检查结果
     */
    fun checkForUpdate(): CheckResult {
        // 冷却期检查
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < COOLDOWN_MS && lastResult != null) {
            return lastResult!!
        }
        lastCheckTime = now

        return try {
            // 获取本地版本
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
            val localVersionCode = try {
                androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)
            } catch (e: Exception) { 1L }
            val localVersionName = packageInfo.versionName ?: "1.0.0"

            // 请求远程版本 API
            val request = Request.Builder()
                .url(VERSION_API_URL)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val result = CheckResult.Error(
                    "检查更新失败，服务器返回 ${response.code}"
                )
                lastResult = result
                return result
            }

            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                val result = CheckResult.Error("检查更新失败，响应为空")
                lastResult = result
                return result
            }

            val gson = Gson()
            val updateInfo = gson.fromJson(body, AppUpdateInfo::class.java)

            // 对比版本号
            if (updateInfo.versionCode > localVersionCode) {
                val result = CheckResult.UpdateAvailable(updateInfo)
                lastResult = result
                result
            } else {
                val result = CheckResult.NoUpdate
                lastResult = result
                result
            }
        } catch (e: Exception) {
            val result = CheckResult.Error("检查更新失败: ${e.message}")
            lastResult = result
            result
        }
    }

    /**
     * 重置冷却期，允许立即重试
     */
    fun resetCooldown() {
        lastCheckTime = 0
        lastResult = null
    }
}
