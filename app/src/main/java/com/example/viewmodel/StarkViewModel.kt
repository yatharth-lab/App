package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionEngine
import com.example.ai.AIManager
import com.example.data.model.AssistantState
import com.example.data.model.ConversationMessage
import com.example.data.model.MessageSender
import com.example.memory.MemoryRepository
import com.example.memory.StarkDatabase
import com.example.security.SecurityManager
import com.example.service.StarkForegroundService
import com.example.settings.StarkPreferences
import com.example.voice.SpeechToTextEngine
import com.example.voice.TextToSpeechEngine
import com.example.wakeword.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StarkViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = StarkPreferences(application)
    val securityManager = SecurityManager(application, preferences)
    val database = StarkDatabase.getInstance(application)
    val memoryRepository = MemoryRepository(database)

    private val actionEngine = ActionEngine(application, memoryRepository)
    private val aiManager = AIManager(application, preferences, securityManager)
    private val wakeWordDetector = WakeWordDetector()
    private val sttEngine = SpeechToTextEngine(application)
    private val ttsEngine = TextToSpeechEngine(application)

    private val _uiState = MutableStateFlow(
        StarkUiState(
            isOnline = aiManager.isOnline(),
            activeProviderName = if (preferences.aiProvider == StarkPreferences.PROVIDER_OFFLINE_RULES) {
                "Local Rule Engine"
            } else {
                "Google Gemini (${preferences.aiModel})"
            },
            isApiKeyConfigured = securityManager.isApiKeyConfigured(),
            isAppUnlocked = !preferences.isAppLocked,
            wakeWordEnabled = preferences.wakeWordEnabled,
            handsFreeEnabled = preferences.handsFreeAutoListen,
            isBackgroundActive = StarkForegroundService.isRunning.value
        )
    )
    val uiState: StateFlow<StarkUiState> = _uiState.asStateFlow()

    init {
        setupVoiceEngines()
        observeData()
        postInitialGreeting()
    }

    private fun setupVoiceEngines() {
        // Speech To Text Callbacks
        sttEngine.setListener(object : SpeechToTextEngine.Listener {
            override fun onListeningStarted() {
                _uiState.update {
                    it.copy(
                        assistantState = AssistantState.LISTENING,
                        errorMessage = null
                    )
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB usually ranges from -2 to ~10
                val normalized = (rmsdB + 2f).coerceAtLeast(0f)
                _uiState.update { it.copy(audioLevel = normalized) }
            }

            override fun onPartialResult(partialText: String) {
                _uiState.update { it.copy(partialTranscript = partialText) }
            }

            override fun onResult(finalText: String) {
                _uiState.update { it.copy(partialTranscript = "", audioLevel = 0f) }
                if (finalText.isNotBlank()) {
                    handleUserSpokenInput(finalText)
                } else {
                    _uiState.update {
                        it.copy(
                            assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE
                        )
                    }
                }
            }

            override fun onError(errorMessage: String) {
                _uiState.update {
                    it.copy(
                        assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE,
                        errorMessage = errorMessage,
                        partialTranscript = "",
                        audioLevel = 0f
                    )
                }
            }

            override fun onListeningEnded() {
                if (_uiState.value.assistantState == AssistantState.LISTENING) {
                    _uiState.update {
                        it.copy(
                            assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE,
                            audioLevel = 0f
                        )
                    }
                }
            }
        })

        // Text To Speech Callbacks
        ttsEngine.setListener(object : TextToSpeechEngine.Listener {
            override fun onSpeechStarted() {
                _uiState.update { it.copy(assistantState = AssistantState.SPEAKING) }
            }

            override fun onSpeechCompleted() {
                _uiState.update {
                    it.copy(
                        assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE
                    )
                }
                // If hands-free continuous mode is enabled, restart listening
                if (preferences.handsFreeAutoListen) {
                    startListening()
                }
            }

            override fun onSpeechError(error: String) {
                _uiState.update {
                    it.copy(
                        assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE,
                        errorMessage = error
                    )
                }
            }
        })

        // Apply TTS user settings
        ttsEngine.updateConfig(
            languageCode = preferences.voiceLanguage,
            speed = preferences.speechSpeed,
            pitch = preferences.speechPitch,
            volume = preferences.speechVolume
        )
    }

    private fun observeData() {
        viewModelScope.launch {
            memoryRepository.allConversations.collect { messages ->
                _uiState.update { it.copy(conversations = messages) }
            }
        }

        viewModelScope.launch {
            memoryRepository.allMemories.collect { mems ->
                _uiState.update { it.copy(memories = mems) }
            }
        }

        viewModelScope.launch {
            StarkForegroundService.isRunning.collect { running ->
                _uiState.update {
                    it.copy(
                        isBackgroundActive = running,
                        assistantState = if (running && it.assistantState == AssistantState.IDLE) {
                            AssistantState.BACKGROUND_ACTIVE
                        } else it.assistantState
                    )
                }
            }
        }
    }

    private fun postInitialGreeting() {
        viewModelScope.launch {
            val count = memoryRepository.getRecentConversations(1).size
            if (count == 0) {
                val greeting = ConversationMessage(
                    sender = MessageSender.STARK,
                    text = "Mr. Stark online, Sir. Speech recognition and AI neural systems are standing by. How may I assist you today?",
                    timestamp = System.currentTimeMillis()
                )
                memoryRepository.saveConversationMessage(greeting)
            }
        }
    }

    fun startListening() {
        ttsEngine.stop()
        sttEngine.startListening(preferences.voiceLanguage)
    }

    fun stopListening() {
        sttEngine.stopListening()
        _uiState.update {
            it.copy(
                assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE,
                audioLevel = 0f,
                partialTranscript = ""
            )
        }
    }

    fun toggleListening() {
        if (_uiState.value.assistantState == AssistantState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun stopSpeaking() {
        ttsEngine.stop()
        _uiState.update {
            it.copy(
                assistantState = if (it.isBackgroundActive) AssistantState.BACKGROUND_ACTIVE else AssistantState.IDLE
            )
        }
    }

    fun handleUserSpokenInput(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return

        // Check wake word if wake word filtering is active
        var effectiveCommand = trimmed
        if (preferences.wakeWordEnabled) {
            val detection = wakeWordDetector.processTranscript(trimmed)
            if (detection.isWakeWordDetected) {
                effectiveCommand = detection.extractedCommand.ifEmpty { "hello" }
            }
        }

        submitUserMessage(effectiveCommand)
    }

    fun submitUserMessage(messageText: String) {
        val clean = messageText.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            // Save User message
            val userMsg = ConversationMessage(
                sender = MessageSender.USER,
                text = clean,
                timestamp = System.currentTimeMillis()
            )
            memoryRepository.saveConversationMessage(userMsg)

            _uiState.update {
                it.copy(
                    assistantState = AssistantState.THINKING,
                    isOnline = aiManager.isOnline(),
                    errorMessage = null
                )
            }

            // Process query through AI Manager
            val memories = memoryRepository.getAllMemoriesList()
            val history = memoryRepository.getRecentConversations(8)

            val aiResponse = aiManager.processQuery(clean, history, memories)

            var actionBadge: String? = null
            var spokenOutput = aiResponse.spokenText

            // If an action command was returned, execute it!
            aiResponse.actionCommand?.let { cmd ->
                val result = actionEngine.executeAction(cmd)
                actionBadge = result.actionBadge
                if (result.spokenText.isNotBlank()) {
                    spokenOutput = result.spokenText
                }
            }

            // Save Stark's response to database
            val starkMsg = ConversationMessage(
                sender = MessageSender.STARK,
                text = spokenOutput,
                actionType = aiResponse.actionCommand?.type,
                actionDetails = actionBadge,
                timestamp = System.currentTimeMillis()
            )
            memoryRepository.saveConversationMessage(starkMsg)

            // Speak response via TTS
            ttsEngine.speak(spokenOutput)
        }
    }

    fun toggleBackgroundService() {
        val context = getApplication<Application>()
        if (StarkForegroundService.isRunning.value) {
            StarkForegroundService.stop(context)
            preferences.foregroundServiceEnabled = false
        } else {
            StarkForegroundService.start(context)
            preferences.foregroundServiceEnabled = true
        }
    }

    fun saveMemory(key: String, value: String, category: String = "general") {
        viewModelScope.launch {
            memoryRepository.saveMemory(key, value, category)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryRepository.clearMemories()
        }
    }

    fun clearConversations() {
        viewModelScope.launch {
            memoryRepository.clearConversations()
        }
    }

    fun verifyPin(pin: String): Boolean {
        val success = securityManager.verifyPin(pin)
        if (success) {
            _uiState.update { it.copy(isAppUnlocked = true) }
        }
        return success
    }

    fun updatePin(pin: String?) {
        securityManager.setPin(pin)
        _uiState.update { it.copy(isAppUnlocked = pin.isNullOrBlank()) }
    }

    fun updateApiKey(newKey: String) {
        preferences.customApiKey = newKey
        _uiState.update {
            it.copy(
                isApiKeyConfigured = securityManager.isApiKeyConfigured()
            )
        }
    }

    fun updateAiProvider(provider: String) {
        preferences.aiProvider = provider
        _uiState.update {
            it.copy(
                activeProviderName = if (provider == StarkPreferences.PROVIDER_OFFLINE_RULES) {
                    "Local Rule Engine"
                } else {
                    "Google Gemini (${preferences.aiModel})"
                }
            )
        }
    }

    fun updateAiModel(model: String) {
        preferences.aiModel = model
        _uiState.update {
            it.copy(
                activeProviderName = "Google Gemini ($model)"
            )
        }
    }

    fun updateVoiceConfig(language: String, speed: Float, pitch: Float, volume: Float) {
        preferences.voiceLanguage = language
        preferences.speechSpeed = speed
        preferences.speechPitch = pitch
        preferences.speechVolume = volume
        ttsEngine.updateConfig(language, speed, pitch, volume)
    }

    fun updateWakeWordSettings(enabled: Boolean, handsFree: Boolean) {
        preferences.wakeWordEnabled = enabled
        preferences.handsFreeAutoListen = handsFree
        _uiState.update {
            it.copy(
                wakeWordEnabled = enabled,
                handsFreeEnabled = handsFree
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sttEngine.stopListening()
        ttsEngine.shutdown()
    }
}
