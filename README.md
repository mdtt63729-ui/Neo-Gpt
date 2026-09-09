# ALTREX CODE - Android App

Native Android app built with Kotlin + Jetpack Compose, matching the ALTREX CODE desktop application.

## Building

1. Open this project in Android Studio (Hedgehog 2023.1.1 or later)
2. Let Gradle sync complete
3. Click Run or use `./gradlew assembleDebug` to build an APK

## Requirements

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34
- Kotlin 1.9.20+

## Architecture

- **UI**: Jetpack Compose + Material3 (warm neutral charcoal dark theme)
- **State**: MVVM with StateFlow
- **Network**: OkHttp with SSE streaming
- **Persistence**: Room database + EncryptedSharedPreferences
- **DI**: Manual dependency injection via Application class

## Features

- 9 cloud AI providers (Google Gemini, Cerebras, Cloudflare, SambaNova, Groq, OpenRouter, NVIDIA NIM, OpenAI, Custom)
- Ask mode (read-only Q&A with streaming)
- Agent mode (tool-calling AI with edit_file, write_file, read_file, list_files)
- Multi-AI mode (Director-orchestrated multi-model task execution)
- AUTO model selection with keyword-based scoring
- Automatic model fallback on failure
- Conversation history with persistence
- Provider management with connection testing
- Settings with diagnostics
- Command palette
- Markdown rendering with code blocks
- Encrypted credential storage
- Error classification (14 categories)
- Request policies per provider
- Loop detection in agent mode
- Context budget management

## Package Structure

```
com.altrex.mobile/
├── AltrexApplication.kt       # App initialization, DI
├── MainActivity.kt             # Single activity, Compose entry point
├── data/
│   ├── model/                  # Data models (all types)
│   ├── provider/               # Provider system (API client, registry, errors, router)
│   ├── agent/                  # Agent system (runner, tool broker, budget, context)
│   ├── multiai/                # Multi-AI system (director, contracts, state store)
│   ├── repository/             # Repositories (provider, conversation, settings)
│   └── local/                  # Room database, type converters
├── ui/
│   ├── theme/                  # Colors, typography, theme
│   ├── components/             # Reusable composables (primitives, markdown, code blocks)
│   └── screens/                # All screens (chat, home, sidebar, dialogs, etc.)
└── viewmodel/                  # AltrexViewModel with StateFlow
```
