package com.aitutor.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

object AudioUtils {

    fun playAudioFile(context: Context, filePath: String, onComplete: (() -> Unit)? = null): MediaPlayer? {
        return try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                setOnCompletionListener {
                    onComplete?.invoke()
                    release()
                }
                setOnErrorListener { _, _, _ ->
                    onComplete?.invoke()
                    release()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun playAudioUri(context: Context, uri: Uri, onComplete: (() -> Unit)? = null): MediaPlayer? {
        return try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener {
                    onComplete?.invoke()
                    release()
                }
                setOnErrorListener { _, _, _ ->
                    onComplete?.invoke()
                    release()
                    true
                }
                prepareAsync()
                setOnPreparedListener { start() }
            }
            mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMaxAmplitude(): Int {
        // Placeholder for audio level detection during recording
        return 0
    }
}
