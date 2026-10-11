package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.actions.ActionEngine
import com.example.ai.AIManager
import com.example.memory.MemoryRepository
import com.example.memory.StarkDatabase
import com.example.security.SecurityManager
import com.example.settings.StarkPreferences
import com.example.voice.SpeechToTextEngine
import com.example.voice.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarkOverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE = "ACTION_HIDE_OVERLAY"
        const val EXTRA_PROMPT = "EXTRA_PROMPT"

        private var isOverlayVisible = false

        fun isVisible(): Boolean = isOverlayVisible

        fun show(context: Context, prompt: String? = null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, StarkOverlayService::class.java).apply {
                action = ACTION_SHOW
                if (prompt != null) putExtra(EXTRA_PROMPT, prompt)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, StarkOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayRootView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var preferences: StarkPreferences
    private lateinit var securityManager: SecurityManager
    private lateinit var aiManager: AIManager
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var actionEngine: ActionEngine

    private var sttEngine: SpeechToTextEngine? = null
    private var ttsEngine: TextToSpeechEngine? = null

    // UI elements
    private var statusTextView: TextView? = null
    private var transcriptTextView: TextView? = null
    private var responseTextView: TextView? = null
    private var arcReactorView: OverlayArcReactorView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val app = applicationContext
        preferences = StarkPreferences(app)
        securityManager = SecurityManager(app, preferences)
        aiManager = AIManager(app, preferences, securityManager)
        val database = StarkDatabase.getInstance(app)
        memoryRepository = MemoryRepository(database)
        actionEngine = ActionEngine(app, memoryRepository)

        initVoiceEngines()
    }

    private fun initVoiceEngines() {
        ttsEngine = TextToSpeechEngine(this).apply {
            updateConfig(
                languageCode = preferences.voiceLanguage,
                speed = preferences.speechSpeed,
                pitch = preferences.speechPitch,
                volume = preferences.speechVolume
            )
            setListener(object : TextToSpeechEngine.Listener {
                override fun onSpeechStarted() {
                    mainHandler.post {
                        statusTextView?.text = "STATUS: VOCALIZING RESPONSE"
                        statusTextView?.setTextColor(Color.parseColor("#00E5FF"))
                        arcReactorView?.setState(OverlayArcReactorView.State.SPEAKING)
                    }
                }

                override fun onSpeechCompleted() {
                    mainHandler.post {
                        statusTextView?.text = "STATUS: STANDING BY"
                        statusTextView?.setTextColor(Color.parseColor("#4EF2BB"))
                        arcReactorView?.setState(OverlayArcReactorView.State.IDLE)
                        // If hands-free is enabled, listen again
                        if (preferences.handsFreeAutoListen && isOverlayVisible) {
                            startOverlayListening()
                        }
                    }
                }

                override fun onSpeechError(error: String) {
                    mainHandler.post {
                        statusTextView?.text = "ERROR: $error"
                        statusTextView?.setTextColor(Color.parseColor("#FF5252"))
                        arcReactorView?.setState(OverlayArcReactorView.State.IDLE)
                    }
                }
            })
        }

        sttEngine = SpeechToTextEngine(this).apply {
            setListener(object : SpeechToTextEngine.Listener {
                override fun onListeningStarted() {
                    mainHandler.post {
                        statusTextView?.text = "STATUS: LISTENING FOR VOICE COMMAND"
                        statusTextView?.setTextColor(Color.parseColor("#00E5FF"))
                        arcReactorView?.setState(OverlayArcReactorView.State.LISTENING)
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    mainHandler.post {
                        arcReactorView?.setAudioRms(rmsdB)
                    }
                }

                override fun onPartialResult(partialText: String) {
                    mainHandler.post {
                        transcriptTextView?.text = "› $partialText"
                    }
                }

                override fun onResult(finalText: String) {
                    mainHandler.post {
                        if (finalText.isNotBlank()) {
                            transcriptTextView?.text = "› $finalText"
                            processUserSpokenCommand(finalText)
                        } else {
                            statusTextView?.text = "STATUS: NO SPEECH DETECTED"
                            statusTextView?.setTextColor(Color.parseColor("#80D8FF"))
                            arcReactorView?.setState(OverlayArcReactorView.State.IDLE)
                        }
                    }
                }

                override fun onError(errorMessage: String) {
                    mainHandler.post {
                        statusTextView?.text = "STATUS: $errorMessage"
                        arcReactorView?.setState(OverlayArcReactorView.State.IDLE)
                    }
                }

                override fun onListeningEnded() {
                    // Handled in onResult
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> removeOverlay()
            else -> {
                val prompt = intent?.getStringExtra(EXTRA_PROMPT)
                createAndShowOverlay(prompt)
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams", "RtlHardcoded")
    private fun createAndShowOverlay(initialPrompt: String?) {
        if (isOverlayVisible) {
            if (initialPrompt != null) {
                processUserSpokenCommand(initialPrompt)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        vibrateShort()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val root = buildOverlayView()
        overlayRootView = root

        try {
            windowManager?.addView(root, params)
            isOverlayVisible = true

            if (initialPrompt != null) {
                transcriptTextView?.text = "› $initialPrompt"
                processUserSpokenCommand(initialPrompt)
            } else {
                startOverlayListening()
            }
        } catch (e: Exception) {
            isOverlayVisible = false
        }
    }

    private fun buildOverlayView(): View {
        val root = FrameLayout(this).apply {
            // 50% opacity translucent futuristic deep blue backdrop
            setBackgroundColor(Color.argb(128, 2, 14, 28))
            isClickable = true
            isFocusable = true
        }

        // Inner Card HUD Frame
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)

            // Neon cyan border with rounded translucent card background (50% capacity blue)
            val bgDrawable = GradientDrawable().apply {
                setColor(Color.argb(190, 4, 20, 38))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(2), Color.parseColor("#00E5FF"))
            }
            background = bgDrawable

            val layoutParams = FrameLayout.LayoutParams(
                dp(340),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            this.layoutParams = layoutParams
        }

        // 1. Header Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val title = TextView(this).apply {
            text = "MR. STARK NEURAL HUD • 50%"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#80D8FF"))
            textSize = 18f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { removeOverlay() }
        }

        header.addView(title)
        header.addView(closeBtn)
        card.addView(header)

        // 2. Animated Arc Reactor Core View
        val arc = OverlayArcReactorView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(110)).apply {
                bottomMargin = dp(8)
            }
        }
        arcReactorView = arc
        card.addView(arc)

        // 3. Status TextView
        val status = TextView(this).apply {
            text = "STATUS: INITIALIZING NEURAL LINK..."
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
        statusTextView = status
        card.addView(status)

        // 4. Live Spoken Transcript box
        val transcriptBox = TextView(this).apply {
            text = "› Speak a command, Sir..."
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            val padInner = dp(10)
            setPadding(padInner, padInner, padInner, padInner)
            background = GradientDrawable().apply {
                setColor(Color.argb(160, 2, 10, 20))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#00838F"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
        transcriptTextView = transcriptBox
        card.addView(transcriptBox)

        // 5. Response output scrollable box
        val responseScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70)
            ).apply {
                bottomMargin = dp(12)
            }
        }
        val responseBox = TextView(this).apply {
            text = "Neural core standing by. Tap 'Read Screen' to inspect the active application or speak to execute actions."
            setTextColor(Color.parseColor("#B2EBF2"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        responseTextView = responseBox
        responseScroll.addView(responseBox)
        card.addView(responseScroll)

        // 6. Action Buttons Grid
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Button: Read Screen
        val readScreenBtn = createHudButton("👁️ READ SCREEN", Color.parseColor("#00E5FF")) {
            processUserSpokenCommand("read screen")
        }

        // Button: Toggle Mic
        val micBtn = createHudButton("🎙️ MIC", Color.parseColor("#4EF2BB")) {
            startOverlayListening()
        }

        // Button: Global Back
        val backBtn = createHudButton("🔙 BACK", Color.parseColor("#80D8FF")) {
            processUserSpokenCommand("go back")
        }

        buttonRow.addView(readScreenBtn)
        buttonRow.addView(micBtn)
        buttonRow.addView(backBtn)
        card.addView(buttonRow)

        root.addView(card)

        // Clicking outside dismisses overlay
        root.setOnClickListener {
            removeOverlay()
        }
        card.setOnClickListener {
            // Prevent dismiss when clicking inside card
        }

        return root
    }

    private fun createHudButton(label: String, colorHex: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.BLACK)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            val btnBg = GradientDrawable().apply {
                setColor(colorHex)
                cornerRadius = dp(8).toFloat()
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun startOverlayListening() {
        ttsEngine?.stop()
        sttEngine?.startListening(preferences.voiceLanguage)
    }

    private fun processUserSpokenCommand(command: String) {
        statusTextView?.text = "STATUS: PROCESSING QUERY..."
        statusTextView?.setTextColor(Color.parseColor("#FFD54F"))
        arcReactorView?.setState(OverlayArcReactorView.State.THINKING)

        serviceScope.launch {
            val history = withContext(Dispatchers.IO) { memoryRepository.getRecentConversations(4) }
            val memories = withContext(Dispatchers.IO) { memoryRepository.getAllMemoriesList() }

            val aiResponse = aiManager.processQuery(command, history, memories)

            var spokenOutput = aiResponse.spokenText
            var actionBadge: String? = null

            aiResponse.actionCommand?.let { cmd ->
                val result = actionEngine.executeAction(cmd)
                actionBadge = result.actionBadge
                if (result.spokenText.isNotBlank()) {
                    spokenOutput = result.spokenText
                }
            }

            withContext(Dispatchers.Main) {
                responseTextView?.text = "› $spokenOutput" + (if (actionBadge != null) "\n[ACTION: $actionBadge]" else "")
                ttsEngine?.speak(spokenOutput)
            }
        }
    }

    private fun removeOverlay() {
        if (isOverlayVisible && overlayRootView != null) {
            try {
                sttEngine?.stopListening()
                ttsEngine?.stop()
                windowManager?.removeView(overlayRootView)
            } catch (_: Exception) {}
            overlayRootView = null
            isOverlayVisible = false
        }
        stopSelf()
    }

    private fun vibrateShort() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
        sttEngine?.stopListening()
        ttsEngine?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Animated High-Tech Holographic Arc Reactor for the Overlay HUD
 */
class OverlayArcReactorView(context: Context) : View(context) {

    enum class State {
        IDLE, LISTENING, THINKING, SPEAKING
    }

    private var currentState = State.IDLE
    private var rotationAngle = 0f
    private var pulseRadius = 0f
    private var audioLevel = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#00E5FF")
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E5FF")
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 0, 229, 255)
    }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    fun setState(state: State) {
        currentState = state
        when (state) {
            State.LISTENING -> {
                ringPaint.color = Color.parseColor("#00E5FF")
                corePaint.color = Color.parseColor("#00E5FF")
                glowPaint.color = Color.argb(120, 0, 229, 255)
            }
            State.THINKING -> {
                ringPaint.color = Color.parseColor("#FFD54F")
                corePaint.color = Color.parseColor("#FFD54F")
                glowPaint.color = Color.argb(120, 255, 213, 79)
            }
            State.SPEAKING -> {
                ringPaint.color = Color.parseColor("#4EF2BB")
                corePaint.color = Color.parseColor("#4EF2BB")
                glowPaint.color = Color.argb(120, 78, 242, 187)
            }
            State.IDLE -> {
                ringPaint.color = Color.parseColor("#00838F")
                corePaint.color = Color.parseColor("#00838F")
                glowPaint.color = Color.argb(50, 0, 131, 143)
            }
        }
        invalidate()
    }

    fun setAudioRms(rmsdB: Float) {
        audioLevel = (rmsdB + 2f).coerceIn(0f, 10f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseR = (width.coerceAtMost(height) / 2f) - 10f

        val dynamicGlow = baseR * 0.55f + (audioLevel * 3f)

        // Outer glow
        canvas.drawCircle(cx, cy, dynamicGlow, glowPaint)

        // Outer ring
        canvas.drawCircle(cx, cy, baseR * 0.85f, ringPaint)

        // Inner core
        canvas.drawCircle(cx, cy, baseR * 0.35f, corePaint)

        // Segments
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        for (i in 0 until 6) {
            val startAngle = i * 60f
            val rect = RectF(cx - baseR * 0.65f, cy - baseR * 0.65f, cx + baseR * 0.65f, cy + baseR * 0.65f)
            canvas.drawArc(rect, startAngle + 10f, 40f, false, ringPaint)
        }
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
