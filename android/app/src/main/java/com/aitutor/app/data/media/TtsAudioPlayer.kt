package com.aitutor.app.data.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS 音频播放器——使用 AudioTrack 播放 PCM 音频块 (F18/F20)
 *
 * 支持：
 * - 逐块追加 PCM 数据并播放
 * - 立即停止并清空缓冲区
 * - 播放状态监听
 * - 打断（stop + flush）延迟 <10ms
 */
enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED
}

@Singleton
class TtsAudioPlayer @Inject constructor() {

    companion object {
        private const val TAG = "TtsAudioPlayer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private val audioBuffer = ConcurrentLinkedQueue<ByteArray>()
    private val isPlaying = AtomicBoolean(false)
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 计算合适的 AudioTrack 缓冲区大小
     */
    private val minBufferSize: Int by lazy {
        val size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // 确保缓冲区足够大，避免播放卡顿
        if (size > 0) size * BUFFER_SIZE_MULTIPLIER else 4096 * BUFFER_SIZE_MULTIPLIER
    }

    /**
     * 初始化并创建 AudioTrack 实例
     */
    private fun ensureAudioTrack(): AudioTrack {
        if (audioTrack == null || audioTrack!!.state == AudioTrack.STATE_UNINITIALIZED) {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build().hashCode(),
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
                    minBufferSize, AudioTrack.MODE_STREAM
                )
            }
        }
        return audioTrack!!
    }

    /**
     * 追加 PCM 数据到播放缓冲区
     */
    fun enqueueChunk(chunk: ByteArray) {
        audioBuffer.offer(chunk)
        if (_playbackState.value == PlaybackState.IDLE ||
            _playbackState.value == PlaybackState.STOPPED
        ) {
            startPlayback()
        } else if (_playbackState.value == PlaybackState.BUFFERING) {
            // 如果有数据进来了，切换为播放状态
            _playbackState.value = PlaybackState.PLAYING
        }
    }

    /**
     * 开始播放
     */
    fun play() {
        if (_playbackState.value == PlaybackState.PAUSED) {
            audioTrack?.play()
            _playbackState.value = PlaybackState.PLAYING
            return
        }
        if (audioBuffer.isNotEmpty()) {
            startPlayback()
        } else {
            _playbackState.value = PlaybackState.BUFFERING
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        audioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                it.pause()
            }
        }
        _playbackState.value = PlaybackState.PAUSED
    }

    /**
     * 立即停止播放 + 清空缓冲区 (用于打断)
     *
     * 打断延迟 <10ms:
     * 1. AudioTrack.stop() — 立即停止
     * 2. 清空缓冲区
     * 3. 状态切换
     */
    fun stop() {
        playJob?.cancel()
        playJob = null
        isPlaying.set(false)

        audioTrack?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.pause()
                }
                it.flush()
                it.stop()
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack stop error: ${e.message}")
            }
        }

        audioBuffer.clear()
        _playbackState.value = PlaybackState.STOPPED
    }

    /**
     * 清空未播放的缓冲区（不停止当前播放）
     */
    fun flush() {
        audioBuffer.clear()
    }

    /**
     * 释放 AudioTrack 资源
     */
    fun release() {
        stop()
        audioTrack?.let {
            try {
                it.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack release error: ${e.message}")
            }
        }
        audioTrack = null
        _playbackState.value = PlaybackState.IDLE
    }

    /**
     * 快速 fade-out (10ms) 用于防止打断时爆音
     */
    fun fadeOut() {
        audioTrack?.let { track ->
            try {
                val fadeSamples = (SAMPLE_RATE * 0.01).toInt() // 10ms
                val fadeBuffer = ShortArray(fadeSamples)
                for (i in fadeBuffer.indices) {
                    fadeBuffer[i] = (Short.MAX_VALUE * (1.0 - i.toFloat() / fadeSamples)).toInt().toShort()
                }
                val byteBuffer = ByteArray(fadeSamples * 2)
                for (i in fadeBuffer.indices) {
                    byteBuffer[i * 2] = (fadeBuffer[i].toInt() and 0xFF).toByte()
                    byteBuffer[i * 2 + 1] = (fadeBuffer[i].toInt() shr 8 and 0xFF).toByte()
                }
                track.write(byteBuffer, 0, byteBuffer.size)
            } catch (e: Exception) {
                // fade-out 失败不影响主流程
            }
        }
    }

    /**
     * 后台循环从缓冲区读取数据并写入 AudioTrack
     */
    private fun startPlayback() {
        if (isPlaying.getAndSet(true)) return

        _playbackState.value = PlaybackState.PLAYING

        playJob = scope.launch(Dispatchers.IO) {
            val track = ensureAudioTrack()
            try {
                track.play()

                while (isActive && isPlaying.get()) {
                    val chunk = audioBuffer.poll() ?: break
                    track.write(chunk, 0, chunk.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback error: ${e.message}")
            } finally {
                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                } catch (e: Exception) {
                    // ignore
                }
                isPlaying.set(false)
                if (audioBuffer.isEmpty()) {
                    _playbackState.value = PlaybackState.IDLE
                } else {
                    // 还有数据，继续播放
                    isPlaying.set(false)
                    startPlayback()
                }
            }
        }
    }
}
