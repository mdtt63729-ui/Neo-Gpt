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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Welcome / empty-state screen. Shows the ALTREX CODE wordmark centered, a
 * tagline, and four suggestion cards that each emit a preset prompt to the
 * composer via [onSuggestionSelected].
 */
@Composable
fun HomeScreen(
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AltrexColors.bgApp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ALTREX CODE",
                    color = AltrexColors.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "A warm, deliberate workspace for shipping code with AI.",
                    color = AltrexColors.textSecondary,
                    fontSize = 14.sp,
                    letterSpacing = 0.3.sp
                )
            }

            SuggestionGrid(onSuggestionSelected = onSuggestionSelected)
        }
    }
}

@Composable
private fun SuggestionGrid(onSuggestionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val cards = listOf(
            Suggestion("Explore", "Explain this codebase and its key modules", Icons.Outlined.Explore),
            Suggestion("Build", "Implement the next feature from the spec", Icons.Outlined.Build),
            Suggestion("Review", "Review my recent changes for issues", Icons.Outlined.RateReview),
            Suggestion("Fix", "Find and fix the failing test", Icons.Outlined.AutoFixHigh)
        )
        cards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { card ->
                    SuggestionCard(card, modifier = Modifier.weight(1f)) {
                        onSuggestionSelected(promptFor(card.title))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    card: Suggestion,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .height(92.dp)
            .background(
                if (hovered) AltrexColors.bgSurfaceHover else AltrexColors.bgSurface,
                RoundedCornerShape(10.dp)
            )
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(AltrexColors.bgApp, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(card.icon, contentDescription = null, tint = AltrexColors.accent, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.title, color = AltrexColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(card.description, color = AltrexColors.textMuted, fontSize = 12.sp, lineHeight = 15.sp)
        }
    }
}

private data class Suggestion(val title: String, val description: String, val icon: ImageVector)

private fun promptFor(title: String): String = when (title) {
    "Explore" -> "Explore this codebase: summarize the architecture and the key modules."
    "Build" -> "Build the next feature described in the project spec, step by step."
    "Review" -> "Review my recent changes and flag any bugs, smells, or risks."
    "Fix" -> "Find and fix the failing test. Explain the root cause before patching."
    else -> title
}
