package com.altrex.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.altrex.mobile.data.model.LocalConversationMessage
import com.altrex.mobile.data.model.SavedConversation
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Top-level state holder for the main screen. The real app would supply these
 * from a ViewModel; here it is captured as composable parameters so the layout
 * stays self-contained and testable.
 *
 * The MainScreen lays out a [Sidebar] on the left and a column on the right
 * containing the [Topbar] and a content area. The content area switches
 * between [HomeScreen] (when there is no active conversation) and
 * [ChatScreen] (once a conversation exists). The [Composer] is docked at the
 * bottom of the content column. [ProviderDialog], [SettingsDialog] and
 * [CommandPalette] render as overlays driven by their respective flags.
 */
@Composable
fun MainScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    conversations: List<SavedConversation>,
    activeConversation: SavedConversation?,
    activeMessages: List<LocalConversationMessage>,
    isStreaming: Boolean,
    workspaceName: String,
    projectName: String,
    mode: ComposerMode,
    onModeChange: (ComposerMode) -> Unit,
    models: List<String>,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    providerConnected: Boolean,
    providerName: String,
    onNewChat: () -> Unit,
    onConversationSelected: (SavedConversation) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit,
    onAttach: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    // ---- Overlays ----
    showProviderDialog: Boolean,
    providerDialogContent: ProviderDialogState,
    onProviderDialogEvent: (ProviderDialogEvent) -> Unit,
    showSettingsDialog: Boolean,
    settingsDialogState: SettingsDialogState,
    onSettingsDialogEvent: (SettingsDialogEvent) -> Unit,
    showCommandPalette: Boolean,
    onCommandPaletteEvent: (CommandPaletteEvent) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(AltrexColors.bgApp)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                expanded = sidebarExpanded,
                onToggleExpanded = onToggleSidebar,
                conversations = conversations,
                activeConversationId = activeConversation?.id,
                onConversationSelected = onConversationSelected,
                onNewChat = onNewChat,
                onOpenSettings = onOpenSettings
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Topbar(
                    workspaceName = workspaceName,
                    projectName = projectName,
                    mode = mode,
                    providerConnected = providerConnected,
                    providerName = providerName,
                    onOpenProviders = onOpenProviders,
                    onOpenSettings = onOpenSettings
                )

                // Content switches between Home and Chat based on conversation state.
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeConversation == null && activeMessages.isEmpty()) {
                        HomeScreen(onSuggestionSelected = onSuggestionSelected)
                    } else {
                        ChatScreen(
                            messages = activeMessages,
                            isStreaming = isStreaming
                        )
                    }
                }

                Composer(
                    onSend = onSendMessage,
                    onStop = onStopStreaming,
                    isStreaming = isStreaming,
                    mode = mode,
                    onModeChange = onModeChange,
                    models = models,
                    selectedModel = selectedModel,
                    onModelChange = onModelChange,
                    onAttach = onAttach
                )
            }
        }

        // ---------- Overlays ----------
        if (showProviderDialog) {
            ProviderDialog(
                providers = providerDialogContent.providers,
                selectedProviderId = providerDialogContent.selectedProviderId,
                onProviderSelected = { onProviderDialogEvent(ProviderDialogEvent.SelectProvider(it)) },
                onApiKeyChange = { id, key -> onProviderDialogEvent(ProviderDialogEvent.ApiKeyChange(id, key)) },
                onModelChange = { id, model -> onProviderDialogEvent(ProviderDialogEvent.ModelChange(id, model)) },
                onBaseUrlChange = { id, url -> onProviderDialogEvent(ProviderDialogEvent.BaseUrlChange(id, url)) },
                onTestConnection = { onProviderDialogEvent(ProviderDialogEvent.TestConnection(it)) },
                onConnect = { onProviderDialogEvent(ProviderDialogEvent.Connect(it)) },
                onDisconnect = { onProviderDialogEvent(ProviderDialogEvent.Disconnect(it)) },
                onDismiss = { onProviderDialogEvent(ProviderDialogEvent.Dismiss) }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                providerProfiles = settingsDialogState.providerProfiles,
                activeProviderId = settingsDialogState.activeProviderId,
                availableModels = settingsDialogState.availableModels,
                selectedModel = settingsDialogState.selectedModel,
                onModelSelected = { onSettingsDialogEvent(SettingsDialogEvent.ModelSelected(it)) },
                onActivateProvider = { onSettingsDialogEvent(SettingsDialogEvent.ActivateProvider(it)) },
                diagnostics = settingsDialogState.diagnostics,
                capabilities = settingsDialogState.capabilities,
                onDismiss = { onSettingsDialogEvent(SettingsDialogEvent.Dismiss) }
            )
        }

        if (showCommandPalette) {
            CommandPalette(
                conversations = conversations,
                onConversationSelected = {
                    onCommandPaletteEvent(CommandPaletteEvent.ConversationSelected(it))
                },
                onNewChat = {
                    onCommandPaletteEvent(CommandPaletteEvent.NewChat)
                },
                onDismiss = { onCommandPaletteEvent(CommandPaletteEvent.Dismiss) }
            )
        }
    }
}

// ---------- Overlay state & events ----------

data class ProviderDialogState(
    val providers: List<ProviderCardData>,
    val selectedProviderId: String?
)

sealed interface ProviderDialogEvent {
    data object Dismiss : ProviderDialogEvent
    data class SelectProvider(val id: String) : ProviderDialogEvent
    data class ApiKeyChange(val id: String, val key: String) : ProviderDialogEvent
    data class ModelChange(val id: String, val model: String) : ProviderDialogEvent
    data class BaseUrlChange(val id: String, val baseUrl: String) : ProviderDialogEvent
    data class TestConnection(val id: String) : ProviderDialogEvent
    data class Connect(val id: String) : ProviderDialogEvent
    data class Disconnect(val id: String) : ProviderDialogEvent
}

data class SettingsDialogState(
    val providerProfiles: List<SettingsProviderProfile>,
    val activeProviderId: String?,
    val availableModels: List<String>,
    val selectedModel: String,
    val diagnostics: List<Pair<String, String>>,
    val capabilities: List<CapabilityRow>
)

sealed interface SettingsDialogEvent {
    data object Dismiss : SettingsDialogEvent
    data class ModelSelected(val model: String) : SettingsDialogEvent
    data class ActivateProvider(val id: String) : SettingsDialogEvent
}

sealed interface CommandPaletteEvent {
    data object Dismiss : CommandPaletteEvent
    data object NewChat : CommandPaletteEvent
    data class ConversationSelected(val conversation: SavedConversation) : CommandPaletteEvent
}
