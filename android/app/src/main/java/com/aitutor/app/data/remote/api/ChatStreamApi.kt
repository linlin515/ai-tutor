package com.aitutor.app.data.remote.api

import com.aitutor.app.data.remote.dto.ChatCompletionChunk
import com.aitutor.app.data.remote.dto.ChatCompletionRequest
import com.aitutor.app.data.remote.dto.ChatMessageDto
import com.aitutor.app.domain.model.StreamEvent
import com.aitutor.app.domain.model.ToolCallInfo
import com.google.gson.Gson
import com.google.gson.JsonParser
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

/**
 * SSE 流式对话 API，v2.0 支持 tool_calls 解析
 * 返回 Flow<StreamEvent> 而非原来的 Flow<String>
 */
class ChatStreamApi(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * 流式对话请求，返回 StreamEvent Flow
     * 包含 textChunk, toolCallChunk, roleChunk, done, errorEvent
     */
    fun streamChat(
        request: ChatCompletionRequest,
        token: String?
    ): Flow<StreamEvent> = callbackFlow {
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
            private var currentToolCalls = mutableMapOf<Int, ToolCallInfoBuilder>()

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done)
                    return
                }
                try {
                    val jsonTree = JsonParser.parseString(data).asJsonObject
                    val choices = jsonTree.getAsJsonArray("choices")
                    if (choices == null || choices.size() == 0) return

                    val choice = choices[0].asJsonObject
                    val delta = choice.getAsJsonObject("delta") ?: return

                    // 1. Parse role
                    val role = delta.get("role")?.asString
                    if (role != null) {
                        trySend(StreamEvent.RoleChunk(role))
                    }

                    // 2. Parse content
                    val content = delta.get("content")?.asString
                    if (content != null && content.isNotEmpty()) {
                        trySend(StreamEvent.TextChunk(content))
                    }

                    // 3. Parse tool_calls
                    val toolCallsArray = delta.getAsJsonArray("tool_calls")
                    if (toolCallsArray != null && toolCallsArray.size() > 0) {
                        val toolCalls = mutableListOf<ToolCallInfo>()

                        for (i in 0 until toolCallsArray.size()) {
                            val tc = toolCallsArray[i].asJsonObject
                            val index = tc.get("index")?.asInt ?: continue
                            val tcId = tc.get("id")?.asString
                            val tcType = tc.get("type")?.asString ?: "function"
                            val func = tc.getAsJsonObject("function")

                            var name = func?.get("name")?.asString
                            var arguments = func?.get("arguments")?.asString

                            // Accumulate streaming tool_calls by index
                            val builder = currentToolCalls.getOrPut(index) {
                                ToolCallInfoBuilder()
                            }
                            if (tcId != null) builder.id = tcId
                            if (name != null) builder.name = name
                            if (arguments != null) builder.arguments.append(arguments)

                            name = builder.name
                            arguments = builder.arguments.toString()

                            if (name != null && arguments != null && arguments.isNotEmpty()) {
                                toolCalls.add(ToolCallInfo(
                                    id = builder.id ?: "call_${index}",
                                    type = tcType,
                                    functionName = name,
                                    arguments = arguments
                                ))
                            }
                        }

                        if (toolCalls.isNotEmpty()) {
                            trySend(StreamEvent.ToolCallChunk(toolCalls))
                        }
                    }
                } catch (e: Exception) {
                    trySend(StreamEvent.ErrorEvent("Parse error: ${e.message}"))
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                trySend(StreamEvent.ErrorEvent(t?.message ?: "Connection failed"))
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

    /**
     * 流式对话请求（兼容 v1.0 接口，返回纯文本流）
     */
    fun streamChatText(
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

/**
 * 用于累积流式 tool_calls 的构建器
 */
private class ToolCallInfoBuilder {
    var id: String? = null
    var name: String? = null
    val arguments = StringBuilder()
}
