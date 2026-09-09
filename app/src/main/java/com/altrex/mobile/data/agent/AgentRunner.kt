package com.altrex.mobile.data.agent

import com.altrex.mobile.data.model.ChatMessage
import com.altrex.mobile.data.provider.ProviderService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Response from a tool-calling completion request.
 *
 * Wraps the assistant [message] (which may contain tool calls) along with
 * the [finishReason] reported by the provider and the [model] that was used.
 */
@Serializable
data class ToolCompletionResponse(
    val message: ChatMessage,
    val finishReason: String,
    val model: String? = null
)

/**
 * Per-run context for the agent.
 *
 * @param connectionId the provider connection to use for LLM calls
 * @param task the user's task description
 * @param projectRoot virtual root path for file operations
 * @param initialFiles files to pre-populate the virtual file system with
 */
data class AgentContext(
    val connectionId: String,
    val task: String,
    val projectRoot: String = "/",
    val initialFiles: Map<String, String> = emptyMap()
)

/**
 * Configuration for an agent run.
 *
 * @param modelFallbackChain models to try in order; if one fails, the next is used
 * @param temperature sampling temperature
 * @param maxTokens maximum tokens for each completion
 * @param maxRounds maximum tool-call rounds before the budget is exhausted
 * @param systemPromptOverride optional custom system prompt
 */
data class AgentConfig(
    val modelFallbackChain: List<String> = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo"),
    val temperature: Double = 0.2,
    val maxTokens: Int = 4_096,
    val maxRounds: Int = TaskBudget.DEFAULT_MAX_ROUNDS,
    val systemPromptOverride: String? = null
)

/**
 * Events emitted during agent execution.
 */
sealed class AgentEvent {
    data class Started(val task: String) : AgentEvent()
    data class LlmCallStarted(val model: String, val round: Int) : AgentEvent()
    data class LlmCallCompleted(val model: String, val finishReason: String) : AgentEvent()
    data class ToolCallStarted(val tool: String, val params: JsonObject) : AgentEvent()
    data class ToolCallCompleted(val tool: String, val result: ToolResult) : AgentEvent()
    data class ContextCompacted(val removedCount: Int) : AgentEvent()
    data class ModelFallback(val fromModel: String, val toModel: String, val reason: String) : AgentEvent()
    data class ProgressExtended(val additionalRounds: Int) : AgentEvent()
    data class Completed(val content: String, val rounds: Int) : AgentEvent()
    data class Failed(val error: String) : AgentEvent()
}

/**
 * Result of an agent run.
 */
sealed class AgentResult {
    data class Success(
        val content: String,
        val conversation: List<ChatMessage>,
        val budgetSnapshot: BudgetSnapshot,
        val modelUsed: String,
        val roundsCompleted: Int,
        val filesModified: Map<String, String>
    ) : AgentResult()

    data class Error(
        val message: String,
        val conversation: List<ChatMessage>,
        val budgetSnapshot: BudgetSnapshot,
        val cause: Throwable? = null
    ) : AgentResult()

    data class BudgetExhausted(
        val lastContent: String?,
        val conversation: List<ChatMessage>,
        val budgetSnapshot: BudgetSnapshot
    ) : AgentResult()

    data class LoopDetected(
        val signature: ToolCallSignature,
        val conversation: List<ChatMessage>,
        val budgetSnapshot: BudgetSnapshot
    ) : AgentResult()
}

/**
 * Callback for receiving agent events during execution.
 */
fun interface AgentEventCallback {
    suspend fun onEvent(event: AgentEvent)
}

/**
 * Builds the system prompt for the agent.
 */
object SystemPromptBuilder {

