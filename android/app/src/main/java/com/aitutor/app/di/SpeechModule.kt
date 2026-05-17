package com.aitutor.app.di

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun provideSpeechRecognizer(
        @ApplicationContext context: Context
    ): SpeechRecognizer {
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideTextToSpeech(
        @ApplicationContext context: Context
    ): TextToSpeech {
        val tts = TextToSpeech(context) { status ->
            // onInit callback - status check can be added if needed
        }
        tts.language = Locale.CHINESE
        tts.setSpeechRate(1.0f)
        return tts
    }
}
