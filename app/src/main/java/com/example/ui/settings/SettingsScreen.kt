package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val selectedFont by viewModel.selectedFont.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    
    var showFontDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp).background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color.DarkGray else Color(0xFFF5F5F5), CircleShape).size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF2994A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DM", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .background(Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DHUN Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            item {
                SectionHeader("My Neo Gpt")
                SettingsGroup {
                    SettingsRow(icon = Icons.Outlined.Face, title = "Personalization")
                    SettingsRow(icon = Icons.Outlined.MenuBook, title = "Memory")
                    SettingsRow(icon = Icons.Outlined.Extension, title = "Plugins", showDivider = false)
                }
            }

            item {
                SectionHeader("Account")
                SettingsGroup {
                    SettingsRow(icon = Icons.Outlined.WorkOutline, title = "Workspace", subtitle = "Personal")
                    SettingsRow(icon = Icons.Outlined.StarOutline, title = "Upgrade plan", titleColor = Color(0xFF1A73E8))
                    SettingsRow(icon = Icons.Outlined.Assessment, title = "Usage and limits")
                    SettingsRow(icon = Icons.Outlined.FamilyRestroom, title = "Parental controls")
                    SettingsRow(icon = Icons.Outlined.Email, title = "Email", subtitle = loggedInUser ?: "dhunmusic521@gmail.com", showDivider = false)
                }
            }
            
            item {
                SectionHeader("App Settings")
                SettingsGroup {
                    SettingsRow(icon = Icons.Outlined.FontDownload, title = "Font Style", subtitle = selectedFont, onClick = { showFontDialog = true })
                    SettingsRow(icon = Icons.Outlined.FormatSize, title = "Text Size", subtitle = "${textSize.toInt()}sp", onClick = { showTextSizeDialog = true })
                    val themeName = when (themeMode) {
                        1 -> "Light"
                        2 -> "Dark"
                        else -> "System Default"
                    }
                    SettingsRow(icon = Icons.Outlined.LightMode, title = "Appearance", subtitle = themeName, onClick = { showThemeDialog = true }, showDivider = false)
                }
            }
            
            item {
                SectionHeader("API Providers")
                val openRouterKey by viewModel.openRouterKey.collectAsState()
                val nvidiaKey by viewModel.nvidiaKey.collectAsState()
                val geminiKey by viewModel.geminiKey.collectAsState()

                SettingsGroup {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = openRouterKey,
                            onValueChange = { viewModel.setOpenRouterKey(it) },
                            label = { Text("OpenRouter API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A73E8),
                                focusedLabelColor = Color(0xFF1A73E8),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nvidiaKey,
                            onValueChange = { viewModel.setNvidiaKey(it) },
                            label = { Text("Nvidia NIM API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A73E8),
                                focusedLabelColor = Color(0xFF1A73E8),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { viewModel.setGeminiKey(it) },
                            label = { Text("Gemini API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A73E8),
                                focusedLabelColor = Color(0xFF1A73E8),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }
            }

            item {
                SettingsGroup(modifier = Modifier.padding(top = 24.dp)) {
                    SettingsRow(icon = Icons.Outlined.Settings, title = "General")
                    SettingsRow(icon = Icons.Outlined.Notifications, title = "Notifications")
                    SettingsRow(icon = Icons.Outlined.GraphicEq, title = "Voice")
                    SettingsRow(icon = Icons.Outlined.GppGood, title = "Safety", showDivider = false)
                }
            }

            item {
                SettingsGroup(modifier = Modifier.padding(top = 24.dp)) {
                    SettingsRow(icon = Icons.Outlined.Logout, title = "Log out", titleColor = Color.Red, showDivider = false, onClick = onLogout)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Select Font") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            viewModel.setFont("Inter")
                            showFontDialog = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inter", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        if (selectedFont == "Inter") Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1A73E8))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            viewModel.setFont("Josefin Sans")
                            showFontDialog = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Josefin Sans", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        if (selectedFont == "Josefin Sans") Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1A73E8))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Close", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showTextSizeDialog) {
        AlertDialog(
            onDismissRequest = { showTextSizeDialog = false },
            title = { Text("Select Text Size") },
            text = {
                Column {
                    listOf(14f, 16f, 18f, 20f).forEach { size ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setTextSize(size)
                                showTextSizeDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${size.toInt()}sp", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (textSize == size) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1A73E8))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTextSizeDialog = false }) { Text("Close", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    val themes = listOf(0 to "System Default", 1 to "Light", 2 to "Dark")
                    themes.forEach { (mode, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (themeMode == mode) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1A73E8))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF222222) else Color(0xFFF5F5F5)),
        content = content
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = titleColor, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showDivider) {
            Divider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}
