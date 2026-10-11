package com.example.security

import android.content.Context
import com.example.BuildConfig
import com.example.settings.StarkPreferences

class SecurityManager(
    private val context: Context,
    private val preferences: StarkPreferences
) {
    fun hasRecordAudioPermission(): Boolean = PermissionManager.hasAudioPermission(context)

    fun hasAccessibilityPermission(): Boolean = PermissionManager.hasAccessibilityPermission(context)

    fun hasOverlayPermission(): Boolean = PermissionManager.hasOverlayPermission(context)

    fun isBatteryOptimizationIgnored(): Boolean = PermissionManager.isBatteryOptimizationIgnored(context)

    fun hasPostNotificationsPermission(): Boolean = PermissionManager.hasNotificationPermission(context)

    fun openAccessibilitySettings() = PermissionManager.openAccessibilitySettings(context)

    fun openOverlaySettings() = PermissionManager.openOverlaySettings(context)

    fun requestIgnoreBatteryOptimizations() = PermissionManager.requestIgnoreBatteryOptimizations(context)

    fun openNotificationSettings() = PermissionManager.openNotificationSettings(context)

    fun openAppDetailsSettings() = PermissionManager.openAppDetailsSettings(context)

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
