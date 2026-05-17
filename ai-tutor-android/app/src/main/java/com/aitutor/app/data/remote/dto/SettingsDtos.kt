package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SettingsDto(
    @SerializedName("model_id") val modelId: String,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("top_p") val topP: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("theme_mode") val themeMode: String,
    @SerializedName("tts_speed") val ttsSpeed: Float,
    @SerializedName("tts_voice") val ttsVoice: String
)
