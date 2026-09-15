package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class PicoRequest(val prompt: String)

@JsonClass(generateAdapter = true)
data class PicoResponse(
    val status: String,
    val text: String? = null,
    val imageUrl: String? = null
)

interface PicoApiService {
    @POST("aero/run/llm-api")
    suspend fun getChatResponse(
        @Query("pk") pk: String = "v1-Z0FBQUFBQnFxSUdJOGJMUDJoUk1ydGlncHR1SGJ2X1lpa0lvRkdubnBiNjc4dkZxcW14SGxVYlJQaEo4YTlfYkJtN2JVcFp1VU9FV0lnaEc1a0VQQjh3M2tPdGN5dE45NFE9PQ==",
        @Body request: PicoRequest
    ): PicoResponse

    @POST("aero/run/image-generation-api")
    suspend fun getImageResponse(
        @Query("pk") pk: String = "v1-Z0FBQUFBQnFxSUdJOGJMUDJoUk1ydGlncHR1SGJ2X1lpa0lvRkdubnBiNjc4dkZxcW14SGxVYlJQaEo4YTlfYkJtN2JVcFp1VU9FV0lnaEc1a0VQQjh3M2tPdGN5dE45NFE9PQ==",
        @Body request: PicoRequest
    ): PicoResponse
}

object PicoApi {
    private const val BASE_URL = "https://backend.buildpicoapps.com/"

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    val retrofitService: PicoApiService by lazy {
        retrofit.create(PicoApiService::class.java)
    }
}
