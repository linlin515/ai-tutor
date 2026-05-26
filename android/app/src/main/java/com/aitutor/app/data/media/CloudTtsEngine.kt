package com.aitutor.app.data.media

import android.util.Base64
import com.aitutor.app.data.remote.interceptor.TokenManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端 TTS 流式播放数据类 (F18)
 */
data class TtsRequest(
    val text: String,
    val voice: String = "zh-CN-XiaoxiaoNeural",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val format: String = "pcm_16000hz_mono"
)

data class TtsChunk(
    val sequence: Int,
    val audioBytes: ByteArray,
    val durationMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TtsChunk
        if (sequence != other.sequence) return false
        if (durationMs != other.durationMs) return false
        if (!audioBytes.contentEquals(other.audioBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sequence
        result = 31 * result + audioBytes.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

/**
 * SSE audio_chunk 事件数据模型
 */
data class AudioChunkEvent(
    @SerializedName("sequence") val sequence: Int = 0,
    @SerializedName("audio_base64") val audioBase64: String = "",
    @SerializedName("duration_ms") val durationMs: Long = 0L
)

data class TtsCompleteEvent(
    @SerializedName("total_chunks") val totalChunks: Int = 0,
    @SerializedName("total_duration_ms") val totalDurationMs: Long = 0L
)

data class TtsErrorEvent(
    @SerializedName("code") val code: String = "",
    @SerializedName("message") val message: String = ""
)

/**
 * 云端 TTS 流式播放封装 (F18)
 *
 * 通过 SSE 接收 POST /api/v1/voice/tts 返回的 audio_chunk 事件，
 * 逐块解码为 PCM 字节流并发射。
 */
@Singleton
class CloudTtsEngine @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    private val baseUrl = "http://34.92.238.135:5100/"

    /**
     * 流式接收云端 TTS 音频块
     *
     * @param request TTS 请求参数
     * @return Flow 逐一发射解码后的音频块
     */
    fun streamTts(request: TtsRequest): Flow<TtsChunk> = callbackFlow {
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val url = "${baseUrl}api/v1/voice/tts"
        val token = tokenManager.getToken()

        val httpRequestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")

        if (token != null) {
            httpRequestBuilder.header("Authorization", "Bearer $token")
        }

        val httpRequest = httpRequestBuilder.build()

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(httpRequest, object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    when (type) {
                        "audio_chunk" -> {
                            val chunk = gson.fromJson(data, AudioChunkEvent::class.java)
                            val audioBytes = Base64.decode(chunk.audioBase64, Base64.DEFAULT)
                            trySend(
                                TtsChunk(
                                    sequence = chunk.sequence,
                                    audioBytes = audioBytes,
                                    durationMs = chunk.durationMs
                                )
                            )
                        }
                        "complete" -> {
                            // 流结束，无需额外操作，onClosed 会触发 close
                        }
                        "error" -> {
                            val err = gson.fromJson(data, TtsErrorEvent::class.java)
                            close(
                                CloudTtsException(
                                    code = err.code,
                                    message = err.message
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // 解析异常，跳过该 chunk
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                close(t ?: CloudTtsException("NETWORK_ERROR", "TTS 流式连接失败"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)
}

class CloudTtsException(
    val code: String,
    override val message: String
) : Exception(message)
