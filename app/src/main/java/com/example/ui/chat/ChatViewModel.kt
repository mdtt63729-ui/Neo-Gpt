package com.example.ui.chat

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Attachment
import com.example.data.AttachmentType
import com.example.data.Message
import com.example.data.MessageDao
import com.example.data.SettingsRepository
import com.example.network.AIProvider
import com.example.network.GeminiProvider
import com.example.network.ModelCatalog
import com.example.network.ModelOption
import com.example.network.NvidiaNimProvider
import com.example.network.OpenRouterProvider
import com.example.network.ProviderType
import com.example.network.PicoApi
import com.example.network.PicoRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ChatRequestState {
    data object Idle : ChatRequestState
    data object Thinking : ChatRequestState
    data object Streaming : ChatRequestState
    data class Error(val message: String) : ChatRequestState
    data object Cancelled : ChatRequestState
}

class ChatViewModel(
    private val messageDao: MessageDao,
    private val settingsRepository: SettingsRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {
    private val userId = "default"

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = kotlinx.coroutines.flow.flowOf(userId)
        .flatMapLatest { messageDao.getMessagesForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _requestState = MutableStateFlow<ChatRequestState>(ChatRequestState.Idle)
    val requestState = _requestState.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<Attachment>>(emptyList())
    val pendingAttachments = _pendingAttachments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var requestJob: Job? = null

    fun addAttachment(attachment: Attachment) {
        if (_requestState.value !is ChatRequestState.Idle && _requestState.value !is ChatRequestState.Error && _requestState.value !is ChatRequestState.Cancelled) return
        if (_pendingAttachments.value.any { it.uri == attachment.uri }) return
        _pendingAttachments.value = _pendingAttachments.value + attachment
    }

    fun removeAttachment(uri: android.net.Uri) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.uri == uri }
    }

    fun clearAttachments() {
        _pendingAttachments.value = emptyList()
    }

    fun sendMessage(text: String) {
        sendMessage(text, _pendingAttachments.value)
    }

    fun sendMessage(text: String, attachments: List<Attachment>) {
        if (text.isBlank() && attachments.isEmpty()) return
        if (requestJob?.isActive == true) return

        val cleanText = text.trim().ifBlank { "Please analyze the attached file/image." }
        val requestAttachments = attachments.toList()
        requestJob = viewModelScope.launch {
            _error.value = null
            _requestState.value = ChatRequestState.Thinking
            val userMessage = Message(
                text = cleanText,
                isUser = true,
                userId = userId,
                attachmentUri = requestAttachments.firstOrNull()?.uri?.toString(),
                attachmentName = requestAttachments.firstOrNull()?.fileName,
                attachmentMimeType = requestAttachments.firstOrNull()?.mimeType,
                attachmentSizeBytes = requestAttachments.firstOrNull()?.sizeBytes,
                imageUrl = requestAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.uri?.toString()
            )
            messageDao.insert(userMessage)
            _pendingAttachments.value = emptyList()

            try {
                val providerName = settingsRepository.selectedProviderFlow.first()
                val modelId = settingsRepository.selectedModelFlow.first()
                val model = resolveModel(providerName, modelId)

                if (model.provider == ProviderType.VENUS) {
                    if (requestAttachments.isNotEmpty()) {
                        throw UnsupportedOperationException("Venus 3.1 currently accepts text through its existing HTML/Pico response path. Please choose an image-capable configured provider for attachments.")
                    }
                    _requestState.value = ChatRequestState.Streaming
                    handleVenusChat(cleanText)
                } else {
                    val provider = createProvider(model.provider)
                        ?: throw IllegalStateException("${model.provider} is not configured. Add its API key in Settings.")
                    _requestState.value = ChatRequestState.Streaming
                    val reply = provider.send(model, cleanText, requestAttachments)
                    if (reply.isBlank()) throw IllegalStateException("The provider returned an empty response.")
                    messageDao.insert(Message(text = reply, isUser = false, userId = userId))
                }
                _requestState.value = ChatRequestState.Idle
            } catch (cancelled: CancellationException) {
                _requestState.value = ChatRequestState.Cancelled
                throw cancelled
            } catch (e: Exception) {
                val friendly = friendlyError(e)
                _error.value = friendly
                _requestState.value = ChatRequestState.Error(friendly)
                messageDao.insert(Message(text = friendly, isUser = false, userId = userId))
                _requestState.value = ChatRequestState.Idle
            } finally {
                requestJob = null
            }
        }
    }

    fun stopGeneration() {
        if (requestJob?.isActive == true) {
            requestJob?.cancel()
            requestJob = null
            _requestState.value = ChatRequestState.Cancelled
        }
    }

    fun clearError() {
        _error.value = null
        if (_requestState.value is ChatRequestState.Error || _requestState.value is ChatRequestState.Cancelled) {
            _requestState.value = ChatRequestState.Idle
        }
    }

    private suspend fun handleVenusChat(text: String) {
        if (text.startsWith("/image", ignoreCase = true)) {
            val prompt = text.substring(6).trim()
            val response = PicoApi.retrofitService.getImageResponse(request = PicoRequest(prompt))
            if (response.status == "success" && !response.imageUrl.isNullOrBlank()) {
                messageDao.insert(Message(text = "Here is your image:", isUser = false, imageUrl = response.imageUrl, userId = userId))
            } else {
                throw IllegalStateException("Venus image generation failed. Please try again later.")
            }
            return
        }

        val prompt = "Follow instructions precisely! If the user asks to generate, create or make an image, photo, or picture by describing it, reply with '/image' + description. Otherwise, respond normally. Avoid additional explanations.\n\nUser: $text"
        val response = PicoApi.retrofitService.getChatResponse(request = PicoRequest(prompt))
        if (response.status != "success") throw IllegalStateException("Venus 3.1 returned an unsuccessful response.")
        val responseText = response.text?.trim().orEmpty()
        if (responseText.isBlank()) throw IllegalStateException("Venus 3.1 returned an empty response.")

        if (responseText.startsWith("/image", ignoreCase = true)) {
            val imagePrompt = responseText.substring(6).trim()
            val imageResponse = PicoApi.retrofitService.getImageResponse(request = PicoRequest(imagePrompt))
            if (imageResponse.status == "success" && !imageResponse.imageUrl.isNullOrBlank()) {
                messageDao.insert(Message(text = "Here is your image:", isUser = false, imageUrl = imageResponse.imageUrl, userId = userId))
            } else {
                throw IllegalStateException("Venus image generation failed. Please try again later.")
            }
        } else {
            messageDao.insert(Message(text = responseText, isUser = false, userId = userId))
        }
    }

    private suspend fun createProvider(provider: String): AIProvider? {
        return when (provider) {
            ProviderType.OPENROUTER -> {
                val key = settingsRepository.openRouterKeyFlow.first()
                key.takeIf { it.isNotBlank() }?.let { OpenRouterProvider(contentResolver, it) }
            }
            ProviderType.NVIDIA -> {
                val key = settingsRepository.nvidiaKeyFlow.first()
                key.takeIf { it.isNotBlank() }?.let { NvidiaNimProvider(contentResolver, it) }
            }
            ProviderType.GEMINI -> {
                val key = settingsRepository.geminiKeyFlow.first()
                key.takeIf { it.isNotBlank() }?.let { GeminiProvider(contentResolver, it) }
            }
            else -> null
        }
    }

    private suspend fun resolveModel(provider: String, modelId: String): ModelOption {
        val defaults = when (provider) {
            ProviderType.OPENROUTER -> ModelCatalog.openRouter
            ProviderType.NVIDIA -> ModelCatalog.nvidia
            ProviderType.GEMINI -> ModelCatalog.gemini
            else -> listOf(ModelCatalog.venus)
        }
        defaults.firstOrNull { it.modelId == modelId }?.let { return it }
        val customJson = settingsRepository.customModelsFlow.first()
        val custom = try {
            val array = JSONArray(customJson)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                if (item.optString("provider") != provider) return@mapNotNull null
                val name = item.optString("displayName").trim()
                val id = item.optString("modelId").trim()
                if (name.isBlank() || id.isBlank()) null else ModelOption(name, id, provider, item.optString("description"), true)
            }
        } catch (_: Exception) { emptyList() }
        return custom.firstOrNull { it.modelId == modelId }
            ?: if (provider == ProviderType.VENUS) ModelCatalog.venus
            else throw IllegalStateException("The selected model is no longer available. Please choose another model.")
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            error is java.net.UnknownHostException -> "No internet connection. Check your network and try again."
            error is java.net.SocketTimeoutException -> "The request timed out. Please try again."
            error is UnsupportedOperationException -> message.ifBlank { "This attachment is not supported by the selected provider." }
            message.contains("401") -> "The API key was rejected. Check the provider key in Settings."
            message.contains("403") -> "The provider denied this request. Check the API key and account access."
            message.contains("404") -> "The selected model or endpoint was not found. Choose another model."
            message.contains("429") -> "The provider rate limit was reached. Please wait and try again."
            message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504") -> "The AI provider is temporarily unavailable. Please try again."
            else -> message.ifBlank { "Something went wrong. Please try again." }
        }
    }
}

class ChatViewModelFactory(
    private val messageDao: MessageDao,
    private val settingsRepository: SettingsRepository,
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(messageDao, settingsRepository, contentResolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
