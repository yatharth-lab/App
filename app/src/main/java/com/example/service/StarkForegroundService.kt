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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StarkForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var sttEngine: SpeechToTextEngine? = null
    private val wakeWordDetector = WakeWordDetector()
    private lateinit var preferences: StarkPreferences
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        preferences = StarkPreferences(applicationContext)
        createNotificationChannel()
        initBackgroundVoiceDetection()
    }

    private fun initBackgroundVoiceDetection() {
        sttEngine = SpeechToTextEngine(this).apply {
            setListener(object : SpeechToTextEngine.Listener {
                override fun onListeningStarted() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onPartialResult(partialText: String) {
                    checkTranscriptForWakeWord(partialText)
                }

                override fun onResult(finalText: String) {
                    if (finalText.isNotBlank()) {
                        checkTranscriptForWakeWord(finalText)
                    }
                }

                override fun onError(errorMessage: String) {}
                override fun onListeningEnded() {}
            })
        }
    }

    private fun checkTranscriptForWakeWord(transcript: String) {
        val detection = wakeWordDetector.processTranscript(transcript)
        if (detection.isWakeWordDetected) {
            vibrateDetection()
            val command = detection.extractedCommand.ifBlank { null }
            launchStarkResponse(command)
        }
    }

    private fun launchStarkResponse(command: String?) {
        if (PermissionManager.hasOverlayPermission(this) && preferences.overlayHudEnabled) {
            // Launch 50% Translucent Blue Holographic HUD Overlay over active app
            StarkOverlayService.show(this, command)
        } else {
            // Fallback to opening MainActivity
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (command != null) {
                    putExtra("INCOMING_VOICE_COMMAND", command)
                }
            }
            startActivity(appIntent)
        }
    }

    private fun vibrateDetection() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
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
                    sttEngine?.startListening(preferences.voiceLanguage, continuous = true)
                }
                return START_STICKY
            }
        }
    }

    private fun startInForeground() {
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

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mr. Stark Listening • Say 'Mr. Stark'")
            .setContentText("Neural Core active • 50% Blue HUD & Screen Reading ready")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Open HUD", triggerHudPendingIntent)
            .addAction(0, "Deactivate", stopPendingIntent)
            .build()

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
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sttEngine?.stopListening()
        _isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
