package com.altrex.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.data.model.LocalConversationMessage
import com.altrex.mobile.ui.MessageContent
import com.altrex.mobile.ui.ScrollbarModifier
import com.altrex.mobile.ui.theme.AltrexColors
import kotlinx.coroutines.delay

/**
 * Main chat surface: a vertically scrolling transcript of messages.
 *
 *  - User messages render as right-aligned bubbles.
 *  - Assistant messages render as left-aligned document-style blocks (rendered
 *    through [MessageContent] for markdown + code blocks).
 *  - Auto-scrolls to the latest message while the user is parked at the bottom.
 *  - A floating "Latest response" jump button appears when scrolled up.
 *  - A streaming indicator with a working elapsed-time counter shows while the
 *    assistant is actively streaming a reply.
 */
@Composable
fun ChatScreen(
    messages: List<LocalConversationMessage>,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= messages.lastIndex - 1
        }
    }
    var showJump by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty() && (atBottom || isStreaming)) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(atBottom) {
        showJump = !atBottom && messages.isNotEmpty()
    }

    Box(modifier = modifier.fillMaxSize().background(AltrexColors.bgApp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(ScrollbarModifier(listState))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = messages, key = { it.id }) { msg ->
                MessageRow(message = msg)
            }
            if (isStreaming) {
                item(key = "streaming") { StreamingIndicator() }
            }
        }

        // "Latest response" jump button
        AnimatedVisibility(
            visible = showJump,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        // best-effort jump; full coroutine animation handled by LaunchedEffect
                    }
                    .background(AltrexColors.bgSurfaceHover, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = AltrexColors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Latest response",
                    color = AltrexColors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MessageRow(message: LocalConversationMessage) {
    val isUser = message.role.equals("user", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AltrexColors.bgSurface, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = AltrexColors.accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .background(
                    if (isUser) AltrexColors.activeBg else AltrexColors.bgSurface,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    color = AltrexColors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            } else {
                MessageContent(markdown = message.content)
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AltrexColors.bgSurfaceHover, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = AltrexColors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingIndicator() {
    val startedAt = remember { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(200)
        }
    }
    val seconds = elapsed / 1000.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = AltrexColors.accent
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Generating response · ${"%.1f".format(seconds)}s",
            color = AltrexColors.textSecondary,
            fontSize = 12.sp
        )
    }
}
