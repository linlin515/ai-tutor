package com.aitutor.app.domain.usecase.voice

import com.aitutor.app.domain.repository.VoiceRepository
import javax.inject.Inject

class StartListeningUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    fun isSupported(): Boolean {
        return voiceRepository.isListeningSupported()
    }
}
