package com.aitutor.app.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Simple ambient-noise-level detector (F47 — optional voice enhancement).
 *
 * Samples the microphone briefly (≈5 frames) and classifies the environment
 * into one of three levels so the app can adapt audio-processing strategies.
 */
class NoiseSuppression(private val context: Context? = null) {

    enum class NoiseLevel {
        QUIET,
        MODERATE,
        NOISY
    }

    /**
     * Detect the current ambient noise level.
     *
     * Returns [NoiseLevel.QUIET] when the RECORD_AUDIO permission is missing
     * or any recording error occurs, so the app degrades gracefully.
     */
    fun detectNoiseLevel(): NoiseLevel {
        // Permission guard
        if (context != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return NoiseLevel.QUIET
        }

        return try {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize <= 0) return NoiseLevel.QUIET

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return NoiseLevel.QUIET
            }

            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()

            var maxAmplitude = 0
            // Collect 5 short frames to estimate ambient level
            repeat(SAMPLE_FRAMES) {
                val bytesRead = audioRecord.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    var frameMax = 0
                    for (i in 0 until bytesRead.coerceAtMost(buffer.size)) {
                        val amp = abs(buffer[i].toInt())
                        if (amp > frameMax) frameMax = amp
                    }
                    if (frameMax > maxAmplitude) maxAmplitude = frameMax
                }
            }

            audioRecord.stop()
            audioRecord.release()

            when {
                maxAmplitude < QUIET_THRESHOLD -> NoiseLevel.QUIET
                maxAmplitude < NOISY_THRESHOLD -> NoiseLevel.MODERATE
                else -> NoiseLevel.NOISY
            }
        } catch (_: Exception) {
            // Graceful fallback on any platform / hardware failure
            NoiseLevel.QUIET
        }
    }

    companion object {
        private const val SAMPLE_FRAMES = 5
        private const val QUIET_THRESHOLD = 3000
        private const val NOISY_THRESHOLD = 15000
    }
}
