package com.example.network

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.Attachment
import com.example.data.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ProviderType {
    const val VENUS = "Venus"
    const val OPENROUTER = "OpenRouter"
    const val NVIDIA = "NVIDIA NIM"
    const val GEMINI = "Gemini"
}

data class ModelOption(
    val displayName: String,
    val modelId: String,
    val provider: String,
    val description: String,
    val supportsImages: Boolean = false
)

object ModelCatalog {
    val venus = ModelOption("Venus 3.1", "venus-3.1", ProviderType.VENUS, "Default Venus 3.1 assistant", true)

    val openRouter = listOf(
        ModelOption("DeepSeek V4 Flash", "deepseek/deepseek-v4-flash-0731", ProviderType.OPENROUTER, "Best for all-rounder, coding, reasoning, general chat"),
        ModelOption("Qwen 3.8 Flash", "qwen/qwen3.8-flash", ProviderType.OPENROUTER, "Best for fast all-round AI, coding, chat"),
        ModelOption("MiniMax M3", "minimax/minimax-m3", ProviderType.OPENROUTER, "Best for coding, reasoning, agent tasks", true),
        ModelOption("Nemotron 3 Super 120B A12B", "nvidia/nemotron-3-super-120b-a12b", ProviderType.OPENROUTER, "Best for advanced reasoning, analysis, general AI"),
        ModelOption("GLM-5.3 Flash", "z-ai/glm-5.3-flash", ProviderType.OPENROUTER, "Best for coding, reasoning, general-purpose tasks", true)
    )

    val nvidia = listOf(
        ModelOption("Nemotron 3 Super 120B A12B", "nvidia/nemotron-3-super-120b-a12b", ProviderType.NVIDIA, "Best for all-rounder, reasoning, coding"),
        ModelOption("DeepSeek V4 Pro", "deepseek-ai/deepseek-v4-pro-0813", ProviderType.NVIDIA, "Best for coding, deep reasoning, complex tasks"),
        ModelOption("MiniMax M3", "minimaxai/minimax-m3", ProviderType.NVIDIA, "Best for coding, agents, reasoning", true),
        ModelOption("Nemotron 3.5 Lightning 30B A3B", "nvidia/nemotron-3.5-lightning-30b-a3b", ProviderType.NVIDIA, "Best for fast all-round AI"),
        ModelOption("Gemma 4 31B IT", "google/gemma-4-31b-it", ProviderType.NVIDIA, "Best for general chat, instruction following, coding", true)
    )

    val gemini = listOf(
        ModelOption("Gemini 3.8 Flash", "gemini-3.8-flash", ProviderType.GEMINI, "Best for all-rounder, coding, reasoning, chat", true),
        ModelOption("Gemini 3.7 Flash", "gemini-3.7-flash", ProviderType.GEMINI, "Best for general-purpose AI, coding, reasoning", true),
        // Google currently exposes 3.5 Flash-Lite rather than a 3.7 Flash-Lite endpoint.
        ModelOption("Gemini 3.5 Flash Lite", "gemini-3.5-flash-lite", ProviderType.GEMINI, "Best for fast, lightweight AI tasks", true),
        ModelOption("Gemini 3.6 Flash", "gemini-3.6-flash", ProviderType.GEMINI, "Best for fast general chat and coding", true)
    )
}

interface AIProvider {
    val providerName: String
    suspend fun send(model: ModelOption, text: String, attachments: List<Attachment>): String
}

class OpenRouterProvider(private val resolver: ContentResolver, private val apiKey: String) : AIProvider {
    override val providerName = ProviderType.OPENROUTER

    override suspend fun send(model: ModelOption, text: String, attachments: List<Attachment>): String {
        val content = buildOpenAiContent(resolver, text, attachments)
        val response = AiApiProviders.openRouter.chat(
            "Bearer $apiKey",
            OpenAiRequest(model.modelId, listOf(OpenAiMessage("user", content)))
        )
        return response.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}

class NvidiaNimProvider(private val resolver: ContentResolver, private val apiKey: String) : AIProvider {
    override val providerName = ProviderType.NVIDIA

    override suspend fun send(model: ModelOption, text: String, attachments: List<Attachment>): String {
        val content = buildOpenAiContent(resolver, text, attachments)
        val response = AiApiProviders.nvidia.chat(
            "Bearer $apiKey",
            OpenAiRequest(model.modelId, listOf(OpenAiMessage("user", content)))
        )
        return response.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}

class GeminiProvider(private val resolver: ContentResolver, private val apiKey: String) : AIProvider {
    override val providerName = ProviderType.GEMINI

    override suspend fun send(model: ModelOption, text: String, attachments: List<Attachment>): String {
        val parts = mutableListOf(GeminiPart(text = text))
        attachments.filter { it.type == AttachmentType.IMAGE }.forEach { attachment ->
            val bytes = readImageForUpload(resolver, attachment)
            parts += GeminiPart(inlineData = GeminiInlineData("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        val unsupportedFiles = attachments.filter { it.type == AttachmentType.FILE }
        if (unsupportedFiles.isNotEmpty()) {
            throw UnsupportedOperationException("Gemini file attachments are not supported by this message path yet. Please attach an image or send the file text.")
        }
        val response = AiApiProviders.gemini.chat(model.modelId, apiKey, GeminiRequest(listOf(GeminiContent(parts = parts))))
        return response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("")?.trim().orEmpty()
    }
}

private suspend fun readImageForUpload(resolver: ContentResolver, attachment: Attachment): ByteArray = withContext(Dispatchers.IO) {
    if (attachment.sizeBytes > 16L * 1024L * 1024L) {
        throw IllegalArgumentException("Image is too large. Please choose an image smaller than 16 MB.")
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(attachment.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        val raw = resolver.openInputStream(attachment.uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read ${attachment.fileName}")
        if (raw.size > 16 * 1024 * 1024) throw IllegalArgumentException("Image is too large. Please choose an image smaller than 16 MB.")
        return@withContext raw
    }

    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
    val bitmap = resolver.openInputStream(attachment.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IllegalArgumentException("Could not decode ${attachment.fileName}")
    try {
        val output = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 84, output)) throw IllegalArgumentException("Could not prepare ${attachment.fileName}")
        output.toByteArray()
    } finally {
        bitmap.recycle()
    }
}

private suspend fun buildOpenAiContent(
    resolver: ContentResolver,
    text: String,
    attachments: List<Attachment>
): List<OpenAiContent> = withContext(Dispatchers.IO) {
    val result = mutableListOf(OpenAiContent(type = "text", text = text))
    attachments.forEach { attachment ->
        if (attachment.type == AttachmentType.FILE) {
            throw UnsupportedOperationException("${attachment.fileName} cannot be sent to this model as a generic file. Please use an image-capable model or paste the file text.")
        }
        val bytes = readImageForUpload(resolver, attachment)
        val dataUrl = "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        result += OpenAiContent(type = "image_url", image_url = OpenAiImageUrl(dataUrl))
    }
    result
}
