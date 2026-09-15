package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.network.ModelCatalog
import com.example.network.ModelOption
import com.example.network.ProviderType
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import org.json.JSONArray

@Composable
fun AppNavigation(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val selectedModel by settingsViewModel.selectedModel.collectAsState()
    val selectedProvider by settingsViewModel.selectedProvider.collectAsState()
    val openRouterKey by settingsViewModel.openRouterKey.collectAsState()
    val nvidiaKey by settingsViewModel.nvidiaKey.collectAsState()
    val geminiKey by settingsViewModel.geminiKey.collectAsState()
    val customModelsJson by settingsViewModel.customModels.collectAsState()

    val configuredModels = buildList {
        add(ModelCatalog.venus)
        if (openRouterKey.isNotBlank()) addAll(ModelCatalog.openRouter)
        if (nvidiaKey.isNotBlank()) addAll(ModelCatalog.nvidia)
        if (geminiKey.isNotBlank()) addAll(ModelCatalog.gemini)
        addAll(parseCustomModels(customModelsJson).filter { model ->
            when (model.provider) {
                ProviderType.OPENROUTER -> openRouterKey.isNotBlank()
                ProviderType.NVIDIA -> nvidiaKey.isNotBlank()
                ProviderType.GEMINI -> geminiKey.isNotBlank()
                else -> false
            }
        })
    }

    val selectedStillAvailable = configuredModels.any { it.modelId == selectedModel }
    LaunchedEffect(selectedStillAvailable, selectedModel) {
        if (!selectedStillAvailable) {
            settingsViewModel.setProvider(ProviderType.VENUS)
            settingsViewModel.setModel(ModelCatalog.venus.modelId)
        }
    }

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            val textSize by settingsViewModel.textSize.collectAsState()
            ChatScreen(
                viewModel = chatViewModel,
                textSize = textSize,
                selectedModel = selectedModel,
                selectedProvider = selectedProvider,
                availableModels = configuredModels,
                onModelChange = { model ->
                    settingsViewModel.setProvider(model.provider)
                    settingsViewModel.setModel(model.modelId)
                },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun parseCustomModels(json: String): List<ModelOption> {
    return try {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("displayName").trim()
                val id = item.optString("modelId").trim()
                val provider = item.optString("provider").trim()
                if (name.isNotBlank() && id.isNotBlank() && provider in setOf(ProviderType.OPENROUTER, ProviderType.NVIDIA, ProviderType.GEMINI)) {
                    add(ModelOption(name, id, provider, item.optString("description"), supportsImages = true))
                }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
