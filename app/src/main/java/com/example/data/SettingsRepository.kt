package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val FONT_PREF = stringPreferencesKey("app_font")
        val TEXT_SIZE_PREF = floatPreferencesKey("text_size")
        val THEME_MODE_PREF = intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark
        val LOGGED_IN_USER_PREF = stringPreferencesKey("logged_in_user")
        
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_key")
        val NVIDIA_KEY = stringPreferencesKey("nvidia_key")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    val selectedFontFlow: Flow<String> = context.dataStore.data.map { it[FONT_PREF] ?: "Inter" }
    val textSizeFlow: Flow<Float> = context.dataStore.data.map { it[TEXT_SIZE_PREF] ?: 16f }
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE_PREF] ?: 0 }
    val loggedInUserFlow: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER_PREF] }
    
    val openRouterKeyFlow: Flow<String> = context.dataStore.data.map { it[OPENROUTER_KEY] ?: "" }
    val nvidiaKeyFlow: Flow<String> = context.dataStore.data.map { it[NVIDIA_KEY] ?: "" }
    val geminiKeyFlow: Flow<String> = context.dataStore.data.map { it[GEMINI_KEY] ?: "" }
    val selectedModelFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_MODEL] ?: "venus-3.1" }

    suspend fun saveFont(fontName: String) { context.dataStore.edit { it[FONT_PREF] = fontName } }
    suspend fun saveTextSize(size: Float) { context.dataStore.edit { it[TEXT_SIZE_PREF] = size } }
    suspend fun saveThemeMode(mode: Int) { context.dataStore.edit { it[THEME_MODE_PREF] = mode } }
    suspend fun saveLoggedInUser(username: String?) {
        context.dataStore.edit {
            if (username == null) it.remove(LOGGED_IN_USER_PREF) else it[LOGGED_IN_USER_PREF] = username
        }
    }
    
    suspend fun saveOpenRouterKey(key: String) { context.dataStore.edit { it[OPENROUTER_KEY] = key } }
    suspend fun saveNvidiaKey(key: String) { context.dataStore.edit { it[NVIDIA_KEY] = key } }
    suspend fun saveGeminiKey(key: String) { context.dataStore.edit { it[GEMINI_KEY] = key } }
    suspend fun saveSelectedModel(model: String) { context.dataStore.edit { it[SELECTED_MODEL] = model } }
}
