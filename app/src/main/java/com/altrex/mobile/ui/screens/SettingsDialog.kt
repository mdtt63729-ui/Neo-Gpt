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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.altrex.mobile.data.model.ProviderStatus
import com.altrex.mobile.ui.AltrexButton
import com.altrex.mobile.ui.AltrexIconButton
import com.altrex.mobile.ui.Divider
import com.altrex.mobile.ui.SectionHeader
import com.altrex.mobile.ui.StatusBadge
import com.altrex.mobile.ui.AltrexStatus
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Simple provider profile entry surfaced in the AI & Models tab.
 */
data class SettingsProviderProfile(
    val id: String,
    val name: String,
    val status: ProviderStatus,
    val model: String
)

/**
 * A capability / permission row surfaced in the Permissions tab.
 */
data class CapabilityRow(val label: String, val granted: Boolean, val detail: String)

private enum class SettingsTab(val label: String) { AI("AI & Models"), APPEARANCE("Appearance"), PERMISSIONS("Permissions") }

/**
 * Application settings dialog with three tabs.
 *
 *  - AI & Models: provider profiles, active model selection, diagnostics readout.
 *  - Appearance: theme information (warm neutral charcoal, read-only).
 *  - Permissions: runtime permission / capability status.
 */
@Composable
fun SettingsDialog(
    providerProfiles: List<SettingsProviderProfile>,
    activeProviderId: String?,
    availableModels: List<String>,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onActivateProvider: (String) -> Unit,
    diagnostics: List<Pair<String, String>>,
    capabilities: List<CapabilityRow>,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(SettingsTab.AI) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(AltrexColors.bgApp.copy(alpha = 0.96f))) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .widthIn(max = 760.dp)
                    .align(Alignment.Center)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Settings",
                        color = AltrexColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AltrexIconButton(onClick = onDismiss, icon = Icons.Outlined.Close, contentDescription = "Close")
                }
                Spacer(Modifier.height(16.dp))

                // Tab bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsTab.entries.forEach { t ->
                        TabChip(t, selected = t == tab) { tab = t }
                    }
                }
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (tab) {
                        SettingsTab.AI -> {
                            item { SectionHeader(title = "Provider profiles") }
                            items(providerProfiles, key = { it.id }) { p ->
                                ProviderProfileRow(
                                    profile = p,
                                    active = p.id == activeProviderId,
                                    onActivate = { onActivateProvider(p.id) }
                                )
                            }
                            item {
                                SectionHeader(title = "Model")
                                ModelPickerRow(availableModels, selectedModel, onModelSelected)
                            }
                            item {
                                SectionHeader(title = "Diagnostics")
                                DiagnosticsCard(diagnostics)
                            }
                        }
                        SettingsTab.APPEARANCE -> {
                            item { AppearanceCard() }
                        }
                        SettingsTab.PERMISSIONS -> {
                            item { SectionHeader(title = "Permissions & capabilities") }
                            items(capabilities, key = { it.label }) { c -> CapabilityCard(c) }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AltrexButton("Done", onClick = onDismiss, primary = false)
                }
            }
        }
    }
}

@Composable
private fun TabChip(tab: SettingsTab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .background(
                if (selected) AltrexColors.activeBg else if (hovered) AltrexColors.bgSurfaceHover else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tab.label,
            color = if (selected) AltrexColors.accent else AltrexColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProviderProfileRow(
    profile: SettingsProviderProfile,
    active: Boolean,
    onActivate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, if (active) AltrexColors.accent else AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Memory, contentDescription = null, tint = AltrexColors.accent, modifier = Modifier.size(16.dp))
            Column {
                Text(profile.name, color = AltrexColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(profile.model, color = AltrexColors.textMuted, fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                label = profile.status.name.lowercase().replaceFirstChar { it.uppercase() },
                status = when (profile.status) {
                    ProviderStatus.CONNECTED -> AltrexStatus.SUCCESS
                    ProviderStatus.TESTING -> AltrexStatus.WARNING
                    ProviderStatus.ERROR -> AltrexStatus.DANGER
                    else -> AltrexStatus.NEUTRAL
                }
            )
            if (!active) {
                AltrexButton("Activate", onClick = onActivate, primary = false)
            } else {
                StatusBadge("Active", AltrexStatus.ACTIVE, icon = Icons.Outlined.CheckCircle)
            }
        }
    }
}

@Composable
private fun ModelPickerRow(models: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        models.forEach { m ->
            val sel = m == selected
            Box(
                modifier = Modifier
                    .background(
                        if (sel) AltrexColors.activeBg else AltrexColors.bgSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, if (sel) AltrexColors.accent else AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onSelect(m) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    m,
                    color = if (sel) AltrexColors.accent else AltrexColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(diagnostics: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        diagnostics.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, color = AltrexColors.textMuted, fontSize = 12.sp)
                Text(v, color = AltrexColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AppearanceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Palette, contentDescription = null, tint = AltrexColors.accent, modifier = Modifier.size(20.dp))
            Column {
                Text("Warm Neutral Charcoal", color = AltrexColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("The single built-in theme. Dark, warm neutrals with a soft accent.", color = AltrexColors.textSecondary, fontSize = 12.sp)
            }
        }
        Divider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SwatchRow("bg.app", AltrexColors.bgApp)
            SwatchRow("bg.surface", AltrexColors.bgSurface)
            SwatchRow("accent", AltrexColors.accent)
            SwatchRow("success", AltrexColors.success)
            SwatchRow("warning", AltrexColors.warning)
            SwatchRow("danger", AltrexColors.danger)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = AltrexColors.textMuted, modifier = Modifier.size(14.dp))
            Text("Additional themes are not available in this build.", color = AltrexColors.textMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SwatchRow(name: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, AltrexColors.borderStrong, RoundedCornerShape(4.dp))
        )
        Text(name, color = AltrexColors.textSecondary, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun CapabilityCard(cap: CapabilityRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = AltrexColors.textSecondary, modifier = Modifier.size(16.dp))
            Column {
                Text(cap.label, color = AltrexColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(cap.detail, color = AltrexColors.textMuted, fontSize = 11.sp)
            }
        }
        StatusBadge(
            label = if (cap.granted) "Granted" else "Not granted",
            status = if (cap.granted) AltrexStatus.SUCCESS else AltrexStatus.WARNING,
            icon = if (cap.granted) Icons.Outlined.CheckCircle else Icons.Outlined.Devices
        )
    }
}
