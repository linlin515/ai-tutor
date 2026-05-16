package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.stream.SolveStreamParser
import com.aitutor.app.domain.model.SolveEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SSE-based solve photo streaming API.
 *
 * Sends a multipart POST to [baseUrl]v1/solve/photo with the image bytes,
 * subject and optional grade, then parses the SSE event stream into
 * [SolveEvent] domain objects.
 */
class SolveApi(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    private val parser = SolveStreamParser()
    private val jpegMediaType = "image/jpeg".toMediaType()

    /**
     * Upload a photo and stream the solve result as [SolveEvent]s.
     *
     * @param imageBytes JPEG-compressed image bytes
     * @param subject    subject hint ("math", "physics", "chemistry", "biology", "chinese", "english", "auto")
     * @param grade      optional student grade for contextualised explanations
     */
    fun streamSolve(
        imageBytes: ByteArray,
        subject: String = "auto",
        grade: String? = null
    ): Flow<SolveEvent> = callbackFlow {
        // --- Build multipart body ---
        val imageBody = imageBytes.toRequestBody(jpegMediaType)
        val imagePart = MultipartBody.Part.createFormData("photo", "photo.jpg", imageBody)

        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(imagePart)
            .addFormDataPart("subject", subject)

        if (grade != null) {
            multipartBuilder.addFormDataPart("grade", grade)
        }

        val requestBody = multipartBuilder.build()
        val url = "${baseUrl}v1/solve/photo"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val call = okHttpClient.newCall(request)
        var response: Response? = null

        try {
            response = call.execute()

            if (!response.isSuccessful) {
                trySend(
                    SolveEvent.SolveError(
                        code = "HTTP_${response.code}",
                        message = "服务器返回错误 (${response.code})"
                    )
                )
                return@callbackFlow
            }

            val body = response.body ?: run {
                trySend(SolveEvent.SolveError("EMPTY_BODY", "响应体为空"))
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var currentEvent: String? = null
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val current = line ?: continue
                when {
                    current.startsWith("event: ") -> {
                        currentEvent = current.removePrefix("event: ").trim()
                    }
                    current.startsWith("data: ") -> {
                        val payload = current.removePrefix("data: ").trim()
                        if (payload.isNotEmpty() && currentEvent != null) {
                            val event = parser.parseEvent(currentEvent!!, payload)
                            if (event != null) {
                                trySend(event)
                            }
                            currentEvent = null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            trySend(SolveEvent.SolveError("STREAM_ERROR", e.message ?: "流读取失败"))
        } finally {
            response?.close()
            close()
        }

        awaitClose {
            call.cancel()
        }
    }.buffer(Channel.BUFFERED)
}
