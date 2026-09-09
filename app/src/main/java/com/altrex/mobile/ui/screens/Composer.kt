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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.AltrexIconButton
import com.altrex.mobile.ui.AltrexTextField
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Chat interaction mode. ASK is single-shot Q&A, AGENT runs the agentic loop,
 * MULTI drives the multi-AI orchestrator.
 */
enum class ComposerMode { ASK, AGENT, MULTI }

/**
 * Bottom-docked message composer. Multiline input where Enter sends and
 * Shift+Enter inserts a newline. Includes a mode selector (ASK/AGENT/MULTI),
 * a model selector, an attach button, and a send/stop button that toggles
 * based on whether a response is currently streaming.
 */
@Composable
fun Composer(
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    mode: ComposerMode = ComposerMode.ASK,
    onModeChange: (ComposerMode) -> Unit = {},
    models: List<String> = listOf("altrex-pro", "altrex-flash"),
    selectedModel: String = models.firstOrNull().orEmpty(),
    onModelChange: (String) -> Unit = {},
    onAttach: () -> Unit = {}
) {
    var draft by remember { mutableStateOf("") }
    val canSend = draft.isNotBlank() && !isStreaming

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AltrexColors.bgApp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Composer surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AltrexColors.bgInput, RoundedCornerShape(10.dp))
                .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AltrexIconButton(
                onClick = onAttach,
                icon = Icons.Outlined.AttachFile,
                contentDescription = "Attach file",
                tint = AltrexColors.textSecondary
            )

            AltrexTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 200.dp)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            if (!event.isShiftPressed && draft.isNotBlank()) {
                                onSend(draft.trim())
                                draft = ""
                                true
                            } else {
                                false // let newline through
                            }
                        } else {
                            false
                        }
                    },
                placeholder = if (isStreaming) "Generating…" else "Message ALTREX CODE…  (Enter to send, Shift+Enter for newline)",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(),
                singleLine = false
            )

            // Send / Stop button
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val bg = if (isStreaming) AltrexColors.danger else if (canSend) AltrexColors.accent else AltrexColors.bgSurfaceHover
            val fg = if (isStreaming || canSend) AltrexColors.bgApp else AltrexColors.textMuted
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bg, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = isStreaming || canSend
                    ) {
                        if (isStreaming) {
                            onStop()
                        } else if (canSend) {
                            onSend(draft.trim())
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isStreaming) Icons.Outlined.Stop else Icons.Outlined.Send,
                    contentDescription = if (isStreaming) "Stop" else "Send",
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Controls row: mode selector + model selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode selector
            Row(
                modifier = Modifier
                    .background(AltrexColors.bgInput, RoundedCornerShape(8.dp))
                    .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ComposerMode.entries.forEach { m ->
                    ModeChip(
                        label = m.name,
                        selected = m == mode,
                        onClick = { onModeChange(m) }
                    )
                }
            }

            // Model selector (simple pill toggling through models)
            if (models.isNotEmpty()) {
                ModelSelector(
                    models = models,
                    selected = selectedModel,
                    onSelect = onModelChange
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 26.dp)
            .background(
                if (selected) AltrexColors.activeBg else if (hovered) AltrexColors.bgSurfaceHover else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AltrexColors.accent else AltrexColors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ModelSelector(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .background(
                if (hovered) AltrexColors.bgSurfaceHover else AltrexColors.bgInput,
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(AltrexColors.accent, RoundedCornerShape(4.dp))
        )
        Text(selected, color = AltrexColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Icon(
            androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = AltrexColors.textMuted,
            modifier = Modifier.size(14.dp)
        )
    }
    // Simple dropdown rendered inline below selector
    if (expanded) {
        Column(
            modifier = Modifier
                .width(160.dp)
                .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
                .border(1.dp, AltrexColors.borderStrong, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            models.forEach { m ->
                val isSel = m == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSel) AltrexColors.activeBg else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onSelect(m)
                            expanded = false
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(m, color = if (isSel) AltrexColors.accent else AltrexColors.textPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

// Local Color alias to avoid extra import churn in this file
private val Color = androidx.compose.ui.graphics.Color
