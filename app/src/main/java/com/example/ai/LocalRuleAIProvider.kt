package com.example.ai

import com.example.data.model.ActionCommand
import com.example.data.model.ConversationMessage
import com.example.data.model.MemoryItem
import java.util.Locale

class LocalRuleAIProvider : AIProvider {

    override val providerName: String = "Local Engine (Offline)"

    override suspend fun processQuery(
        userMessage: String,
        history: List<ConversationMessage>,
        memories: List<MemoryItem>
    ): AIResponse {
        val trimmed = userMessage.trim()
        val lower = trimmed.lowercase(Locale.getDefault())

        // 1. Remove activation phrases if present
        val clean = lower
            .replace("mr stark", "")
            .replace("mr. stark", "")
            .replace("mister stark", "")
            .replace("hey stark", "")
            .replace("stark", "")
            .trim()

        if (clean.isEmpty()) {
            return AIResponse(
                spokenText = "Yes Sir, I am online and listening. How may I assist you?",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = "Yes Sir, I am online and listening."
                )
            )
        }

        // 2. Check for compound commands (" and ", " aur ", " then ", " phir ")
        val compoundSplitters = listOf(" and then ", " and ", " aur ", " then ", " phir ")
        for (splitter in compoundSplitters) {
            if (clean.contains(splitter)) {
                val parts = clean.split(splitter).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    val subCommands = mutableListOf<ActionCommand>()
                    for (part in parts) {
                        val subResponse = parseSingleIntent(part)
                        if (subResponse.actionCommand != null) {
                            subCommands.add(subResponse.actionCommand)
                        }
                    }
                    if (subCommands.isNotEmpty()) {
                        return AIResponse(
                            spokenText = "Executing multi-step protocol, Sir.",
                            actionCommand = ActionCommand(
                                type = ActionCommand.TYPE_COMPOUND,
                                subCommands = subCommands,
                                spokenResponse = "Executing multi-step protocol, Sir."
                            )
                        )
                    }
                }
            }
        }

        return parseSingleIntent(clean)
    }

    private fun parseSingleIntent(clean: String): AIResponse {
        // Screen Reading (Hindi, English, Hinglish)
        if (clean.contains("read screen") || clean.contains("screen padho") ||
            clean.contains("screen par kya likha hai") || clean.contains("kya likha hai") ||
            clean.contains("screen read karo") || clean.contains("read display") ||
            clean.contains("screen scan") || clean.contains("kya chal raha hai")
        ) {
            return AIResponse(
                spokenText = "Scanning active screen content, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_READ_SCREEN,
                    spokenResponse = "Scanning active screen content, Sir."
                )
            )
        }

        // Show Holographic HUD Overlay
        if (clean.contains("open hud") || clean.contains("show overlay") ||
            clean.contains("blue screen kholo") || clean.contains("hud chalao") ||
            clean.contains("overlay screen") || clean.contains("show hud")
        ) {
            return AIResponse(
                spokenText = "Deploying holographic 50% capacity neural overlay, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_SHOW_OVERLAY,
                    spokenResponse = "Deploying holographic overlay, Sir."
                )
            )
        }

        // Flashlight / Torch
        if (clean.contains("torch on") || clean.contains("flashlight on") ||
            clean.contains("torch jalao") || clean.contains("light on karo") ||
            clean.contains("flash on")
        ) {
            return AIResponse(
                spokenText = "Activating torch, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_FLASHLIGHT,
                    target = "on",
                    spokenResponse = "Activating torch, Sir."
                )
            )
        }
        if (clean.contains("torch off") || clean.contains("flashlight off") ||
            clean.contains("torch band karo") || clean.contains("light band karo") ||
            clean.contains("flash off")
        ) {
            return AIResponse(
                spokenText = "Deactivating torch, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_FLASHLIGHT,
                    target = "off",
                    spokenResponse = "Deactivating torch, Sir."
                )
            )
        }

        // Screen Clicking ("click [button]", "tap [text]", "[button] par click karo")
        val clickPrefixes = listOf("click on ", "click ", "tap on ", "tap ", "press ")
        for (prefix in clickPrefixes) {
            if (clean.startsWith(prefix)) {
                val target = clean.removePrefix(prefix).trim()
                if (target.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Tapping $target on screen, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_CLICK_ELEMENT,
                            target = target,
                            spokenResponse = "Tapping $target on screen, Sir."
                        )
                    )
                }
            }
        }
        if (clean.endsWith(" par click karo") || clean.endsWith(" pe click karo") || clean.endsWith(" dabaao")) {
            val target = clean
                .removeSuffix(" par click karo")
                .removeSuffix(" pe click karo")
                .removeSuffix(" dabaao")
                .trim()
            if (target.isNotEmpty()) {
                return AIResponse(
                    spokenText = "Clicking $target, Sir.",
                    actionCommand = ActionCommand(
                        type = ActionCommand.TYPE_CLICK_ELEMENT,
                        target = target,
                        spokenResponse = "Clicking $target, Sir."
                    )
                )
            }
        }

        // Screen Writing ("type [text]", "write [text]", "[text] likho")
        val writePrefixes = listOf("type ", "write ", "enter text ", "likho ")
        for (prefix in writePrefixes) {
            if (clean.startsWith(prefix)) {
                val text = clean.removePrefix(prefix).trim()
                if (text.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Entering text into active field, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_WRITE_TEXT,
                            target = text,
                            spokenResponse = "Entering text into active field, Sir."
                        )
                    )
                }
            }
        }

        // Global Navigation (Back, Home, Recents, Notifications)
        if (clean == "go back" || clean == "back" || clean == "back jao" || clean == "back karo" || clean == "piche jao") {
            return AIResponse(
                spokenText = "Navigating back, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_GLOBAL_ACTION,
                    target = "back",
                    spokenResponse = "Navigating back, Sir."
                )
            )
        }
        if (clean == "go home" || clean == "home" || clean == "home screen" || clean == "home jao" || clean == "home karo") {
            return AIResponse(
                spokenText = "Returning to Home screen, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_GLOBAL_ACTION,
                    target = "home",
                    spokenResponse = "Returning to Home screen, Sir."
                )
            )
        }
        if (clean.contains("notification") || clean.contains("notifications kholo")) {
            return AIResponse(
                spokenText = "Opening notifications panel, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_GLOBAL_ACTION,
                    target = "notifications",
                    spokenResponse = "Opening notifications panel, Sir."
                )
            )
        }
        if (clean.contains("recent apps") || clean.contains("recents") || clean.contains("app switcher")) {
            return AIResponse(
                spokenText = "Opening recent applications, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_GLOBAL_ACTION,
                    target = "recents",
                    spokenResponse = "Opening recent applications, Sir."
                )
            )
        }

        // Device Telemetry & Battery
        if (clean.contains("battery") || clean.contains("charging") || clean.contains("telemetry") ||
            clean.contains("device status") || clean.contains("system status") ||
            clean.contains("kitni charging") || clean.contains("battery kitni")
        ) {
            return AIResponse(
                spokenText = "Checking device telemetry, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_DEVICE_INFO,
                    spokenResponse = ""
                )
            )
        }

        // Time / Date
        if (clean.contains("what time") || clean.contains("time kya") || clean.contains("current time") ||
            clean.contains("aaj ki date") || clean.contains("what is today's date") || clean.contains("kitne baje")
        ) {
            return AIResponse(
                spokenText = "Reading telemetry time, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_DEVICE_INFO,
                    spokenResponse = ""
                )
            )
        }

        // App Launching (English + Hindi + Hinglish)
        val openPrefixes = listOf("open ", "launch ", "start ")
        val openSuffixes = listOf(" kholo", " chalao", " open karo", " start karo", " kholein")

        for (prefix in openPrefixes) {
            if (clean.startsWith(prefix)) {
                val app = clean.removePrefix(prefix).trim()
                if (app.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Opening $app, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_OPEN_APP,
                            target = app,
                            spokenResponse = "Opening $app, Sir."
                        )
                    )
                }
            }
        }

        for (suffix in openSuffixes) {
            if (clean.endsWith(suffix)) {
                val app = clean.removeSuffix(suffix).trim()
                if (app.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Certainly, opening $app.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_OPEN_APP,
                            target = app,
                            spokenResponse = "Opening $app, Sir."
                        )
                    )
                }
            }
        }

        // Timers
        if (clean.contains("timer")) {
            val seconds = extractSeconds(clean)
            return AIResponse(
                spokenText = "Setting a timer for $seconds seconds, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_SET_TIMER,
                    parameters = mapOf("seconds" to seconds.toString(), "label" to "Stark Timer"),
                    spokenResponse = "Setting a timer for $seconds seconds, Sir."
                )
            )
        }

        // Alarms
        if (clean.contains("alarm")) {
            val hour = extractHour(clean)
            val min = extractMinute(clean)
            return AIResponse(
                spokenText = "Setting alarm for $hour:${if (min < 10) "0$min" else "$min"}, Sir.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_SET_ALARM,
                    parameters = mapOf("hour" to hour.toString(), "minutes" to min.toString(), "label" to "Stark Alarm"),
                    spokenResponse = "Setting alarm for $hour:$min, Sir."
                )
            )
        }

        // Memory Save
        val rememberPrefixes = listOf("remember that ", "remember ", "yaad rakhna ki ", "yaad rakho ")
        for (prefix in rememberPrefixes) {
            if (clean.startsWith(prefix)) {
                val fact = clean.removePrefix(prefix).trim()
                if (fact.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Stored securely in my memory core, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_REMEMBER,
                            target = fact,
                            parameters = mapOf("category" to "user_noted"),
                            spokenResponse = "Stored securely in memory, Sir."
                        )
                    )
                }
            }
        }

        // Web Search
        val searchPrefixes = listOf("search for ", "search ", "google ", "find ", "dhoondo ")
        for (prefix in searchPrefixes) {
            if (clean.startsWith(prefix)) {
                val query = clean.removePrefix(prefix).trim()
                if (query.isNotEmpty()) {
                    return AIResponse(
                        spokenText = "Initiating search protocol for $query, Sir.",
                        actionCommand = ActionCommand(
                            type = ActionCommand.TYPE_WEB_SEARCH,
                            query = query,
                            spokenResponse = "Searching the web for $query, Sir."
                        )
                    )
                }
            }
        }

        // Greetings
        if (clean == "hello" || clean == "hi" || clean == "hey" || clean == "kaise ho" || clean == "who are you") {
            return AIResponse(
                spokenText = "Hello Sir. All Stark protocols are fully operational. I am standing by to control your device or answer your queries.",
                actionCommand = ActionCommand(
                    type = ActionCommand.TYPE_NORMAL_RESPONSE,
                    spokenResponse = "Hello Sir. All Stark protocols are fully operational."
                )
            )
        }

        // Default query search fallback
        return AIResponse(
            spokenText = "Processing request, Sir.",
            actionCommand = ActionCommand(
                type = ActionCommand.TYPE_WEB_SEARCH,
                query = clean,
                spokenResponse = "Searching for $clean, Sir."
            )
        )
    }

    private fun extractSeconds(text: String): Int {
        val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        val num = numbers.firstOrNull() ?: 60
        return when {
            text.contains("minute") || text.contains("min") -> num * 60
            text.contains("hour") || text.contains("ghante") -> num * 3600
            else -> num
        }
    }

    private fun extractHour(text: String): Int {
        val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        val h = numbers.firstOrNull() ?: 7
        return if (text.contains("pm") && h < 12) h + 12 else h
    }

    private fun extractMinute(text: String): Int {
        val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return if (numbers.size > 1) numbers[1] else 0
    }
}
