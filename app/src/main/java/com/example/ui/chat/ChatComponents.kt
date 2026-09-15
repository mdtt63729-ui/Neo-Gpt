package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
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
import com.example.data.Message

@Composable
fun MessageBubble(message: Message, textSize: Float = 16f) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isUser) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE8F0FE))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(text = message.text, fontSize = textSize.sp, color = Color.Black)
            }
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (message.text.isNotBlank()) {
                    Text(text = message.text, fontSize = textSize.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                if (message.imageUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Generated Image",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                    MessageContextMenu(expanded = expanded, onDismiss = { expanded = false })
                }
            }
        }
    }
}

@Composable
fun MessageContextMenu(expanded: Boolean, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        DropdownMenuItem(text = { Text("Share", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Pin", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Add to project", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Uploaded files", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Find in chat", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Add to home", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Archive", color = MaterialTheme.colorScheme.onBackground) }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF333333) else Color(0xFFF0F0F0), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color.DarkGray)
        }
        
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ask Neo Gpt", color = Color.Gray) },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (text.isNotBlank()) {
            Button(
                onClick = { 
                    onSendMessage(text)
                    text = "" 
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Send") // Using Add as placeholder for Send arrow
            }
        } else {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = onVoiceClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(40.dp)
            ) {
                // Using VolumeUp as placeholder for AudioLines/Conversation icon
                Icon(Icons.Outlined.VolumeUp, contentDescription = "Voice Conversation", modifier = Modifier.size(20.dp))
            }
        }
    }
}
