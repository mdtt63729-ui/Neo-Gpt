package com.altrex.mobile.data.provider

import com.altrex.mobile.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.*
import java.util.UUID

class ProviderService {
    private val json = Json { ignoreUnknownKeys = true }
    private var activeConnection: ProviderConnection? = null
    private val connections = mutableMapOf<String, ProviderConnection>()

    data class ProviderConnection(
        val providerId: String,
        val displayName: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val additionalFields: Map<String, String> = emptyMap(),
        val client: ApiClient,
        val adapter: ProviderAdapter,
        val policy: RequestPolicy
    )

    fun connect(input: ProviderConnectionInput): ProviderTestResult {
        val def = ProviderRegistry.byId(input.providerId) ?: return ProviderTestResult(
            ok = false, message = "Unknown provider", latencyMs = 0
        )
        val baseUrl = if (input.baseUrl.isNotBlank()) input.baseUrl else def.baseUrl
        val model = if (input.model.isNotBlank()) input.model else def.defaultModel
        val adapter = ProviderAdapterFactory.get(input.providerId)
        val policy = RequestPolicies.forProvider(input.providerId)
        val client = ApiClient(baseUrl, input.apiKey, adapter)

        val testResult = client.testConnection(model)
        return if (testResult.isSuccess) {
            val (latency, _) = testResult.getOrThrow()
            val connection = ProviderConnection(
                providerId = input.providerId,
                displayName = def.name,
                baseUrl = baseUrl,
                apiKey = input.apiKey,
                model = model,
                additionalFields = input.additionalFields,
                client = client,
                adapter = adapter,
                policy = policy
            )
            connections[input.providerId] = connection
            activeConnection = connection
            ProviderTestResult(
                ok = true,
                message = "Connected successfully",
                latencyMs = latency,
                capabilities = ProviderCapabilities(chat = true, streaming = true, tools = null)
            )
        } else {
            val err = testResult.exceptionOrNull()
            val errorMsg = when (err) {
                is ProviderException -> {
                    val cat = ProviderErrors.classifyHttpError(err.status, err.body)
                    ProviderErrors.describeError(cat)
                }
                else -> err?.message ?: "Connection failed"
            }
            ProviderTestResult(ok = false, message = errorMsg, latencyMs = 0)
        }
    }

    fun disconnect(providerId: String) {
        connections.remove(providerId)
        if (activeConnection?.providerId == providerId) activeConnection = null
    }

    fun status(): ProviderStatus {
        val conn = activeConnection ?: return ProviderStatus(connected = false)
        return ProviderStatus(
            connected = true,
            providerId = conn.providerId,
            displayName = conn.displayName,
            baseUrl = conn.baseUrl,
            model = conn.model,
            profiles = connections.values.map { c ->
                ProviderProfileStatus(
                    providerId = c.providerId,
                    displayName = c.displayName,
                    model = c.model,
                    baseUrl = c.baseUrl,
                    health = "HEALTHY",
                    modelsDiscovered = 0,
                    toolCompatibleModels = 0,
                    connectionState = "CONNECTED",
                    keySuffix = c.apiKey.takeLast(4),
                    statusMessage = null
                )
            }
        )
    }

    fun listModels(providerId: String? = null): List<String> {
        val conn = if (providerId != null) connections[providerId] else activeConnection
        ?: return emptyList()
        val result = conn.client.listModels()
        return if (result.isSuccess) {
            val models = result.getOrDefault(emptyList())
            if (conn.providerId == "google") {
                models.map { it.removePrefix("models/") }
            } else models
        } else emptyList()
    }

    fun streamChat(
        messages: List<ChatMessage>,
        mode: Mode,
        modelSelection: String,
        onDelta: (String) -> Unit,
        onActivity: (String) -> Unit
    ): Result<String> {
        val conn = activeConnection ?: return Result.failure(IllegalStateException("No provider connected"))
        val model = if (modelSelection == "AUTO" || modelSelection.isBlank()) conn.model else modelSelection

        val jsonMessages = messages.map { msg ->
            JsonObject(mapOf(
                "role" to JsonPrimitive(msg.role),
                "content" to JsonPrimitive(msg.content)
            ))
        }

        val tools = if (mode == Mode.AGENT || mode == Mode.MULTI) codingTools() else null
        val maxTokens = conn.policy.outputTokens

        val result = kotlinx.coroutines.runBlocking {
            val sb = StringBuilder()
            conn.client.stream(model, jsonMessages, tools, maxTokens).collect { delta ->
                delta.content?.let { sb.append(it); onDelta(it) }
                delta.error?.let { onActivity(it) }
            }
            sb.toString()
        }
        return Result.success(result)
    }

