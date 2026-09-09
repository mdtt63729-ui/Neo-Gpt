package com.altrex.mobile.data.repository

import com.altrex.mobile.data.local.AppDatabase
import com.altrex.mobile.data.local.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Known setting keys used throughout the app.
 * Using a sealed-like pattern with constants for type safety.
 */
object SettingsKeys {
    const val THEME = "theme"
    const val DEFAULT_PROVIDER_ID = "default_provider_id"
    const val DEFAULT_MODEL = "default_model"
    const val TEMPERATURE = "temperature"
    const val MAX_TOKENS = "max_tokens"
    const val MAX_ROUNDS = "max_rounds"
    const val SYSTEM_PROMPT = "system_prompt"
    const val STREAM_RESPONSES = "stream_responses"
    const val SEND_USAGE_DATA = "send_usage_data"
    const val AUTO_COMPACT = "auto_compact"
    const val CONTEXT_TOKEN_BUDGET = "context_token_budget"
    const val DIRECTOR_PLANNING_MODEL = "director_planning_model"
    const val DIRECTOR_EXECUTION_MODEL = "director_execution_model"
    const val DIRECTOR_INTEGRATION_MODEL = "director_integration_model"
}

/** Predefined theme values. */
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

/**
 * A single setting entry.
 */
data class SettingEntry(
    val key: String,
    val value: String,
    val updatedAt: Long
)

/**
 * Repository for app settings persistence.
 *
 * Uses Room ([SettingsDao]) for a typed, observable key-value store.
 * Settings are stored as strings and converted to typed values at the
 * call site via the typed getter helpers below.
 */
