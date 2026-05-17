package com.aitutor.app.domain.repository

import com.aitutor.app.data.media.AsrResult
import java.io.File

interface VoiceRepository {
    // === 本地语音功能 ===
    fun isListeningSupported(): Boolean
    fun speak(text: String, speed: Float)
    fun stopSpeaking()
    fun isSpeaking(): Boolean

    // === 云端 ASR 备选 (F16) ===
    /**
     * 启动云端 ASR 识别
     * @param audioFile 录音文件
     * @return 识别文本
     */
    suspend fun startCloudListening(audioFile: File): String

    /**
     * 检测云端 ASR 是否可用
     */
    fun isCloudAsrAvailable(): Boolean

    // === 云端 TTS (F18) ===
    /**
     * 使用云端 TTS 朗读文本
     * @param text 朗读文本
     * @param speed 语速 (0.5~2.0)
     */
    suspend fun speakCloud(text: String, speed: Float)

    // === 打断协调 (F20) ===
    /**
     * 打断当前 TTS 播放并切换状态
     */
    fun interruptTts()

    /**
     * 获取 TTS 播放状态
     */
    fun getTtsPlaybackState(): com.aitutor.app.data.media.PlaybackState
}
