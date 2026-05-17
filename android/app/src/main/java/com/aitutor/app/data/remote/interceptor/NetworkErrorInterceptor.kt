package com.aitutor.app.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Singleton

/**
 * P1-3: 全局网络请求错误拦截器。
 *
 * 统一处理 OkHttp 请求中的各类网络错误：
 * - HTTP 错误状态码（4xx / 5xx）
 * - 网络连接异常（超时、DNS 解析失败、无网络）
 *
 * 职责：
 * 1. 记录错误日志用于调试
 * 2. 对可恢复的错误静默处理
 * 3. 对不可恢复的错误添加标记（方便上层感知）
 *
 * 注意：
 * - 401 由 [AuthInterceptor] 处理（token 刷新），本拦截器仅记录未能恢复的 401
 * - 必须放在 AuthInterceptor **之后** 注册，以确保观察的是最终响应
 */
@Singleton
class NetworkErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        return try {
            val response = chain.proceed(request)
            handleHttpError(response, url)
        } catch (e: IOException) {
            handleNetworkError(e, chain, url)
        } catch (e: Exception) {
            handleUnexpectedError(e, chain, url)
        }
    }

    // ---------------------------------------------------------------
    // HTTP 错误状态码处理
    // ---------------------------------------------------------------

    private fun handleHttpError(response: Response, url: String): Response {
        val code = response.code
        var modifiedResponse = response

        when {
            // 2xx — 成功，无需处理
            code in 200..299 -> {
                logInfo("OK", code, url)
            }

            // 4xx — 客户端错误
            code in 400..499 -> {
                when (code) {
                    400 -> logWarn("Bad Request", code, url)
                    401 -> {
                        // AuthInterceptor 已尝试刷新 token，若仍为 401 则登录态失效
                        logError("Unauthorized — session expired, login required", code, url)
                        modifiedResponse = response.newBuilder()
                            .header("X-Auth-Expired", "true")
                            .build()
                    }
                    403 -> logError("Forbidden — insufficient permissions", code, url)
                    404 -> logWarn("Not Found", code, url)
                    408 -> logWarn("Request Timeout", code, url)
                    429 -> logWarn("Rate Limited", code, url)
                    else -> logWarn("Client Error", code, url)
                }
            }

            // 5xx — 服务端错误
            code in 500..599 -> {
                when (code) {
                    500 -> logError("Internal Server Error", code, url)
                    502 -> logError("Bad Gateway", code, url)
                    503 -> logError("Service Unavailable", code, url)
                    504 -> logError("Gateway Timeout", code, url)
                    else -> logError("Server Error", code, url)
                }

                // 标记响应以便上层感知服务端错误
                modifiedResponse = response.newBuilder()
                    .header("X-Server-Error", "true")
                    .build()
            }

            // 其他状态码
            else -> logWarn("Unexpected HTTP status", code, url)
        }

        return modifiedResponse
    }

    // ---------------------------------------------------------------
    // 网络连接异常处理
    // ---------------------------------------------------------------

    private fun handleNetworkError(
        e: IOException,
        chain: Interceptor.Chain,
        url: String
    ): Response {
        val errorType = when (e) {
            is SocketTimeoutException -> "TIMEOUT"
            is ConnectException -> "CONNECTION_REFUSED"
            is UnknownHostException -> "DNS_FAILURE"
            else -> "NETWORK_ERROR"
        }

        val message = when (e) {
            is SocketTimeoutException -> {
                "请求超时: ${e.message ?: "连接超时"}"
            }
            is ConnectException -> {
                "连接被拒绝: ${e.message ?: "无法连接到服务器"}"
            }
            is UnknownHostException -> {
                "DNS 解析失败: ${e.message ?: "无法解析服务器地址"}"
            }
            else -> {
                "网络错误: ${e.message ?: "未知网络错误"}"
            }
        }

        logError("$errorType — $message", -1, url)

        // 抛出包装后的异常，让上层统一捕获和处理
        // 这样 Retrofit 的 Call 会收到 NetworkErrorException
        throw NetworkErrorException(
            errorType = errorType,
            message = message,
            cause = e
        )
    }

    // ---------------------------------------------------------------
    // 非预期异常处理
    // ---------------------------------------------------------------

    private fun handleUnexpectedError(
        e: Exception,
        chain: Interceptor.Chain,
        url: String
    ): Response {
        logError("UNEXPECTED — ${e.javaClass.simpleName}: ${e.message}", -1, url)
        throw NetworkErrorException(
            errorType = "UNEXPECTED",
            message = "非预期错误: ${e.message ?: "未知错误"}",
            cause = e
        )
    }

    // ---------------------------------------------------------------
    // 日志工具
    // ---------------------------------------------------------------

    private fun logInfo(message: String, code: Int, url: String) {
        Log.i(TAG, "[$code] $message — ${truncateUrl(url)}")
    }

    private fun logWarn(message: String, code: Int, url: String) {
        Log.w(TAG, "[$code] $message — ${truncateUrl(url)}")
    }

    private fun logError(message: String, code: Int, url: String) {
        Log.e(TAG, "[$code] $message — ${truncateUrl(url)}")
    }

    /**
     * 截断过长 URL，保留路径和查询参数的前 120 字符。
     */
    private fun truncateUrl(url: String): String {
        return if (url.length > 120) url.take(120) + "…" else url
    }

    companion object {
        private const val TAG = "NetworkInterceptor"
    }
}

/**
 * 网络错误异常 — 由 [NetworkErrorInterceptor] 在发生网络连接异常时抛出。
 *
 * 上层可通过 [Throwable.cause] 获取原始 [IOException]。
 */
class NetworkErrorException(
    val errorType: String,
    override val message: String,
    cause: Throwable? = null
) : IOException(message, cause) {

    val isTimeout: Boolean get() = errorType == "TIMEOUT"
    val isConnectionFailed: Boolean get() = errorType == "CONNECTION_REFUSED"
    val isDnsFailure: Boolean get() = errorType == "DNS_FAILURE"
    val isServerError: Boolean get() = errorType == "SERVER_ERROR"

    override fun toString(): String {
        return "NetworkErrorException(errorType='$errorType', message='$message')"
    }
}
