package com.altrex.mobile.data.provider

object ModelRouter {
    fun codingModelScore(model: String, taskKeywords: Set<String>): Int {
        var score = 0
        val lower = model.lowercase()
        if (lower.contains("coder") || lower.contains("code")) score += 120
        if (lower.contains("qwen")) score += 60
        if (lower.contains("deepseek")) score += 58
        if (lower.contains("llama")) score += 30
        if (lower.contains("codestral")) score += 55
        if (lower.contains("gpt")) score += 25
        if (lower.contains("claude")) score += 40
        
        if (taskKeywords.any { listOf("debug", "security", "architect", "migration").contains(it) }) {
            if (lower.contains("70b") || lower.contains("405b") || lower.contains("ultra")) score += 50
        }
        if (taskKeywords.any { listOf("doc", "lint", "rename", "format").contains(it) }) {
            if (lower.contains("8b") || lower.contains("mini") || lower.contains("flash")) score += 30
        }
        return score
    }
    
    fun selectCodingModelCandidates(
        providerId: String,
        prompt: String,
        availableModels: List<String>,
        fallback: String,
        limit: Int = 4
    ): List<String> {
        if (availableModels.isEmpty()) return listOf(fallback)
        val taskKeywords = extractKeywords(prompt)
        return availableModels
            .map { it to codingModelScore(it, taskKeywords) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .let { if (it.isEmpty()) listOf(fallback) else it }
    }
    
    fun selectGoogleModel(availableModels: List<String>, fallback: String): String {
        val flash = availableModels.filter { it.lowercase().contains("flash") }
            .filterNot { it.lowercase().contains("experimental") || it.lowercase().contains("preview") }
        return flash.minByOrNull { it.length } ?: fallback
    }
    
    private fun extractKeywords(prompt: String): Set<String> {
        val words = prompt.lowercase()
            .replace(Regex("[^a-z\s]"), " ")
            .split(Regex("\s+"))
            .filter { it.length > 3 }
        return words.toSet()
    }
}
