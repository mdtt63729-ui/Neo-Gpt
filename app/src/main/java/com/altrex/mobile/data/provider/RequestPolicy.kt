package com.altrex.mobile.data.provider

import com.altrex.mobile.data.model.RequestPolicy

object RequestPolicies {
    val DEFAULT = RequestPolicy()
    val GROQ = RequestPolicy(inputTokens = 3500, concurrency = 1)
    
    fun forProvider(providerId: String): RequestPolicy = when (providerId) {
        "groq" -> GROQ
        else -> DEFAULT
    }
    
    fun clamp(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max)
    fun clampLong(value: Long, min: Long, max: Long): Long = value.coerceIn(min, max)
}
