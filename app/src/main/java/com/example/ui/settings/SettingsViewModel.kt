package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val selectedFont: StateFlow<String> = repository.selectedFontFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Inter")
    val textSize: StateFlow<Float> = repository.textSizeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 16f)
    val themeMode: StateFlow<Int> = repository.themeModeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val selectedProvider: StateFlow<String> = repository.selectedProviderFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Venus")
    val selectedModel: StateFlow<String> = repository.selectedModelFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "venus-3.1")
    val customModels: StateFlow<String> = repository.customModelsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "[]")

    val openRouterKey: StateFlow<String> = repository.openRouterKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val nvidiaKey: StateFlow<String> = repository.nvidiaKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val geminiKey: StateFlow<String> = repository.geminiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setFont(value: String) = viewModelScope.launch { repository.saveFont(value) }
    fun setTextSize(value: Float) = viewModelScope.launch { repository.saveTextSize(value) }
    fun setThemeMode(value: Int) = viewModelScope.launch { repository.saveThemeMode(value) }
    fun setProvider(value: String) = viewModelScope.launch { repository.saveSelectedProvider(value) }
    fun setModel(value: String) = viewModelScope.launch { repository.saveSelectedModel(value) }
    fun setOpenRouterKey(value: String) = viewModelScope.launch { repository.saveOpenRouterKey(value) }
    fun setNvidiaKey(value: String) = viewModelScope.launch { repository.saveNvidiaKey(value) }
    fun setGeminiKey(value: String) = viewModelScope.launch { repository.saveGeminiKey(value) }
    fun addCustomModel(displayName: String, modelId: String, provider: String, description: String) = viewModelScope.launch {
        repository.saveCustomModel(displayName, modelId, provider, description)
    }
    fun removeCustomModel(provider: String, modelId: String) = viewModelScope.launch {
        repository.removeCustomModel(provider, modelId)
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
