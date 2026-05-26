package com.aitutor.app.data.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTtsEngine @Inject constructor(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var initialized = false
    
    fun speak(text: String): Flow<Unit> = callbackFlow {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                tts?.language = Locale.CHINESE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(uttId: String?) {}
                    override fun onDone(uttId: String?) { trySend(Unit) }
                    override fun onError(uttId: String?) {}
                })
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt")
            }
        }
        awaitClose { tts?.stop(); tts?.shutdown() }
    }
    
    fun stop() { tts?.stop() }
    fun destroy() { tts?.stop(); tts?.shutdown() }
}
