package com.altrex.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.theme.AltrexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A fenced code block with a language label header, a copy-to-clipboard button,
 * and horizontally scrollable monospace body text.
 */
@Composable
fun CodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AltrexColors.bgApp, RoundedCornerShape(8.dp))
            .background(AltrexColors.bgSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        // Header: language label + copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AltrexColors.bgApp.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = AltrexColors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    language.uppercase().ifEmpty { "CODE" },
                    color = AltrexColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
            AltrexIconButton(
                onClick = {
                    copyToClipboard(context, code)
                    scope.launch {
                        copied = true
                        delay(1500)
                        copied = false
                    }
                },
                icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy code",
                tint = if (copied) AltrexColors.success else AltrexColors.textSecondary,
                size = 26
            )
        }

        // Divider between header and body
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AltrexColors.borderSubtle)
        )

        // Scrollable code body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = code,
                color = AltrexColors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                softWrap = false
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Altrex code", text))
}
