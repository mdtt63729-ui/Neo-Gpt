package com.altrex.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Primary brand button for the ALTREX CODE app. Uses the warm neutral charcoal
 * palette and supports a leading icon plus optional loading state.
 */
@Composable
fun AltrexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    primary: Boolean = true
) {
    val container = if (primary) AltrexColors.accent else AltrexColors.bgSurface
    val content = if (primary) AltrexColors.bgApp else AltrexColors.textPrimary
    val borderColor = if (primary) Color.Transparent else AltrexColors.borderStrong

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = AltrexColors.bgSurfaceHover,
            disabledContentColor = AltrexColors.textMuted
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = content
            )
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Subtle square icon button with hover highlight, matching the charcoal surface.
 */
@Composable
fun AltrexIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Outlined.Settings,
    contentDescription: String? = null,
    tint: Color = AltrexColors.textSecondary,
    size: Int = 32
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val bg = when {
        pressed -> AltrexColors.bgSurfaceHover
        hovered -> AltrexColors.bgSurfaceHover
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(size.dp)
            .background(bg, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else AltrexColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Outlined text field styled to the warm charcoal input surface.
 */
@Composable
fun AltrexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String = "",
    singleLine: Boolean = false,
    isError: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = TextStyle(
        color = AltrexColors.textPrimary,
        fontSize = 14.sp,
        fontFamily = FontFamily.Default
    )
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = isError,
        placeholder = {
            Text(placeholder, color = AltrexColors.textMuted, fontSize = 14.sp)
        },
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = AltrexColors.textMuted) }
        } else null,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AltrexColors.bgInput,
            unfocusedContainerColor = AltrexColors.bgInput,
            disabledContainerColor = AltrexColors.bgSurface,
            errorContainerColor = AltrexColors.bgInput,
            focusedTextColor = AltrexColors.textPrimary,
            unfocusedTextColor = AltrexColors.textPrimary,
            disabledTextColor = AltrexColors.textMuted,
            focusedBorderColor = AltrexColors.borderStrong,
            unfocusedBorderColor = AltrexColors.borderSubtle,
            disabledBorderColor = AltrexColors.borderSubtle,
            errorBorderColor = AltrexColors.danger,
            focusedPlaceholderColor = AltrexColors.textMuted,
            unfocusedPlaceholderColor = AltrexColors.textMuted
        )
    )
}

/**
 * Small status pill used to convey provider/connection/run states.
 */
@Composable
fun StatusBadge(
    label: String,
    status: AltrexStatus = AltrexStatus.NEUTRAL,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (bg, fg) = when (status) {
        AltrexStatus.SUCCESS -> AltrexColors.success to AltrexColors.bgApp
        AltrexStatus.WARNING -> AltrexColors.warning to AltrexColors.bgApp
        AltrexStatus.DANGER -> AltrexColors.danger to AltrexColors.bgApp
        AltrexStatus.ACTIVE -> AltrexColors.activeBg to AltrexColors.accent
        AltrexStatus.NEUTRAL -> AltrexColors.bgSurfaceHover to AltrexColors.textSecondary
    }
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, fg.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        }
        Text(
            label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class AltrexStatus { SUCCESS, WARNING, DANGER, ACTIVE, NEUTRAL }

/**
 * Section header with a label and optional trailing action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = AltrexColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        trailing?.invoke()
    }
}

/**
 * Thin divider using the subtle border tone.
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = if (strong) AltrexColors.borderStrong else AltrexColors.borderSubtle
    )
}

/**
 * Draws a thin hover-aware vertical scrollbar over a LazyColumn-like scroll area.
 * Returns a modifier that should be applied to the scrolling container.
 */
fun ScrollbarModifier(
    state: LazyListState,
    modifier: Modifier = Modifier
): Modifier {
    return modifier.drawWithContent {
        drawContent()
        val firstVisible = state.firstVisibleItemIndex
        val total = state.layoutInfo.totalItemsCount
        if (total <= 0) return@drawWithContent
        val visible = state.layoutInfo.visibleItemsInfo.size
        if (visible >= total) return@drawWithContent
        val scrollFraction = firstVisible.toFloat() / (total - visible).coerceAtLeast(1)
        val barHeight = (visible.toFloat() / total) * size.height
        val barTop = scrollFraction * (size.height - barHeight)
        drawRoundRect(
            color = AltrexColors.borderStrong,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - 3f, barTop),
            size = androidx.compose.ui.geometry.Size(3f, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
    }
}
