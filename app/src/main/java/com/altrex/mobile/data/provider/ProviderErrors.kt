package com.altrex.mobile.data.provider

import com.altrex.mobile.data.model.ProviderErrorCategory
import com.altrex.mobile.data.model.FailureKind

object ProviderErrors {
    fun classifyHttpError(status: Int, body: String, retryAfter: String? = null): ProviderErrorCategory {
        return when (status) {
            401 -> if (body.contains("invalid", true) || body.contains("incorrect", true))
                ProviderErrorCategory.INVALID_API_KEY else ProviderErrorCategory.AUTH_ERROR
            402, 403 -> if (body.contains("quota", true) || body.contains("billing", true))
                ProviderErrorCategory.QUOTA_EXHAUSTED else ProviderErrorCategory.INVALID_API_KEY
            404 -> if (body.contains("model", true)) ProviderErrorCategory.MODEL_NOT_FOUND
                else ProviderErrorCategory.MODEL_UNAVAILABLE
            413 -> ProviderErrorCategory.CONTEXT_TOO_LARGE
            429 -> if (body.contains("quota", true) || (retryAfter != null && retryAfter.toLongOrNull() ?: 0 > 3600))
                ProviderErrorCategory.QUOTA_EXHAUSTED else ProviderErrorCategory.RATE_LIMITED
            in 500..599 -> ProviderErrorCategory.PROVIDER_SERVER_ERROR
            else -> {
                if (status in 400..499) {
                    when {
                        body.contains("tool", true) -> ProviderErrorCategory.TOOLS_UNSUPPORTED
                        body.contains("context", true) || body.contains("token", true) -> ProviderErrorCategory.CONTEXT_TOO_LARGE
                        body.contains("model", true) -> ProviderErrorCategory.MODEL_NOT_FOUND
                        else -> ProviderErrorCategory.BAD_REQUEST
                    }
                } else ProviderErrorCategory.UNKNOWN
            }
        }
    }

    fun toFailureKind(category: ProviderErrorCategory): FailureKind = when (category) {
        ProviderErrorCategory.INVALID_API_KEY, ProviderErrorCategory.AUTH_ERROR -> FailureKind.AUTHENTICATION
        ProviderErrorCategory.QUOTA_EXHAUSTED -> FailureKind.QUOTA_EXHAUSTED
        ProviderErrorCategory.RATE_LIMITED -> FailureKind.RATE_LIMIT
        ProviderErrorCategory.MODEL_NOT_FOUND -> FailureKind.MODEL_UNAVAILABLE
        ProviderErrorCategory.MODEL_UNAVAILABLE -> FailureKind.UNAVAILABLE
        ProviderErrorCategory.TOOLS_UNSUPPORTED -> FailureKind.TOOLS_UNSUPPORTED
        ProviderErrorCategory.CONTEXT_TOO_LARGE -> FailureKind.TOO_LARGE
        ProviderErrorCategory.TIMEOUT -> FailureKind.TIMEOUT
        ProviderErrorCategory.CONNECTION_ERROR -> FailureKind.NETWORK
        ProviderErrorCategory.PROVIDER_SERVER_ERROR -> FailureKind.UNAVAILABLE
        ProviderErrorCategory.CANCELLED -> FailureKind.CANCELLED
        else -> FailureKind.INVALID_REQUEST
    }

    fun isRetryable(category: ProviderErrorCategory): Boolean = when (category) {
        ProviderErrorCategory.RATE_LIMITED, ProviderErrorCategory.TIMEOUT,
        ProviderErrorCategory.CONNECTION_ERROR, ProviderErrorCategory.PROVIDER_SERVER_ERROR -> true
        else -> false
    }

    fun redactApiKey(text: String): String {
        var result = text
        result = result.replace(Regex("Bearer\s+[A-Za-z0-9_\-]+"), "Bearer [REDACTED]")
        result = result.replace(Regex("sk-[A-Za-z0-9]+"), "sk-[REDACTED]")
        result = result.replace(Regex("nvapi-[A-Za-z0-9]+"), "nvapi-[REDACTED]")
        result = result.replace(Regex("gsk_[A-Za-z0-9]+"), "gsk_[REDACTED]")
        result = result.replace(Regex("csk-[A-Za-z0-9]+"), "csk-[REDACTED]")
        result = result.replace(Regex("or-[A-Za-z0-9]+"), "or-[REDACTED]")
        return result
    }

    fun parseRetryAfter(value: String?): Long? {
        if (value == null) return null
        value.toLongOrNull()?.let { return it * 1000 }
        return null
    }

    fun describeError(category: ProviderErrorCategory): String = when (category) {
        ProviderErrorCategory.AUTH_ERROR -> "Authentication failed. Check your API key."
        ProviderErrorCategory.INVALID_API_KEY -> "Invalid API key. Verify your credentials."
        ProviderErrorCategory.MODEL_NOT_FOUND -> "Model not found. Select a different model."
        ProviderErrorCategory.MODEL_UNAVAILABLE -> "Model temporarily unavailable. Try again."
        ProviderErrorCategory.RATE_LIMITED -> "Rate limited. Please wait a moment."
        ProviderErrorCategory.QUOTA_EXHAUSTED -> "Quota exhausted. Check your account."
        ProviderErrorCategory.BAD_REQUEST -> "Bad request. Check your input."
        ProviderErrorCategory.TOOLS_UNSUPPORTED -> "This model doesn't support tool calling."
        ProviderErrorCategory.CONTEXT_TOO_LARGE -> "Context too large. Reduce input length."
        ProviderErrorCategory.TIMEOUT -> "Request timed out. Please try again."
        ProviderErrorCategory.CONNECTION_ERROR -> "Connection error. Check your network."
        ProviderErrorCategory.PROVIDER_SERVER_ERROR -> "Provider server error. Try again later."
        ProviderErrorCategory.CANCELLED -> "Request cancelled."
        ProviderErrorCategory.UNKNOWN -> "An unknown error occurred."
    }
}
