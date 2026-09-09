package com.altrex.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.AltrexIconButton
import com.altrex.mobile.ui.StatusBadge
import com.altrex.mobile.ui.AltrexStatus
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Slim 48dp top application bar. Shows the active workspace/project name on the
 * left (with a folder glyph), the current interaction mode indicator, a
 * provider connection status badge, and action buttons (provider manager,
 * settings) on the right.
 */
@Composable
fun Topbar(
    workspaceName: String,
    projectName: String,
    mode: ComposerMode,
    providerConnected: Boolean,
    providerName: String,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AltrexColors.bgSidebar)
            .border(width = 1.dp, color = AltrexColors.borderSubtle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: workspace / project
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                tint = AltrexColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                workspaceName,
                color = AltrexColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "/",
                color = AltrexColors.textMuted,
                fontSize = 13.sp
            )
            Text(
                projectName,
                color = AltrexColors.textSecondary,
                fontSize = 13.sp
            )
        }

        // Right: mode indicator + provider status + actions
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeIndicator(mode)
            StatusBadge(
                label = if (providerConnected) providerName else "Offline",
                status = if (providerConnected) AltrexStatus.SUCCESS else AltrexStatus.DANGER,
                icon = if (providerConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff
            )
            Spacer(Modifier.width(4.dp))
            AltrexIconButton(
                onClick = onOpenProviders,
                icon = Icons.Outlined.Tune,
                contentDescription = "Manage providers"
            )
            AltrexIconButton(
                onClick = onOpenSettings,
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
private fun ModeIndicator(mode: ComposerMode) {
    Row(
        modifier = Modifier
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(AltrexColors.accent, RoundedCornerShape(3.dp))
        )
        Text(
            mode.name,
            color = AltrexColors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}
