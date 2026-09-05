package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.settings.StarkPreferences

class SecurityManager(
    private val context: Context,
    private val preferences: StarkPreferences
) {
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotificationsPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Resolves the active Gemini API Key securely.
     * Checks user custom API key first, then falls back to BuildConfig.GEMINI_API_KEY.
     * Never logs or prints the raw key.
     */
    fun getResolvedGeminiApiKey(): String {
        val customKey = preferences.customApiKey.trim()
        if (customKey.isNotEmpty() && !customKey.startsWith("MY_GEMINI_API_KEY")) {
            return customKey
        }
        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        if (buildConfigKey.isNotEmpty() && !buildConfigKey.startsWith("MY_GEMINI_API_KEY")) {
            return buildConfigKey
        }
        return ""
    }

    fun isApiKeyConfigured(): Boolean {
        return getResolvedGeminiApiKey().isNotBlank()
    }

    fun getMaskedApiKey(): String {
        val key = getResolvedGeminiApiKey()
        if (key.length <= 8) return if (key.isEmpty()) "Not Configured" else "••••••••"
        val start = key.take(4)
        val end = key.takeLast(4)
        return "$start••••••••$end"
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedPin = preferences.appLockPin
        return storedPin == null || storedPin == enteredPin
    }

    fun setPin(pin: String?) {
        preferences.appLockPin = pin?.trim()?.ifEmpty { null }
    }
}
