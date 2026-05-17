package com.aitutor.app.domain.usecase.voice

import com.aitutor.app.domain.repository.VoiceRepository
import javax.inject.Inject

class SpeakTextUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    operator fun invoke(text: String, speed: Float = 1.0f) {
        voiceRepository.speak(text, speed)
    }

    fun stop() {
        voiceRepository.stopSpeaking()
    }

    fun isSpeaking(): Boolean {
        return voiceRepository.isSpeaking()
    }
}
