package com.aitutor.app.domain.repository

interface VoiceRepository {
    fun isListeningSupported(): Boolean
    fun speak(text: String, speed: Float)
    fun stopSpeaking()
    fun isSpeaking(): Boolean
}
