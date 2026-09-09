package com.altrex.mobile.data.provider

import com.altrex.mobile.data.model.ProviderSection

data class ProviderFieldDefinition(
    val id: String,  // "apiKey" | "accountId"
    val label: String,
    val placeholder: String,
    val secret: Boolean,
    val helper: String? = null
)

data class ProviderDefinition(
    val id: String,
    val name: String,
    val description: String,
    val logo: String,
    val section: ProviderSection,
    val apiKeyUrl: String?,
    val accountIdUrl: String? = null,
    val installUrl: String? = null,
    val docsUrl: String,
    val requiresApiKey: Boolean,
    val requiredFields: List<ProviderFieldDefinition>,
    val recommended: Boolean,
    val supportsLocal: Boolean,
    val testStrategy: String,
    val baseUrl: String,
    val defaultModel: String,
    val approvedHosts: List<String>
)

object ProviderRegistry {
    val providers = listOf(
        ProviderDefinition(
            id = "google", name = "Google Gemini", description = "Gemini models via OpenAI-compatible API",
            logo = "G", section = ProviderSection.RECOMMENDED,
            apiKeyUrl = "https://aistudio.google.com/apikey",
            docsUrl = "https://ai.google.dev/docs",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "AIza...", true)),
            recommended = true, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-2.0-flash",
            approvedHosts = listOf("generativelanguage.googleapis.com")
        ),
        ProviderDefinition(
            id = "cerebras", name = "Cerebras", description = "Fast inference for open models",
            logo = "C", section = ProviderSection.RECOMMENDED,
            apiKeyUrl = "https://cloud.cerebras.ai",
            docsUrl = "https://docs.cerebras.ai",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "csk-...", true)),
            recommended = true, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://api.cerebras.ai/v1",
            defaultModel = "llama3.1-8b",
            approvedHosts = listOf("api.cerebras.ai")
        ),
        ProviderDefinition(
            id = "cloudflare", name = "Cloudflare Workers AI", description = "Serverless GPU inference",
            logo = "CF", section = ProviderSection.RECOMMENDED,
            apiKeyUrl = "https://dash.cloudflare.com/profile/api-tokens",
            accountIdUrl = "https://dash.cloudflare.com",
            docsUrl = "https://developers.cloudflare.com/workers-ai/",
            requiresApiKey = true,
            requiredFields = listOf(
                ProviderFieldDefinition("apiKey", "API Token", "cf-...", true),
                ProviderFieldDefinition("accountId", "Account ID", "abc123", false)
            ),
            recommended = true, supportsLocal = false, testStrategy = "cloudflare-workers-ai",
            baseUrl = "https://api.cloudflare.com/client/v4/accounts/{accountId}/ai/v1",
            defaultModel = "@cf/meta/llama-3.1-8b-instruct",
            approvedHosts = listOf("api.cloudflare.com")
        ),
        ProviderDefinition(
            id = "sambanova", name = "SambaNova", description = "Fast inference for Llama models",
            logo = "S", section = ProviderSection.ADDITIONAL,
            apiKeyUrl = "https://cloud.sambanova.ai",
            docsUrl = "https://docs.sambanova.ai",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "svk-...", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://api.sambanova.ai/v1",
            defaultModel = "Meta-Llama-3.1-70B-Instruct",
            approvedHosts = listOf("api.sambanova.ai")
        ),
        ProviderDefinition(
            id = "groq", name = "Groq", description = "Ultra-fast inference for open models",
            logo = "Q", section = ProviderSection.ADDITIONAL,
            apiKeyUrl = "https://console.groq.com/keys",
            docsUrl = "https://console.groq.com/docs",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "gsk_...", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            approvedHosts = listOf("api.groq.com")
        ),
        ProviderDefinition(
            id = "openrouter", name = "OpenRouter", description = "Access 200+ models from one API",
            logo = "OR", section = ProviderSection.ADDITIONAL,
            apiKeyUrl = "https://openrouter.ai/keys",
            docsUrl = "https://openrouter.ai/docs",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "or-...", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "openai/gpt-4o-mini",
            approvedHosts = listOf("openrouter.ai")
        ),
        ProviderDefinition(
            id = "nvidia", name = "NVIDIA NIM", description = "NVIDIA inference microservices",
            logo = "N", section = ProviderSection.ADDITIONAL,
            apiKeyUrl = "https://org.ngc.nvidia.com/setup/api-key",
            docsUrl = "https://docs.nvidia.com/nim",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "nvapi-...", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "qwen/qwen2.5-coder-32b-instruct",
            approvedHosts = listOf("integrate.api.nvidia.com")
        ),
        ProviderDefinition(
            id = "openai", name = "OpenAI", description = "GPT-4o and o-series models",
            logo = "AI", section = ProviderSection.ADVANCED,
            apiKeyUrl = "https://platform.openai.com/api-keys",
            docsUrl = "https://platform.openai.com/docs",
            requiresApiKey = true,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key", "sk-...", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o",
            approvedHosts = listOf("api.openai.com")
        ),
        ProviderDefinition(
            id = "custom", name = "Custom", description = "Any OpenAI-compatible endpoint",
            logo = "+", section = ProviderSection.ADVANCED,
            apiKeyUrl = null,
            docsUrl = "https://platform.openai.com/docs",
            requiresApiKey = false,
            requiredFields = listOf(ProviderFieldDefinition("apiKey", "API Key (optional)", "", true)),
            recommended = false, supportsLocal = false, testStrategy = "openai-compatible",
            baseUrl = "",
            defaultModel = "",
            approvedHosts = emptyList()
        ),
    )

    fun byId(id: String): ProviderDefinition? = providers.find { it.id == id }
    fun recommended() = providers.filter { it.section == ProviderSection.RECOMMENDED }
    fun additional() = providers.filter { it.section == ProviderSection.ADDITIONAL }
    fun advanced() = providers.filter { it.section == ProviderSection.ADVANCED }

    fun officialUrl(providerId: String, kind: String): String? {
        val def = byId(providerId) ?: return null
        return when (kind) {
            "apiKey" -> def.apiKeyUrl
            "accountId" -> def.accountIdUrl
            "install" -> def.installUrl
            "docs" -> def.docsUrl
            else -> null
        }
    }
}
