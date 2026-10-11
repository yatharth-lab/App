package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpeechToTextEngine(private val context: Context) {

    companion object {
        private const val TAG = "StarkSTTEngine"
        private const val RESTART_DELAY_MS = 250L
        private const val ERROR_BACKOFF_DELAY_MS = 500L
    }

    interface Listener {
        fun onListeningStarted()
        fun onRmsChanged(rmsdB: Float)
        fun onPartialResult(partialText: String)
        fun onResult(finalText: String)
        fun onError(errorMessage: String)
        fun onListeningEnded()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    var isContinuousMode: Boolean = false
    private var activeLanguageCode: String = "en-IN"
    private var isDestroyed: Boolean = false
    private var isStarting: Boolean = false

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "en-IN", continuous: Boolean = false) {
        Log.d(TAG, "startListening called with continuous=$continuous, lang=$languageCode")
        isDestroyed = false
        this.activeLanguageCode = languageCode
        this.isContinuousMode = continuous

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            executeStartListening()
        }
    }

    private fun executeStartListening() {
        if (isDestroyed) {
            Log.d(TAG, "executeStartListening aborted: engine is destroyed")
            return
        }

        if (!isRecognitionAvailable()) {
            Log.e(TAG, "Speech recognition unavailable on device")
            listener?.onError("Speech recognition is not available on this device.")
            return
        }

        if (isStarting) {
            Log.d(TAG, "Already starting recognizer, skipping redundant call")
            return
        }

        isStarting = true
        stopRecognizerInstance()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isStarting = false
                        _isListening.value = true
                        listener?.onListeningStarted()
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        listener?.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        isStarting = false
                        _isListening.value = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Recognition error ($error)"
                        }

                        Log.d(TAG, "SpeechRecognizer onError: $error ($message). continuous=$isContinuousMode")

                        // Fatal errors that require user intervention
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            listener?.onError(message)
                            listener?.onListeningEnded()
                            return
                        }

                        // In continuous mode, safely restart with slight backoff
                        if (isContinuousMode && !isDestroyed) {
                            val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                                ERROR_BACKOFF_DELAY_MS
                            } else {
                                RESTART_DELAY_MS
                            }
                            scheduleRestart(delay)
                        } else {
                            listener?.onError(message)
                            listener?.onListeningEnded()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isStarting = false
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""

                        Log.d(TAG, "SpeechRecognizer onResults: '$text'. continuous=$isContinuousMode")

                        if (text.isNotBlank()) {
                            listener?.onResult(text)
                        } else {
                            listener?.onResult("")
                        }
                        listener?.onListeningEnded()

                        // CRITICAL FIX: In continuous mode, MUST restart listening immediately after delivering results!
                        if (isContinuousMode && !isDestroyed) {
                            scheduleRestart(RESTART_DELAY_MS)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            listener?.onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLanguageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isStarting = false
            _isListening.value = false
            Log.e(TAG, "Exception starting speechRecognizer", e)
            if (isContinuousMode && !isDestroyed) {
                scheduleRestart(ERROR_BACKOFF_DELAY_MS)
            } else {
                listener?.onError("Failed to start speech recognizer: ${e.message}")
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isContinuousMode && !isDestroyed) {
                executeStartListening()
            }
        }, delayMs)
    }

    private fun stopRecognizerInstance() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer instance: ${e.message}")
        }
        speechRecognizer = null
    }

    fun stopListening() {
        Log.d(TAG, "stopListening called")
        isContinuousMode = false
        isDestroyed = true
        isStarting = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            stopRecognizerInstance()
            _isListening.value = false
        }
    }
}
