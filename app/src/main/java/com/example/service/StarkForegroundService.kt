package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.overlay.StarkOverlayService
import com.example.security.PermissionManager
import com.example.settings.StarkPreferences
import com.example.voice.SpeechToTextEngine
import com.example.wakeword.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StarkForegroundService : Service() {

    companion object {
        private const val TAG = "StarkForegroundService"
        const val CHANNEL_ID = "mr_stark_assistant_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_STARK"
        const val ACTION_STOP = "ACTION_STOP_STARK"
        const val ACTION_TRIGGER_OVERLAY = "ACTION_TRIGGER_OVERLAY"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, StarkForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StarkForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service: ${e.message}")
            }
        }
    }

    private var sttEngine: SpeechToTextEngine? = null
    private val wakeWordDetector = WakeWordDetector()
    private lateinit var preferences: StarkPreferences
    private val mainHandler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        preferences = StarkPreferences(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        initBackgroundVoiceDetection()
    }

    private fun initBackgroundVoiceDetection() {
        sttEngine = SpeechToTextEngine(this).apply {
            setListener(object : SpeechToTextEngine.Listener {
                override fun onListeningStarted() {
                    Log.d(TAG, "Foreground STT listening started")
                    updateNotificationStatus("Listening active • Say 'Mr. Stark'")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onPartialResult(partialText: String) {
                    Log.d(TAG, "Partial: $partialText")
                    checkTranscriptForWakeWord(partialText)
                }

                override fun onResult(finalText: String) {
                    Log.d(TAG, "Final: $finalText")
                    if (finalText.isNotBlank()) {
                        checkTranscriptForWakeWord(finalText)
                    }
                }

                override fun onError(errorMessage: String) {
                    Log.w(TAG, "STT error in bg: $errorMessage")
                    updateNotificationStatus("Standby: $errorMessage")
                }

                override fun onListeningEnded() {
                    Log.d(TAG, "Listening ended in bg")
                }
            })
        }
    }

    private fun checkTranscriptForWakeWord(transcript: String) {
        val detection = wakeWordDetector.processTranscript(transcript)
        if (detection.isWakeWordDetected) {
            Log.i(TAG, "WAKE WORD DETECTED! Extracted command: '${detection.extractedCommand}'")
            vibrateDetection()
            val command = detection.extractedCommand.ifBlank { null }
            launchStarkResponse(command)
        }
    }

    private fun launchStarkResponse(command: String?) {
        mainHandler.post {
            if (PermissionManager.hasOverlayPermission(this) && preferences.overlayHudEnabled) {
                Log.d(TAG, "Deploying 50% Blue HUD Overlay over active app")
                StarkOverlayService.show(this, command)
            } else {
                Log.d(TAG, "Opening MainActivity as fallback")
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    if (command != null) {
                        putExtra("INCOMING_VOICE_COMMAND", command)
                    }
                }
                startActivity(appIntent)
            }
        }
    }

    private fun vibrateDetection() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                sttEngine?.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isRunning.value = false
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_OVERLAY -> {
                launchStarkResponse(null)
                return START_STICKY
            }
            else -> {
                startInForeground()
                _isRunning.value = true
                if (PermissionManager.hasAudioPermission(this)) {
                    Log.d(TAG, "Starting continuous speech recognition in background foreground service")
                    sttEngine?.startListening(preferences.voiceLanguage, continuous = true)
                } else {
                    Log.e(TAG, "Audio permission missing in foreground service")
                    updateNotificationStatus("Microphone permission required")
                }
                return START_STICKY
            }
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerHudIntent = Intent(this, StarkForegroundService::class.java).apply {
            action = ACTION_TRIGGER_OVERLAY
        }
        val triggerHudPendingIntent = PendingIntent.getService(
            this,
            1,
            triggerHudIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StarkForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mr. Stark Listening • Say 'Mr. Stark'")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Open HUD", triggerHudPendingIntent)
            .addAction(0, "Deactivate", stopPendingIntent)
            .build()
    }

    private fun startInForeground() {
        val notification = buildNotification("Surveillance active • 50% Blue HUD ready")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationStatus(statusText: String) {
        try {
            val notification = buildNotification(statusText)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mr. Stark Assistant Background",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification when Mr. Stark background assistant is active"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sttEngine?.stopListening()
        _isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
