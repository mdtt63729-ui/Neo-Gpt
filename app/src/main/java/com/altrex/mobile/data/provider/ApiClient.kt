package com.altrex.mobile.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class StreamDelta(
    val content: String? = null,
    val done: Boolean = false,
    val toolCalls: List<JsonObject> = emptyList(),
    val error: String? = null
)

class ApiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val adapter: ProviderAdapter,
    private val timeoutMs: Long = 600_000
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun listModels(): Result<List<String>> = try {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/models")
            .headers(adapter.authHeaders(apiKey).toHeaders())
            .get()
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            Result.failure(ProviderException(
                status = response.code,
                body = body,
                message = "Failed to list models: ${response.code}"
            ))
        } else {
            val body = response.body?.string() ?: "{}"
            val jsonBody = json.parseToJsonElement(body).jsonObject
            val models = jsonBody["data"]?.jsonArray
                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                ?: emptyList()
            Result.success(models.sorted())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun stream(
        model: String,
        messages: List<JsonObject>,
        tools: List<JsonObject>? = null,
        maxTokens: Int = 2048
    ): Flow<StreamDelta> = callbackFlow {
        val body = adapter.buildBody(model, messages, tools, maxTokens, stream = true)
        val requestBody = json.encodeToString(JsonObject.serializer(), body)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .headers(adapter.authHeaders(apiKey).toHeaders())
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        val response = call.execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            val category = ProviderErrors.classifyHttpError(response.code, errorBody)
            trySend(StreamDelta(error = ProviderErrors.describeError(category)))
            channel.close()
            return@callbackFlow
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var line: String?
        val buffer = StringBuilder()

        while (reader.readLine().also { line = it } != null) {
            if (line!!.startsWith("data: ")) {
                val data = line!!.substring(6).trim()
                if (data == "[DONE]") {
                    trySend(StreamDelta(done = true))
                    break
                }
                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val choices = chunk["choices"]?.jsonArray
                    if (choices != null && choices.isNotEmpty()) {
                        val delta = choices[0].jsonObject["delta"]?.jsonObject
                        if (delta != null) {
                            val content = delta["content"]?.jsonPrimitive?.contentOrNull
                            val toolCalls = delta["tool_calls"]?.jsonArray?.map { it.jsonObject }
                            if (content != null || !toolCalls.isNullOrEmpty()) {
                                trySend(StreamDelta(
                                    content = content,
                                    toolCalls = toolCalls ?: emptyList()
                                ))
                            }
                        }
                    }
                } catch (_: Exception) { /* skip malformed chunks */ }
            }
        }

        response.close()
        channel.close()
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    suspend fun complete(
        model: String,
        messages: List<JsonObject>,
        tools: List<JsonObject>? = null,
        maxTokens: Int = 2048
    ): Result<Pair<String, List<JsonObject>>> = try {
        val body = adapter.buildBody(model, messages, tools, maxTokens, stream = false)
        val requestBody = json.encodeToString(JsonObject.serializer(), body)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .headers(adapter.authHeaders(apiKey).toHeaders())
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            Result.failure(ProviderException(
                status = response.code,
                body = errorBody,
                message = "Request failed: ${response.code}"
            ))
        } else {
            val responseBody = response.body?.string() ?: "{}"
            val jsonBody = json.parseToJsonElement(responseBody).jsonObject
            val choices = jsonBody["choices"]?.jsonArray
            val content = choices?.get(0)?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            val toolCalls = choices?.get(0)?.jsonObject?.get("message")?.jsonObject?.get("tool_calls")?.jsonArray
                ?.map { it.jsonObject } ?: emptyList()
            Result.success(content to toolCalls)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun testConnection(model: String): Result<Pair<Long, Boolean>> = try {
        val startTime = System.currentTimeMillis()
        val messages = listOf(JsonObject(mapOf(
            "role" to JsonPrimitive("user"),
            "content" to JsonPrimitive("Reply only with: OK")
        )))
        val body = adapter.buildBody(model, messages, null, 16, stream = false)
        val requestBody = json.encodeToString(JsonObject.serializer(), body)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .headers(adapter.authHeaders(apiKey).toHeaders())
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - startTime
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            Result.failure(ProviderException(response.code, errorBody, "Connection test failed"))
        } else {
            Result.success(latency to true)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class ProviderException(
    val status: Int,
    val body: String,
    message: String
) : Exception(message)

private fun JsonPrimitive.contentOrNull(): String? = if (this.isString) this.content else null

private fun Map<String, String>.toHeaders(): Headers {
    val builder = Headers.Builder()
    forEach { (k, v) -> builder.add(k, v) }
    return builder.build()
}