    fun build(task: String, fileListing: List<String>, customOverride: String? = null): String {
        if (customOverride != null) return customOverride

        return buildString {
            appendLine("You are ALTREX CODE, an AI coding assistant running on Android.")
            appendLine("Your job is to complete the given task using the available tools.")
            appendLine()
            appendLine("## Available Tools")
            appendLine("- write_file(path, content): Write content to a file. Creates or overwrites.")
            appendLine("- read_file(path): Read the full content of a file.")
            appendLine("- edit_file(path, old_text, new_text): Replace text in a file.")
            appendLine("- list_files(dir?): List files, optionally filtered by directory prefix.")
            appendLine()
            appendLine("## Current Project Files")
            if (fileListing.isEmpty()) {
                appendLine("(no files yet — use write_file to create them)")
            } else {
                fileListing.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("## Task")
            appendLine(task)
            appendLine()
            appendLine("## Guidelines")
            appendLine("- Always read a file before editing it to ensure old_text matches.")
            appendLine("- Prefer targeted edits (edit_file) over full rewrites (write_file) when possible.")
            appendLine("- When your task is complete, provide a concise summary of changes made.")
            appendLine("- Do not repeat the same tool call if it failed; try a different approach.")
        }
    }
}

/**
 * Agent runner that executes a tool-call loop using [ProviderService].
 *
 * The loop:
 *  1. Builds a system prompt and initial conversation.
 *  2. Calls [ProviderService.completeWithTools] with the conversation and tools.
 *  3. If the response contains tool calls, executes them via [ToolBroker],
 *     adds the results to the conversation, and continues.
 *  4. If the response has no tool calls, the run is complete.
 *  5. On LLM errors, tries the next model in the fallback chain.
 *
 * The budget ([TaskBudget]) limits total rounds and detects repetitive loops.
 * The context manager ([ContextManager]) compacts the conversation when it
 * exceeds the token budget.
 *
 * This is the Android adaptation of the desktop app's agent runner — it uses
 * the provider's completeWithTools/stream methods and operates on an in-memory
 * virtual file system instead of the real file system.
 */
class AgentRunner(
    private val providerService: ProviderService,
    private val toolBroker: ToolBroker,
    private val contextManager: ContextManager = ContextManager(),
    private val eventCallback: AgentEventCallback = AgentEventCallback { }
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Runs the agent for the given [context] and [config].
     */
    suspend fun run(context: AgentContext, config: AgentConfig): AgentResult {
        // Pre-populate the virtual file system
        if (context.initialFiles.isNotEmpty()) {
            toolBroker.fileSystem().importFiles(context.initialFiles)
        }

        val budget = TaskBudget(maxRounds = config.maxRounds)
        eventCallback.onEvent(AgentEvent.Started(context.task))

        // Build the initial conversation
        val fileListing = toolBroker.fileSystem().listFiles()
        val systemPrompt = SystemPromptBuilder.build(
            task = context.task,
            fileListing = fileListing,
            customOverride = config.systemPromptOverride
        )

        val messages = mutableListOf<ChatMessage>(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = context.task)
        )

        var currentModel: String = config.modelFallbackChain.firstOrNull()
            ?: return AgentResult.Error(
                message = "No models in fallback chain",
                conversation = messages.toList(),
                budgetSnapshot = budget.snapshot()
            )

        // Main tool-call loop
        while (budget.canContinue()) {
            if (!budget.advanceRound()) {
                eventCallback.onEvent(AgentEvent.Failed("Budget exhausted"))
                return AgentResult.BudgetExhausted(
                    lastContent = messages.lastOrNull { it.role == "assistant" }?.content,
                    conversation = messages.toList(),
                    budgetSnapshot = budget.snapshot()
                )
            }

            // Compact context if needed
            val tokensBefore = contextManager.estimateTokens(messages)
            if (contextManager.needsCompaction(messages)) {
                val beforeCount = messages.size
                val compacted = contextManager.compact(messages)
                messages.clear()
                messages.addAll(compacted)
                val removed = beforeCount - messages.size
                if (removed > 0) {
                    eventCallback.onEvent(AgentEvent.ContextCompacted(removed))
                }
            }

            // Try models in fallback order
            val response = tryModels(
                context = context,
                config = config,
                messages = messages.toList(),
                currentModel = currentModel,
                budget = budget
            )

            when (response) {
                is ModelAttempt.Success -> {
                    val assistantMessage = response.response.message
                    currentModel = response.response.model ?: currentModel
                    eventCallback.onEvent(
                        AgentEvent.LlmCallCompleted(
                            model = currentModel,
                            finishReason = response.response.finishReason
                        )
                    )

                    // Add assistant message to conversation
                    messages.add(assistantMessage)

                    // Check if there are tool calls to execute
                    val toolCalls = assistantMessage.toolCalls
                    if (toolCalls.isNullOrEmpty()) {
                        // No tool calls — the run is complete
                        val content = assistantMessage.content
                        eventCallback.onEvent(
                            AgentEvent.Completed(content, budget.snapshot().currentRound)
                        )
                        return AgentResult.Success(
                            content = content,
                            conversation = messages.toList(),
                            budgetSnapshot = budget.snapshot(),
                            modelUsed = currentModel,
                            roundsCompleted = budget.snapshot().currentRound,
                            filesModified = toolBroker.fileSystem().snapshot()
                        )
                    }

                    // Execute each tool call
                    var progressMade = false
                    for (toolCall in toolCalls) {
                        val toolName = toolCall.function.name
                        val params = parseToolParams(toolCall.function.arguments)

                        eventCallback.onEvent(
                            AgentEvent.ToolCallStarted(toolName, params)
                        )

                        // Record in budget for loop detection
                        val loopStatus = budget.recordToolCall(toolName, params)
                        if (loopStatus is TaskBudget.LoopStatus.LoopDetected) {
                            eventCallback.onEvent(
                                AgentEvent.Failed(
                                    "Loop detected: tool '$toolName' called " +
                                        "${loopStatus.count} times with identical arguments"
                                )
                            )
                            return AgentResult.LoopDetected(
                                signature = loopStatus.signature,
                                conversation = messages.toList(),
                                budgetSnapshot = budget.snapshot()
                            )
                        }

                        // Execute the tool
                        val result = toolBroker.executeTool(toolName, params)
                        eventCallback.onEvent(
                            AgentEvent.ToolCallCompleted(toolName, result)
                        )

                        // Add tool result to conversation
                        messages.add(
                            ChatMessage(
                                role = "tool",
                                content = toolBroker.resultToJson(result),
                                toolCallId = toolCall.id,
                                name = toolName
                            )
                        )

                        // Track progress
                        if (result is ToolResult.Success) {
                            progressMade = true
                        }
                    }

                    // Extend budget if progress was made
                    if (progressMade) {
                        budget.extend()
                        eventCallback.onEvent(
                            AgentEvent.ProgressExtended(TaskBudget.PROGRESS_EXTENSION)
                        )
                    }
                }

                is ModelAttempt.AllFailed -> {
                    eventCallback.onEvent(
                        AgentEvent.Failed(response.message)
                    )
                    return AgentResult.Error(
                        message = response.message,
                        conversation = messages.toList(),
                        budgetSnapshot = budget.snapshot(),
                        cause = response.lastError
                    )
                }
            }
        }

        // Budget exhausted
        eventCallback.onEvent(AgentEvent.Failed("Budget exhausted after ${budget.snapshot().currentRound} rounds"))
        return AgentResult.BudgetExhausted(
            lastContent = messages.lastOrNull { it.role == "assistant" }?.content,
            conversation = messages.toList(),
            budgetSnapshot = budget.snapshot()
        )
    }

