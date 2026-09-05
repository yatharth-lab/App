package com.example.voice

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TextToSpeechEngine(private val context: Context) {

    interface Listener {
        fun onSpeechStarted()
        fun onSpeechCompleted()
        fun onSpeechError(error: String)
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var listener: Listener? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        initializeTts()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private fun initializeTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureVoiceParams()
            } else {
                isInitialized = false
            }
        }
    }

    private fun configureVoiceParams() {
        tts?.let { engine ->
            // Try Indian English or US English default
            val locale = Locale.forLanguageTag("en-IN")
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }

            // Cinematic, composed masculine pitch (0.93f gives a calm, resonant tone)
            engine.setPitch(0.93f)
            engine.setSpeechRate(1.0f)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    listener?.onSpeechStarted()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    listener?.onSpeechCompleted()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    listener?.onSpeechError("TTS audio playback error")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    listener?.onSpeechError("TTS audio error code: $errorCode")
                }
            })
        }
    }

    fun updateConfig(languageCode: String, speed: Float, pitch: Float, volume: Float) {
        tts?.let { engine ->
            val locale = when (languageCode) {
                "hi-IN" -> Locale.forLanguageTag("hi-IN")
                "en-US" -> Locale.US
                else -> Locale.forLanguageTag("en-IN")
            }
            engine.setLanguage(locale)
            engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
            engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        }
    }

    fun speak(text: String, utteranceId: String = "mr_stark_speech") {
        if (text.isBlank()) return

        stop()

        if (!isInitialized) {
            initializeTts()
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
