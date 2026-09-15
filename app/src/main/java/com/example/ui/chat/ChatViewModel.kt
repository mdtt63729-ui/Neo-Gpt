package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Message
import com.example.data.MessageDao
import com.example.data.SettingsRepository
import com.example.network.PicoApi
import com.example.network.PicoRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first

class ChatViewModel(private val messageDao: MessageDao, private val settingsRepository: SettingsRepository) : ViewModel() {
    private val currentUser = settingsRepository.loggedInUserFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = currentUser.flatMapLatest { userId ->
        val id = userId ?: "default"
        messageDao.getMessagesForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val userId = currentUser.value ?: "default"
            messageDao.insert(Message(text = text, isUser = true, userId = userId))
            _isLoading.value = true
            try {
                val selectedModel = settingsRepository.selectedModelFlow.first()
                if (selectedModel == "venus-3.1") {
                    handleVenusChat(text, userId)
                } else if (selectedModel.startsWith("openrouter/")) {
                    val key = settingsRepository.openRouterKeyFlow.first()
                    val actualModel = selectedModel.substringAfter("/")
                    val response = com.example.network.AiApiProviders.openRouter.chat("Bearer $key", com.example.network.OpenAiRequest(actualModel, listOf(com.example.network.OpenAiMessage("user", text))))
                    val reply = response.choices?.firstOrNull()?.message?.content ?: "No response"
                    messageDao.insert(Message(text = reply, isUser = false, userId = userId))
                } else if (selectedModel.startsWith("nvidia/")) {
                    val key = settingsRepository.nvidiaKeyFlow.first()
                    val actualModel = selectedModel.substringAfter("/")
                    val response = com.example.network.AiApiProviders.nvidia.chat("Bearer $key", com.example.network.OpenAiRequest(actualModel, listOf(com.example.network.OpenAiMessage("user", text))))
                    val reply = response.choices?.firstOrNull()?.message?.content ?: "No response"
                    messageDao.insert(Message(text = reply, isUser = false, userId = userId))
                } else if (selectedModel.startsWith("gemini/")) {
                    val key = settingsRepository.geminiKeyFlow.first()
                    val actualModel = selectedModel.substringAfter("/")
                    val response = com.example.network.AiApiProviders.gemini.chat(actualModel, key, com.example.network.GeminiRequest(listOf(com.example.network.GeminiContent(listOf(com.example.network.GeminiPart(text))))))
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                    messageDao.insert(Message(text = reply, isUser = false, userId = userId))
                }
            } catch (e: Exception) {
                val userId = currentUser.value ?: "default"
                messageDao.insert(Message(text = "Network error: ${e.message}", isUser = false, userId = userId))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleVenusChat(text: String, userId: String) {
        if (text.startsWith("/image", ignoreCase = true)) {
            val prompt = text.substring(6).trim()
            val response = PicoApi.retrofitService.getImageResponse(request = PicoRequest(prompt))
            if (response.status == "success") {
                messageDao.insert(Message(text = "Here is your image:", isUser = false, imageUrl = response.imageUrl, userId = userId))
            } else {
                messageDao.insert(Message(text = "Error: API monthly limit reached or unable to generate image.", isUser = false, userId = userId))
            }
        } else {
            val prompt = "Follow instructions precisely! If the user asks to generate, create or make an image, photo, or picture by describing it, You will reply with '/image' + description. Otherwise, You will respond normally. Avoid additional explanations." + text
            val response = PicoApi.retrofitService.getChatResponse(request = PicoRequest(prompt))
            if (response.status == "success") {
                val responseText = response.text ?: ""
                if (responseText.trim().startsWith("/image", ignoreCase = true)) {
                     val imagePrompt = responseText.substring(responseText.indexOf("/image", ignoreCase = true) + 6).trim()
                     val imgResponse = PicoApi.retrofitService.getImageResponse(request = PicoRequest(imagePrompt))
                     if (imgResponse.status == "success") {
                         messageDao.insert(Message(text = "Here is your image:", isUser = false, imageUrl = imgResponse.imageUrl, userId = userId))
                     } else {
                         messageDao.insert(Message(text = "Error: API monthly limit reached or unable to generate image.", isUser = false, userId = userId))
                     }
                } else {
                    messageDao.insert(Message(text = responseText, isUser = false, userId = userId))
                }
            } else {
                messageDao.insert(Message(text = "Error: API monthly limit reached or invalid response. Please configure your own API keys in Settings.", isUser = false, userId = userId))
            }
        }
    }
}

class ChatViewModelFactory(private val messageDao: MessageDao, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(messageDao, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
