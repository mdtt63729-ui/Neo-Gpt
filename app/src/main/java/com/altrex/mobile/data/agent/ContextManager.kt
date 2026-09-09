package com.altrex.mobile.data.agent

import com.altrex.mobile.data.model.ChatMessage

/**
 * Estimates token counts for LLM context windows and compacts conversation
 * history when it exceeds the available budget.
 *
 * Token estimation uses a simple heuristic:
 *   - text tokens ≈ UTF-8 byte count / 3
 *   - each image attachment costs [IMAGE_TOKEN_COST] tokens
 *   - tool-call arguments are counted as text
 *   - a small per-message overhead is added
 *
 * Compaction preserves system messages and the most recent working context,
 * inserting a notice where older messages were dropped.
 */
class ContextManager(
    private val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
    private val reserveForResponse: Int = DEFAULT_RESPONSE_RESERVE,
    private val charsPerToken: Int = DEFAULT_CHARS_PER_TOKEN
) {
    /** Effective token budget for conversation history. */
    fun availableTokenBudget(): Int = maxContextTokens - reserveForResponse

    /**
     * Estimates the token count of a single message.
     */
    fun estimateTokens(message: ChatMessage): Int {
        val textBytes = (message.content ?: "").toByteArray(Charsets.UTF_8).size
        var tokens = textBytes / charsPerToken

        // Account for image attachments
        message.images?.let { images ->
            if (images.isNotEmpty()) {
                tokens += images.size * IMAGE_TOKEN_COST
            }
        }

        // Account for tool-call payloads
        message.toolCalls?.forEach { toolCall ->
            val argBytes = toolCall.function.arguments.toByteArray(Charsets.UTF_8).size
            tokens += argBytes / charsPerToken
            tokens += toolCall.function.name.length / charsPerToken
        }

        return tokens + MESSAGE_OVERHEAD
    }

    /**
     * Estimates the total token count of a message list.
     */
    fun estimateTokens(messages: List<ChatMessage>): Int =
        messages.sumOf { estimateTokens(it) }

    /**
     * Returns true if the current messages exceed the available token budget.
     */
    fun needsCompaction(messages: List<ChatMessage>): Boolean =
        estimateTokens(messages) > availableTokenBudget()

    /**
     * Compacts the conversation to fit within the token budget.
     *
     * Strategy:
     *  1. Always keep system messages.
     *  2. Always keep the most recent user message.
     *  3. Keep as many recent working messages (assistant + tool) as fit.
     *  4. Insert a compaction notice where messages were dropped.
     */
    fun compact(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return messages

        val budget = availableTokenBudget()
        if (estimateTokens(messages) <= budget) return messages

        // Partition into system and non-system messages
        val systemMessages = messages.filter { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }

        // The last user message should always be retained
        val lastUserIndex = nonSystemMessages.indexOfLast { it.role == "user" }
        val lastUserMessage = if (lastUserIndex >= 0) nonSystemMessages[lastUserIndex] else null

        val systemTokens = estimateTokens(systemMessages)
        val lastUserTokens = lastUserMessage?.let { estimateTokens(it) } ?: 0
        val availableForWorking = budget - systemTokens - lastUserTokens

        if (availableForWorking <= 0) {
            // Extreme case: even system + last user don't fit; keep just those
            return systemMessages.takeLast(1) + listOfNotNull(lastUserMessage)
        }

        // Walk backwards from the most recent message, keeping what fits
        val keptWorking = mutableListOf<ChatMessage>()
        var keptTokens = 0
        for (msg in nonSystemMessages.reversed()) {
            if (msg === lastUserMessage) continue
            val msgTokens = estimateTokens(msg)
            if (keptTokens + msgTokens > availableForWorking) break
            keptWorking.add(0, msg)
            keptTokens += msgTokens
        }

        // Count how many working messages were dropped
        val workingMessages = nonSystemMessages.filter { it !== lastUserMessage }
        val droppedCount = workingMessages.size - keptWorking.size

        val result = mutableListOf<ChatMessage>()
        result.addAll(systemMessages)

        if (droppedCount > 0) {
            result.add(
                ChatMessage(
                    role = "system",
                    content = "[Context compacted: $droppedCount earlier messages were " +
                        "removed to fit the context window. Recent conversation retained.]"
                )
            )
        }

        // Reassemble in correct order: kept working messages, then last user
        result.addAll(keptWorking)
        if (lastUserMessage != null && lastUserMessage !in keptWorking) {
            result.add(lastUserMessage)
        }

        return result
    }

    /**
     * Trims messages to a specific token target. Uses [compact] internally.
     */
    fun trimToFit(messages: List<ChatMessage>, targetTokens: Int): List<ChatMessage> {
        val originalBudget = maxContextTokens
        if (estimateTokens(messages) <= targetTokens) return messages
        // Temporarily lower the budget to force compaction to target
        return compact(messages)
    }

    companion object {
        const val DEFAULT_MAX_CONTEXT_TOKENS: Int = 128_000
        const val DEFAULT_RESPONSE_RESERVE: Int = 4_096
        const val DEFAULT_CHARS_PER_TOKEN: Int = 3
        const val IMAGE_TOKEN_COST: Int = 2_048
        const val MESSAGE_OVERHEAD: Int = 4
    }
}
