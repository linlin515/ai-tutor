package com.aitutor.app.data.repository

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.aitutor.app.data.media.AsrConfig
import com.aitutor.app.data.media.AsrFallbackStrategy
import com.aitutor.app.data.media.AsrResult
import com.aitutor.app.data.media.CloudAsrEngine
import com.aitutor.app.data.media.CloudTtsEngine
import com.aitutor.app.data.media.FallbackDecision
import com.aitutor.app.data.media.PlaybackState
import com.aitutor.app.data.media.TtsAudioPlayer
import com.aitutor.app.data.media.TtsRequest
import com.aitutor.app.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRecognizer: SpeechRecognizer,
    private val textToSpeech: TextToSpeech,
    private val cloudAsrEngine: CloudAsrEngine,
    private val cloudTtsEngine: CloudTtsEngine,
    private val ttsAudioPlayer: TtsAudioPlayer
) : VoiceRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val asrConfig = AsrConfig()

    // === 本地语音功能 ===

    override fun isListeningSupported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun speak(text: String, speed: Float) {
        // 长文本（>500 字）→ 使用云端 TTS
        if (text.length > 500) {
            scope.launch {
                speakCloud(text, speed)
            }
            return
        }
        // 短文本 → 使用本地 TTS
        textToSpeech.setSpeechRate(speed)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun stopSpeaking() {
        // 同时停止本地和云端 TTS
        textToSpeech.stop()
        ttsAudioPlayer.stop()
    }

    override fun isSpeaking(): Boolean {
        return textToSpeech.isSpeaking || ttsAudioPlayer.playbackState.value == PlaybackState.PLAYING
    }

    // === 云端 ASR 备选 (F16) ===

    /**
     * 启动云端 ASR，配合降级策略
     *
     * 决策链：
     * 1. 本地 ASR 识别
     * 2. 判断是否需要降级（时长 >30s 或置信度 <0.6）
     * 3. 如果需要降级 → 调用云端 ASR
     * 4. 返回最终结果
     */
    override suspend fun startCloudListening(audioFile: File): String {
        // TODO: 实际项目中应先运行本地 ASR 获取置信度
        // 此处直接调用云端 ASR

        val result = cloudAsrEngine.recognize(audioFile, asrConfig).first()
        return result.text
    }

    override fun isCloudAsrAvailable(): Boolean {
        return cloudAsrEngine.isCloudAvailable()
    }

    /**
     * 执行带降级策略的完整识别流程
     *
     * @param audioFile 录音文件
     * @param localConfidence 本地 ASR 置信度
     * @param audioDurationMs 音频时长
     * @return 最终识别结果
     */
    suspend fun recognizeWithFallback(
        audioFile: File,
        localConfidence: Float,
        audioDurationMs: Long
    ): AsrResult {
        val decision = AsrFallbackStrategy.shouldFallback(
            localConfidence = localConfidence,
            audioDurationMs = audioDurationMs,
            config = asrConfig
        )

        return when (decision) {
            FallbackDecision.USE_LOCAL -> {
                // 使用本地 ASR 结果（外部传入的 localConfidence）
                AsrResult(
                    text = "", // 本地文本由上层提供
                    confidence = localConfidence,
                    isFromCloud = false,
                    durationMs = audioDurationMs
                )
            }
            FallbackDecision.FALLBACK -> {
                // 降级到云端 ASR
                try {
                    cloudAsrEngine.recognize(audioFile, asrConfig).first()
                } catch (e: Exception) {
                    AsrResult(
                        text = "",
                        confidence = 0f,
                        isFromCloud = true,
                        durationMs = audioDurationMs
                    )
                }
            }
            FallbackDecision.SKIP -> {
                AsrResult(
                    text = "",
                    confidence = 0f,
                    isFromCloud = false,
                    durationMs = 0L
                )
            }
        }
    }

    // === 云端 TTS (F18) ===

    override suspend fun speakCloud(text: String, speed: Float) {
        // 打断当前播放
        interruptTts()

        val request = TtsRequest(
            text = text,
            speed = speed
        )

        try {
            cloudTtsEngine.streamTts(request).collect { chunk ->
                ttsAudioPlayer.enqueueChunk(chunk.audioBytes)
            }
        } catch (e: Exception) {
            // TTS 流式播放失败，可尝试本地 TTS 兜底
            if (text.length <= 500) {
                textToSpeech.setSpeechRate(speed)
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    // === 打断协调 (F20) ===

    override fun interruptTts() {
        // 打断流程（精确到毫秒）
        // 1. TtsAudioPlayer.stop() — 立即停止播放
        // 2. AudioTrack.pause() + flush() — 清空缓冲区
        // 3. 停止本地 TTS
        ttsAudioPlayer.fadeOut()  // 防爆音
        ttsAudioPlayer.stop()     // <10ms: stop + flush
        textToSpeech.stop()
    }

    override fun getTtsPlaybackState(): PlaybackState {
        return ttsAudioPlayer.playbackState.value
    }

    /**
     * 全局释放资源
     */
    fun release() {
        ttsAudioPlayer.release()
        scope.cancel()
    }
}
