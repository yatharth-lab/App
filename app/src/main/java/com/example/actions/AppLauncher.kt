package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

class AppLauncher(private val context: Context) {

    data class AppInfo(
        val label: String,
        val packageName: String
    )

    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )

        return resolveInfos.mapNotNull { info ->
            val label = info.loadLabel(packageManager)?.toString()?.trim()
            val pkg = info.activityInfo?.packageName
            if (!label.isNullOrEmpty() && !pkg.isNullOrEmpty()) {
                AppInfo(label, pkg)
            } else null
        }
    }

    fun launchAppByName(name: String): AppLaunchResult {
        val raw = name.trim().lowercase()
        val normalizedTarget = when (raw) {
            "yt", "u tube", "you tube" -> "youtube"
            "insta", "ig" -> "instagram"
            "fb" -> "facebook"
            "whats app", "wp" -> "whatsapp"
            else -> raw
        }
        val installed = getInstalledApps()

        // 1. Exact match
        var match = installed.find { it.label.lowercase() == normalizedTarget }

        // 2. Starts with / contains match
        if (match == null) {
            match = installed.find {
                it.label.lowercase().contains(normalizedTarget) ||
                        normalizedTarget.contains(it.label.lowercase())
            }
        }

        // 3. Common aliases fallback
        if (match == null) {
            val aliasPkg = resolveCommonAlias(normalizedTarget)
            if (aliasPkg != null) {
                match = installed.find { it.packageName.lowercase().contains(aliasPkg) }
            }
        }

        if (match == null) {
            return AppLaunchResult.NotFound("Sir, I couldn't find \"$name\" installed on this device.")
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(match.packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                AppLaunchResult.Success(match.label)
            } catch (e: Exception) {
                AppLaunchResult.Error("Failed to launch ${match.label}: ${e.message}")
            }
        } else {
            AppLaunchResult.NotFound("Sir, \"${match.label}\" is installed but does not have a standard launch activity.")
        }
    }

    private fun resolveCommonAlias(target: String): String? {
        return when {
            target.contains("youtube") || target == "yt" -> "com.google.android.youtube"
            target.contains("chrome") -> "com.android.chrome"
            target.contains("map") -> "maps"
            target.contains("calc") -> "calculator"
            target.contains("camera") -> "camera"
            target.contains("clock") || target.contains("alarm") -> "deskclock"
            target.contains("setting") -> "settings"
            target.contains("message") || target.contains("sms") -> "messaging"
            target.contains("whatsapp") || target == "wp" -> "whatsapp"
            target.contains("instagram") || target == "insta" -> "instagram"
            target.contains("spotify") -> "spotify"
            target.contains("gmail") || target.contains("mail") -> "gm"
            target.contains("browser") -> "chrome"
            else -> null
        }
    }
}

sealed class AppLaunchResult {
    data class Success(val appName: String) : AppLaunchResult()
    data class NotFound(val message: String) : AppLaunchResult()
    data class Error(val message: String) : AppLaunchResult()
}
