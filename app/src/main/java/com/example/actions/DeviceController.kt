package com.example.actions

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.accessibilityservice.AccessibilityService
import com.example.accessibility.StarkAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceController(private val context: Context) {

    fun setTimer(seconds: Int, label: String = "Timer"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                "Timer set for $seconds seconds, Sir."
            } else {
                "Sir, your clock application does not support setting timers via intent."
            }
        } catch (e: Exception) {
            "Unable to set timer: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    fun setAlarm(hour: Int, minutes: Int, message: String = "Alarm"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                String.format(Locale.getDefault(), "Alarm set for %02d:%02d, Sir.", hour, minutes)
            } else {
                "Sir, no compatible alarm application was found."
            }
        } catch (e: Exception) {
            "Unable to set alarm: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    fun performWebSearch(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching the web for \"$query\", Sir."
        } catch (e: Exception) {
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
            "Searching online for \"$query\"."
        }
    }

    /**
     * Explicit search directly on YouTube app or YouTube mobile web
     */
    fun performYouTubeSearch(query: String): String {
        val cleanQuery = query.trim().ifEmpty { "trending movies hindi" }
        val targetQuery = if (cleanQuery.equals("random", ignoreCase = true) || 
                              cleanQuery.equals("movie", ignoreCase = true) || 
                              cleanQuery.equals("random movie", ignoreCase = true)) {
            "popular full movies hindi"
        } else cleanQuery

        val ytUrl = "https://www.youtube.com/results?search_query=${Uri.encode(targetQuery)}"

        // 1. First attempt: Direct YouTube app launch
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
            return "Opening YouTube for \"$targetQuery\", Sir."
        } catch (_: Exception) {
            // YouTube app package not present or activity blocked
        }

        // 2. Second attempt: Broadcast generic VIEW intent (browser or YouTube client)
        return try {
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
            "Searching YouTube for \"$targetQuery\", Sir."
        } catch (e: Exception) {
            openUrl(ytUrl)
            "Opening YouTube search for \"$targetQuery\" in browser, Sir."
        }
    }

    fun openUrl(url: String): String {
        return try {
            val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening $finalUrl."
        } catch (e: Exception) {
            "Unable to open link: ${e.localizedMessage}"
        }
    }

    fun getDeviceInfo(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular data"
            else -> "Offline"
        }

        val timeStr = SimpleDateFormat("h:mm a, EEEE, MMM d", Locale.getDefault()).format(Date())
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE}"

        val chargingText = if (isCharging) " (Charging)" else ""
        return "Telemetry Report, Sir: Battery is at $batteryPct%$chargingText. Network connection: $networkType. Time: $timeStr. Running on $deviceModel with $osVersion."
    }

    fun toggleFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                if (turnOn) "Flashlight activated, Sir." else "Flashlight turned off, Sir."
            } else {
                "Sir, no camera flash hardware was detected."
            }
        } catch (e: Exception) {
            "Unable to toggle flashlight: ${e.localizedMessage}"
        }
    }

    fun readCurrentScreen(): String {
        if (!StarkAccessibilityService.isServiceConnected()) {
            return "Sir, accessibility permissions are required to scan screen content. Please enable Mr. Stark in Settings."
        }
        val report = StarkAccessibilityService.readScreenContent()
        if (!report.success) {
            return report.fullText
        }
        val cleanSummary = report.fullText
            .lines()
            .filter { it.isNotBlank() }
            .take(6)
            .joinToString(". ")

        val interactive = if (report.interactiveSummary.isNotBlank()) {
            " Available interactive controls: ${report.interactiveSummary}."
        } else ""

        return "Screen scan complete for ${report.packageName}. Content reads: $cleanSummary.$interactive"
    }

    fun clickOnScreen(targetText: String): String {
        if (!StarkAccessibilityService.isServiceConnected()) {
            return "Accessibility permission required to interact with screen buttons."
        }
        val clicked = StarkAccessibilityService.clickElementWithText(targetText)
        return if (clicked) {
            "Tapped on \"$targetText\", Sir."
        } else {
            "Could not locate a clickable element matching \"$targetText\" on current screen."
        }
    }

    fun writeTextToScreen(textToType: String): String {
        if (!StarkAccessibilityService.isServiceConnected()) {
            return "Accessibility permission required to write text."
        }
        val typed = StarkAccessibilityService.writeTextToInput(textToType)
        return if (typed) {
            "Entered text: \"$textToType\", Sir."
        } else {
            "No active text input field found on screen to write text."
        }
    }

    /**
     * Search directly inside current foreground app using accessibility automation
     */
    fun searchInsideCurrentApp(query: String): String {
        val currentPkg = StarkAccessibilityService.getInstance()?.currentPackage ?: ""
        if (currentPkg.contains("youtube")) {
            return performYouTubeSearch(query)
        }

        if (StarkAccessibilityService.isServiceConnected()) {
            val searched = StarkAccessibilityService.searchInActiveScreen(query)
            if (searched) {
                return "Searching for \"$query\" on screen, Sir."
            }
        }

        // Fallback to general web search
        return performWebSearch(query)
    }

    fun performGlobalScreenAction(actionType: String): String {
        if (!StarkAccessibilityService.isServiceConnected()) {
            return "Accessibility permission required for system global controls."
        }
        val actionId = when (actionType.lowercase()) {
            "back" -> AccessibilityService.GLOBAL_ACTION_BACK
            "home" -> AccessibilityService.GLOBAL_ACTION_HOME
            "recents", "recent_apps" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            "lock" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN else AccessibilityService.GLOBAL_ACTION_BACK
            else -> AccessibilityService.GLOBAL_ACTION_BACK
        }
        val success = StarkAccessibilityService.performGlobal(actionId)
        return if (success) {
            "Action $actionType executed successfully, Sir."
        } else {
            "Failed to execute global action $actionType."
        }
    }

    fun openSettings(target: String? = null): String {
        return try {
            val intent = when (target?.lowercase()) {
                "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "display", "screen" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening ${target ?: "device"} settings, Sir."
        } catch (e: Exception) {
            "Unable to open settings: ${e.localizedMessage}"
        }
    }

    fun initiatePhoneCallUi(phoneNumberOrName: String): String {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumberOrName.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening phone dialer for $phoneNumberOrName, Sir."
        } catch (e: Exception) {
            "Unable to open dialer: ${e.localizedMessage}"
        }
    }

    fun prepareSmsUi(recipient: String, messageText: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${recipient.trim()}")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Prepared message for $recipient, Sir."
        } catch (e: Exception) {
            "Unable to prepare message: ${e.localizedMessage}"
        }
    }

    fun copyToClipboard(text: String): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mr. Stark", text)
            clipboard.setPrimaryClip(clip)
            "Copied to clipboard, Sir."
        } catch (e: Exception) {
            "Unable to copy text: ${e.localizedMessage}"
        }
    }

    fun shareText(text: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share via Mr. Stark").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            "Opening share options, Sir."
        } catch (e: Exception) {
            "Unable to share: ${e.localizedMessage}"
        }
    }
}
