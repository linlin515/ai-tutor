package com.aitutor.app.domain.model

/**
 * 功能类型枚举，用于配额检查和消耗跟踪。
 */
enum class FeatureType(val key: String, val displayName: String) {
    CHAT("chat", "AI 对话"),
    SOLVE_PHOTO("solve_photo", "拍照解题"),
    VOICE_ASR_CLOUD("voice_asr_cloud", "语音识别"),
    VOICE_TTS_CLOUD("voice_tts_cloud", "语音合成"),
    DASHBOARD("dashboard", "学习报告"),
    QUIZ("quiz", "智能测验"),
    REVIEW("review", "错题复习"),
    GAMIFICATION("gamification", "游戏化学习");

    companion object {
        fun fromKey(key: String): FeatureType? = entries.find { it.key == key }
    }
}
