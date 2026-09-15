package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val FONT_PREF = stringPreferencesKey("app_font")
        val TEXT_SIZE_PREF = stringPreferencesKey("text_size")
        val THEME_MODE_PREF = intPreferencesKey("theme_mode") // 0 system, 1 light, 2 dark
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val CUSTOM_MODELS = stringPreferencesKey("custom_models")
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_key_enc")
        val NVIDIA_KEY = stringPreferencesKey("nvidia_key_enc")
        val GEMINI_KEY = stringPreferencesKey("gemini_key_enc")
    }

    val selectedFontFlow: Flow<String> = context.dataStore.data.map { it[FONT_PREF] ?: "Inter" }
    val textSizeFlow: Flow<Float> = context.dataStore.data.map { it[TEXT_SIZE_PREF]?.toFloatOrNull() ?: 16f }
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE_PREF] ?: 0 }
    val selectedProviderFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_PROVIDER] ?: "Venus" }
    val selectedModelFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_MODEL] ?: "venus-3.1" }

    val openRouterKeyFlow: Flow<String> = context.dataStore.data.map { SecretBox.decrypt(it[OPENROUTER_KEY] ?: "") }
    val nvidiaKeyFlow: Flow<String> = context.dataStore.data.map { SecretBox.decrypt(it[NVIDIA_KEY] ?: "") }
    val geminiKeyFlow: Flow<String> = context.dataStore.data.map { SecretBox.decrypt(it[GEMINI_KEY] ?: "") }

    val customModelsFlow: Flow<String> = context.dataStore.data.map { it[CUSTOM_MODELS] ?: "[]" }

    suspend fun saveFont(fontName: String) = context.dataStore.edit { it[FONT_PREF] = fontName }
    suspend fun saveTextSize(size: Float) = context.dataStore.edit { it[TEXT_SIZE_PREF] = size.toString() }
    suspend fun saveThemeMode(mode: Int) = context.dataStore.edit { it[THEME_MODE_PREF] = mode }
    suspend fun saveSelectedProvider(provider: String) = context.dataStore.edit { it[SELECTED_PROVIDER] = provider }
    suspend fun saveSelectedModel(model: String) = context.dataStore.edit { it[SELECTED_MODEL] = model }

    suspend fun saveOpenRouterKey(key: String) = saveSecret(OPENROUTER_KEY, key)
    suspend fun saveNvidiaKey(key: String) = saveSecret(NVIDIA_KEY, key)
    suspend fun saveGeminiKey(key: String) = saveSecret(GEMINI_KEY, key)

    private suspend fun saveSecret(pref: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            if (value.isBlank()) preferences.remove(pref) else preferences[pref] = SecretBox.encrypt(value.trim())
        }
    }

    suspend fun saveCustomModel(displayName: String, modelId: String, provider: String, description: String) {
        context.dataStore.edit { preferences ->
            val current = parseCustomModels(preferences[CUSTOM_MODELS] ?: "[]").toMutableList()
            val duplicate = current.any {
                it["provider"] == provider && it["modelId"] == modelId
            }
            if (!duplicate) {
                current += JSONObject().apply {
                    put("displayName", displayName.trim())
                    put("modelId", modelId.trim())
                    put("provider", provider)
                    put("description", description.trim())
                }
                preferences[CUSTOM_MODELS] = JSONArray().apply { current.forEach { put(it) } }.toString()
            }
        }
    }

    suspend fun removeCustomModel(provider: String, modelId: String) {
        context.dataStore.edit { preferences ->
            val filtered = parseCustomModels(preferences[CUSTOM_MODELS] ?: "[]").filterNot {
                it["provider"] == provider && it["modelId"] == modelId
            }
            preferences[CUSTOM_MODELS] = JSONArray().apply { filtered.forEach { put(it) } }.toString()
        }
    }

    private fun parseCustomModels(json: String): List<JSONObject> {
        return try {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optJSONObject(index) ?: JSONObject() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