class SettingsRepository(
    private val database: AppDatabase
) {
    private val dao = database.settingsDao()

    // ── Core get/set ────────────────────────────────────────────────────────

    /**
     * Returns the raw string value for [key], or [default] if not set.
     */
    suspend fun get(key: String, default: String? = null): String? {
        return dao.get(key)?.value ?: default
    }

    /**
     * Sets a string value for [key].
     */
    suspend fun set(key: String, value: String) {
        dao.upsert(
            SettingsEntity(
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Removes the setting for [key].
     */
    suspend fun remove(key: String) {
        dao.delete(key)
    }

    /**
     * Returns all settings.
     */
    suspend fun getAll(): Map<String, String> {
        return dao.getAll().associate { it.key to it.value }
    }

    /**
     * Observes all settings as a Flow.
     */
    fun observeAll(): Flow<Map<String, String>> {
        return dao.observeAll().map { entities ->
            entities.associate { it.key to it.value }
        }
    }

    /**
     * Observes a single setting as a Flow.
     */
    fun observe(key: String, default: String? = null): Flow<String?> {
        return dao.observeAll().map { entities ->
            entities.find { it.key == key }?.value ?: default
        }
    }

    /**
     * Clears all settings.
     */
    suspend fun clear() {
        dao.deleteAll()
    }

    // ── Typed helpers ───────────────────────────────────────────────────────

    /** Returns the setting as an Int, or [default] if not set or invalid. */
    suspend fun getInt(key: String, default: Int): Int {
        return get(key)?.toIntOrNull() ?: default
    }

    /** Sets an Int setting. */
    suspend fun setInt(key: String, value: Int) {
        set(key, value.toString())
    }

    /** Returns the setting as a Long, or [default] if not set or invalid. */
    suspend fun getLong(key: String, default: Long): Long {
        return get(key)?.toLongOrNull() ?: default
    }

    /** Sets a Long setting. */
    suspend fun setLong(key: String, value: Long) {
        set(key, value.toString())
    }

    /** Returns the setting as a Float, or [default] if not set or invalid. */
    suspend fun getFloat(key: String, default: Float): Float {
        return get(key)?.toFloatOrNull() ?: default
    }

    /** Sets a Float setting. */
    suspend fun setFloat(key: String, value: Float) {
        set(key, value.toString())
    }

    /** Returns the setting as a Double, or [default] if not set or invalid. */
    suspend fun getDouble(key: String, default: Double): Double {
        return get(key)?.toDoubleOrNull() ?: default
    }

    /** Sets a Double setting. */
    suspend fun setDouble(key: String, value: Double) {
        set(key, value.toString())
    }

    /** Returns the setting as a Boolean, or [default] if not set or invalid. */
    suspend fun getBoolean(key: String, default: Boolean): Boolean {
        val value = get(key) ?: return default
        return value == "true" || value == "1"
    }

    /** Sets a Boolean setting. */
    suspend fun setBoolean(key: String, value: Boolean) {
        set(key, value.toString())
    }

    // ── App-specific convenience methods ────────────────────────────────────

    /** Returns the configured theme mode (defaults to system). */
    suspend fun getTheme(): String =
        get(SettingsKeys.THEME, ThemeMode.SYSTEM) ?: ThemeMode.SYSTEM

    /** Sets the theme mode. */
    suspend fun setTheme(mode: String) =
        set(SettingsKeys.THEME, mode)

    /** Returns the default provider ID. */
    suspend fun getDefaultProviderId(): String? =
        get(SettingsKeys.DEFAULT_PROVIDER_ID)

    /** Sets the default provider ID. */
    suspend fun setDefaultProviderId(providerId: String) =
        set(SettingsKeys.DEFAULT_PROVIDER_ID, providerId)

    /** Returns the default model name. */
    suspend fun getDefaultModel(): String? =
        get(SettingsKeys.DEFAULT_MODEL)

    /** Sets the default model name. */
    suspend fun setDefaultModel(model: String) =
        set(SettingsKeys.DEFAULT_MODEL, model)

    /** Returns the LLM temperature (defaults to 0.2). */
    suspend fun getTemperature(): Double =
        getDouble(SettingsKeys.TEMPERATURE, 0.2)

    /** Sets the LLM temperature. */
    suspend fun setTemperature(value: Double) =
        setDouble(SettingsKeys.TEMPERATURE, value)

    /** Returns the max tokens setting (defaults to 4096). */
    suspend fun getMaxTokens(): Int =
        getInt(SettingsKeys.MAX_TOKENS, 4_096)

    /** Sets the max tokens. */
    suspend fun setMaxTokens(value: Int) =
        setInt(SettingsKeys.MAX_TOKENS, value)

    /** Returns the max agent rounds (defaults to 50). */
    suspend fun getMaxRounds(): Int =
        getInt(SettingsKeys.MAX_ROUNDS, 50)

    /** Sets the max agent rounds. */
    suspend fun setMaxRounds(value: Int) =
        setInt(SettingsKeys.MAX_ROUNDS, value)

    /** Returns whether streaming is enabled (defaults to true). */
    suspend fun isStreamingEnabled(): Boolean =
        getBoolean(SettingsKeys.STREAM_RESPONSES, true)

    /** Sets whether streaming is enabled. */
    suspend fun setStreamingEnabled(enabled: Boolean) =
        setBoolean(SettingsKeys.STREAM_RESPONSES, enabled)

    /** Returns whether auto context compaction is enabled (defaults to true). */
    suspend fun isAutoCompactEnabled(): Boolean =
        getBoolean(SettingsKeys.AUTO_COMPACT, true)

    /** Returns the context token budget (defaults to 128000). */
    suspend fun getContextTokenBudget(): Int =
        getInt(SettingsKeys.CONTEXT_TOKEN_BUDGET, 128_000)

    /** Returns the custom system prompt, if any. */
    suspend fun getSystemPrompt(): String? =
        get(SettingsKeys.SYSTEM_PROMPT)

    /** Sets a custom system prompt. */
    suspend fun setSystemPrompt(prompt: String?) {
        if (prompt == null) remove(SettingsKeys.SYSTEM_PROMPT)
        else set(SettingsKeys.SYSTEM_PROMPT, prompt)
    }
}
