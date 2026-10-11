package com.example.settings

import android.content.Context
import android.content.SharedPreferences

class StarkPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var aiProvider: String
        get() = prefs.getString(KEY_AI_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()

    var aiTemperature: Float
        get() = prefs.getFloat(KEY_AI_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_AI_TEMPERATURE, value).apply()

    var voiceLanguage: String
        get() = prefs.getString(KEY_VOICE_LANGUAGE, "en-IN") ?: "en-IN"
        set(value) = prefs.edit().putString(KEY_VOICE_LANGUAGE, value).apply()

    var speechSpeed: Float
        get() = prefs.getFloat(KEY_SPEECH_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_SPEED, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var speechVolume: Float
        get() = prefs.getFloat(KEY_SPEECH_VOLUME, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_VOLUME, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var handsFreeAutoListen: Boolean
        get() = prefs.getBoolean(KEY_HANDS_FREE, true)
        set(value) = prefs.edit().putBoolean(KEY_HANDS_FREE, value).apply()

    var foregroundServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_FG_SERVICE, false)
        set(value) = prefs.edit().putBoolean(KEY_FG_SERVICE, value).apply()

    var overlayHudEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_HUD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_HUD_ENABLED, value).apply()

    var overlayCapacity: Float
        get() = prefs.getFloat(KEY_OVERLAY_CAPACITY, 0.50f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_CAPACITY, value).apply()

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var animationIntensity: Float
        get() = prefs.getFloat(KEY_ANIMATION_INTENSITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ANIMATION_INTENSITY, value).apply()

    var appLockPin: String?
        get() = prefs.getString(KEY_APP_LOCK_PIN, null)
        set(value) = prefs.edit().putString(KEY_APP_LOCK_PIN, value).apply()

    var isAppLocked: Boolean
        get() = !appLockPin.isNullOrBlank()
        set(_) {}

    companion object {
        private const val PREFS_NAME = "mr_stark_preferences"
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OFFLINE_RULES = "local_rules"

        private const val KEY_AI_PROVIDER = "key_ai_provider"
        private const val KEY_CUSTOM_API_KEY = "key_custom_api_key"
        private const val KEY_AI_MODEL = "key_ai_model"
        private const val KEY_AI_TEMPERATURE = "key_ai_temperature"
        private const val KEY_VOICE_LANGUAGE = "key_voice_language"
        private const val KEY_SPEECH_SPEED = "key_speech_speed"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_SPEECH_VOLUME = "key_speech_volume"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_HANDS_FREE = "key_hands_free"
        private const val KEY_FG_SERVICE = "key_fg_service"
        private const val KEY_OVERLAY_HUD_ENABLED = "key_overlay_hud_enabled"
        private const val KEY_OVERLAY_CAPACITY = "key_overlay_capacity"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_ANIMATION_INTENSITY = "key_animation_intensity"
        private const val KEY_APP_LOCK_PIN = "key_app_lock_pin"
    }
}
