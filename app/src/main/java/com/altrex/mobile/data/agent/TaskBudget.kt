package com.altrex.mobile.data.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Represents a unique signature for a tool call, used for loop detection.
 * Two tool calls with the same [tool] name and [paramsDigest] are considered
 * identical and may indicate the agent is stuck in a loop.
 */
@Serializable
data class ToolCallSignature(
    val tool: String,
    val paramsDigest: String
) {
    companion object {
        fun create(tool: String, params: JsonObject): ToolCallSignature {
            val canonical = Json.encodeToString(JsonObject.serializer(), params)
            val digest = MessageDigest.getInstance("SHA-256")
            val hashHex = digest.digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
                .take(16)
            return ToolCallSignature(tool, hashHex)
        }
    }
}

/**
 * Immutable snapshot of the current budget state.
 */
@Serializable
data class BudgetSnapshot(
    val currentRound: Int,
    val maxRounds: Int,
    val extraRounds: Int,
    val remainingRounds: Int,
    val totalToolCalls: Int,
    val uniqueToolCalls: Int
)

class BudgetExhaustedException(
    message: String,
    val snapshot: BudgetSnapshot
) : Exception(message)

class LoopDetectedException(
    message: String,
    val signature: ToolCallSignature,
    val repetitionCount: Int
) : Exception(message)

/**
 * Tracks the number of LLM rounds an agent is allowed to consume, detects
 * repetitive tool-call loops, and supports extending the budget when the
 * agent makes measurable progress.
 *
 * Loop detection: if the same [ToolCallSignature] is recorded [loopThreshold]
 * times, [recordToolCall] returns [LoopStatus.LoopDetected].
 *
 * Progress extension: callers that observe forward progress should call
 * [extend] to grant additional rounds, preventing premature exhaustion on
 * legitimately long tasks.
 */
class TaskBudget(
    private val maxRounds: Int = DEFAULT_MAX_ROUNDS,
    private val loopThreshold: Int = DEFAULT_LOOP_THRESHOLD,
    private var extraRounds: Int = 0
) {
    private var currentRound: Int = 0
    private val callHistory: MutableList<ToolCallSignature> = mutableListOf()
    private val repetitionCounts: MutableMap<ToolCallSignature, Int> = mutableMapOf()

    /** How many rounds the agent may still execute. */
    val remainingRounds: Int
        get() = (maxRounds + extraRounds) - currentRound

    /** True when no more rounds are available. */
    val isExhausted: Boolean
        get() = remainingRounds <= 0

    /** Total tool calls recorded so far. */
    val totalToolCalls: Int
        get() = callHistory.size

    /** Number of distinct tool-call signatures observed. */
    val uniqueToolCalls: Int
        get() = repetitionCounts.size

    /** Returns true if the budget has rounds remaining. */
    fun canContinue(): Boolean = !isExhausted

    /**
     * Consumes one round. Returns false (and does not advance) when the
     * budget is already exhausted.
     */
    fun advanceRound(): Boolean {
        if (isExhausted) return false
        currentRound++
        return true
    }

    /**
     * Records a tool call and checks for repetition.
     *
     * @return [LoopStatus.Ok] if under the threshold, or
     *         [LoopStatus.LoopDetected] if the threshold has been reached.
     */
    fun recordToolCall(tool: String, params: JsonObject): LoopStatus {
        val signature = ToolCallSignature.create(tool, params)
        callHistory.add(signature)
        val count = (repetitionCounts[signature] ?: 0) + 1
        repetitionCounts[signature] = count
        return if (count >= loopThreshold) {
            LoopStatus.LoopDetected(signature, count)
        } else {
            LoopStatus.Ok
        }
    }

    /**
     * Grants additional rounds to the budget. Call this when the agent
     * makes observable progress (e.g. a file was successfully edited).
     */
    fun extend(rounds: Int = PROGRESS_EXTENSION) {
        if (rounds > 0) {
            extraRounds += rounds
        }
    }

    /** Returns true when at least one tool call has been recorded. */
    fun hasActivity(): Boolean = callHistory.isNotEmpty()

    /** Resets the budget to its initial state. */
    fun reset() {
        currentRound = 0
        extraRounds = 0
        callHistory.clear()
        repetitionCounts.clear()
    }

    /** Returns an immutable snapshot of the current budget state. */
    fun snapshot(): BudgetSnapshot = BudgetSnapshot(
        currentRound = currentRound,
        maxRounds = maxRounds,
        extraRounds = extraRounds,
        remainingRounds = remainingRounds,
        totalToolCalls = callHistory.size,
        uniqueToolCalls = repetitionCounts.size
    )

    /** Returns the full call history for debugging or logging. */
    fun callHistory(): List<ToolCallSignature> = callHistory.toList()

    sealed class LoopStatus {
        /** The tool call did not exceed the repetition threshold. */
        object Ok : LoopStatus()

        /** The same tool call signature has been seen [count] times. */
        data class LoopDetected(
            val signature: ToolCallSignature,
            val count: Int
        ) : LoopStatus()
    }

    companion object {
        const val DEFAULT_MAX_ROUNDS: Int = 50
        const val DEFAULT_LOOP_THRESHOLD: Int = 3
        const val PROGRESS_EXTENSION: Int = 10
    }
}
