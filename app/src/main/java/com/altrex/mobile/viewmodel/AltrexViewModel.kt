package com.altrex.mobile.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.altrex.mobile.AltrexApplication
import com.altrex.mobile.data.agent.AgentRunner
import com.altrex.mobile.data.agent.AgentRunner.AgentEvent
import com.altrex.mobile.data.agent.ToolBroker
import com.altrex.mobile.data.model.*
import com.altrex.mobile.data.multiai.Director
import com.altrex.mobile.data.provider.ProviderRegistry
import com.altrex.mobile.data.provider.ProviderService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class AltrexUiState(
    val conversations: List<SavedConversation> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<LocalConversationMessage> = emptyList(),
    val inputText: String = "",
    val mode: Mode = Mode.ASK,
    val modelSelection: String = "AUTO",
    val availableModels: List<String> = emptyList(),
    val providerStatus: ProviderStatus = ProviderStatus(connected = false),
    val sidebarCollapsed: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val showProviderDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showCommandPalette: Boolean = false,
    val showMultiAiRun: Boolean = false,
    val activeRun: ProjectRun? = null,
    val notification: String? = null,
    val diagnostics: List<ProviderRequestDiagnostic> = emptyList(),
    val providerTestResult: ProviderTestResult? = null,
    val connectionState: String = "NOT_CONFIGURED",
)

