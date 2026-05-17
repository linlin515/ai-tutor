package com.aitutor.app.data.repository

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.aitutor.app.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRecognizer: SpeechRecognizer,
    private val textToSpeech: TextToSpeech
) : VoiceRepository {

    override fun isListeningSupported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun speak(text: String, speed: Float) {
        textToSpeech.setSpeechRate(speed)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun stopSpeaking() {
        textToSpeech.stop()
    }

    override fun isSpeaking(): Boolean {
        return textToSpeech.isSpeaking
    }
}
