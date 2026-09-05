package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem
import com.example.security.SecurityManager
import com.example.settings.StarkPreferences

class AIManager(
    private val context: Context,
    private val preferences: StarkPreferences,
    private val securityManager: SecurityManager
) {
    private val geminiProvider = GeminiAIProvider(securityManager, preferences)
    private val localRuleProvider = LocalRuleAIProvider()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun processQuery(
        userMessage: String,
        history: List<ConversationMessage>,
        memories: List<MemoryItem>
    ): AIResponse {
        val selectedProvider = preferences.aiProvider

        // If user explicitly chose local rule mode
        if (selectedProvider == StarkPreferences.PROVIDER_OFFLINE_RULES) {
            return localRuleProvider.processQuery(userMessage, history, memories)
        }

        // Check if offline
        if (!isOnline()) {
            val localResponse = localRuleProvider.processQuery(userMessage, history, memories)
            return localResponse.copy(
                spokenText = "Offline mode active. " + localResponse.spokenText,
                isOffline = true
            )
        }

        // Check if API key configured
        if (!securityManager.isApiKeyConfigured()) {
            val localResponse = localRuleProvider.processQuery(userMessage, history, memories)
            return localResponse.copy(
                spokenText = "AI API key not set. Running on local engine: " + localResponse.spokenText
            )
        }

        // Run Gemini
        val geminiResponse = geminiProvider.processQuery(userMessage, history, memories)
        if (geminiResponse.isOffline || geminiResponse.error != null) {
            // Fallback to local rule if Gemini fails
            val fallback = localRuleProvider.processQuery(userMessage, history, memories)
            return fallback.copy(
                spokenText = fallback.spokenText
            )
        }

        return geminiResponse
    }
}