class AltrexViewModel(
    application: AltrexApplication
) : AndroidViewModel(application) {

    private val app = application
    private val providerService = app.providerService
    private val providerRepo = app.providerRepository
    private val conversationRepo = app.conversationRepository
    private val settingsRepo = app.settingsRepository

    private val _uiState = MutableStateFlow(AltrexUiState())
    val uiState: StateFlow<AltrexUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var agentRunner: AgentRunner? = null
    private var director: Director? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load conversations
            val conversations = conversationRepo.getAllConversations()
            val sidebarCollapsed = settingsRepo.getBoolean("sidebar_collapsed", false)
            val modelSelection = settingsRepo.getString("model_selection", "AUTO")

            _uiState.update { it.copy(
                conversations = conversations,
                sidebarCollapsed = sidebarCollapsed,
                modelSelection = modelSelection
            )}

            // Check for existing provider connection
            val savedConnection = providerRepo.getActiveConnection()
            if (savedConnection != null) {
                val testResult = providerService.connect(savedConnection)
                if (testResult.ok) {
                    val models = providerService.listModels()
                    _uiState.update { it.copy(
                        providerStatus = providerService.status(),
                        availableModels = models,
                        connectionState = "CONNECTED"
                    )}
                }
            }
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isStreaming) return

        val conversationId = _uiState.value.activeConversationId ?: UUID.randomUUID().toString()
        val userMessage = LocalConversationMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text,
            createdAt = Instant.now().toString(),
            status = "complete"
        )
        val assistantMessage = LocalConversationMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = "",
            createdAt = Instant.now().toString(),
            status = "streaming"
        )

        _uiState.update { it.copy(
            inputText = "",
            isStreaming = true,
            streamingContent = "",
            activeConversationId = conversationId,
            messages = it.messages + userMessage + assistantMessage
        )}

        val mode = _uiState.value.mode
        val modelSelection = _uiState.value.modelSelection
        val chatMessages = _uiState.value.messages
            .filter { it.role == "user" || (it.role == "assistant" && it.content.isNotBlank()) }
            .map { ChatMessage(it.role, it.content) }

        streamJob = viewModelScope.launch {
            if (mode == Mode.AGENT) {
                runAgent(text, chatMessages, modelSelection, assistantMessage, conversationId)
            } else if (mode == Mode.MULTI) {
                runMultiAi(text, conversationId, assistantMessage)
            } else {
                runAsk(chatMessages, modelSelection, assistantMessage, conversationId)
            }
        }
    }

    private suspend fun runAsk(
        messages: List<ChatMessage>,
        model: String,
        assistantMsg: LocalConversationMessage,
        conversationId: String
    ) {
        val sb = StringBuilder()
        val result = providerService.streamChat(messages, Mode.ASK, model,
            onDelta = { delta ->
                sb.append(delta)
                _uiState.update { st ->
                    val updatedMessages = st.messages.map { msg ->
                        if (msg.id == assistantMsg.id) msg.copy(content = sb.toString())
                        else msg
                    }
                    st.copy(messages = updatedMessages, streamingContent = sb.toString())
                }
            },
            onActivity = { /* ignore for ask mode */ }
        )

        val finalContent = sb.toString()
        val finalMessage = assistantMsg.copy(
            content = finalContent,
            status = if (result.isSuccess) "complete" else "error",
            finishedAt = Instant.now().toString(),
            provider = providerService.getActiveConnection()?.displayName,
            model = providerService.getActiveConnection()?.model
        )

        _uiState.update { it.copy(
            messages = it.messages.map { msg -> if (msg.id == assistantMsg.id) finalMessage else msg },
            isStreaming = false,
            streamingContent = ""
        )}

        saveConversation(conversationId)
    }

    private suspend fun runAgent(
        prompt: String,
        messages: List<ChatMessage>,
        model: String,
        assistantMsg: LocalConversationMessage,
        conversationId: String
    ) {
        val connection = providerService.getActiveConnection()
        if (connection == null) {
            _uiState.update { it.copy(isStreaming = false, notification = "No provider connected") }
            return
        }

        val toolBroker = ToolBroker()
        agentRunner = AgentRunner(providerService, toolBroker)

        val sb = StringBuilder()
        val activities = mutableListOf<String>()

        agentRunner!!.run(messages, model, object : AgentRunner.AgentEventCallback {
            override fun onDelta(delta: String) {
                sb.append(delta)
                _uiState.update { st ->
                    val updatedMessages = st.messages.map { msg ->
                        if (msg.id == assistantMsg.id) msg.copy(content = sb.toString())
                        else msg
                    }
                    st.copy(messages = updatedMessages, streamingContent = sb.toString())
                }
            }
            override fun onActivity(activity: String) {
                activities.add(activity)
                _uiState.update { st ->
                    val updatedMessages = st.messages.map { msg ->
                        if (msg.id == assistantMsg.id) msg.copy(activities = activities.toList())
                        else msg
                    }
                    st.copy(messages = updatedMessages)
                }
            }
            override fun onFilesChanged(files: List<String>) {
                _uiState.update { st ->
                    val updatedMessages = st.messages.map { msg ->
                        if (msg.id == assistantMsg.id) msg.copy(files = (msg.files + files).distinct())
                        else msg
                    }
                    st.copy(messages = updatedMessages)
                }
            }
            override fun onComplete(content: String) {
                val finalMessage = assistantMsg.copy(
                    content = content.ifBlank { sb.toString() },
                    status = "complete",
                    finishedAt = Instant.now().toString(),
                    provider = connection.displayName,
                    model = connection.model,
                    activities = activities.toList()
                )
                _uiState.update { st ->
                    st.copy(
                        messages = st.messages.map { msg -> if (msg.id == assistantMsg.id) finalMessage else msg },
                        isStreaming = false,
                        streamingContent = ""
                    )
                }
            }
            override fun onError(error: String) {
                val finalMessage = assistantMsg.copy(
                    content = sb.toString().ifBlank { error },
                    status = "error",
                    finishedAt = Instant.now().toString(),
                    provider = connection.displayName,
                    model = connection.model,
                    activity = error
                )
                _uiState.update { st ->
                    st.copy(
                        messages = st.messages.map { msg -> if (msg.id == assistantMsg.id) finalMessage else msg },
                        isStreaming = false,
                        streamingContent = "",
                        notification = error
                    )
                }
            }
        })

        saveConversation(conversationId)
    }

    private suspend fun runMultiAi(prompt: String, conversationId: String, assistantMsg: LocalConversationMessage) {
        val connection = providerService.getActiveConnection()
        if (connection == null) {
            _uiState.update { it.copy(isStreaming = false, notification = "No provider connected") }
            return
        }

        _uiState.update { it.copy(showMultiAiRun = true) }

        val stateStore = com.altrex.mobile.data.multiai.StateStore(getApplication())
        director = Director(providerService, stateStore, getApplication())

        director!!.start(prompt, null, object : Director.DirectorCallback {
            override fun onPlanCreated(plan: DirectorPlan) {
                _uiState.update { it.copy(
                    notification = "Plan created with ${plan.tasks.size} tasks",
                    activeRun = it.activeRun?.copy(
                        status = "RUNNING",
                        spec = plan.spec,
                        tasks = plan.tasks.map { t ->
                            SpecialistTask(
                                id = t.id, title = t.title, description = t.description,
                                role = t.role, priority = t.priority,
                                dependencies = t.dependencies, allowedFiles = t.allowedFiles,
                                restrictedFiles = t.restrictedFiles, inputs = t.inputs,
                                outputs = t.outputs, acceptance = t.acceptance,
                                status = "QUEUED"
                            )
                        }
                    )
                )}
            }
            override fun onTaskStarted(taskId: String) {
                _uiState.update { st ->
                    st.copy(activeRun = st.activeRun?.copy(
                        tasks = st.activeRun.tasks.map { t ->
                            if (t.id == taskId) t.copy(status = "RUNNING") else t
                        }
                    ))
                }
            }
            override fun onTaskCompleted(taskId: String, result: String) {
                _uiState.update { st ->
                    st.copy(activeRun = st.activeRun?.copy(
                        tasks = st.activeRun.tasks.map { t ->
                            if (t.id == taskId) t.copy(status = "COMPLETED", result = result) else t
                        }
                    ))
                }
            }
            override fun onActivity(activity: String) {
                _uiState.update { st ->
                    st.copy(activeRun = st.activeRun?.copy(
                        activity = (st.activeRun.activity + activity).takeLast(100)
                    ))
                }
            }
            override fun onComplete(run: ProjectRun) {
                _uiState.update { st ->
                    val finalMessage = assistantMsg.copy(
                        content = run.tasks.joinToString("\n\n") { "## ${it.title}\n${it.result}" },
                        status = "complete",
                        finishedAt = Instant.now().toString(),
                        provider = connection.displayName,
                        model = connection.model,
                        files = run.filesChanged
                    )
                    st.copy(
                        messages = st.messages.map { msg -> if (msg.id == assistantMsg.id) finalMessage else msg },
                        isStreaming = false,
                        streamingContent = "",
                        activeRun = run,
                        notification = "Multi-AI run completed"
                    )
                }
            }
            override fun onError(error: String) {
                _uiState.update { st ->
                    val finalMessage = assistantMsg.copy(
                        content = "Multi-AI run failed: $error",
                        status = "error",
                        finishedAt = Instant.now().toString()
                    )
                    st.copy(
                        messages = st.messages.map { msg -> if (msg.id == assistantMsg.id) finalMessage else msg },
                        isStreaming = false,
                        streamingContent = "",
                        notification = error
                    )
                }
            }
        })

        saveConversation(conversationId)
    }

    fun cancelRequest() {
        streamJob?.cancel()
        _uiState.update { it.copy(
            isStreaming = false,
            streamingContent = "",
            messages = it.messages.map { msg ->
                if (msg.status == "streaming") msg.copy(status = "cancelled", finishedAt = Instant.now().toString())
                else msg
            }
        )}
    }

    fun newConversation() {
        _uiState.update { it.copy(
            activeConversationId = null,
            messages = emptyList(),
            inputText = "",
            showMultiAiRun = false,
            activeRun = null
        )}
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            val conv = conversationRepo.getConversation(id)
            if (conv != null) {
                _uiState.update { it.copy(
                    activeConversationId = id,
                    messages = conv.messages,
                    showCommandPalette = false,
                    showMultiAiRun = false
                )}
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(id)
            val conversations = conversationRepo.getAllConversations()
            _uiState.update { it.copy(
                conversations = conversations,
                activeConversationId = if (it.activeConversationId == id) null else it.activeConversationId,
                messages = if (it.activeConversationId == id) emptyList() else it.messages
            )}
        }
    }

    fun setMode(mode: Mode) { _uiState.update { it.copy(mode = mode) } }
    fun setModelSelection(model: String) {
        _uiState.update { it.copy(modelSelection = model) }
        viewModelScope.launch { settingsRepo.setString("model_selection", model) }
    }
    fun updateInput(text: String) { _uiState.update { it.copy(inputText = text) } }
    fun toggleSidebar() {
        val newCollapsed = !_uiState.value.sidebarCollapsed
        _uiState.update { it.copy(sidebarCollapsed = newCollapsed) }
        viewModelScope.launch { settingsRepo.setBoolean("sidebar_collapsed", newCollapsed) }
    }

    fun showProviderDialog() { _uiState.update { it.copy(showProviderDialog = true) } }
    fun showSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = true) } }
    fun showCommandPalette() { _uiState.update { it.copy(showCommandPalette = true) } }
    fun dismissDialog() {
        _uiState.update { it.copy(
            showProviderDialog = false,
            showSettingsDialog = false,
            showCommandPalette = false
        )}
    }

    fun sendSuggestion(prompt: String) {
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }

    fun connectProvider(input: ProviderConnectionInput) {
        viewModelScope.launch {
            val result = providerService.connect(input)
            if (result.ok) {
                providerRepo.saveConnection(input)
                val models = providerService.listModels()
                _uiState.update { it.copy(
                    providerStatus = providerService.status(),
                    availableModels = models,
                    connectionState = "CONNECTED",
                    providerTestResult = result,
                    notification = "Provider connected: ${providerService.status().displayName}"
                )}
            } else {
                _uiState.update { it.copy(
                    providerTestResult = result,
                    notification = result.message
                )}
            }
        }
    }

    fun disconnectProvider(providerId: String) {
        viewModelScope.launch {
            providerService.disconnect(providerId)
            providerRepo.deleteConnection(providerId)
            _uiState.update { it.copy(
                providerStatus = providerService.status(),
                availableModels = emptyList(),
                connectionState = "NOT_CONFIGURED",
                notification = "Provider disconnected"
            )}
        }
    }

    fun testProvider(input: ProviderConnectionInput) {
        viewModelScope.launch {
            val result = providerService.connect(input)
            _uiState.update { it.copy(providerTestResult = result) }
        }
    }

    fun multiAiRevise(text: String) { director?.revise(text) }
    fun multiAiCancel() { director?.cancel(); _uiState.update { it.copy(isStreaming = false, showMultiAiRun = false) } }
    fun multiAiRestart() { /* TODO: restart unfinished tasks */ }

    fun jumpToLatest() { /* Handled by ChatScreen's scroll state */ }

    fun copyToClipboard(content: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ALTREX", content))
        _uiState.update { it.copy(notification = "Copied to clipboard") }
    }

    private suspend fun saveConversation(conversationId: String) {
        val messages = _uiState.value.messages
        val conv = SavedConversation(
            id = conversationId,
            projectPath = null,
            messages = messages,
            updatedAt = Instant.now().toString()
        )
        conversationRepo.saveConversation(conv)
        val conversations = conversationRepo.getAllConversations()
        _uiState.update { it.copy(conversations = conversations) }
    }

    fun clearNotification() { _uiState.update { it.copy(notification = null) } }
}

class AltrexViewModelFactory(private val app: AltrexApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AltrexViewModel(app) as T
}
