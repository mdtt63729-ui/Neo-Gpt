package com.altrex.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,  // "user" | "assistant"
    val content: String
)

@Serializable
data class ChatAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val kind: String,  // "image" | "text" | "file"
    val previewDataUrl: String? = null
)

@Serializable
data class ChatRequest(
    val requestId: String,
    val projectPath: String? = null,
    val messages: List<ChatMessage>,
    val attachments: List<ChatAttachment> = emptyList(),
    val mode: String,  // "ASK" | "AGENT" | "MULTI"
    val modelSelection: String,
    val resumeRunId: String? = null
)

@Serializable
data class ChatStreamEvent(
    val requestId: String,
    val type: String,
    val run: ProjectRun? = null,
    val delta: String? = null,
    val message: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val files: List<String>? = null,
    val command: String? = null,
    val exitCode: Int? = null,
    val output: String? = null
)

@Serializable
data class LocalConversationMessage(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String,
    val status: String,
    val provider: String? = null,
    val model: String? = null,
    val activity: String? = null,
    val activities: List<String> = emptyList(),
    val finishedAt: String? = null,
    val files: List<String> = emptyList(),
    val commands: List<LocalCommandResult> = emptyList(),
    val attachments: List<ChatAttachment> = emptyList()
)

@Serializable
data class LocalCommandResult(
    val command: String,
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean
)

@Serializable
data class SavedConversation(
    val id: String,
    val projectPath: String? = null,
    val messages: List<LocalConversationMessage>,
    val updatedAt: String
)

@Serializable
data class ProjectSummary(
    val name: String,
    val path: String,
    val branch: String? = null,
    val markers: List<String> = emptyList()
)

@Serializable
data class ProviderConnectionInput(
    val providerId: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val additionalFields: Map<String, String> = emptyMap(),
    val requestPolicy: PartialRequestPolicy? = null
)

@Serializable
data class PartialRequestPolicy(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val maxAttempts: Int? = null,
    val concurrency: Int? = null
)

@Serializable
data class ProviderStatus(
    val connected: Boolean,
    val providerId: String? = null,
    val displayName: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val profiles: List<ProviderProfileStatus> = emptyList(),
    val warning: String? = null
)

@Serializable
data class ProviderProfileStatus(
    val providerId: String,
    val displayName: String,
    val model: String,
    val baseUrl: String,
    val health: String,
    val modelsDiscovered: Int,
    val toolCompatibleModels: Int,
    val lastErrorCategory: String? = null,
    val lastCheckedAt: String? = null,
    val connectionState: String,
    val keySuffix: String? = null,
    val statusMessage: String? = null,
    val additionalFields: Map<String, String> = emptyMap()
)

@Serializable
data class ProviderTestResult(
    val ok: Boolean,
    val message: String,
    val latencyMs: Long,
    val failureKind: String? = null,
    val errorCategory: String? = null,
    val modelsDiscovered: Int? = null,
    val resolvedModel: String? = null,
    val capabilities: ProviderCapabilities? = null
)

@Serializable
data class ProviderCapabilities(
    val chat: Boolean? = null,
    val streaming: Boolean? = null,
    val tools: Boolean? = null
)

@Serializable
data class ProviderRequestDiagnostic(
    val id: String,
    val startedAt: String,
    val provider: String,
    val model: String,
    val durationMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val retries: Int,
    val status: String,
    val httpStatus: Int? = null,
    val errorCategory: String? = null,
    val retryable: Boolean? = null,
    val retryAfterMs: Long? = null,
    val technicalDetails: String? = null,
    val fallbackDestination: String? = null
)

@Serializable
data class ModelCapabilities(
    val supportsChat: Boolean? = null,
    val supportsStreaming: Boolean? = null,
    val supportsTools: Boolean? = null,
    val supportsParallelTools: Boolean? = null,
    val supportsVision: Boolean? = null,
    val supportsJSON: Boolean? = null,
    val supportsReasoning: Boolean? = null,
    val contextWindow: Int? = null,
    val maxOutput: Int? = null
)

@Serializable
data class RequestPolicy(
    val inputTokens: Int = 6000,
    val outputTokens: Int = 2048,
    val connectionMs: Long = 20000,
    val firstTokenMs: Long = 180000,
    val idleMs: Long = 90000,
    val overallMs: Long = 600000,
    val maxAttempts: Int = 2,
    val concurrency: Int = 2
)

@Serializable
data class ProviderToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

@Serializable
data class ProviderMessage(
    val role: String,
    val content: String? = null,
    val toolCallId: String? = null,
    val toolCalls: List<ProviderToolCallData>? = null
)

@Serializable
data class ProviderToolCallData(
    val id: String,
    val type: String = "function",
    val function: ProviderToolCallFunction
)

@Serializable
data class ProviderToolCallFunction(
    val name: String,
    val arguments: String
)

@Serializable
data class ToolExecutionResult(
    val toolCallId: String,
    val name: String,
    val content: String,
    val changedFile: String? = null,
    val changedFiles: List<String> = emptyList(),
    val commandResult: LocalCommandResult? = null
)

@Serializable
data class CapabilityStatus(
    val available: Boolean,
    val label: String
)
