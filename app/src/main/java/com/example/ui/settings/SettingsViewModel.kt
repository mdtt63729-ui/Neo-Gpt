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
    val selectedFont: StateFlow<String> = repository.selectedFontFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Inter")
    val textSize: StateFlow<Float> = repository.textSizeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)
    val themeMode: StateFlow<Int> = repository.themeModeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val loggedInUser: StateFlow<String?> = repository.loggedInUserFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val openRouterKey: StateFlow<String> = repository.openRouterKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val nvidiaKey: StateFlow<String> = repository.nvidiaKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val geminiKey: StateFlow<String> = repository.geminiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val selectedModel: StateFlow<String> = repository.selectedModelFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "venus-3.1")

    fun setFont(fontName: String) = viewModelScope.launch { repository.saveFont(fontName) }
    fun setTextSize(size: Float) = viewModelScope.launch { repository.saveTextSize(size) }
    fun setThemeMode(mode: Int) = viewModelScope.launch { repository.saveThemeMode(mode) }
    fun login(username: String) = viewModelScope.launch { repository.saveLoggedInUser(username) }
    fun logout() = viewModelScope.launch { repository.saveLoggedInUser(null) }
    
    fun setOpenRouterKey(key: String) = viewModelScope.launch { repository.saveOpenRouterKey(key) }
    fun setNvidiaKey(key: String) = viewModelScope.launch { repository.saveNvidiaKey(key) }
    fun setGeminiKey(key: String) = viewModelScope.launch { repository.saveGeminiKey(key) }
    fun setSelectedModel(model: String) = viewModelScope.launch { repository.saveSelectedModel(model) }
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
