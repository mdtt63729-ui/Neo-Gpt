package com.altrex.mobile.data.provider

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

interface ProviderAdapter {
    fun buildBody(
        model: String,
        messages: List<JsonObject>,
        tools: List<JsonObject>?,
        maxTokens: Int,
        stream: Boolean,
        temperature: Double? = null
    ): JsonObject
    fun authHeaders(apiKey: String): Map<String, String>
}

class BaseProviderAdapter : ProviderAdapter {
    override fun buildBody(
        model: String, messages: List<JsonObject>, tools: List<JsonObject>?,
        maxTokens: Int, stream: Boolean, temperature: Double?
    ): JsonObject = JsonObject(buildMap {
        put("model", JsonPrimitive(model))
        put("messages", kotlinx.serialization.json.JsonArray(messages))
        put("max_tokens", JsonPrimitive(maxTokens))
        put("stream", JsonPrimitive(stream))
        temperature?.let { put("temperature", JsonPrimitive(it)) }
        if (tools != null) {
            put("tools", kotlinx.serialization.json.JsonArray(tools))
            put("tool_choice", JsonPrimitive("auto"))
        }
    })
    override fun authHeaders(apiKey: String) = mapOf("Authorization" to "Bearer $apiKey")
}

class OpenAIAdapter : BaseProviderAdapter() {
    override fun buildBody(model: String, messages: List<JsonObject>, tools: List<JsonObject>?,
        maxTokens: Int, stream: Boolean, temperature: Double?
    ): JsonObject {
        val body = super.buildBody(model, messages, tools, maxTokens, stream, temperature).toMutableMap()
        if (model.startsWith("o") || model.contains("gpt-5") || model.contains("o1") || model.contains("o3") || model.contains("o4")) {
            body.remove("max_tokens")
            body["max_completion_tokens"] = JsonPrimitive(maxTokens)
        }
        return JsonObject(body)
    }
}

class NvidiaNimAdapter : BaseProviderAdapter() {
    override fun buildBody(model: String, messages: List<JsonObject>, tools: List<JsonObject>?,
        maxTokens: Int, stream: Boolean, temperature: Double?
    ): JsonObject {
        val body = super.buildBody(model, messages, tools, maxTokens, stream, temperature).toMutableMap()
        if (model.contains("nemotron")) {
            body["temperature"] = JsonPrimitive(1.0)
            body["top_p"] = JsonPrimitive(0.95)
        }
        return JsonObject(body)
    }
}

object ProviderAdapterFactory {
    fun get(providerId: String): ProviderAdapter = when (providerId) {
        "openai" -> OpenAIAdapter()
        "nvidia" -> NvidiaNimAdapter()
        else -> BaseProviderAdapter()
    }
}
