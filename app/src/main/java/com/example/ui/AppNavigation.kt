package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

import com.example.ui.auth.LoginScreen

@Composable
fun AppNavigation(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val loggedInUser by settingsViewModel.loggedInUser.collectAsState()

    val startDest = if (loggedInUser != null) "chat" else "login"

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                viewModel = settingsViewModel,
                onLoginSuccess = {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("chat") {
            val textSize by settingsViewModel.textSize.collectAsState()
            val selectedModel by settingsViewModel.selectedModel.collectAsState()
            
            val openRouterKey by settingsViewModel.openRouterKey.collectAsState()
            val nvidiaKey by settingsViewModel.nvidiaKey.collectAsState()
            val geminiKey by settingsViewModel.geminiKey.collectAsState()
            
            val availableModels = mutableListOf("venus-3.1")
            if (openRouterKey.isNotBlank()) {
                availableModels.addAll(listOf(
                    "openrouter/deepseek/deepseek-chat",
                    "openrouter/qwen/qwen-2.5-72b-instruct",
                    "openrouter/minimax/minimax-01",
                    "openrouter/nvidia/llama-3.1-nemotron-70b-instruct",
                    "openrouter/zhipu/glm-4"
                ))
            }
            if (nvidiaKey.isNotBlank()) {
                availableModels.addAll(listOf(
                    "nvidia/nvidia/llama-3.1-nemotron-70b-instruct",
                    "nvidia/deepseek-ai/deepseek-coder-33b-instruct",
                    "nvidia/meta/llama3-70b-instruct"
                ))
            }
            if (geminiKey.isNotBlank()) {
                availableModels.addAll(listOf(
                    "gemini/gemini-1.5-pro",
                    "gemini/gemini-1.5-flash",
                    "gemini/gemini-1.5-flash-8b"
                ))
            }

            ChatScreen(
                viewModel = chatViewModel,
                textSize = textSize,
                selectedModel = selectedModel,
                onModelChange = { settingsViewModel.setSelectedModel(it) },
                availableModels = availableModels,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    settingsViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
    }
}
