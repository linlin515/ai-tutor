package com.aitutor.app.data.media

/**
 * ASR 降级判定逻辑 (F16)
 *
 * 纯 Kotlin 实现，无 Android 依赖。
 * 根据本地识别置信度、音频时长、环境噪声判定是否降级到云端 ASR。
 *
 * 决策链:
 * - 音频 >30s → 直接使用云端
 * - 本地置信度 < 0.6 → 降级到云端备选
 * - 置信度足够且音频正常 → 使用本地结果
 */
object AsrFallbackStrategy {

    /**
     * 判定是否应该降级到云端 ASR
     *
     * @param localConfidence 本地 ASR 识别置信度 (0.0 ~ 1.0)
     * @param audioDurationMs 音频时长 (毫秒)
     * @param noiseDb 环境噪声分贝值 (可选)
     * @param config 降级配置参数
     * @return FallbackDecision USE_LOCAL / FALLBACK / SKIP
     */
    fun shouldFallback(
        localConfidence: Float,
        audioDurationMs: Long,
        noiseDb: Float = 0f,
        config: AsrConfig = AsrConfig()
    ): FallbackDecision {
        // 音频太短，跳过识别
        if (audioDurationMs < 500L) {
            return FallbackDecision.SKIP
        }

        // 长音频（>30s）→ 直接走云端
        if (audioDurationMs > config.durationThresholdMs) {
            return FallbackDecision.FALLBACK
        }

        // 本地置信度过低 → 降级到云端
        if (localConfidence < config.confidenceThreshold) {
            return FallbackDecision.FALLBACK
        }

        // 高噪声环境 → 降级到云端
        if (noiseDb > config.noiseThresholdDb) {
            return FallbackDecision.FALLBACK
        }

        // 正常情况 → 使用本地结果
        return FallbackDecision.USE_LOCAL
    }

    /**
     * 两段式策略：先试本地（低延迟），失败/低分后触发云端备选
     *
     * @param localResult 本地 ASR 结果
     * @param audioDurationMs 音频时长
     * @param noiseDb 环境噪声
     * @return 是否需要云端备选
     */
    fun needsCloudFallback(
        localResult: AsrResult,
        audioDurationMs: Long,
        noiseDb: Float = 0f
    ): Boolean {
        return shouldFallback(
            localConfidence = localResult.confidence,
            audioDurationMs = audioDurationMs,
            noiseDb = noiseDb
        ) == FallbackDecision.FALLBACK
    }
}
