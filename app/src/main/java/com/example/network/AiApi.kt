package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// --- OpenRouter / Nvidia NIM Models (OpenAI compatible) ---

@JsonClass(generateAdapter = true)
data class OpenAiMessage(val role: String, val content: String)

@JsonClass(generateAdapter = true)
data class OpenAiRequest(val model: String, val messages: List<OpenAiMessage>)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(val message: OpenAiMessage?)

@JsonClass(generateAdapter = true)
data class OpenAiResponse(val choices: List<OpenAiChoice>?)

// --- Gemini Models ---

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

// --- Services ---

interface OpenRouterService {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

interface NvidiaService {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

interface GeminiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun chat(
        @Path("model") model: String,
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object AiApiProviders {
    private val moshi = com.squareup.moshi.Moshi.Builder()
        
        .build()

    val openRouter: OpenRouterService by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterService::class.java)
    }

    val nvidia: NvidiaService by lazy {
        Retrofit.Builder()
            .baseUrl("https://integrate.api.nvidia.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NvidiaService::class.java)
    }

    val gemini: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }
}
