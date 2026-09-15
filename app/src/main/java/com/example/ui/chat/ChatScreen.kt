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
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.ArrowDropDown
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
    selectedModel: String,
    onModelChange: (String) -> Unit,
    availableModels: List<String>,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }
    
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
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { showModelSelector = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(selectedModel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                            }
                            
                            DropdownMenu(
                                expanded = showModelSelector,
                                onDismissRequest = { showModelSelector = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            onModelChange(model)
                                            showModelSelector = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Incognito */ }) {
                            Icon(Icons.Outlined.VisibilityOff, contentDescription = "Incognito")
                        }
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
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("How can I help you today?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else {
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
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                    ThinkingAnimation()
                                }
                            }
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

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.sendMessage("Image attached: $uri") // Simplistic representation for now
        }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.sendMessage("File attached: $uri")
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            viewModel.sendMessage("Camera photo attached")
        }
    }

    if (showAttachmentMenu) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentMenu = false },
            onPhotoClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onFileClick = { filePickerLauncher.launch("*/*") },
            onCameraClick = { cameraLauncher.launch(null) }
        )
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

@Composable
fun ThinkingAnimation() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).scale(scale).background(Color(0xFF1A73E8), CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Thinking...", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
