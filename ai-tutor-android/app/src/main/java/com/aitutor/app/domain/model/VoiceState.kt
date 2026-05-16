package com.aitutor.app.domain.model

enum class VoiceStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

data class VoiceState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val recognizedText: String = "",
    val errorMessage: String? = null
)
