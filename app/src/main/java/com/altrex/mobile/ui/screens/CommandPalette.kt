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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.altrex.mobile.data.model.SavedConversation
import com.altrex.mobile.ui.AltrexTextField
import com.altrex.mobile.ui.Divider
import com.altrex.mobile.ui.SectionHeader
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Search overlay for navigating conversations and triggering quick actions.
 *
 * Renders a centered modal with an autofocus search input, a "New chat" action,
 * a filtered list of conversations matching the query, and full keyboard
 * navigation (Up/Down to move selection, Enter to activate, Esc to dismiss).
 */
@Composable
fun CommandPalette(
    conversations: List<SavedConversation>,
    onConversationSelected: (SavedConversation) -> Unit,
    onNewChat: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(conversations, query) {
        if (query.isBlank()) conversations
        else conversations.filter { it.title.contains(query, ignoreCase = true) }
    }
    // Row 0 = New chat; rows 1..n = filtered conversations.
    val totalCount = filtered.size + 1

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(query) { selectedIndex = 0 }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AltrexColors.bgApp.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 96.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .background(AltrexColors.bgSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, AltrexColors.borderStrong, RoundedCornerShape(10.dp))
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                        when (event.key) {
                            Key.Escape -> { onDismiss(); true }
                            Key.DirectionDown -> {
                                selectedIndex = (selectedIndex + 1).coerceAtMost(totalCount - 1); true
                            }
                            Key.DirectionUp -> {
                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0); true
                            }
                            Key.Enter -> {
                                when (selectedIndex) {
                                    0 -> { onNewChat() }
                                    else -> {
                                        val convo = filtered.getOrNull(selectedIndex - 1)
                                        if (convo != null) onConversationSelected(convo)
                                    }
                                }
                                onDismiss(); true
                            }
                            else -> false
                        }
                    }
            ) {
                // Search input
                AltrexTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = "Search conversations or type a command…",
                    leadingIcon = Icons.Outlined.Search
                )
                Divider()

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item {
                        PaletteRow(
                            icon = Icons.Outlined.Add,
                            title = "New chat",
                            subtitle = "Start a fresh conversation",
                            selected = selectedIndex == 0,
                            onClick = { onNewChat(); onDismiss() }
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                if (query.isBlank()) "No recent conversations"
                                else "No conversations match \"$query\"",
                                color = AltrexColors.textMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    } else {
                        item {
                            SectionHeader(title = "Recent", modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                    itemsIndexed(filtered) { index, convo ->
                        val realIndex = index + 1
                        PaletteRow(
                            icon = Icons.Outlined.Chat,
                            title = convo.title.ifBlank { "Untitled" },
                            subtitle = convo.project ?: "No project",
                            selected = selectedIndex == realIndex,
                            onClick = { onConversationSelected(convo); onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val highlight = selected || hovered
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlight) AltrexColors.bgSurfaceHover else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AltrexColors.textSecondary, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (highlight) AltrexColors.textPrimary else AltrexColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = AltrexColors.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
