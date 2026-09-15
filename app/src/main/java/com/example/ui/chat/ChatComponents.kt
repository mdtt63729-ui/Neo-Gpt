package com.example.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Attachment
import com.example.data.AttachmentType
import com.example.data.Message

@Composable
fun MessageBubble(message: Message, textSize: Float = 16f) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isUser) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (!message.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Attached image",
                            modifier = Modifier.sizeIn(maxWidth = 240.dp, maxHeight = 240.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(message.text, fontSize = textSize.sp)
                    if (message.attachmentName != null && message.imageUrl == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "📎 ${message.attachmentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                if (message.text.isNotBlank()) {
                    Text(message.text, fontSize = textSize.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                if (!message.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Generated image",
                        modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ContentCopy, "Copy") }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ThumbUp, "Good response") }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ThumbDown, "Bad response") }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.VolumeUp, "Read aloud") }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Share, "Share") }
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.MoreVert, "More") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Copy") }, onClick = { expanded = false })
                        DropdownMenuItem(text = { Text("Share") }, onClick = { expanded = false })
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentPreview(attachment: Attachment, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (attachment.type == AttachmentType.IMAGE) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = "Selected image",
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FILE", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(attachment.fileName, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${attachment.mimeType.ifBlank { "File" }} • ${formatBytes(attachment.sizeBytes)}",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove attachment") }
        }
    }
}

private fun formatBytes(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> "${size / (1024 * 1024)} MB"
}

@Composable
fun ChatInputBar(
    attachments: List<Attachment>,
    requestState: ChatRequestState,
    onSendMessage: (String) -> Unit,
    onStop: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val active = requestState is ChatRequestState.Thinking || requestState is ChatRequestState.Streaming
    val canSend = text.isNotBlank() || attachments.isNotEmpty()
    val action = when {
        active -> ComposerAction.STOP
        canSend -> ComposerAction.SEND
        else -> ComposerAction.CONVERSATION
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        attachments.forEach { attachment ->
            AttachmentPreview(attachment, onRemove = { onRemoveAttachment(attachment) })
            Spacer(modifier = Modifier.height(6.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = onAttachClick, enabled = !active) {
                    Icon(Icons.Default.Add, "Attach")
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Neo Gpt") },
                    minLines = 1,
                    maxLines = 5,
                    enabled = !active,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                AnimatedContent(
                    targetState = action,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 2 } + scaleIn(initialScale = 0.82f)) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.82f)) using
                            SizeTransform(clip = false)
                    },
                    label = "composer-action"
                ) { current ->
                    FilledIconButton(
                        onClick = when (current) {
                            ComposerAction.STOP -> onStop
                            ComposerAction.SEND -> {
                                {
                                    val outgoing = text
                                    text = ""
                                    onSendMessage(outgoing)
                                }
                            }
                            ComposerAction.CONVERSATION -> onVoiceClick
                        },
                        modifier = Modifier.size(44.dp),
                        enabled = true
                    ) {
                        Icon(
                            when (current) {
                                ComposerAction.STOP -> Icons.Default.Stop
                                ComposerAction.SEND -> Icons.Default.Send
                                ComposerAction.CONVERSATION -> Icons.Outlined.VolumeUp
                            },
                            contentDescription = when (current) {
                                ComposerAction.STOP -> "Stop generation"
                                ComposerAction.SEND -> "Send message"
                                ComposerAction.CONVERSATION -> "Voice conversation"
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class ComposerAction { CONVERSATION, SEND, STOP }
