package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class SpeechToTextEngine(private val context: Context) {

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

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "en-IN", continuous: Boolean = false) {
        isDestroyed = false
        this.activeLanguageCode = languageCode
        this.isContinuousMode = continuous

        mainHandler.post {
            executeStartListening()
        }
    }

    private fun executeStartListening() {
        if (isDestroyed) return

        if (!isRecognitionAvailable()) {
            listener?.onError("Speech recognition is not available on this device.")
            return
        }

        stopRecognizerInstance()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
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

                        val isTransient = error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                error == SpeechRecognizer.ERROR_CLIENT

                        if (isContinuousMode && !isDestroyed && isTransient) {
                            // Automatically restart listening for hands-free / wake word detection
                            mainHandler.postDelayed({
                                if (isContinuousMode && !isDestroyed) {
                                    executeStartListening()
                                }
                            }, 250)
                        } else {
                            listener?.onError(message)
                            listener?.onListeningEnded()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""

                        if (text.isNotBlank()) {
                            listener?.onResult(text)
                        } else {
                            if (isContinuousMode && !isDestroyed) {
                                mainHandler.postDelayed({
                                    if (isContinuousMode && !isDestroyed) {
                                        executeStartListening()
                                    }
                                }, 200)
                            } else {
                                listener?.onResult("")
                            }
                        }
                        listener?.onListeningEnded()
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
            _isListening.value = false
            listener?.onError("Failed to start speech recognizer: ${e.message}")
        }
    }

    private fun stopRecognizerInstance() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun stopListening() {
        isContinuousMode = false
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            stopRecognizerInstance()
            _isListening.value = false
        }
    }
}
