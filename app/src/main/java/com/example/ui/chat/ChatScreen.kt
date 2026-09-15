package com.example.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.Attachment
import com.example.data.AttachmentType
import com.example.network.ModelOption
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    textSize: Float,
    selectedModel: String,
    selectedProvider: String,
    availableModels: List<ModelOption>,
    onModelChange: (ModelOption) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val requestState by viewModel.requestState.collectAsState()
    val attachments by viewModel.pendingAttachments.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val voiceHandler = remember { VoiceHandler(context) }
    DisposableEffect(Unit) {
        voiceHandler.onListeningStatusChanged = { isListening = it }
        voiceHandler.onSpeechRecognized = { recognizedText ->
            if (recognizedText.isNotBlank()) viewModel.sendMessage(recognizedText)
            showVoiceOverlay = false
        }
        onDispose { voiceHandler.destroy() }
    }

    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        if (last != null && !last.isUser && last.text.isNotBlank()) {
            voiceHandler.speak(last.text)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showVoiceOverlay = true
            voiceHandler.startListening()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addAttachment(it.toAttachment(context, AttachmentType.IMAGE)) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            viewModel.addAttachment(it.toAttachment(context, AttachmentType.FILE))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            viewModel.addAttachment(uri.toAttachment(context, AttachmentType.IMAGE))
        } else {
            runCatching { cameraFile?.delete() }
        }
        cameraUri = null
        cameraFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("neo_camera_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraFile = photoFile
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val photoFile = File.createTempFile("neo_camera_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        cameraFile = photoFile
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text("Neo Gpt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    NavigationDrawerItem(
                        label = { Text("Chat") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Outlined.ChatBubbleOutline, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        icon = { Icon(Icons.Outlined.Settings, null) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("AI chat • Android", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Open menu")
                        }
                    },
                    title = {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(enabled = requestState !is ChatRequestState.Thinking && requestState !is ChatRequestState.Streaming) { showModelSelector = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedProvider, style = MaterialTheme.typography.labelLarge)
                                Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    availableModels.firstOrNull { it.modelId == selectedModel }?.displayName ?: selectedModel,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size, requestState) {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(message, textSize)
                        }
                        if (requestState is ChatRequestState.Thinking) {
                            item { ThinkingAnimation() }
                        }
                    }
                }

                ChatInputBar(
                    attachments = attachments,
                    requestState = requestState,
                    onSendMessage = viewModel::sendMessage,
                    onStop = viewModel::stopGeneration,
                    onAttachClick = { showAttachmentMenu = true },
                    onVoiceClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            showVoiceOverlay = true
                            voiceHandler.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRemoveAttachment = { viewModel.removeAttachment(it.uri) }
                )
            }
        }
    }

    if (showAttachmentMenu) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentMenu = false },
            onPhotoClick = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onCameraClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }

    if (showModelSelector) {
        ModelSelectorDialog(
            selectedProvider = selectedProvider,
            selectedModel = selectedModel,
            models = availableModels,
            onSelect = {
                onModelChange(it)
                showModelSelector = false
            },
            onDismiss = { showModelSelector = false }
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
private fun ModelSelectorDialog(
    selectedProvider: String,
    selectedModel: String,
    models: List<ModelOption>,
    onSelect: (ModelOption) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember(selectedProvider) { mutableStateOf(selectedProvider) }
    val providers = models.map { it.provider }.distinct()
    val providerModels = models.filter { it.provider == provider }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose AI model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    providers.forEach { item ->
                        FilterChip(
                            selected = item == provider,
                            onClick = { provider = item },
                            label = { Text(item.replace("NVIDIA NIM", "NVIDIA")) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                providerModels.forEach { model ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(model) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (model.modelId == selectedModel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(model.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                if (model.modelId == selectedModel) Text("✓", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ThinkingAnimation() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "thinking-phase"
    )
    Row(
        modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale = 0.72f + (phase * (0.35f - index * 0.07f))
            Box(Modifier.size(8.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Text("Thinking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Uri.toAttachment(context: Context, type: AttachmentType): Attachment {
    val resolver = context.contentResolver
    var name = "attachment"
    var size = 0L
    resolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    val mime = resolver.getType(this) ?: if (type == AttachmentType.IMAGE) "image/*" else "application/octet-stream"
    return Attachment(this, mime, name, size, type)
}