    fun complete(
        messages: List<ChatMessage>,
        mode: Mode,
        modelSelection: String,
        tools: List<JsonObject>? = null
    ): Result<String> {
        val conn = activeConnection ?: return Result.failure(IllegalStateException("No provider connected"))
        val model = if (modelSelection == "AUTO" || modelSelection.isBlank()) conn.model else modelSelection

        val jsonMessages = messages.map { msg ->
            JsonObject(mapOf(
                "role" to JsonPrimitive(msg.role),
                "content" to JsonPrimitive(msg.content)
            ))
        }

        val effectiveTools = tools ?: if (mode == Mode.AGENT || mode == Mode.MULTI) codingTools() else null
        val maxTokens = conn.policy.outputTokens

        return kotlinx.coroutines.runBlocking {
            val result = conn.client.complete(model, jsonMessages, effectiveTools, maxTokens)
            result.map { it.first }
        }
    }

    fun completeWithTools(
        messages: List<JsonObject>,
        tools: List<JsonObject>? = null,
        modelSelection: String = "AUTO"
    ): Result<Pair<String, List<JsonObject>>> {
        val conn = activeConnection ?: return Result.failure(IllegalStateException("No provider connected"))
        val model = if (modelSelection == "AUTO" || modelSelection.isBlank()) conn.model else modelSelection
        return kotlinx.coroutines.runBlocking {
            conn.client.complete(model, messages, tools, conn.policy.outputTokens)
        }
    }

    fun getActiveConnection(): ProviderConnection? = activeConnection

    private fun codingTools(): List<JsonObject> = listOf(
        JsonObject(mapOf(
            "type" to JsonPrimitive("function"),
            "function" to JsonObject(mapOf(
                "name" to JsonPrimitive("edit_file"),
                "description" to JsonPrimitive("Replace an exact text segment in a file"),
                "parameters" to JsonObject(mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(mapOf(
                        "path" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("File path"))),
                        "old" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Text to replace"))),
                        "new" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Replacement text")))
                    )),
                    "required" to JsonArray(listOf(JsonPrimitive("path"), JsonPrimitive("old"), JsonPrimitive("new")))
                ))
            ))
        )),
        JsonObject(mapOf(
            "type" to JsonPrimitive("function"),
            "function" to JsonObject(mapOf(
                "name" to JsonPrimitive("write_file"),
                "description" to JsonPrimitive("Create or replace a file"),
                "parameters" to JsonObject(mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(mapOf(
                        "path" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("File path"))),
                        "content" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("File content")))
                    )),
                    "required" to JsonArray(listOf(JsonPrimitive("path"), JsonPrimitive("content")))
                ))
            ))
        )),
        JsonObject(mapOf(
            "type" to JsonPrimitive("function"),
            "function" to JsonObject(mapOf(
                "name" to JsonPrimitive("read_file"),
                "description" to JsonPrimitive("Read a file with optional line range"),
                "parameters" to JsonObject(mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(mapOf(
                        "path" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("File path"))),
                        "start" to JsonObject(mapOf("type" to JsonPrimitive("integer"), "description" to JsonPrimitive("Start line (1-based)"))),
                        "end" to JsonObject(mapOf("type" to JsonPrimitive("integer"), "description" to JsonPrimitive("End line")))
                    )),
                    "required" to JsonArray(listOf(JsonPrimitive("path")))
                ))
            ))
        )),
        JsonObject(mapOf(
            "type" to JsonPrimitive("function"),
            "function" to JsonObject(mapOf(
                "name" to JsonPrimitive("list_files"),
                "description" to JsonPrimitive("List directory contents"),
                "parameters" to JsonObject(mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(mapOf(
                        "path" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Directory path")))
                    )),
                    "required" to JsonArray(listOf(JsonPrimitive("path")))
                ))
            ))
        ))
    )
}
