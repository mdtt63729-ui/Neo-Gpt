package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Message
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    textSize: Float,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val voiceHandler = remember { VoiceHandler(context) }
    var isListening by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
        voiceHandler.onListeningStatusChanged = { isListening = it }
        voiceHandler.onSpeechRecognized = { recognizedText ->
            viewModel.sendMessage(recognizedText)
            showVoiceOverlay = false
        }
        onDispose { voiceHandler.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceOverlay = true
            voiceHandler.startListening()
        }
    }
    
    // Auto-speak new AI messages
    LaunchedEffect(messages) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser && lastMessage.text.isNotBlank()) {
            voiceHandler.speak(lastMessage.text)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                DrawerContent(
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.3f)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFE8F0FE))
                                .clickable { }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Plus", color = Color(0xFF1A73E8), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* New Chat */ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "New Chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = false
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message, textSize = textSize)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (isLoading) {
                        item {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
                
                ChatInputBar(
                    onSendMessage = { viewModel.sendMessage(it) },
                    onAttachClick = { showAttachmentMenu = true },
                    onVoiceClick = { 
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    if (showAttachmentMenu) {
        AttachmentBottomSheet(onDismiss = { showAttachmentMenu = false })
    }

    if (showVoiceOverlay) {
        VoiceOverlay(
            onDismiss = {
                voiceHandler.stopListening()
                showVoiceOverlay = false
            },
            isListening = isListening
        )
    }
}

@Composable
fun DrawerContent(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Neo Gpt", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            IconButton(onClick = { /* Search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.background(if (isSystemInDarkTheme()) Color.DarkGray else Color(0xFFF0F0F0), CircleShape).padding(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        DrawerItem(icon = Icons.Outlined.Image, text = "Images")
        DrawerItem(icon = Icons.Outlined.LibraryBooks, text = "Library")
        DrawerItem(icon = Icons.Outlined.Folder, text = "Projects")
        DrawerItem(icon = Icons.Outlined.Schedule, text = "Scheduled")
        DrawerItem(icon = Icons.Outlined.Extension, text = "Plugins")

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
        
        Text("Greeting response", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("Project Dekho", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder skeletons
        Box(modifier = Modifier.width(150.dp).height(20.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).padding(vertical = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.width(150.dp).height(20.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).padding(vertical = 8.dp))
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat")
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF2994A), CircleShape)
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text("DM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}
