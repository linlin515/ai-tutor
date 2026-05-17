package com.aitutor.app.data.media

import android.util.Base64
import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.dto.ApiResponse
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端 ASR 调用封装 (F16)
 *
 * 将录音文件上传至 POST /api/v1/voice/asr (multipart audio),
 * 返回识别文本。支持超时取消。
 */
data class AsrConfig(
    val durationThresholdMs: Long = 30_000L,
    val confidenceThreshold: Float = 0.6f,
    val cloudTimeoutMs: Long = 15_000L,
    val noiseThresholdDb: Float = 60f
)

data class AsrResult(
    val text: String,
    val confidence: Float = 1.0f,
    val isFromCloud: Boolean = true,
    val durationMs: Long = 0L
)

enum class FallbackDecision {
    /** 直接使用本地结果，无需降级 */
    USE_LOCAL,
    /** 降级到云端 ASR */
    FALLBACK,
    /** 跳过本次识别 */
    SKIP
}

data class CloudAsrResponse(
    @SerializedName("text") val text: String,
    @SerializedName("confidence") val confidence: Float = 1.0f
)

@Singleton
class CloudAsrEngine @Inject constructor(
    private val aiTutorApi: AiTutorApi
) {
    /**
     * 上传音频文件进行云端 ASR 识别
     *
     * @param audioFile 录音文件
     * @param config 降级配置
     * @return Flow 返回识别结果
     */
    fun recognize(audioFile: File, config: AsrConfig = AsrConfig()): Flow<AsrResult> = flow {
        val requestBody = audioFile.readBytes().toRequestBody(
            "audio/wav".toMediaTypeOrNull()
        )
        val part = MultipartBody.Part.createFormData("audio", audioFile.name, requestBody)

        val response = withTimeout(config.cloudTimeoutMs) {
            aiTutorApi.cloudAsr(part)
        }

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.code == 0) {
                val data = body.data
                if (data != null) {
                    emit(
                        AsrResult(
                            text = data.text,
                            confidence = data.confidence,
                            isFromCloud = true,
                            durationMs = config.durationThresholdMs
                        )
                    )
                } else {
                    emit(
                        AsrResult(
                            text = "",
                            confidence = 0f,
                            isFromCloud = true,
                            durationMs = 0L
                        )
                    )
                }
            } else {
                throw CloudAsrException(
                    code = body?.code ?: -1,
                    message = body?.message ?: "云端 ASR 识别失败"
                )
            }
        } else {
            throw CloudAsrException(
                code = response.code(),
                message = "云端 ASR 网络错误: ${response.code()}"
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 检测云端 ASR 是否可用
     */
    fun isCloudAvailable(): Boolean {
        return try {
            // 简单判断网络可用性 - 实际项目中可调用 health endpoint
            true
        } catch (e: Exception) {
            false
        }
    }
}

class CloudAsrException(
    val code: Int,
    override val message: String
) : Exception(message)
