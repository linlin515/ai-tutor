package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ChatCompletionChunk
import com.aitutor.app.data.remote.dto.ChatCompletionRequest
import com.aitutor.app.data.remote.dto.ChatMessageDto
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class ChatStreamApi(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    fun streamChat(
        request: ChatCompletionRequest,
        token: String?
    ): Flow<String> = callbackFlow {
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val url = "${baseUrl}v1/chat/completions"
        val httpRequestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")

        if (token != null) {
            httpRequestBuilder.header("Authorization", "Bearer $token")
        }

        val httpRequest = httpRequestBuilder.build()

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(httpRequest, object : EventSourceListener() {
            private var accumulatedContent = StringBuilder()

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    trySend(accumulatedContent.toString())
                    accumulatedContent = StringBuilder()
                    return
                }
                try {
                    val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                    val content = chunk?.choices?.firstOrNull()?.delta?.content
                    if (content != null) {
                        accumulatedContent.append(content)
                        trySend(content)
                    }
                } catch (e: Exception) {
                    // Skip malformed chunks
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }.buffer(Channel.BUFFERED)
}
