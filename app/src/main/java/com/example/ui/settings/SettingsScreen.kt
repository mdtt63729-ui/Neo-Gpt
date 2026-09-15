package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.network.ProviderType
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val font by viewModel.selectedFont.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val openRouterKey by viewModel.openRouterKey.collectAsState()
    val nvidiaKey by viewModel.nvidiaKey.collectAsState()
    val geminiKey by viewModel.geminiKey.collectAsState()
    val customModels by viewModel.customModels.collectAsState()

    var showFont by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showTextSize by remember { mutableStateOf(false) }
    var showCustomModel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SectionHeader("Appearance") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.DarkMode, "Appearance", when (theme) { 1 -> "Light"; 2 -> "Dark"; else -> "System default" }) { showTheme = true }
                    SettingsRow(Icons.Outlined.TextFields, "UI Font", font) { showFont = true }
                    SettingsRow(Icons.Outlined.Tune, "Text size", "${textSize.toInt()}sp") { showTextSize = true }
                }
            }

            item { SectionHeader("AI Providers") }
            item {
                SettingsGroup {
                    ProviderKeyField("OpenRouter API Key", openRouterKey, viewModel::setOpenRouterKey)
                    ProviderKeyField("NVIDIA NIM API Key", nvidiaKey, viewModel::setNvidiaKey)
                    ProviderKeyField("Gemini API Key", geminiKey, viewModel::setGeminiKey)
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Key, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Venus 3.1", style = MaterialTheme.typography.titleMedium)
                            Text("Always available • no API key required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { SectionHeader("Custom Models") }
            item {
                SettingsGroup {
                    Button(
                        onClick = { showCustomModel = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("Add custom model") }
                    val models = parseCustomModels(customModels)
                    if (models.isEmpty()) {
                        Text("No custom models added", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        models.forEach { model ->
                            ListItem(
                                headlineContent = { Text(model.name) },
                                supportingContent = { Text("${model.provider} • ${model.id}") },
                                trailingContent = {
                                    TextButton(onClick = { viewModel.removeCustomModel(model.provider, model.id) }) { Text("Remove") }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showFont) {
        ChoiceDialog("UI Font", listOf("Inter" to "Inter", "Josefin Sans" to "Josefin Sans"), font, { viewModel.setFont(it) }) { showFont = false }
    }
    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    listOf(0 to "System default", 1 to "Light", 2 to "Dark").forEach { (value, label) ->
                        ChoiceRow(label, theme == value) { viewModel.setThemeMode(value); showTheme = false }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTheme = false }) { Text("Close") } }
        )
    }
    if (showTextSize) {
        ChoiceDialog("Text size", listOf("14" to "14sp", "16" to "16sp", "18" to "18sp", "20" to "20sp"), textSize.toInt().toString(), { viewModel.setTextSize(it.toFloat()) }) { showTextSize = false }
    }
    if (showCustomModel) {
        CustomModelDialog(
            onDismiss = { showCustomModel = false },
            onAdd = { name, id, provider, description ->
                viewModel.addCustomModel(name, id, provider, description)
                showCustomModel = false
            }
        )
    }
}

@Composable
private fun ProviderKeyField(label: String, value: String, onSave: (String) -> Unit) {
    var draft by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Hide key" else "Show key")
                }
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { draft = ""; onSave("") }) { Text("Clear") }
            Button(onClick = { onSave(draft) }, enabled = draft != value) { Text("Save") }
        }
    }
}

@Composable
private fun ChoiceDialog(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column { options.forEach { (value, label) -> ChoiceRow(label, value == selected) { onSelect(value); onDismiss() } } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label) },
        trailingContent = { if (selected) Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary) }
    )
}

@Composable
private fun CustomModelDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(ProviderType.OPENROUTER) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(id, { id = it }, label = { Text("Model ID") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ProviderType.OPENROUTER, ProviderType.NVIDIA, ProviderType.GEMINI).forEach { item ->
                        FilterChip(selected = provider == item, onClick = { provider = item }, label = { Text(item.replace("NVIDIA NIM", "NVIDIA")) })
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && id.isNotBlank(), onClick = { onAdd(name.trim(), id.trim(), provider, description.trim()) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private data class CustomModelUi(val name: String, val id: String, val provider: String)

private fun parseCustomModels(json: String): List<CustomModelUi> = try {
    val array = JSONArray(json)
    buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(CustomModelUi(item.optString("displayName"), item.optString("modelId"), item.optString("provider")))
        }
    }
} catch (_: Exception) { emptyList() }

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { Column(content = content) }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}