    /**
     * Tries each model in the fallback chain until one succeeds.
     */
    private suspend fun tryModels(
        context: AgentContext,
        config: AgentConfig,
        messages: List<ChatMessage>,
        currentModel: String,
        budget: TaskBudget
    ): ModelAttempt {
        var lastError: Throwable? = null
        val modelChain = buildList {
            add(currentModel)
            config.modelFallbackChain.filter { it != currentModel }.forEach { add(it) }
        }

        for ((index, model) in modelChain.withIndex()) {
            eventCallback.onEvent(
                AgentEvent.LlmCallStarted(model, budget.snapshot().currentRound)
            )

            try {
                val result = providerService.completeWithTools(
                    connectionId = context.connectionId,
                    messages = messages,
                    tools = toolBroker.toolDefinitions(),
                    model = model,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens
                )

                if (result.isSuccess) {
                    return ModelAttempt.Success(result.getOrThrow())
                } else {
                    lastError = result.exceptionOrNull()
                    val errorMsg = lastError?.message ?: "Unknown error"
                    if (index < modelChain.size - 1) {
                        val nextModel = modelChain[index + 1]
                        eventCallback.onEvent(
                            AgentEvent.ModelFallback(model, nextModel, errorMsg)
                        )
                    }
                }
            } catch (e: Exception) {
                lastError = e
                if (index < modelChain.size - 1) {
                    val nextModel = modelChain[index + 1]
                    eventCallback.onEvent(
                        AgentEvent.ModelFallback(model, nextModel, e.message ?: "Exception")
                    )
                }
            }
        }

        return ModelAttempt.AllFailed(
            message = "All models in fallback chain failed: ${modelChain.joinToString(", ")}",
            lastError = lastError
        )
    }

    /**
     * Parses tool call arguments (JSON string) into a JsonObject.
     */
    private fun parseToolParams(arguments: String): JsonObject {
        return try {
            json.parseToJsonElement(arguments).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
    }

    /**
     * Internal result of attempting to call the LLM with model fallback.
     */
    private sealed class ModelAttempt {
        data class Success(val response: ToolCompletionResponse) : ModelAttempt()
        data class AllFailed(val message: String, val lastError: Throwable?) : ModelAttempt()
    }
}
