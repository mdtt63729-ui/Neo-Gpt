package com.altrex.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.altrex.mobile.data.model.ProviderStatus
import com.altrex.mobile.ui.AltrexButton
import com.altrex.mobile.ui.AltrexIconButton
import com.altrex.mobile.ui.AltrexTextField
import com.altrex.mobile.ui.Divider
import com.altrex.mobile.ui.SectionHeader
import com.altrex.mobile.ui.StatusBadge
import com.altrex.mobile.ui.AltrexStatus
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Lightweight view of a provider entry used by the dialog. Decouples the UI
 * from the registry's concrete type so the screen stays testable.
 */
data class ProviderCardData(
    val id: String,
    val name: String,
    val section: ProviderSection,
    val status: ProviderStatus,
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    val availableModels: List<String>
)

enum class ProviderSection { RECOMMENDED, ADDITIONAL, ADVANCED }

/**
 * Full-screen provider management dialog. Lists provider cards grouped into
 * sections (Recommended / Additional / Advanced), each with a status badge and
 * a config form (API key, model, base URL), plus Test connection and
 * Connect/Disconnect actions for the selected provider.
 */
@Composable
fun ProviderDialog(
    providers: List<ProviderCardData>,
    selectedProviderId: String?,
    onProviderSelected: (String) -> Unit,
    onApiKeyChange: (id: String, key: String) -> Unit,
    onModelChange: (id: String, model: String) -> Unit,
    onBaseUrlChange: (id: String, baseUrl: String) -> Unit,
    onTestConnection: (id: String) -> Unit,
    onConnect: (id: String) -> Unit,
    onDisconnect: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AltrexColors.bgApp.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Providers",
                            color = AltrexColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Connect and configure AI providers",
                            color = AltrexColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    AltrexIconButton(
                        onClick = onDismiss,
                        icon = Icons.Outlined.Close,
                        contentDescription = "Close"
                    )
                }
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ProviderSection.entries.forEach { section ->
                        val items = providers.filter { it.section == section }
                        if (items.isEmpty()) return@forEach
                        item {
                            SectionHeader(title = sectionTitle(section))
                        }
                        items(items = items, key = { it.id }) { provider ->
                            ProviderCard(
                                data = provider,
                                selected = provider.id == selectedProviderId,
                                onSelect = { onProviderSelected(provider.id) },
                                onApiKeyChange = { onApiKeyChange(provider.id, it) },
                                onModelChange = { onModelChange(provider.id, it) },
                                onBaseUrlChange = { onBaseUrlChange(provider.id, it) },
                                onTestConnection = { onTestConnection(provider.id) },
                                onConnect = { onConnect(provider.id) },
                                onDisconnect = { onDisconnect(provider.id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AltrexButton("Done", onClick = onDismiss, primary = false)
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    data: ProviderCardData,
    selected: Boolean,
    onSelect: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = data.status == ProviderStatus.CONNECTED
    val testing = data.status == ProviderStatus.TESTING
    val borderColor = if (selected) AltrexColors.accent else AltrexColors.borderSubtle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        // Card header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(AltrexColors.bgApp, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        providerIcon(data.name),
                        contentDescription = null,
                        tint = AltrexColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    data.name,
                    color = AltrexColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusBadge(
                label = data.status.name.lowercase().replaceFirstChar { it.uppercase() },
                status = when (data.status) {
                    ProviderStatus.CONNECTED -> AltrexStatus.SUCCESS
                    ProviderStatus.TESTING -> AltrexStatus.WARNING
                    ProviderStatus.ERROR -> AltrexStatus.DANGER
                    else -> AltrexStatus.NEUTRAL
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Config form
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AltrexTextField(
                value = data.apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "API key",
                leadingIcon = Icons.Outlined.Key
            )
            AltrexTextField(
                value = data.model,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Model (e.g. gpt-4o, claude-opus)",
                leadingIcon = Icons.Outlined.Memory
            )
            AltrexTextField(
                value = data.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "https://api.example.com/v1",
                leadingIcon = Icons.Outlined.Cloud
            )
        }

        Spacer(Modifier.height(12.dp))
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AltrexButton(
                text = if (testing) "Testing…" else "Test connection",
                onClick = onTestConnection,
                loading = testing,
                primary = false,
                icon = Icons.Outlined.Bolt
            )
            Spacer(Modifier.weight(1f))
            if (connected) {
                AltrexButton(
                    text = "Disconnect",
                    onClick = onDisconnect,
                    primary = false
                )
            } else {
                AltrexButton(
                    text = "Connect",
                    onClick = onConnect,
                    primary = true
                )
            }
        }
    }
}

private fun sectionTitle(section: ProviderSection): String = when (section) {
    ProviderSection.RECOMMENDED -> "Recommended"
    ProviderSection.ADDITIONAL -> "Additional"
    ProviderSection.ADVANCED -> "Advanced"
}

private fun providerIcon(name: String): ImageVector = when {
    name.contains("altrex", ignoreCase = true) -> Icons.Outlined.Bolt
    name.contains("anthropic", ignoreCase = true) || name.contains("claude", ignoreCase = true) -> Icons.Outlined.Memory
    name.contains("openai", ignoreCase = true) || name.contains("gpt", ignoreCase = true) -> Icons.Outlined.Cloud
    else -> Icons.Outlined.Explore
}
