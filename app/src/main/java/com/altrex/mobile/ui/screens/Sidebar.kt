package com.altrex.mobile.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.data.model.SavedConversation
import com.altrex.mobile.ui.AltrexIconButton
import com.altrex.mobile.ui.Divider
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Collapsible left navigation rail.
 *
 * Expanded width is 240dp, collapsed (rail) width is 56dp, with an animated
 * width transition between the two states. Contains a New chat button at the
 * top, the recent conversations list (optionally filtered by [currentProject]),
 * and a settings button pinned to the bottom.
 */
@Composable
fun Sidebar(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    conversations: List<SavedConversation>,
    activeConversationId: String?,
    onConversationSelected: (SavedConversation) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    currentProject: String? = null
) {
    val filtered = remember(conversations, currentProject) {
        if (currentProject.isNullOrBlank()) conversations
        else conversations.filter { it.project.equals(currentProject, ignoreCase = true) }
    }

    val width by animateDpAsState(
        targetValue = if (expanded) 240.dp else 56.dp,
        animationSpec = tween(durationMillis = 220),
        label = "sidebarWidth"
    )

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(AltrexColors.bgSidebar)
            .border(width = 1.dp, color = AltrexColors.borderSubtle, shape = androidx.compose.ui.graphics.RectangleShape)
    ) {
        // Header row: New chat + collapse toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavRailButton(
                icon = Icons.Outlined.Add,
                label = "New chat",
                expanded = expanded,
                onClick = onNewChat,
                primary = true
            )
            AltrexIconButton(
                onClick = onToggleExpanded,
                icon = if (expanded) Icons.Outlined.ChevronLeft else Icons.Outlined.ChevronRight,
                contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar"
            )
        }

        if (expanded) {
            Text(
                "Recent",
                color = AltrexColors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items(items = filtered, key = { it.id }) { convo ->
                ConversationItem(
                    conversation = convo,
                    isActive = convo.id == activeConversationId,
                    expanded = expanded,
                    onClick = { onConversationSelected(convo) }
                )
            }
        }

        Divider()
        // Settings pinned to bottom
        NavRailButton(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            expanded = expanded,
            onClick = onOpenSettings,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun NavRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        hovered -> AltrexColors.bgSurfaceHover
        primary -> AltrexColors.activeBg.copy(alpha = 0.5f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val tint = if (primary) AltrexColors.accent else AltrexColors.textSecondary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        if (expanded) {
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                color = if (primary) AltrexColors.accent else AltrexColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: SavedConversation,
    isActive: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        isActive -> AltrexColors.activeBg
        hovered -> AltrexColors.bgSurfaceHover
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    if (!expanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(bg, RoundedCornerShape(8.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(AltrexColors.accent, RoundedCornerShape(2.dp))
            )
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            conversation.title.ifBlank { "Untitled" },
            color = if (isActive) AltrexColors.accent else AltrexColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        conversation.updatedAt?.let {
            Text(
                formatRelative(it),
                color = AltrexColors.textMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

private fun formatRelative(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    val mins = diff / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}
